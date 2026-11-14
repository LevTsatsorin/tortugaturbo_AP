package com.example.tortugaturbo;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.function.Consumer;

public class PerfilActivity extends AppCompatActivity {

    private boolean propio;
    private Usuario objetivo;
    private EditText campoNombre;
    private EditText campoApellido;
    private EditText campoTelefono;
    private TextInputLayout tilNombre;
    private TextInputLayout tilApellido;
    private TextInputLayout tilTelefono;
    private TextView textoEmail;
    private TextView avatar;
    private TextView textoNombreCompleto;
    private TextView textoPeligro;
    private Chip chipRol;
    private Chip chipActivo;
    private View bloqueAdmin;
    private View cardPeligro;
    private RadioButton radioOperador;
    private RadioButton radioCliente;
    private RadioGroup grupoRol;
    private boolean ignorarRol;
    private Button botonGuardar;
    private MaterialButton botonPeligro;
    private ProgressBar progreso;
    private ListenerRegistration escuchaSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        Usuario sesion = Sesion.actual();
        if (sesion == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String id = getIntent().getStringExtra("USUARIO_ID");
        propio = id == null || id.equals(sesion.getUid());
        if (!propio && !Sesion.esOperador()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        tilNombre = findViewById(R.id.tilNombre);
        tilApellido = findViewById(R.id.tilApellido);
        tilTelefono = findViewById(R.id.tilTelefono);
        campoNombre = findViewById(R.id.campoNombre);
        campoApellido = findViewById(R.id.campoApellido);
        campoTelefono = findViewById(R.id.campoTelefono);
        textoEmail = findViewById(R.id.textoEmail);
        avatar = findViewById(R.id.avatar);
        textoNombreCompleto = findViewById(R.id.textoNombreCompleto);
        textoPeligro = findViewById(R.id.textoPeligro);
        chipRol = findViewById(R.id.chipRol);
        chipActivo = findViewById(R.id.chipActivo);
        bloqueAdmin = findViewById(R.id.bloqueAdmin);
        cardPeligro = findViewById(R.id.cardPeligro);
        radioCliente = findViewById(R.id.radioCliente);
        radioOperador = findViewById(R.id.radioOperador);
        grupoRol = findViewById(R.id.grupoRol);
        botonGuardar = findViewById(R.id.botonGuardar);
        botonPeligro = findViewById(R.id.botonPeligro);
        progreso = findViewById(R.id.progreso);

        botonGuardar.setOnClickListener(v -> guardar());
        if (propio) {
            toolbar.setTitle(R.string.perfil_titulo);
            botonPeligro.setText(R.string.perfil_eliminar);
            textoPeligro.setText(R.string.perfil_eliminar_explicacion);
            botonPeligro.setOnClickListener(v -> confirmarEliminar());
        } else {
            toolbar.setTitle(R.string.perfil_titulo_usuario);
            botonPeligro.setOnClickListener(v -> confirmarActivo());
            grupoRol.setOnCheckedChangeListener((g, idRadio) -> {
                if (!ignorarRol) {
                    confirmarRol(idRadio == R.id.radioOperador ? Usuario.ROL_OPERADOR : Usuario.ROL_CLIENTE);
                }
            });
        }
        botonGuardar.setEnabled(false);
        cardPeligro.setVisibility(View.GONE);
        progreso.setVisibility(View.VISIBLE);
        UsuariosFirestore.obtener(propio ? sesion.getUid() : id, new Resultado<Usuario>() {
            @Override
            public void ok(Usuario u) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                if (u == null) {
                    Toast.makeText(PerfilActivity.this, R.string.error_generico, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                botonGuardar.setEnabled(true);
                mostrar(u);
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(PerfilActivity.this, UsuariosFirestore.mensajeError(PerfilActivity.this, e), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void mostrar(Usuario u) {
        objetivo = u;
        campoNombre.setText(u.getNombre());
        campoApellido.setText(u.getApellido());
        campoTelefono.setText(u.getTelefono());
        textoEmail.setText(u.getEmail());
        refrescarCabecera(u);
        if (propio) {
            cardPeligro.setVisibility(View.VISIBLE);
        } else {
            bloqueAdmin.setVisibility(View.VISIBLE);
            cardPeligro.setVisibility(View.VISIBLE);
            marcarRol(u.esOperador());
        }
    }

    private void refrescarCabecera(Usuario u) {
        avatar.setText(Usuario.iniciales(u));
        textoNombreCompleto.setText(u.getNombreCompleto());
        chipRol.setText(u.esOperador() ? R.string.perfil_rol_operador : R.string.perfil_rol_cliente);
        if (!propio) {
            chipActivo.setVisibility(View.VISIBLE);
            chipActivo.setText(u.isActivo() ? R.string.usuarios_activa : R.string.usuarios_desactivada);
            chipActivo.setChipBackgroundColorResource(u.isActivo() ? R.color.chip_activa_fondo : R.color.chip_desactivada_fondo);
            chipActivo.setTextColor(getColor(u.isActivo() ? R.color.tortuga_verde : R.color.peligro));
            int color = getColor(u.isActivo() ? R.color.peligro : R.color.tortuga_verde);
            botonPeligro.setText(u.isActivo() ? R.string.perfil_desactivar : R.string.perfil_reactivar);
            botonPeligro.setTextColor(color);
            botonPeligro.setStrokeColor(ColorStateList.valueOf(color));
            textoPeligro.setText(u.isActivo() ? R.string.perfil_desactivar_explicacion : R.string.perfil_reactivar_explicacion);
        }
    }

    private void ocupado(boolean ocupado) {
        botonGuardar.setEnabled(!ocupado);
        botonPeligro.setEnabled(!ocupado);
        radioCliente.setEnabled(!ocupado);
        radioOperador.setEnabled(!ocupado);
        progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        escuchaSesion = Sesion.vigilar(this, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escuchaSesion != null) {
            escuchaSesion.remove();
            escuchaSesion = null;
        }
    }

    private boolean invalido(TextInputLayout til, EditText campo, int mensaje) {
        til.setError(getString(mensaje));
        campo.requestFocus();
        return false;
    }

    private void marcarRol(boolean operador) {
        ignorarRol = true;
        radioOperador.setChecked(operador);
        radioCliente.setChecked(!operador);
        ignorarRol = false;
    }

    private Usuario copia() {
        Usuario u = new Usuario();
        u.setUid(objetivo.getUid());
        u.setNombre(objetivo.getNombre());
        u.setApellido(objetivo.getApellido());
        u.setTelefono(objetivo.getTelefono());
        u.setRol(objetivo.getRol());
        u.setActivo(objetivo.isActivo());
        return u;
    }

    private void enviar(Consumer<Resultado<Void>> escritura, int aviso, Runnable alOk, Runnable alFallar) {
        ocupado(true);
        escritura.accept(new Resultado<Void>() {
            @Override
            public void ok(Void v) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado(false);
                alOk.run();
                refrescarCabecera(objetivo);
                Snackbar.make(findViewById(R.id.main), aviso, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado(false);
                alFallar.run();
                Toast.makeText(PerfilActivity.this, UsuariosFirestore.mensajeError(PerfilActivity.this, e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void guardar() {
        String nombre = campoNombre.getText().toString().trim();
        String apellido = campoApellido.getText().toString().trim();
        String telefono = campoTelefono.getText().toString().trim();
        tilNombre.setError(null);
        tilApellido.setError(null);
        tilTelefono.setError(null);
        boolean ok = true;
        if (!RegistroActivity.esTelefonoValido(telefono)) {
            ok = invalido(tilTelefono, campoTelefono, R.string.error_telefono_invalido);
        }
        if (!RegistroActivity.esNombreValido(apellido)) {
            ok = invalido(tilApellido, campoApellido, R.string.error_apellido_invalido);
        }
        if (!RegistroActivity.esNombreValido(nombre)) {
            ok = invalido(tilNombre, campoNombre, R.string.error_nombre_invalido);
        }
        if (!ok) {
            return;
        }
        Usuario u = copia();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setTelefono(telefono);
        enviar(r -> UsuariosFirestore.actualizarDatos(u, r), R.string.perfil_guardado, () -> {
            objetivo.setNombre(nombre);
            objetivo.setApellido(apellido);
            objetivo.setTelefono(telefono);
            if (propio) {
                Usuario s = Sesion.actual();
                s.setNombre(nombre);
                s.setApellido(apellido);
                s.setTelefono(telefono);
                s.setRol(objetivo.getRol());
                s.setActivo(objetivo.isActivo());
            }
        }, () -> {
        });
    }

    private void confirmarActivo() {
        boolean activar = !objetivo.isActivo();
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(activar ? R.string.perfil_confirmar_reactivar_titulo : R.string.perfil_confirmar_desactivar_titulo, objetivo.getNombreCompleto()))
                .setMessage(activar ? R.string.perfil_confirmar_reactivar_mensaje : R.string.perfil_confirmar_desactivar_mensaje)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(activar ? R.string.perfil_reactivar_confirmar : R.string.perfil_desactivar_confirmar, (d, w) -> {
                    enviar(r -> UsuariosFirestore.cambiarActivo(objetivo.getUid(), activar, r), activar ? R.string.perfil_cuenta_reactivada : R.string.perfil_cuenta_desactivada,
                            () -> objetivo.setActivo(activar), () -> {
                            });
                })
                .show();
    }

    private void confirmarRol(String rol) {
        boolean operador = Usuario.ROL_OPERADOR.equals(rol);
        Runnable revertir = () -> marcarRol(objetivo.esOperador());
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.perfil_confirmar_rol_titulo, objetivo.getNombreCompleto(),
                        getString(operador ? R.string.perfil_rol_operador : R.string.perfil_rol_cliente)))
                .setNegativeButton(R.string.accion_cancelar, (d, w) -> revertir.run())
                .setOnCancelListener(d -> revertir.run())
                .setPositiveButton(R.string.perfil_cambiar_confirmar, (d, w) -> {
                    enviar(r -> UsuariosFirestore.cambiarRol(objetivo.getUid(), rol, r), R.string.perfil_rol_actualizado, () -> objetivo.setRol(rol), revertir);
                })
                .show();
    }

    private void confirmarEliminar() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_password, null);
        EditText campo = vista.findViewById(R.id.campoPassword);
        TextInputLayout til = vista.findViewById(R.id.tilPassword);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perfil_eliminar_titulo)
                .setMessage(R.string.perfil_eliminar_mensaje)
                .setView(vista)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.perfil_eliminar_confirmar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = campo.getText().toString();
            til.setError(null);
            if (password.isEmpty()) {
                til.setError(getString(R.string.error_password_vacia));
                return;
            }
            botonesDialogo(dialogo, false);
            ocupado(true);
            if (escuchaSesion != null) {
                escuchaSesion.remove();
                escuchaSesion = null;
            }
            UsuariosFirestore.eliminarMiCuenta(password, new Resultado<Void>() {
                @Override
                public void ok(Void x) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    dialogo.dismiss();
                    new MaterialAlertDialogBuilder(PerfilActivity.this)
                            .setTitle(R.string.perfil_eliminada_titulo)
                            .setMessage(R.string.perfil_eliminada_mensaje)
                            .setCancelable(false)
                            .setPositiveButton(R.string.accion_entendido, (dd, w) -> Sesion.cerrar(PerfilActivity.this))
                            .show();
                }

                @Override
                public void error(Exception e) {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ocupado(false);
                    botonesDialogo(dialogo, true);
                    if (escuchaSesion == null) {
                        escuchaSesion = Sesion.vigilar(PerfilActivity.this, null);
                    }
                    boolean credencial = e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException;
                    til.setError(credencial ? getString(R.string.error_password_incorrecta) : UsuariosFirestore.mensajeError(PerfilActivity.this, e));
                }
            });
        }));
        dialogo.show();
    }

    private void botonesDialogo(AlertDialog d, boolean habilitar) {
        d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(habilitar);
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(habilitar);
        d.setCancelable(habilitar);
    }
}
