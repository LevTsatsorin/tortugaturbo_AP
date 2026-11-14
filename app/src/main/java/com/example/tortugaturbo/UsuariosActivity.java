package com.example.tortugaturbo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.chip.ChipGroup;

import java.util.Collections;
import java.util.List;

public class UsuariosActivity extends AppCompatActivity {

    private UsuariosAdapter adapter;
    private ProgressBar progreso;
    private boolean valido;
    private ListenerRegistration escuchaSesion;
    private EditText campoBuscar;
    private ChipGroup grupoFiltro;
    private RecyclerView lista;
    private TextView textoVacio;
    private int filtro = UsuariosAdapter.FILTRO_TODOS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        if (Sesion.actual() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!Sesion.esOperador()) {
            finish();
            return;
        }
        valido = true;
        setContentView(R.layout.activity_usuarios);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        progreso = findViewById(R.id.progreso);
        adapter = new UsuariosAdapter(u -> {
            Intent i = new Intent(this, PerfilActivity.class);
            i.putExtra("USUARIO_ID", u.getUid());
            startActivity(i);
        });
        lista = findViewById(R.id.lista);
        textoVacio = findViewById(R.id.textoVacio);
        campoBuscar = findViewById(R.id.campoBuscar);
        grupoFiltro = findViewById(R.id.grupoFiltro);
        campoBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                aplicar();
            }
        });
        findViewById(R.id.tileTotal).setOnClickListener(v -> grupoFiltro.check(R.id.chipTodos));
        findViewById(R.id.tileClientes).setOnClickListener(v -> grupoFiltro.check(R.id.chipClientes));
        findViewById(R.id.tileOperadores).setOnClickListener(v -> grupoFiltro.check(R.id.chipOperadores));
        findViewById(R.id.tileDesactivados).setOnClickListener(v -> grupoFiltro.check(R.id.chipDesactivados));
        grupoFiltro.setOnCheckedStateChangeListener((g, ids) -> {
            int id = ids.isEmpty() ? R.id.chipTodos : ids.get(0);
            if (id == R.id.chipClientes) {
                filtro = UsuariosAdapter.FILTRO_CLIENTES;
            } else if (id == R.id.chipOperadores) {
                filtro = UsuariosAdapter.FILTRO_OPERADORES;
            } else if (id == R.id.chipDesactivados) {
                filtro = UsuariosAdapter.FILTRO_DESACTIVADOS;
            } else {
                filtro = UsuariosAdapter.FILTRO_TODOS;
            }
            aplicar();
        });
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escuchaSesion != null) {
            escuchaSesion.remove();
            escuchaSesion = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        escuchaSesion = Sesion.vigilar(this, () -> {
            if (!Sesion.esOperador()) {
                finish();
            }
        });
        if (!valido) {
            return;
        }
        progreso.setVisibility(View.VISIBLE);
        UsuariosFirestore.listarTodos(new Resultado<List<Usuario>>() {
            @Override
            public void ok(List<Usuario> datos) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                Collections.sort(datos, (a, b) -> {
                    int c = String.valueOf(a.getApellido()).compareToIgnoreCase(String.valueOf(b.getApellido()));
                    return c != 0 ? c : String.valueOf(a.getNombre()).compareToIgnoreCase(String.valueOf(b.getNombre()));
                });
                adapter.setTodos(datos);
                estadisticas(datos);
                aplicar();
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                progreso.setVisibility(View.GONE);
                Toast.makeText(UsuariosActivity.this, UsuariosFirestore.mensajeError(UsuariosActivity.this, e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void aplicar() {
        int n = adapter.filtrar(campoBuscar.getText().toString(), filtro);
        textoVacio.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        lista.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    }

    private void estadisticas(List<Usuario> datos) {
        int operadores = 0;
        int desactivados = 0;
        for (Usuario u : datos) {
            if (u.esOperador()) {
                operadores++;
            }
            if (!u.isActivo()) {
                desactivados++;
            }
        }
        ((TextView) findViewById(R.id.numTotal)).setText(String.valueOf(datos.size()));
        ((TextView) findViewById(R.id.numClientes)).setText(String.valueOf(datos.size() - operadores));
        ((TextView) findViewById(R.id.numOperadores)).setText(String.valueOf(operadores));
        ((TextView) findViewById(R.id.numDesactivados)).setText(String.valueOf(desactivados));
    }
}
