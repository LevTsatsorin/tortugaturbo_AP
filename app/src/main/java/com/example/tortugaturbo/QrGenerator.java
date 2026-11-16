package com.example.tortugaturbo;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

final class QrGenerator {

    private QrGenerator() {
    }

    static Bitmap generar(String contenido, int tamanoPx) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx);
            int[] pixeles = new int[tamanoPx * tamanoPx];
            for (int y = 0; y < tamanoPx; y++) {
                for (int x = 0; x < tamanoPx; x++) {
                    pixeles[y * tamanoPx + x] = matriz.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixeles, 0, tamanoPx, 0, 0, tamanoPx, tamanoPx);
            return bitmap;
        } catch (WriterException e) {
            throw new IllegalStateException(e);
        }
    }
}
