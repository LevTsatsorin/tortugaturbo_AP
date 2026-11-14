package com.example.tortugaturbo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

public final class Sesion {
    private static Usuario actual;

    private Sesion() {
    }

    public static Usuario actual() {
        return actual;
    }

    public static boolean esOperador() {
        return actual != null && actual.esOperador();
    }

    public static void iniciar(Context c, Usuario u) {
        actual = u;
        SimulacionScheduler.reanudarMisEnvios(c.getApplicationContext());
    }

    public static void cerrar(Activity a) {
        WorkManager.getInstance(a).cancelAllWork();
        FirebaseAuth.getInstance().signOut();
        actual = null;
        Intent i = new Intent(a, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    public static ListenerRegistration vigilar(Activity a, Runnable alCambiar) {
        Usuario actual = actual();
        if (actual == null) {
            return null;
        }
        return UsuariosFirestore.escuchar(actual.getUid(), new Resultado<Usuario>() {
            @Override
            public void ok(Usuario u) {
                Usuario s = Sesion.actual();
                if (s == null || a.isFinishing() || a.isDestroyed()) {
                    return;
                }
                if (u == null || !u.isActivo()) {
                    Toast.makeText(a, R.string.error_cuenta_desactivada_kick, Toast.LENGTH_LONG).show();
                    cerrar(a);
                    return;
                }
                s.setRol(u.getRol());
                s.setNombre(u.getNombre());
                s.setApellido(u.getApellido());
                s.setTelefono(u.getTelefono());
                s.setActivo(u.isActivo());
                s.setAvatar(u.getAvatar());
                if (alCambiar != null) {
                    alCambiar.run();
                }
            }

            @Override
            public void error(Exception e) {
            }
        });
    }
}
