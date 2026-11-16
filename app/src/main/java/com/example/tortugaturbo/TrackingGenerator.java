package com.example.tortugaturbo;

import java.util.Locale;
import java.util.Random;

public final class TrackingGenerator {

    private TrackingGenerator() {
    }

    static String generar(Random r) {
        StringBuilder cuerpo = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            cuerpo.append(r.nextInt(10));
        }
        return "TT-" + cuerpo + "-" + digitoControl(cuerpo.toString());
    }

    static boolean esValido(String tracking) {
        if (tracking == null || !tracking.matches("^TT-\\d{9}-\\d$")) {
            return false;
        }
        String cuerpo = tracking.substring(3, 12);
        return digitoControl(cuerpo) == tracking.charAt(13) - '0';
    }

    static String normalizar(String input) {
        return input == null ? null : input.trim().toUpperCase(Locale.ROOT);
    }

    private static int digitoControl(String cuerpo) {
        int suma = 0;
        boolean duplicar = true;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            int d = cuerpo.charAt(i) - '0';
            if (duplicar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            suma += d;
            duplicar = !duplicar;
        }
        return (10 - suma % 10) % 10;
    }
}
