package com.ratio.ratapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CameraCapture {

    public static Map<String, Object> snap(Context ctx, boolean front) {
        Map<String, Object> out = new HashMap<>();
        if (ctx.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            out.put("error", "no camera permission");
            return out;
        }

        CameraManager mgr = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try {
            String camId = pick(mgr, front);
            if (camId == null) {
                out.put("error", "no " + (front ? "front" : "back") + " camera found");
                return out;
            }

            CameraCharacteristics cc = mgr.getCameraCharacteristics(camId);
            Size[] sizes = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
            if (sizes == null || sizes.length == 0) {
                out.put("error", "no jpeg stream available");
                return out;
            }

            // pick the mid-size — big enough for detail, small enough for RTDB
            Size best = sizes[0];
            for (Size s : sizes) {
                long a = (long) s.getWidth() * s.getHeight();
                long b = (long) best.getWidth() * best.getHeight();
                if (a > b) best = s;
            }

            final CountDownLatch opening = new CountDownLatch(1);
            final CountDownLatch captured = new CountDownLatch(1);
            final ImageReader reader = ImageReader.newInstance(
                best.getWidth(), best.getHeight(), ImageFormat.JPEG, 1);
            final Image[] first = new Image[1];

            final HandlerThread ht = new HandlerThread("cam");
            ht.start();
            Handler h = new Handler(ht.getLooper());

            @SuppressLint("MissingPermission")
            CameraDevice.StateCallback open = new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice cam) {
                    opening.countDown();
                    try {
                        cam.createCaptureSession(
                            java.util.Collections.singletonList(reader.getSurface()),
                            new CameraCaptureSession.StateCallback() {
                                @Override public void onConfigured(CameraCaptureSession ses) {
                                    try {
                                        CaptureRequest.Builder rb =
                                            cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                        rb.addTarget(reader.getSurface());
                                        rb.set(CaptureRequest.CONTROL_MODE,
                                            CaptureRequest.CONTROL_MODE_AUTO);
                                        ses.capture(rb.build(), null, h);
                                    } catch (Exception e) {
                                        captured.countDown();
                                    }
                                }
                                @Override public void onConfigureFailed(CameraCaptureSession s) {
                                    captured.countDown();
                                }
                            }, h);
                    } catch (Exception e) {
                        captured.countDown();
                    }
                }
                @Override public void onError(CameraDevice cam, int err) {
                    opening.countDown();
                    captured.countDown();
                    cam.close();
                }
                @Override public void onDisconnected(CameraDevice cam) {
                    opening.countDown();
                    captured.countDown();
                    cam.close();
                }
            };

            mgr.openCamera(camId, open, h);

            reader.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if (img != null) {
                    if (first[0] == null) first[0] = img;
                    captured.countDown();
                }
            }, h);

            // gate the waits: no unbounded hangs
            if (!opening.await(4, TimeUnit.SECONDS)
                || !captured.await(6, TimeUnit.SECONDS)) {
                out.put("error", "camera timeout");
                ht.quitSafely();
                reader.close();
                return out;
            }

            byte[] jpg = null;
            try {
                Image img = first[0];
                if (img != null) {
                    ByteBuffer buf = img.getPlanes()[0].getBuffer();
                    jpg = new byte[buf.remaining()];
                    buf.get(jpg);
                }
            } finally {
                if (first[0] != null) first[0].close();
                reader.close();
            }
            if (jpg == null) {
                out.put("error", "no image captured");
                return out;
            }

            // scale down before shoving into RTDB so a 3MB pic doesn't blow past 1MB write cap
            ByteArrayOutputStream trimmed = downscale(jpg, 400);
            out.put("body", Base64.encodeToString(trimmed.toByteArray(), Base64.NO_WRAP));
            out.put("size", trimmed.size());
            out.put("facing", front ? "front" : "back");

            ht.quitSafely();
        } catch (Exception e) {
            out.put("error", "camera exception: " + e.getMessage());
        }
        return out;
    }

    private static String pick(CameraManager mgr, boolean front) {
        try {
            for (String id : mgr.getCameraIdList()) {
                CameraCharacteristics cc = mgr.getCameraCharacteristics(id);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (front
                    ? facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT
                    : facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // simple pixel shrink to keep the base64 body under the RTDB cap
    private static ByteArrayOutputStream downscale(byte[] src, int target) {
        try {
            android.graphics.Bitmap bmp =
                android.graphics.BitmapFactory.decodeByteArray(src, 0, src.length);
            int w = bmp.getWidth(), h = bmp.getHeight();
            float k = Math.min(1f, (float) Math.sqrt(
                (float) (target * target * 4) / (w * h)));
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(
                bmp, Math.max(1, (int) (w * k)), Math.max(1, (int) (h * k)), true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, bos);
            return bos;
        } catch (Exception e) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                int best = Math.min(src.length, 900 * 1024);
                bos.write(src, 0, best);
            } catch (Exception ignored) {}
            return bos;
        }
    }
}
