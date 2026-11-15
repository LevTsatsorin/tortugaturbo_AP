package com.example.tortugaturbo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class EnvioFormActivity extends AppCompatActivity {

    private static final Locale AR = new Locale("es", "AR");

    private EditText destinatario;
    private EditText telefono;
    private EditText direccion;
    private EditText peso;
    private EditText descripcion;
    private Spinner origen;
    private Spinner destino;
    private RadioGroup servicio;
    private TextView preview;
    private TextView errorDestino;
    private Button guardar;
    private View progreso;
    private TextView avisoRetirado;
    private String tracking;
    private Envio envio;
    private boolean cargando;
    private boolean guardando;
    private ListenerRegistration escuchaSesion;

    static boolean puedeEditar(EstadoEnvio estado, boolean operador, boolean duenio) {
        return estado != null && !estado.esFinal()
                && (operador || (duenio && estado.getPaso() < EstadoEnvio.EN_REPARTO.getPaso()));
    }

    static boolean puedeEditarRuta(EstadoEnvio estado, boolean operador) {
        return estado != null && !estado.esFinal() && (operador || estado == EstadoEnvio.CREADO);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        if (Sesion.actual() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        tracking = getIntent().getStringExtra("ENVIO_ID");
        setContentView(R.layout.activity_envio_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.formTitulo);
        toolbar.setNavigationOnClickListener(v -> finish());
        destinatario = findViewById(R.id.inputDestinatario);
        telefono = findViewById(R.id.inputTelDestinatario);
        direccion = findViewById(R.id.inputDireccion);
        peso = findViewById(R.id.inputPeso);
        descripcion = findViewById(R.id.inputDescripcion);
        origen = findViewById(R.id.spinnerOrigen);
        destino = findViewById(R.id.spinnerDestino);
        servicio = findViewById(R.id.radioServicio);
        preview = findViewById(R.id.textPreview);
        errorDestino = findViewById(R.id.textErrorDestino);
        guardar = findViewById(R.id.botonGuardar);
        progreso = findViewById(R.id.progreso);
        avisoRetirado = findViewById(R.id.textAvisoRetirado);
        toolbar.setTitle(tracking == null ? R.string.envio_titulo_crear : R.string.envio_titulo_editar);
        guardar.setText(tracking == null ? R.string.envio_crear : R.string.envio_guardar);
        cargarCiudades();
        observarCampo(destinatario, R.id.tilDestinatario);
        observarCampo(telefono, R.id.tilTelDestinatario);
        observarCampo(direccion, R.id.tilDireccion);
        observarCampo(peso, R.id.tilPeso);
        AdapterView.OnItemSelectedListener seleccion = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                errorDestino.setVisibility(View.GONE);
                actualizarPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                actualizarPreview();
            }
        };
        origen.setOnItemSelectedListener(seleccion);
        destino.setOnItemSelectedListener(seleccion);
        servicio.setOnCheckedChangeListener((group, checkedId) -> actualizarPreview());
        guardar.setOnClickListener(v -> guardar());
        actualizarPreview();
        if (tracking != null) {
            cargarEnvio();
        }
    }

    private void cargarCiudades() {
        Ciudad[] ciudades = Ciudad.values();
        String[] etiquetas = new String[ciudades.length];
        for (int i = 0; i < ciudades.length; i++) {
            etiquetas[i] = getString(ciudades[i].getEtiqueta());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, etiquetas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        origen.setAdapter(adapter);
        destino.setAdapter(adapter);
        destino.setSelection(Ciudad.LA_PLATA.ordinal());
    }

    private void observarCampo(EditText campo, int layoutId) {
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ((TextInputLayout) findViewById(layoutId)).setError(null);
                if (campo == peso) {
                    actualizarPreview();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private String texto(EditText campo) {
        return campo.getText().toString().trim();
    }

    private Ciudad ciudad(Spinner spinner) {
        return Ciudad.values()[spinner.getSelectedItemPosition()];
    }

    private Servicio servicioElegido() {
        return servicio.getCheckedRadioButtonId() == R.id.radioExpress ? Servicio.EXPRESS : Servicio.ESTANDAR;
    }

    private void actualizarPreview() {
        Double kg = Tarifador.parsearPeso(texto(peso));
        if (kg == null || origen.getSelectedItemPosition() < 0 || destino.getSelectedItemPosition() < 0
                || ciudad(origen) == ciudad(destino)) {
            preview.setVisibility(View.GONE);
            return;
        }
        int km = Tarifador.distanciaKm(ciudad(origen), ciudad(destino));
        Servicio tipo = servicioElegido();
        int dias = Tarifador.diasEstimados(km, tipo);
        NumberFormat moneda = NumberFormat.getCurrencyInstance(AR);
        moneda.setMaximumFractionDigits(0);
        preview.setText(getString(R.string.envio_preview, NumberFormat.getIntegerInstance(AR).format(km),
                moneda.format(Tarifador.precio(kg, km, tipo)),
                getResources().getQuantityString(R.plurals.detalle_dias, dias, dias)));
        preview.setVisibility(View.VISIBLE);
    }

    private boolean invalido(int layoutId, EditText campo, int mensaje) {
        ((TextInputLayout) findViewById(layoutId)).setError(getString(mensaje));
        campo.requestFocus();
        return false;
    }

    private boolean validar() {
        for (int id : new int[]{R.id.tilDestinatario, R.id.tilTelDestinatario, R.id.tilDireccion, R.id.tilPeso}) {
            ((TextInputLayout) findViewById(id)).setError(null);
        }
        errorDestino.setVisibility(View.GONE);
        boolean valido = true;
        if (Tarifador.parsearPeso(texto(peso)) == null) {
            valido = invalido(R.id.tilPeso, peso, R.string.error_envio_peso);
        }
        if (ciudad(origen) == ciudad(destino)) {
            errorDestino.setVisibility(View.VISIBLE);
            destino.requestFocus();
            valido = false;
        }
        if (texto(direccion).isEmpty()) {
            valido = invalido(R.id.tilDireccion, direccion, R.string.error_campo_obligatorio);
        }
        if (!RegistroActivity.esTelefonoValido(texto(telefono))) {
            valido = invalido(R.id.tilTelDestinatario, telefono, R.string.error_telefono_invalido);
        }
        if (texto(destinatario).isEmpty()) {
            valido = invalido(R.id.tilDestinatario, destinatario, R.string.error_campo_obligatorio);
        }
        return valido;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Sesion.actual() != null && guardar != null) {
            escuchaSesion = Sesion.vigilar(this, () -> {
                if (envio != null && !editable(envio)) {
                    noEditable();
                } else {
                    habilitar();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escuchaSesion != null) {
            escuchaSesion.remove();
            escuchaSesion = null;
        }
    }

    private boolean activa() {
        return !isFinishing() && !isDestroyed();
    }

    private boolean editable(Envio e) {
        Usuario u = Sesion.actual();
        return u != null && puedeEditar(e.getEstadoEnum(), u.esOperador(), u.getUid().equals(e.getClienteUid()));
    }

    private void noEditable() {
        Toast.makeText(this, R.string.error_envio_no_editable, Toast.LENGTH_LONG).show();
        finish();
    }

    private void habilitar() {
        boolean disponible = !cargando && !guardando;
        boolean rutaEditable = tracking == null || (envio != null
                && puedeEditarRuta(envio.getEstadoEnum(), Sesion.esOperador()));
        destinatario.setEnabled(disponible);
        telefono.setEnabled(disponible);
        direccion.setEnabled(disponible);
        origen.setEnabled(disponible && rutaEditable);
        destino.setEnabled(disponible && rutaEditable);
        peso.setEnabled(disponible && rutaEditable);
        descripcion.setEnabled(disponible && rutaEditable);
        servicio.setEnabled(disponible && rutaEditable);
        for (int i = 0; i < servicio.getChildCount(); i++) {
            servicio.getChildAt(i).setEnabled(disponible && rutaEditable);
        }
        guardar.setEnabled(disponible && (tracking == null || envio != null));
        progreso.setVisibility(disponible ? View.GONE : View.VISIBLE);
        avisoRetirado.setVisibility(envio != null && !rutaEditable ? View.VISIBLE : View.GONE);
    }

    private void cargarEnvio() {
        cargando = true;
        habilitar();
        EnviosFirestore.obtener(tracking, new Resultado<Envio>() {
            @Override
            public void ok(Envio e) {
                if (!activa()) {
                    return;
                }
                cargando = false;
                if (e == null) {
                    Toast.makeText(EnvioFormActivity.this, R.string.detalle_eliminado, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (!editable(e)) {
                    noEditable();
                    return;
                }
                envio = e;
                destinatario.setText(e.getDestinatarioNombre());
                telefono.setText(e.getDestinatarioTelefono());
                direccion.setText(e.getDireccion());
                mostrarPaquete(e);
                habilitar();
            }

            @Override
            public void error(Exception e) {
                if (!activa()) {
                    return;
                }
                Toast.makeText(EnvioFormActivity.this,
                        UsuariosFirestore.mensajeError(EnvioFormActivity.this, e), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void mostrarPaquete(Envio e) {
        origen.setSelection(e.getOrigenEnum().ordinal());
        destino.setSelection(e.getDestinoEnum().ordinal());
        peso.setText(Double.toString(e.getPesoKg()).replace('.', ','));
        servicio.check(e.getServicioEnum() == Servicio.EXPRESS ? R.id.radioExpress : R.id.radioEstandar);
        descripcion.setText(e.getDescripcion());
        actualizarPreview();
    }

    private void aplicarCampos(Envio e, boolean paqueteEditable) {
        e.setDestinatarioNombre(texto(destinatario));
        e.setDestinatarioTelefono(texto(telefono));
        e.setDireccion(texto(direccion));
        if (paqueteEditable) {
            e.setOrigen(ciudad(origen).name());
            e.setDestino(ciudad(destino).name());
            e.setPesoKg(Tarifador.parsearPeso(texto(peso)));
            e.setServicio(servicioElegido().name());
            e.setDescripcion(texto(descripcion));
        }
    }

    private void guardar() {
        if (cargando || guardando || !validar()) {
            return;
        }
        guardando = true;
        habilitar();
        if (tracking != null) {
            actualizar();
            return;
        }
        Envio borrador = new Envio();
        aplicarCampos(borrador, true);
        EnviosFirestore.crear(this, borrador, new Resultado<String>() {
            @Override
            public void ok(String id) {
                if (!activa()) {
                    return;
                }
                startActivity(new Intent(EnvioFormActivity.this, EnvioDetalleActivity.class).putExtra("ENVIO_ID", id));
                finish();
            }

            @Override
            public void error(Exception e) {
                mostrarError(e);
            }
        });
    }

    private void actualizar() {
        EnviosFirestore.obtener(tracking, new Resultado<Envio>() {
            @Override
            public void ok(Envio actual) {
                if (!activa()) {
                    return;
                }
                if (actual == null) {
                    Toast.makeText(EnvioFormActivity.this, R.string.detalle_eliminado, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (!editable(actual)) {
                    noEditable();
                    return;
                }
                boolean paqueteEditable = puedeEditarRuta(actual.getEstadoEnum(), Sesion.esOperador());
                boolean cambioPermiso = !paqueteEditable
                        && puedeEditarRuta(envio.getEstadoEnum(), Sesion.esOperador());
                envio = actual;
                if (cambioPermiso) {
                    mostrarPaquete(actual);
                    guardando = false;
                    habilitar();
                    Snackbar.make(findViewById(R.id.main), R.string.envio_aviso_retirado, Snackbar.LENGTH_LONG).show();
                    return;
                }
                aplicarCampos(actual, paqueteEditable);
                EnviosFirestore.actualizar(actual, new Resultado<Void>() {
                    @Override
                    public void ok(Void v) {
                        if (activa()) {
                            finish();
                        }
                    }

                    @Override
                    public void error(Exception e) {
                        mostrarError(e);
                    }
                });
            }

            @Override
            public void error(Exception e) {
                mostrarError(e);
            }
        });
    }

    private void mostrarError(Exception e) {
        if (!activa()) {
            return;
        }
        guardando = false;
        habilitar();
        Snackbar.make(findViewById(R.id.main), UsuariosFirestore.mensajeError(this, e), Snackbar.LENGTH_LONG).show();
    }

}
