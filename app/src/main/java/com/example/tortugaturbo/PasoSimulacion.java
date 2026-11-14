package com.example.tortugaturbo;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class PasoSimulacion {

    enum Tipo { AVANZO, PAUSADO, ESPERAR, FIN }

    static final class Salida {
        final Tipo tipo;
        final Envio envio;
        final int demora;

        Salida(Tipo tipo, Envio envio, int demora) {
            this.tipo = tipo;
            this.envio = envio;
            this.demora = demora;
        }
    }

    private static final long TOLERANCIA_MS = 5000;

    private PasoSimulacion() {
    }

    static Salida aplicar(Transaction tx, DocumentReference ref, boolean respetarProximoPaso,
                          Random random) throws FirebaseFirestoreException {
        DocumentSnapshot snap = tx.get(ref);
        if (!snap.exists()) {
            return new Salida(Tipo.FIN, null, 0);
        }
        Envio envio = snap.toObject(Envio.class);
        if (envio == null || envio.getEstadoEnum().esFinal()) {
            return new Salida(Tipo.FIN, envio, 0);
        }
        if (envio.isPausado()) {
            return new Salida(Tipo.PAUSADO, envio, 0);
        }
        Timestamp fecha = Timestamp.now();
        long ahora = fecha.toDate().getTime();
        Timestamp proximo = envio.getProximoPasoEn();
        if (respetarProximoPaso && proximo != null && proximo.toDate().getTime() > ahora + TOLERANCIA_MS) {
            return new Salida(Tipo.ESPERAR, envio, 0);
        }
        SimuladorRuta.Paso paso = SimuladorRuta.siguiente(envio.getEstadoEnum(), envio.isIntentoFallido(), random);
        if (paso == null) {
            return new Salida(Tipo.FIN, envio, 0);
        }
        int demora = SimuladorRuta.demoraSegundos(envio.getServicioEnum(), random);

        EventoEnvio evento = new EventoEnvio();
        evento.setEstado(paso.estado.name());
        evento.setCiudad(SimuladorRuta.ciudadEvento(paso.estado, envio.getOrigen(), envio.getDestino()));
        evento.setMotivo(paso.motivo == null ? null : paso.motivo.name());
        evento.setFecha(fecha);

        List<EventoEnvio> eventos = envio.getEventos() == null ? new ArrayList<>() : new ArrayList<>(envio.getEventos());
        eventos.add(evento);
        boolean intentoFallido = envio.isIntentoFallido() || paso.estado == EstadoEnvio.INTENTO_FALLIDO;
        Timestamp siguiente = paso.estado.esFinal()
                ? null
                : new Timestamp(new Date(ahora + demora * 1000L));

        Map<String, Object> cambios = new HashMap<>();
        cambios.put("estado", paso.estado.name());
        cambios.put("intentoFallido", intentoFallido);
        cambios.put("actualizadoEn", fecha);
        cambios.put("eventos", eventos);
        cambios.put("proximoPasoEn", siguiente);
        tx.update(ref, cambios);

        envio.setEstado(paso.estado.name());
        envio.setIntentoFallido(intentoFallido);
        envio.setActualizadoEn(fecha);
        envio.setEventos(eventos);
        envio.setProximoPasoEn(siguiente);
        return new Salida(Tipo.AVANZO, envio, demora);
    }
}
