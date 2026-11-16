package com.example.tortugaturbo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText campoEmail;
    private EditText campoPassword;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private Button botonIngresar;
    private TextView linkRegistro;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        campoEmail = findViewById(R.id.campoEmail);
        campoPassword = findViewById(R.id.campoPassword);
        botonIngresar = findViewById(R.id.botonIngresar);
        linkRegistro = findViewById(R.id.linkRegistro);
        progreso = findViewById(R.id.progreso);

        botonIngresar.setOnClickListener(v -> ingresar());
        linkRegistro.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));

        FirebaseUser actual = FirebaseAuth.getInstance().getCurrentUser();
        if (actual != null) {
            cargarPerfil(actual.getUid());
        }
    }

    private void ocupado(boolean ocupado) {
        botonIngresar.setEnabled(!ocupado);
        progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
    }

    private void ingresar() {
        String email = campoEmail.getText().toString().trim().toLowerCase();
        String password = campoPassword.getText().toString();
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_invalido));
            campoEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_login));
            campoPassword.requestFocus();
            return;
        }
        ocupado(true);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> cargarPerfil(r.getUser().getUid()))
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ocupado(false);
                    mostrarError(e);
                });
    }

    private void mostrarError(Exception e) {
        String mensaje = UsuariosFirestore.mensajeError(this, e);
        if (mensaje.equals(getString(R.string.error_login))) {
            tilPassword.setError(mensaje);
            campoPassword.requestFocus();
        } else {
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }
    }

    private void cargarPerfil(String uid) {
        ocupado(true);
        UsuariosFirestore.obtener(uid, new Resultado<Usuario>() {
            @Override
            public void ok(Usuario u) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (u == null) {
                    borrarHuerfano();
                } else if (!u.isActivo()) {
                    rechazar(R.string.error_cuenta_desactivada);
                } else {
                    Sesion.iniciar(LoginActivity.this, u);
                    Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("ENVIO_ID", getIntent().getStringExtra("ENVIO_ID"));
                    startActivity(i);
                }
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado(false);
                mostrarError(e);
            }
        });
    }

    private void rechazar(int mensaje) {
        FirebaseAuth.getInstance().signOut();
        ocupado(false);
        Toast.makeText(this, getString(mensaje), Toast.LENGTH_LONG).show();
    }

    private void borrarHuerfano() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            rechazar(R.string.error_generico);
            return;
        }
        user.delete().addOnCompleteListener(t -> {
            if (isFinishing() || isDestroyed()) {
                FirebaseAuth.getInstance().signOut();
                return;
            }
            rechazar(R.string.error_generico);
        });
    }
}
