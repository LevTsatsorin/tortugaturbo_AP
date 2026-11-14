package com.example.tortugaturbo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegistroActivity extends AppCompatActivity {

    private EditText campoNombre;
    private EditText campoApellido;
    private EditText campoEmail;
    private EditText campoTelefono;
    private EditText campoPassword;
    private EditText campoPassword2;
    private TextInputLayout tilNombre;
    private TextInputLayout tilApellido;
    private TextInputLayout tilEmail;
    private TextInputLayout tilTelefono;
    private TextInputLayout tilPassword;
    private TextInputLayout tilPassword2;
    private Button botonRegistrar;
    private TextView linkLogin;
    private ProgressBar progreso;

    static boolean esNombreValido(String s) {
        return s != null && s.trim().matches("^[\\p{L} ]{2,30}$");
    }

    static boolean esTelefonoValido(String s) {
        return s != null && s.matches("^[0-9]{8,15}$");
    }

    static boolean esPasswordValida(String s) {
        if (s == null || s.length() < 6) {
            return false;
        }
        boolean letra = false;
        boolean digito = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            letra |= Character.isLetter(c);
            digito |= Character.isDigit(c);
        }
        return letra && digito;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        tilNombre = findViewById(R.id.tilNombre);
        tilApellido = findViewById(R.id.tilApellido);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefono = findViewById(R.id.tilTelefono);
        tilPassword = findViewById(R.id.tilPassword);
        tilPassword2 = findViewById(R.id.tilPassword2);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        campoNombre = findViewById(R.id.campoNombre);
        campoApellido = findViewById(R.id.campoApellido);
        campoEmail = findViewById(R.id.campoEmail);
        campoTelefono = findViewById(R.id.campoTelefono);
        campoPassword = findViewById(R.id.campoPassword);
        campoPassword2 = findViewById(R.id.campoPassword2);
        botonRegistrar = findViewById(R.id.botonRegistrar);
        linkLogin = findViewById(R.id.linkLogin);
        progreso = findViewById(R.id.progreso);

        botonRegistrar.setOnClickListener(v -> registrar());
        linkLogin.setOnClickListener(v -> finish());
    }

    private void ocupado(boolean ocupado) {
        botonRegistrar.setEnabled(!ocupado);
        progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
    }

    private boolean invalido(TextInputLayout til, EditText campo, int mensaje) {
        til.setError(getString(mensaje));
        campo.requestFocus();
        return false;
    }

    private void registrar() {
        String nombre = campoNombre.getText().toString().trim();
        String apellido = campoApellido.getText().toString().trim();
        String email = campoEmail.getText().toString().trim().toLowerCase();
        String telefono = campoTelefono.getText().toString().trim();
        String password = campoPassword.getText().toString();
        String password2 = campoPassword2.getText().toString();

        for (TextInputLayout til : new TextInputLayout[]{tilNombre, tilApellido, tilEmail, tilTelefono, tilPassword, tilPassword2}) {
            til.setError(null);
        }
        boolean ok = true;
        if (!password2.equals(password)) {
            ok = invalido(tilPassword2, campoPassword2, R.string.error_password_no_coincide);
        }
        if (!esPasswordValida(password)) {
            ok = invalido(tilPassword, campoPassword, R.string.error_password_invalida);
        }
        if (!esTelefonoValido(telefono)) {
            ok = invalido(tilTelefono, campoTelefono, R.string.error_telefono_invalido);
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ok = invalido(tilEmail, campoEmail, R.string.error_email_invalido);
        }
        if (!esNombreValido(apellido)) {
            ok = invalido(tilApellido, campoApellido, R.string.error_apellido_invalido);
        }
        if (!esNombreValido(nombre)) {
            ok = invalido(tilNombre, campoNombre, R.string.error_nombre_invalido);
        }
        if (!ok) {
            return;
        }

        ocupado(true);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> crearPerfil(r.getUser(), nombre, apellido, email, telefono))
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ocupado(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        invalido(tilEmail, campoEmail, R.string.error_email_duplicado);
                    } else {
                        Toast.makeText(this, UsuariosFirestore.mensajeError(this, e), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void crearPerfil(FirebaseUser user, String nombre, String apellido, String email, String telefono) {
        Usuario u = new Usuario();
        u.setUid(user.getUid());
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        u.setTelefono(telefono);
        u.setRol(Usuario.ROL_CLIENTE);
        u.setActivo(true);
        UsuariosFirestore.crearPerfil(u, new Resultado<Void>() {
            @Override
            public void ok(Void v) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Sesion.iniciar(RegistroActivity.this, u);
                Intent i = new Intent(RegistroActivity.this, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }

            @Override
            public void error(Exception e) {
                user.delete();
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado(false);
                Toast.makeText(RegistroActivity.this, UsuariosFirestore.mensajeError(RegistroActivity.this, e), Toast.LENGTH_LONG).show();
            }
        });
    }
}
