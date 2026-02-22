package com.whoswho.app.ui.person;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class providing static helpers for camera and gallery photo capture,
 * and for decoding/scaling bitmaps from URIs in a memory-efficient way.
 */
public final class PhotoHelper {

    /** URI that the camera will write its full-resolution image to. */
    private static Uri sCameraOutputUri;

    private PhotoHelper() {}

    // -------------------------------------------------------------------------
    // Intent factories
    // -------------------------------------------------------------------------

    /**
     * Creates a camera capture intent that writes the full-resolution image to a
     * temporary file in the app's external files directory.
     *
     * @param ctx Application or Activity context.
     * @return A ready-to-use {@link Intent} for {@code startActivityForResult}, or
     *         {@code null} if no camera app is available.
     */
    public static Intent createCameraIntent(Context ctx) {
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return null;
        }

        // Create a unique temp file so the camera has a destination
        File photoFile = createTempImageFile(ctx);
        if (photoFile == null) {
            return null;
        }

        Uri outputUri = Uri.fromFile(photoFile);
        sCameraOutputUri = outputUri;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        return intent;
    }

    /**
     * Creates a gallery pick intent for selecting an image.
     *
     * @return A ready-to-use {@link Intent} for {@code startActivityForResult}.
     */
    public static Intent createGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return intent;
    }

    /**
     * Returns the URI that was passed to the last camera intent via
     * {@link MediaStore#EXTRA_OUTPUT}. May be {@code null} if
     * {@link #createCameraIntent(Context)} was never called.
     */
    public static Uri getCameraOutputUri() {
        return sCameraOutputUri;
    }

    // -------------------------------------------------------------------------
    // Bitmap processing
    // -------------------------------------------------------------------------

    /**
     * Reads a bitmap from {@code uri} via the content resolver, scales it so that
     * neither dimension exceeds {@code maxSize} pixels, and returns the result.
     *
     * <p>Uses {@link BitmapFactory.Options#inSampleSize} for the initial power-of-two
     * down-sample, then performs a precise scale if the result is still larger than
     * {@code maxSize}.
     *
     * @param ctx     Context used to open the content resolver stream.
     * @param uri     URI of the source image (content:// or file://).
     * @param maxSize Maximum pixel dimension for width and height.
     * @return A scaled {@link Bitmap}, or {@code null} on any error.
     */
    public static Bitmap processPhoto(Context ctx, Uri uri, int maxSize) {
        if (ctx == null || uri == null) {
            return null;
        }

        // Step 1: decode only the bounds to determine inSampleSize
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        InputStream boundsStream = openStream(ctx, uri);
        if (boundsStream == null) {
            return null;
        }
        try {
            BitmapFactory.decodeStream(boundsStream, null, opts);
        } finally {
            closeQuietly(boundsStream);
        }

        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
            return null;
        }

        // Step 2: calculate power-of-two sample size
        int inSampleSize = 1;
        int w = opts.outWidth;
        int h = opts.outHeight;
        while (w / 2 >= maxSize || h / 2 >= maxSize) {
            inSampleSize *= 2;
            w /= 2;
            h /= 2;
        }

        // Step 3: decode the actual bitmap at reduced size
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = inSampleSize;
        InputStream decodeStream = openStream(ctx, uri);
        if (decodeStream == null) {
            return null;
        }
        Bitmap bmp;
        try {
            bmp = BitmapFactory.decodeStream(decodeStream, null, opts);
        } finally {
            closeQuietly(decodeStream);
        }

        if (bmp == null) {
            return null;
        }

        // Step 4: precise scale if still over the limit
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();
        if (bw > maxSize || bh > maxSize) {
            float scale = Math.min((float) maxSize / bw, (float) maxSize / bh);
            int newW = Math.round(bw * scale);
            int newH = Math.round(bh * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true);
            if (scaled != bmp) {
                bmp.recycle();
                bmp = scaled;
            }
        }

        return bmp;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a uniquely named temporary JPEG file inside the app's external
     * pictures directory (or internal files dir as fallback).
     */
    private static File createTempImageFile(Context ctx) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        String fileName = "PHOTO_" + timeStamp;

        File storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = new File(ctx.getFilesDir(), "photos");
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        try {
            return File.createTempFile(fileName, ".jpg", storageDir);
        } catch (IOException e) {
            return null;
        }
    }

    /** Opens a content/file URI as an InputStream; returns null on failure. */
    private static InputStream openStream(Context ctx, Uri uri) {
        try {
            return ctx.getContentResolver().openInputStream(uri);
        } catch (Exception e) {
            return null;
        }
    }

    /** Closes a stream without throwing. */
    private static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }
}
