package com.example.tortugaturbo;

public final class Tarifador {

    private static final double RADIO_TIERRA_KM = 6371.0;
    private static final double COEFICIENTE_RUTAS = 1.25;

    private Tarifador() {
    }

    static int distanciaKm(Ciudad origen, Ciudad destino) {
        if (origen == destino) {
            throw new IllegalArgumentException();
        }
        double f1 = Math.toRadians(origen.getLat());
        double f2 = Math.toRadians(destino.getLat());
        double df = f2 - f1;
        double dl = Math.toRadians(destino.getLng() - origen.getLng());
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        double km = 2 * RADIO_TIERRA_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(km * COEFICIENTE_RUTAS);
    }

    static double precio(double pesoKg, int distanciaKm, Servicio s) {
        double base = 1500 + 400 * pesoKg + 3 * distanciaKm;
        return redondear10(base * (s == Servicio.EXPRESS ? 1.5 : 1.0));
    }

    static int diasEstimados(int distanciaKm, Servicio s) {
        if (s == Servicio.EXPRESS) {
            return Math.max(1, (int) Math.ceil(distanciaKm / 800.0));
        }
        return (int) Math.ceil(distanciaKm / 400.0) + 1;
    }

    static Double parsearPeso(String texto) {
        if (texto == null || !texto.trim().matches("\\d+([.,]\\d+)?")) {
            return null;
        }
        try {
            double v = Double.parseDouble(texto.trim().replace(',', '.'));
            if (Double.isNaN(v) || v < 0.1 || v > 30) {
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double redondear10(double x) {
        return Math.round(x / 10.0) * 10;
    }
}
