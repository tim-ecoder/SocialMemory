package com.whoswho.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    private static final LruCache<String, Bitmap> sCache;

    static {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        sCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    /**
     * Loads and scales down a bitmap from the given file path so that neither
     * dimension exceeds maxSize.  Results are cached by path.
     */
    public static Bitmap loadScaled(String path, int maxSize) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String cacheKey = path + "@" + maxSize;
        Bitmap cached = sCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // First decode just the bounds
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);

        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
            return null;
        }

        // Calculate inSampleSize
        int inSampleSize = 1;
        int width = opts.outWidth;
        int height = opts.outHeight;
        while (width / 2 >= maxSize || height / 2 >= maxSize) {
            inSampleSize *= 2;
            width /= 2;
            height /= 2;
        }

        opts.inJustDecodeBounds = false;
        opts.inSampleSize = inSampleSize;
        Bitmap bmp = BitmapFactory.decodeFile(path, opts);
        if (bmp == null) {
            return null;
        }

        // If still larger than maxSize, scale precisely
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        if (w > maxSize || h > maxSize) {
            float scale = Math.min((float) maxSize / w, (float) maxSize / h);
            int newW = Math.round(w * scale);
            int newH = Math.round(h * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true);
            if (scaled != bmp) {
                bmp.recycle();
                bmp = scaled;
            }
        }

        sCache.put(cacheKey, bmp);
        return bmp;
    }

    /**
     * Returns a new circular bitmap clipped from src (square crop from center).
     */
    public static Bitmap getCircularBitmap(Bitmap src) {
        if (src == null) {
            return null;
        }

        int size = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Rect srcRect = new Rect(x, y, x + size, y + size);
        Rect dstRect = new Rect(0, 0, size, size);
        RectF dstRectF = new RectF(dstRect);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(dstRectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, srcRect, dstRect, paint);

        return output;
    }

    /**
     * Saves a bitmap to the app's internal storage under photos/<filename>.
     * Returns the absolute path of the saved file, or null on failure.
     */
    public static String saveBitmap(Context ctx, Bitmap bmp, String filename) {
        if (ctx == null || bmp == null || filename == null || filename.isEmpty()) {
            return null;
        }

        File dir = new File(ctx.getFilesDir(), "photos");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        File file = new File(dir, filename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Removes a cached entry for the given path/maxSize combination.
     */
    public static void evictCache(String path, int maxSize) {
        sCache.remove(path + "@" + maxSize);
    }

    /**
     * Clears the entire in-memory cache.
     */
    public static void clearCache() {
        sCache.evictAll();
    }
}
