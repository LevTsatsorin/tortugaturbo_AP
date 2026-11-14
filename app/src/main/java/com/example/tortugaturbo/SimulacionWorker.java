package com.example.tortugaturbo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimulacionWorker extends Worker {

    public SimulacionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Result.success();
        }
        String tracking = getInputData().getString("tracking");
        if (tracking == null) {
            return Result.failure();
        }
        Context c = getApplicationContext();
        DocumentReference ref = FirebaseFirestore.getInstance().collection("envios").document(tracking);
        Random random = new Random();
        try {
            PasoSimulacion.Salida s = Tasks.await(
                    FirebaseFirestore.getInstance().runTransaction(tx -> PasoSimulacion.aplicar(tx, ref, true, random)),
                    60, TimeUnit.SECONDS);
            if (isStopped()) {
                return Result.success();
            }
            switch (s.tipo) {
                case AVANZO:
                    Notificador.mostrar(c, s.envio);
                    if (!s.envio.getEstadoEnum().esFinal()) {
                        SimulacionScheduler.programar(c, tracking, s.demora);
                    }
                    break;
                case PAUSADO:
                case ESPERAR:
                    SimulacionScheduler.programar(c, tracking, 60);
                    break;
                default:
                    break;
            }
            return Result.success();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (ExecutionException e) {
            return reintentable(e.getCause()) ? Result.retry() : Result.failure();
        } catch (TimeoutException e) {
            return Result.retry();
        } catch (RuntimeException e) {
            return Result.failure();
        }
    }

    private static boolean reintentable(Throwable t) {
        if (t instanceof FirebaseNetworkException || t instanceof TimeoutException) {
            return true;
        }
        if (t instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) t).getCode();
            return code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED;
        }
        return false;
    }
}
