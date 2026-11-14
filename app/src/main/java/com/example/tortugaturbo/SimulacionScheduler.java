package com.example.tortugaturbo;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;
import java.util.concurrent.TimeUnit;

final class SimulacionScheduler {

    private SimulacionScheduler() {
    }

    static void programar(Context c, String tracking, int demoraSeg) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SimulacionWorker.class)
                .setInitialDelay(demoraSeg, TimeUnit.SECONDS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag("envio_" + tracking)
                .setInputData(new Data.Builder().putString("tracking", tracking).build())
                .build();
        WorkManager.getInstance(c).enqueueUniqueWork("envio_" + tracking, ExistingWorkPolicy.APPEND_OR_REPLACE, req);
    }

    static void cancelar(Context c, String tracking) {
        WorkManager.getInstance(c).cancelUniqueWork("envio_" + tracking);
    }

    static void reanudarMisEnvios(Context c) {
        Context app = c.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            WorkManager.getInstance(app).cancelAllWork();
            return;
        }
        FirebaseFirestore.getInstance().collection("envios")
                .whereEqualTo("clienteUid", user.getUid())
                .get()
                .addOnSuccessListener(snap -> {
                    WorkManager.getInstance(app).cancelAllWork();
                    Random random = new Random();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Envio e = d.toObject(Envio.class);
                        if (e == null || e.getEstado() == null || e.getServicio() == null) {
                            continue;
                        }
                        try {
                            if (e.getEstadoEnum().esFinal()) {
                                continue;
                            }
                            programar(app, d.getId(), e.isPausado() ? 60 : SimuladorRuta.demoraSegundos(e.getServicioEnum(), random));
                        } catch (IllegalArgumentException ignorado) {
                        }
                    }
                });
    }
}
