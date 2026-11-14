package com.example.tortugaturbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Avatar {
    public static final int LADO = 256;
    public static final int MAX_CHARS = 150_000;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(24);

    private Avatar() {
    }

    public static String codificar(Bitmap original) {
        int w = original.getWidth();
        int h = original.getHeight();
        int lado = Math.min(w, h);
        Bitmap cuadrado = Bitmap.createBitmap(original, (w - lado) / 2, (h - lado) / 2, lado, lado);
        Bitmap chico = Bitmap.createScaledBitmap(cuadrado, LADO, LADO, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        chico.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decodificarUri(Context c, Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.getContentResolver(), uri), (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int corto = Math.min(info.getSize().getWidth(), info.getSize().getHeight());
                int escala = Math.max(1, corto / LADO);
                decoder.setTargetSampleSize(Integer.highestOneBit(escala));
            });
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int corto = Math.min(o.outWidth, o.outHeight);
        int muestra = 1;
        while (corto / (muestra * 2) >= LADO) {
            muestra *= 2;
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = muestra;
        Bitmap b;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            b = BitmapFactory.decodeStream(in, null, o);
        }
        if (b == null) {
            throw new IOException();
        }
        return b;
    }

    public static void mostrar(ImageView foto, TextView iniciales, Usuario u) {
        String avatar = u.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            foto.setTag(null);
            foto.setVisibility(View.GONE);
            iniciales.setText(Usuario.iniciales(u));
            iniciales.setVisibility(View.VISIBLE);
            return;
        }
        String clave = u.getUid() + avatar.hashCode();
        foto.setTag(clave);
        iniciales.setText(Usuario.iniciales(u));
        Bitmap cacheado = CACHE.get(clave);
        if (cacheado != null) {
            ponerFoto(foto, iniciales, cacheado);
            return;
        }
        foto.setVisibility(View.GONE);
        iniciales.setVisibility(View.VISIBLE);
        AppExecutors.background().execute(() -> {
            byte[] bytes;
            try {
                bytes = Base64.decode(avatar, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                return;
            }
            Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (b == null) {
                return;
            }
            CACHE.put(clave, b);
            AppExecutors.main().post(() -> {
                if (clave.equals(foto.getTag())) {
                    ponerFoto(foto, iniciales, b);
                }
            });
        });
    }

    private static void ponerFoto(ImageView foto, TextView iniciales, Bitmap b) {
        foto.setImageBitmap(b);
        foto.setVisibility(View.VISIBLE);
        iniciales.setVisibility(View.GONE);
    }
}
