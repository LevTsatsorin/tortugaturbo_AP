package com.example.tortugaturbo;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeActivity extends AppCompatActivity {

    private TextView botonCuenta;
    private TextView textoSaludo;
    private ListenerRegistration escucha;
    private BottomSheetDialog hoja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        if (Sesion.actual() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        textoSaludo = findViewById(R.id.textoSaludo);
        botonCuenta = findViewById(R.id.botonCuenta);
        botonCuenta.setOnClickListener(v -> abrirCuenta());
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            }).launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Usuario actual = Sesion.actual();
        if (actual == null) {
            return;
        }
        escucha = UsuariosFirestore.escuchar(actual.getUid(), new Resultado<Usuario>() {
            @Override
            public void ok(Usuario u) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (u == null || !u.isActivo()) {
                    detener();
                    Toast.makeText(HomeActivity.this, R.string.error_cuenta_desactivada_kick, Toast.LENGTH_LONG).show();
                    Sesion.cerrar(HomeActivity.this);
                    return;
                }
                Usuario s = Sesion.actual();
                if (s != null) {
                    s.setRol(u.getRol());
                    s.setNombre(u.getNombre());
                    s.setApellido(u.getApellido());
                    s.setTelefono(u.getTelefono());
                    s.setActivo(u.isActivo());
                    refrescar();
                }
            }

            @Override
            public void error(Exception e) {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        detener();
    }

    private void detener() {
        cerrarHoja();
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
    }

    private void cerrarHoja() {
        if (hoja != null) {
            hoja.dismiss();
            hoja = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescar();
    }

    private void refrescar() {
        Usuario u = Sesion.actual();
        if (u != null && textoSaludo != null) {
            textoSaludo.setText(getString(R.string.home_saludo, u.getNombre()));
            botonCuenta.setText(Usuario.iniciales(u));
        }
    }

    private void abrirCuenta() {
        Usuario u = Sesion.actual();
        if (u == null) {
            return;
        }
        cerrarHoja();
        hoja = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_cuenta, null);
        hoja.setContentView(v);
        View contenedor = hoja.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (contenedor != null) {
            contenedor.setBackgroundColor(Color.TRANSPARENT);
        }
        ((TextView) v.findViewById(R.id.sheetAvatar)).setText(Usuario.iniciales(u));
        ((TextView) v.findViewById(R.id.sheetNombre)).setText(u.getNombreCompleto());
        ((TextView) v.findViewById(R.id.sheetEmail)).setText(u.getEmail());
        ((Chip) v.findViewById(R.id.sheetRol)).setText(u.esOperador() ? R.string.perfil_rol_operador : R.string.perfil_rol_cliente);
        View filaUsuarios = v.findViewById(R.id.filaUsuarios);
        filaUsuarios.setVisibility(Sesion.esOperador() ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.filaPerfil).setOnClickListener(x -> {
            cerrarHoja();
            startActivity(new Intent(this, PerfilActivity.class));
        });
        filaUsuarios.setOnClickListener(x -> {
            cerrarHoja();
            startActivity(new Intent(this, UsuariosActivity.class));
        });
        v.findViewById(R.id.filaCerrar).setOnClickListener(x -> {
            cerrarHoja();
            Sesion.cerrar(this);
        });
        hoja.show();
    }
}
