package com.example.tortugaturbo;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView botonCuenta;
    private TextView textoSaludo;
    private ListenerRegistration escucha;
    private boolean eraOperador;
    private ListenerRegistration escuchaEnvios;
    private Chip chipRol;
    private TextView textoResumen;
    private TextView headerTitulo;
    private TextView headerCuenta;
    private View progresoEnvio;
    private View botonNuevo;
    private View textoSinEnvios;
    private TextView numTotal;
    private TextView numEnCurso;
    private TextView numEntregados;
    private TextView numCancelados;
    private TextInputLayout tilBuscar;
    private TextInputEditText campoBuscar;
    private EnviosAdapter adapter;
    private boolean buscando;
    private View botonEscanear;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
            handler.postDelayed(this, 30000);
        }
    };
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
        String envioId = getIntent().getStringExtra("ENVIO_ID");
        if (envioId != null && savedInstanceState == null) {
            abrirDetalle(envioId);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                ((ExtendedFloatingActionButton) botonNuevo).hide();
            } else {
                ((ExtendedFloatingActionButton) botonNuevo).show();
            }
            return insets;
        });
        textoSaludo = findViewById(R.id.textoSaludo);
        botonCuenta = findViewById(R.id.botonCuenta);
        botonCuenta.setOnClickListener(v -> abrirCuenta());
        chipRol = findViewById(R.id.chipRol);
        chipRol.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(0x40FFFFFF));
        chipRol.setTextColor(Color.WHITE);
        textoResumen = findViewById(R.id.textoResumen);
        headerTitulo = findViewById(R.id.headerTitulo);
        headerCuenta = findViewById(R.id.headerCuenta);
        progresoEnvio = findViewById(R.id.progresoEnvio);
        textoSinEnvios = findViewById(R.id.textoSinEnvios);
        numTotal = findViewById(R.id.numTotal);
        numEnCurso = findViewById(R.id.numEnCurso);
        numEntregados = findViewById(R.id.numEntregados);
        numCancelados = findViewById(R.id.numCancelados);
        tilBuscar = findViewById(R.id.tilBuscar);
        campoBuscar = findViewById(R.id.campoBuscar);
        adapter = new EnviosAdapter(this::abrirDetalle);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        botonNuevo = findViewById(R.id.botonNuevo);
        botonNuevo.setOnClickListener(v -> crearPrueba());
        tilBuscar.setEndIconOnClickListener(v -> buscar());
        campoBuscar.setOnEditorActionListener((v, accion, ev) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });
        campoBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                tilBuscar.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        botonEscanear = findViewById(R.id.botonEscanear);
        botonEscanear.setOnClickListener(v -> escanear());
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
        eraOperador = Sesion.esOperador();
        escucha = Sesion.vigilar(this, () -> {
            refrescar();
            if (eraOperador != Sesion.esOperador()) {
                eraOperador = Sesion.esOperador();
                if (escuchaEnvios != null) {
                    escucharEnvios();
                }
            }
        });
        handler.postDelayed(tick, 30000);
        escucharEnvios();
    }

    private void escucharEnvios() {
        if (escuchaEnvios != null) {
            escuchaEnvios.remove();
        }
        escuchaEnvios = EnviosFirestore.escucharLista(new Resultado<List<Envio>>() {
            @Override
            public void ok(List<Envio> lista) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                mostrarEnvios(lista);
            }

            @Override
            public void error(Exception e) {
            }
        });
    }

    private void abrirDetalle(Envio e) {
        abrirDetalle(e.getTracking());
    }

    private void abrirDetalle(String tracking) {
        startActivity(new Intent(this, EnvioDetalleActivity.class).putExtra("ENVIO_ID", tracking));
    }

    private void buscar() {
        if (buscando) {
            return;
        }
        String t = TrackingGenerator.normalizar(String.valueOf(campoBuscar.getText()));
        if (!TrackingGenerator.esValido(t)) {
            tilBuscar.setError(getString(R.string.error_tracking_invalido));
            return;
        }
        tilBuscar.setError(null);
        estadoBusqueda(true);
        EnviosFirestore.obtener(t, new Resultado<Envio>() {
            @Override
            public void ok(Envio e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                estadoBusqueda(false);
                if (e == null) {
                    Toast.makeText(HomeActivity.this, R.string.home_no_encontrado, Toast.LENGTH_SHORT).show();
                } else {
                    abrirDetalle(e.getTracking());
                }
            }

            @Override
            public void error(Exception ex) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                estadoBusqueda(false);
                boolean denegado = ex instanceof FirebaseFirestoreException
                        && ((FirebaseFirestoreException) ex).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                Toast.makeText(HomeActivity.this, denegado ? getString(R.string.home_no_encontrado)
                        : UsuariosFirestore.mensajeError(HomeActivity.this, ex), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void estadoBusqueda(boolean activo) {
        buscando = activo;
        progresoEnvio.setVisibility(activo ? View.VISIBLE : View.GONE);
        botonEscanear.setEnabled(!activo);
        tilBuscar.setEndIconOnClickListener(v -> buscar());
    }

    private void escanear() {
        GmsBarcodeScanning.getClient(this, new GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
                .startScan()
                .addOnSuccessListener(codigo -> {
                    String v = codigo.getRawValue();
                    if (v != null && !isFinishing() && !isDestroyed()) {
                        campoBuscar.setText(v);
                        buscar();
                    }
                })
                .addOnFailureListener(ex -> Toast.makeText(this, R.string.home_error_escaner, Toast.LENGTH_SHORT).show());
    }

    private void crearPrueba() {
        botonNuevo.setEnabled(false);
        progresoEnvio.setVisibility(View.VISIBLE);
        Envio b = new Envio();
        b.setDestinatarioNombre(getString(R.string.demo_destinatario));
        b.setDestinatarioTelefono(getString(R.string.demo_telefono));
        b.setDireccion(getString(R.string.demo_direccion));
        b.setOrigen(Ciudad.BUENOS_AIRES.name());
        b.setDestino(Ciudad.USHUAIA.name());
        b.setPesoKg(1.5);
        b.setServicio(Servicio.EXPRESS.name());
        b.setDescripcion("");
        EnviosFirestore.crear(this, b, new Resultado<String>() {
            @Override
            public void ok(String tracking) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                terminarPrueba();
                abrirDetalle(tracking);
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                terminarPrueba();
                Toast.makeText(HomeActivity.this, UsuariosFirestore.mensajeError(HomeActivity.this, e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void terminarPrueba() {
        botonNuevo.setEnabled(true);
        progresoEnvio.setVisibility(View.GONE);
    }

    private void mostrarEnvios(List<Envio> lista) {
        int curso = 0;
        int entregados = 0;
        int cancelados = 0;
        for (Envio e : lista) {
            try {
                EstadoEnvio s = e.getEstadoEnum();
                if (s == EstadoEnvio.ENTREGADO) {
                    entregados++;
                } else if (s == EstadoEnvio.CANCELADO) {
                    cancelados++;
                } else {
                    curso++;
                }
            } catch (IllegalArgumentException ignorado) {
                curso++;
            }
        }
        numTotal.setText(String.valueOf(lista.size()));
        numEnCurso.setText(String.valueOf(curso));
        numEntregados.setText(String.valueOf(entregados));
        numCancelados.setText(String.valueOf(cancelados));
        headerCuenta.setText(String.valueOf(lista.size()));
        textoResumen.setText(curso == 0 ? getString(R.string.home_resumen_nada)
                : getResources().getQuantityString(R.plurals.home_resumen_en_curso, curso, curso));
        textoSinEnvios.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.setItems(lista);
    }

    @Override
    protected void onStop() {
        super.onStop();
        detener();
    }

    private void detener() {
        handler.removeCallbacks(tick);
        cerrarHoja();
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
        if (escuchaEnvios != null) {
            escuchaEnvios.remove();
            escuchaEnvios = null;
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
            headerTitulo.setText(u.esOperador() ? R.string.home_todos_envios : R.string.home_tus_envios);
            chipRol.setText(u.esOperador() ? R.string.perfil_rol_operador : R.string.perfil_rol_cliente);
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
