package com.example.tortugaturbo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

final class Notificador {

    private static final String CANAL = "envios";

    private Notificador() {
    }

    static void mostrar(Context c, Envio envio) {
        if (envio == null || envio.getTracking() == null) {
            return;
        }
        crearCanal(c);
        NotificationManagerCompat manager = NotificationManagerCompat.from(c);
        if (!manager.areNotificationsEnabled()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<EventoEnvio> eventos = envio.getEventos();
        if (eventos == null || eventos.isEmpty()) {
            return;
        }
        EventoEnvio ultimo = eventos.get(eventos.size() - 1);
        String texto = textoEvento(c, envio, ultimo);
        String ciudad = etiquetaCiudad(c, ultimo.getCiudad());
        String cuerpo = ciudad == null ? texto : c.getString(R.string.notif_cuerpo, texto, ciudad);
        String tracking = envio.getTracking();
        int id = tracking.hashCode();

        Intent intent = new Intent(c, HomeActivity.class);
        intent.putExtra("ENVIO_ID", tracking);
        PendingIntent pi = PendingIntent.getActivity(c, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setColor(ContextCompat.getColor(c, R.color.turbo_naranja))
                .setContentTitle(tracking)
                .setContentText(cuerpo)
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            manager.notify(id, b.build());
        } catch (SecurityException ignorado) {
        }
    }

    static String textoEvento(Context c, Envio e, EventoEnvio ev) {
        try {
            EstadoEnvio estado = EstadoEnvio.valueOf(ev.getEstado());
            switch (estado) {
                case EN_TRANSITO:
                    return c.getString(estado.getEventoTexto(), etiquetaCiudad(c, e.getDestino()));
                case INTENTO_FALLIDO:
                    return c.getString(estado.getEventoTexto(),
                            c.getString(MotivoIncidente.valueOf(ev.getMotivo()).getTexto()));
                default:
                    return c.getString(estado.getEventoTexto());
            }
        } catch (IllegalArgumentException | NullPointerException ex) {
            return "";
        }
    }

    static String etiquetaCiudad(Context c, String ciudad) {
        if (ciudad == null) {
            return null;
        }
        try {
            return c.getString(Ciudad.valueOf(ciudad).getEtiqueta());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void crearCanal(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel canal = new NotificationChannel(CANAL,
                    c.getString(R.string.notif_canal_nombre), NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription(c.getString(R.string.notif_canal_descripcion));
            c.getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
    }
}
