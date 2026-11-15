package com.example.tortugaturbo;

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

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
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
        guardar.setOnClickListener(v -> validar());
        actualizarPreview();
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
}
