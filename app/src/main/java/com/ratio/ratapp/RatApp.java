package com.ratio.ratapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class RatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
