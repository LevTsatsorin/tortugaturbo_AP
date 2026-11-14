package com.example.tortugaturbo;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class EnviosFirestore {
    private static final String COLECCION = "envios";
    private static final long TIMEOUT_S = 30;
    private static final long TIMEOUT_ESCRITURA_S = 15;
    private static final int MAX_INTENTOS = 5;

    private EnviosFirestore() {
    }

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    private static DocumentReference doc(String tracking) {
        return db().collection(COLECCION).document(tracking);
    }

    private static Envio aEnvio(DocumentSnapshot d) {
        return d.exists() ? d.toObject(Envio.class) : null;
    }

    private static Timestamp en(int segundos) {
        return new Timestamp(new Date(System.currentTimeMillis() + segundos * 1000L));
    }

    private static boolean esDueno(Envio e) {
        Usuario u = Sesion.actual();
        return u != null && u.getUid() != null && u.getUid().equals(e.getClienteUid());
    }

    private static void recalcular(Envio e) {
        int km = Tarifador.distanciaKm(e.getOrigenEnum(), e.getDestinoEnum());
        e.setDistanciaKm(km);
        e.setPrecio(Tarifador.precio(e.getPesoKg(), km, e.getServicioEnum()));
        e.setDiasEstimados(Tarifador.diasEstimados(km, e.getServicioEnum()));
    }

    private static void esperar(Task<Void> tarea, Runnable alExito, Resultado<Void> r) {
        AppExecutors.background().execute(() -> {
            try {
                Tasks.await(tarea, TIMEOUT_ESCRITURA_S, TimeUnit.SECONDS);
                if (alExito != null) {
                    alExito.run();
                }
                AppExecutors.main().post(() -> r.ok(null));
            } catch (ExecutionException e) {
                Exception causa = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                AppExecutors.main().post(() -> r.error(causa));
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                AppExecutors.main().post(() -> r.error(e));
            }
        });
    }

    public static void crear(Context c, Envio borrador, Resultado<String> r) {
        Context app = c.getApplicationContext();
        AppExecutors.background().execute(() -> {
            try {
                Usuario u = Sesion.actual();
                if (u == null) {
                    throw new IllegalStateException();
                }
                Random random = new Random();
                borrador.setClienteUid(u.getUid());
                borrador.setClienteNombre(u.getNombreCompleto());
                recalcular(borrador);
                Timestamp ahora = Timestamp.now();
                borrador.setEstado(EstadoEnvio.CREADO.name());
                borrador.setPausado(false);
                borrador.setIntentoFallido(false);
                borrador.setCreadoEn(ahora);
                borrador.setActualizadoEn(ahora);
                EventoEnvio ev = new EventoEnvio();
                ev.setEstado(EstadoEnvio.CREADO.name());
                ev.setCiudad(SimuladorRuta.ciudadEvento(EstadoEnvio.CREADO, borrador.getOrigen(), borrador.getDestino()));
                ev.setMotivo(null);
                ev.setFecha(ahora);
                List<EventoEnvio> eventos = new ArrayList<>();
                eventos.add(ev);
                borrador.setEventos(eventos);
                int demora = SimuladorRuta.demoraSegundos(borrador.getServicioEnum(), random);
                borrador.setProximoPasoEn(new Timestamp(new Date(ahora.toDate().getTime() + demora * 1000L)));

                String tracking = null;
                for (int i = 0; i < MAX_INTENTOS && tracking == null; i++) {
                    String candidato = TrackingGenerator.generar(random);
                    borrador.setTracking(candidato);
                    DocumentReference ref = doc(candidato);
                    Boolean creado = Tasks.await(db().runTransaction(tx -> {
                        if (tx.get(ref).exists()) {
                            return false;
                        }
                        tx.set(ref, borrador);
                        return true;
                    }), TIMEOUT_S, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(creado)) {
                        tracking = candidato;
                    }
                }
                if (tracking == null) {
                    throw new IllegalStateException();
                }
                try {
                    SimulacionScheduler.programar(app, tracking, demora);
                } catch (RuntimeException ignorada) {
                }
                String listo = tracking;
                AppExecutors.main().post(() -> r.ok(listo));
            } catch (ExecutionException e) {
                Exception causa = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                AppExecutors.main().post(() -> r.error(causa));
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                AppExecutors.main().post(() -> r.error(e));
            }
        });
    }

    public static void actualizar(Envio e, Resultado<Void> r) {
        try {
            recalcular(e);
        } catch (IllegalArgumentException ex) {
            AppExecutors.main().post(() -> r.error(ex));
            return;
        }
        esperar(doc(e.getTracking()).update(
                        "destinatarioNombre", e.getDestinatarioNombre(),
                        "destinatarioTelefono", e.getDestinatarioTelefono(),
                        "direccion", e.getDireccion(),
                        "origen", e.getOrigen(),
                        "destino", e.getDestino(),
                        "pesoKg", e.getPesoKg(),
                        "servicio", e.getServicio(),
                        "descripcion", e.getDescripcion(),
                        "distanciaKm", e.getDistanciaKm(),
                        "precio", e.getPrecio(),
                        "diasEstimados", e.getDiasEstimados(),
                        "actualizadoEn", Timestamp.now()), null, r);
    }

    public static void obtener(String tracking, Resultado<Envio> r) {
        doc(tracking).get()
                .addOnSuccessListener(d -> r.ok(aEnvio(d)))
                .addOnFailureListener(r::error);
    }

    public static ListenerRegistration escuchar(String tracking, Resultado<Envio> r) {
        return doc(tracking).addSnapshotListener((d, e) -> {
            if (e != null) {
                r.error(e);
            } else if (d != null && !d.exists() && d.getMetadata().isFromCache()) {
                return;
            } else {
                r.ok(d == null ? null : aEnvio(d));
            }
        });
    }

    public static ListenerRegistration escucharLista(Resultado<List<Envio>> r) {
        Usuario u = Sesion.actual();
        Query q = db().collection(COLECCION);
        if (!Sesion.esOperador()) {
            q = q.whereEqualTo("clienteUid", u == null ? "" : u.getUid());
        }
        return q.addSnapshotListener((s, e) -> {
            if (e != null || s == null) {
                r.error(e != null ? e : new IllegalStateException());
                return;
            }
            List<Envio> lista = s.toObjects(Envio.class);
            Collections.sort(lista, (a, b) -> {
                Timestamp x = a.getActualizadoEn();
                Timestamp y = b.getActualizadoEn();
                if (x == null && y == null) {
                    return 0;
                }
                if (x == null) {
                    return 1;
                }
                if (y == null) {
                    return -1;
                }
                return y.compareTo(x);
            });
            r.ok(lista);
        });
    }

    public static void cancelar(Context c, Envio e, Resultado<Void> r) {
        Context app = c.getApplicationContext();
        DocumentReference ref = doc(e.getTracking());
        db().runTransaction(tx -> {
                    DocumentSnapshot snap = tx.get(ref);
                    Envio fresco = aEnvio(snap);
                    if (fresco == null || fresco.getEstadoEnum().esFinal()) {
                        return null;
                    }
                    Timestamp ahora = Timestamp.now();
                    EventoEnvio ev = new EventoEnvio();
                    ev.setEstado(EstadoEnvio.CANCELADO.name());
                    ev.setCiudad(null);
                    ev.setMotivo(null);
                    ev.setFecha(ahora);
                    List<EventoEnvio> eventos = fresco.getEventos() == null
                            ? new ArrayList<>() : new ArrayList<>(fresco.getEventos());
                    eventos.add(ev);
                    tx.update(ref,
                            "estado", EstadoEnvio.CANCELADO.name(),
                            "eventos", eventos,
                            "proximoPasoEn", null,
                            "actualizadoEn", ahora);
                    return null;
                })
                .addOnSuccessListener(v -> {
                    if (esDueno(e)) {
                        SimulacionScheduler.cancelar(app, e.getTracking());
                    }
                    r.ok(null);
                })
                .addOnFailureListener(r::error);
    }

    public static void eliminar(Context c, Envio e, Resultado<Void> r) {
        Context app = c.getApplicationContext();
        boolean dueno = esDueno(e);
        esperar(doc(e.getTracking()).delete(), () -> {
            if (dueno) {
                SimulacionScheduler.cancelar(app, e.getTracking());
            }
        }, r);
    }

    public static void pausar(Envio e, Resultado<Void> r) {
        esperar(doc(e.getTracking()).update(
                        "pausado", true,
                        "proximoPasoEn", null,
                        "actualizadoEn", Timestamp.now()), null, r);
    }

    public static void reanudar(Envio e, Resultado<Void> r) {
        int demora = SimuladorRuta.demoraSegundos(e.getServicioEnum(), new Random());
        esperar(doc(e.getTracking()).update(
                        "pausado", false,
                        "proximoPasoEn", en(demora),
                        "actualizadoEn", Timestamp.now()), null, r);
    }

    public static void avanzar(Envio e, Resultado<Void> r) {
        DocumentReference ref = doc(e.getTracking());
        Random random = new Random();
        db().runTransaction(tx -> PasoSimulacion.aplicar(tx, ref, false, random))
                .addOnSuccessListener(s -> r.ok(null))
                .addOnFailureListener(r::error);
    }
}
