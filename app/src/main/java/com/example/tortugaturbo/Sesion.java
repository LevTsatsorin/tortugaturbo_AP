package com.example.tortugaturbo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;

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
}
