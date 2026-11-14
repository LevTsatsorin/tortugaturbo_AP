package com.example.tortugaturbo;

import java.util.Random;

final class SimuladorRuta {

    static final class Paso {
        final EstadoEnvio estado;
        final MotivoIncidente motivo;

        Paso(EstadoEnvio estado, MotivoIncidente motivo) {
            this.estado = estado;
            this.motivo = motivo;
        }
    }

    private static final double PROBABILIDAD_INCIDENTE = 0.30;

    private SimuladorRuta() {
    }

    static Paso siguiente(EstadoEnvio actual, boolean intentoFallido, Random random) {
        if (actual.esFinal()) {
            return null;
        }
        switch (actual) {
            case CREADO:
                return new Paso(EstadoEnvio.RETIRADO, null);
            case RETIRADO:
                return new Paso(EstadoEnvio.EN_CENTRO_ORIGEN, null);
            case EN_CENTRO_ORIGEN:
                return new Paso(EstadoEnvio.EN_TRANSITO, null);
            case EN_TRANSITO:
                return new Paso(EstadoEnvio.EN_CENTRO_DESTINO, null);
            case EN_CENTRO_DESTINO:
                return new Paso(EstadoEnvio.EN_REPARTO, null);
            case INTENTO_FALLIDO:
                return new Paso(EstadoEnvio.EN_REPARTO, null);
            case EN_REPARTO:
                if (!intentoFallido && random.nextDouble() < PROBABILIDAD_INCIDENTE) {
                    return new Paso(EstadoEnvio.INTENTO_FALLIDO,
                            MotivoIncidente.values()[random.nextInt(MotivoIncidente.values().length)]);
                }
                return new Paso(EstadoEnvio.ENTREGADO, null);
            default:
                return null;
        }
    }

    static int demoraSegundos(Servicio servicio, Random random) {
        if (servicio == Servicio.EXPRESS) {
            return 30 + random.nextInt(91);
        }
        return 60 + random.nextInt(241);
    }

    static String ciudadEvento(EstadoEnvio estado, String origen, String destino) {
        switch (estado) {
            case CREADO:
            case RETIRADO:
            case EN_CENTRO_ORIGEN:
                return origen;
            case EN_CENTRO_DESTINO:
            case EN_REPARTO:
            case INTENTO_FALLIDO:
            case ENTREGADO:
                return destino;
            default:
                return null;
        }
    }
}
