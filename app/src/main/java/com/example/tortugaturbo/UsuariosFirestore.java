package com.example.tortugaturbo;

import android.content.Context;

import androidx.work.WorkManager;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UsuariosFirestore {
    private static final String COLECCION = "usuarios";
    private static final long TIMEOUT_S = 30;

    private UsuariosFirestore() {
    }

    private static DocumentReference doc(String uid) {
        return FirebaseFirestore.getInstance().collection(COLECCION).document(uid);
    }

    private static Usuario aUsuario(DocumentSnapshot d) {
        return d.exists() ? d.toObject(Usuario.class) : null;
    }

    public static void obtener(String uid, Resultado<Usuario> r) {
        doc(uid).get()
                .addOnSuccessListener(d -> r.ok(aUsuario(d)))
                .addOnFailureListener(r::error);
    }

    public static void crearPerfil(Usuario u, Resultado<Void> r) {
        doc(u.getUid()).set(u)
                .addOnSuccessListener(r::ok)
                .addOnFailureListener(r::error);
    }

    public static void actualizarDatos(Usuario u, Resultado<Void> r) {
        doc(u.getUid()).update(
                        "nombre", u.getNombre(),
                        "apellido", u.getApellido(),
                        "telefono", u.getTelefono())
                .addOnSuccessListener(r::ok)
                .addOnFailureListener(r::error);
    }

    public static void cambiarRol(String uid, String rol, Resultado<Void> r) {
        doc(uid).update("rol", rol)
                .addOnSuccessListener(r::ok)
                .addOnFailureListener(r::error);
    }

    public static void cambiarActivo(String uid, boolean activo, Resultado<Void> r) {
        doc(uid).update("activo", activo)
                .addOnSuccessListener(r::ok)
                .addOnFailureListener(r::error);
    }

    public static void listarTodos(Resultado<List<Usuario>> r) {
        FirebaseFirestore.getInstance().collection(COLECCION).get()
                .addOnSuccessListener(q -> r.ok(q.toObjects(Usuario.class)))
                .addOnFailureListener(r::error);
    }

    public static ListenerRegistration escuchar(String uid, Resultado<Usuario> r) {
        return doc(uid).addSnapshotListener((d, e) -> {
            if (e != null) {
                r.error(e);
            } else if (d != null && !d.exists() && d.getMetadata().isFromCache()) {
                return;
            } else {
                r.ok(d == null ? null : aUsuario(d));
            }
        });
    }

    public static void eliminarMiCuenta(String password, Resultado<Void> r) {
        AppExecutors.background().execute(() -> {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user.getUid();
                Tasks.await(user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password)), TIMEOUT_S, TimeUnit.SECONDS);
                Context app = FirebaseApp.getInstance().getApplicationContext();
                WorkManager.getInstance(app).cancelAllWork();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                QuerySnapshot envios = Tasks.await(db.collection("envios").whereEqualTo("clienteUid", uid).get(Source.SERVER), TIMEOUT_S, TimeUnit.SECONDS);
                WriteBatch batch = db.batch();
                for (DocumentSnapshot d : envios.getDocuments()) {
                    batch.delete(d.getReference());
                }
                Tasks.await(batch.commit(), TIMEOUT_S, TimeUnit.SECONDS);
                Tasks.await(doc(uid).delete(), TIMEOUT_S, TimeUnit.SECONDS);
                Tasks.await(user.delete(), TIMEOUT_S, TimeUnit.SECONDS);
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

    public static String mensajeError(Context c, Exception e) {
        if (e instanceof FirebaseNetworkException || e instanceof TimeoutException
                || (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE)) {
            return c.getString(R.string.error_sin_conexion);
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return c.getString(R.string.error_email_duplicado);
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException) {
            return c.getString(R.string.error_login);
        }
        return c.getString(R.string.error_generico);
    }
}
