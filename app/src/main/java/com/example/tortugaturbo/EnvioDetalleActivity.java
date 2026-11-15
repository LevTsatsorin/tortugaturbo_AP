package com.example.tortugaturbo;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EnvioDetalleActivity extends AppCompatActivity {

    private static final Locale AR = new Locale("es", "AR");

    private String tracking;
    private Envio envio;
    private ListenerRegistration escucha;
    private ListenerRegistration escuchaSesion;
    private Bitmap qr;
    private boolean ocupado;
    private boolean eliminando;
    private AlertDialog dialogo;
    private MaterialToolbar toolbar;
    private Chip chipEstado;
    private LinearProgressIndicator progreso;
    private TextView textoPaso;
    private TextView textoRuta;
    private TextView textoServicio;
    private TextView textoProximo;
    private TextView textoEtiqueta;
    private ImageView imagenQr;
    private LinearLayout timeline;
    private View cardAcciones;
    private View filaOperador;
    private Button botonEditar;
    private Button botonCancelar;
    private Button botonEliminar;
    private Button botonPausar;
    private Button botonAvanzar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        if (Sesion.actual() == null) {
            startActivity(new Intent(this, LoginActivity.class).putExtra("ENVIO_ID", getIntent().getStringExtra("ENVIO_ID")));
            finish();
            return;
        }
        tracking = getIntent().getStringExtra("ENVIO_ID");
        if (tracking == null || tracking.isEmpty()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_envio_detalle);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        toolbar = findViewById(R.id.detalleTracking);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(tracking);
        chipEstado = findViewById(R.id.detalleEstado);
        progreso = findViewById(R.id.detalleProgreso);
        textoPaso = findViewById(R.id.detallePaso);
        textoRuta = findViewById(R.id.detalleRuta);
        textoServicio = findViewById(R.id.detalleServicioPrecio);
        textoProximo = findViewById(R.id.detalleProximo);
        textoEtiqueta = findViewById(R.id.detalleEtiquetaTracking);
        imagenQr = findViewById(R.id.detalleQr);
        timeline = findViewById(R.id.detalleTimeline);
        cardAcciones = findViewById(R.id.cardAcciones);
        filaOperador = findViewById(R.id.filaOperador);
        botonEditar = findViewById(R.id.botonEditar);
        botonCancelar = findViewById(R.id.botonCancelar);
        botonEliminar = findViewById(R.id.botonEliminar);
        botonPausar = findViewById(R.id.botonPausar);
        botonAvanzar = findViewById(R.id.botonAvanzar);
        textoEtiqueta.setText(tracking);
        botonEditar.setOnClickListener(v -> startActivity(new Intent(this, EnvioFormActivity.class)
                .putExtra("ENVIO_ID", tracking)));
        botonCancelar.setOnClickListener(v -> confirmarCancelar());
        botonEliminar.setOnClickListener(v -> confirmarEliminar());
        botonPausar.setOnClickListener(v -> enviar(r -> {
            if (envio.isPausado()) {
                EnviosFirestore.reanudar(envio, r);
            } else {
                EnviosFirestore.pausar(envio, r);
            }
        }, envio.isPausado() ? R.string.detalle_reanudado : R.string.detalle_pausado));
        botonAvanzar.setOnClickListener(v -> enviar(r -> EnviosFirestore.avanzar(envio, r), R.string.detalle_avanzado));
        generarQr();
    }

    private void generarQr() {
        AppExecutors.background().execute(() -> {
            Bitmap bitmap = QrGenerator.generar(tracking, 512);
            AppExecutors.main().post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                qr = bitmap;
                imagenQr.setImageBitmap(qr);
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tracking == null || Sesion.actual() == null) {
            return;
        }
        escuchaSesion = Sesion.vigilar(this, () -> {
            if (envio != null) {
                mostrar(envio);
            }
        });
        escucha = EnviosFirestore.escuchar(tracking, new Resultado<Envio>() {
            @Override
            public void ok(Envio e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (e == null) {
                    Toast.makeText(EnvioDetalleActivity.this, eliminando ? R.string.detalle_eliminado_ok : R.string.detalle_eliminado, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                envio = e;
                mostrar(e);
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(EnvioDetalleActivity.this, UsuariosFirestore.mensajeError(EnvioDetalleActivity.this, e), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        cerrarDialogo();
        if (escuchaSesion != null) {
            escuchaSesion.remove();
            escuchaSesion = null;
        }
        if (escucha != null) {
            escucha.remove();
            escucha = null;
        }
    }

    private void cerrarDialogo() {
        if (dialogo != null) {
            dialogo.dismiss();
            dialogo = null;
        }
    }

    private void mostrar(Envio e) {
        EstadoEnvio estado;
        try {
            estado = e.getEstadoEnum();
        } catch (IllegalArgumentException ex) {
            estado = null;
        }
        if (estado == null) {
            cardAcciones.setVisibility(View.GONE);
            return;
        }
        if (estado.esFinal()) {
            cerrarDialogo();
        }
        int color = ContextCompat.getColor(this, estado.getColor());
        chipEstado.setText(estado.getEtiqueta());
        chipEstado.setTextColor(color);
        chipEstado.setChipBackgroundColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0x33)));
        boolean cancelado = estado == EstadoEnvio.CANCELADO;
        int paso = estado.getPaso();
        progreso.setProgress(paso);
        progreso.setIndicatorColor(cancelado ? ContextCompat.getColor(this, R.color.estado_cancelado)
                : ContextCompat.getColor(this, R.color.turbo_naranja));
        textoPaso.setText(getString(R.string.detalle_paso, paso));

        textoRuta.setText(getString(R.string.detalle_ruta,
                Notificador.etiquetaCiudad(this, e.getOrigen()), Notificador.etiquetaCiudad(this, e.getDestino()),
                NumberFormat.getIntegerInstance(AR).format(e.getDistanciaKm())));
        NumberFormat moneda = NumberFormat.getCurrencyInstance(AR);
        moneda.setMaximumFractionDigits(0);
        String servicio = "";
        try {
            servicio = getString(e.getServicioEnum().getEtiqueta());
        } catch (IllegalArgumentException | NullPointerException ex) {
        }
        textoServicio.setText(getString(R.string.detalle_servicio_precio, servicio, moneda.format(e.getPrecio()),
                getResources().getQuantityString(R.plurals.detalle_dias, e.getDiasEstimados(), e.getDiasEstimados())));

        String proximo = Notificador.textoProximo(this, e, estado);
        textoProximo.setVisibility(proximo == null ? View.GONE : View.VISIBLE);
        textoProximo.setText(proximo);

        mostrarTimeline(e);
        aplicarBotones(e, estado);
    }

    private void mostrarTimeline(Envio e) {
        timeline.removeAllViews();
        List<EventoEnvio> eventos = e.getEventos();
        if (eventos == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM HH:mm", AR);
        for (int i = eventos.size() - 1; i >= 0; i--) {
            EventoEnvio ev = eventos.get(i);
            View fila = inflater.inflate(R.layout.item_evento, timeline, false);
            ((TextView) fila.findViewById(R.id.eventoFecha)).setText(ev.getFecha() == null ? "" : formato.format(ev.getFecha().toDate()));
            ((TextView) fila.findViewById(R.id.eventoTexto)).setText(Notificador.textoEvento(this, e, ev));
            TextView ciudad = fila.findViewById(R.id.eventoCiudad);
            String etiqueta = Notificador.etiquetaCiudad(this, ev.getCiudad());
            ciudad.setVisibility(etiqueta == null ? View.GONE : View.VISIBLE);
            ciudad.setText(etiqueta);
            if (i == eventos.size() - 1) {
                fila.findViewById(R.id.eventoPunto).setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.turbo_naranja)));
            }
            timeline.addView(fila);
        }
    }

    private void aplicarBotones(Envio e, EstadoEnvio estado) {
        Usuario u = Sesion.actual();
        boolean duenio = u != null && e.getClienteUid() != null && e.getClienteUid().equals(u.getUid());
        boolean oper = Sesion.esOperador();
        boolean fin = estado.esFinal();
        boolean cancelable = !fin && (oper || duenio);
        boolean eliminable = oper || (duenio && (estado == EstadoEnvio.CREADO
                || estado == EstadoEnvio.ENTREGADO || estado == EstadoEnvio.CANCELADO));
        boolean editable = EnvioFormActivity.puedeEditar(estado, oper, duenio);
        botonEditar.setVisibility(editable ? View.VISIBLE : View.GONE);
        botonCancelar.setVisibility(cancelable ? View.VISIBLE : View.GONE);
        botonEliminar.setVisibility(eliminable ? View.VISIBLE : View.GONE);
        boolean pausable = oper && !fin;
        filaOperador.setVisibility(pausable ? View.VISIBLE : View.GONE);
        botonPausar.setText(e.isPausado() ? R.string.detalle_reanudar : R.string.detalle_pausar);
        botonAvanzar.setVisibility(e.isPausado() ? View.GONE : View.VISIBLE);
        cardAcciones.setVisibility(editable || cancelable || eliminable || pausable ? View.VISIBLE : View.GONE);
        habilitar();
    }

    private void habilitar() {
        botonEditar.setEnabled(!ocupado);
        botonCancelar.setEnabled(!ocupado);
        botonEliminar.setEnabled(!ocupado);
        botonPausar.setEnabled(!ocupado);
        botonAvanzar.setEnabled(!ocupado);
    }

    private void enviar(Consumer<Resultado<Void>> escritura, int aviso) {
        enviar(escritura, aviso, null);
    }

    private void enviar(Consumer<Resultado<Void>> escritura, int aviso, Runnable alOk) {
        if (envio == null || ocupado) {
            return;
        }
        ocupado = true;
        habilitar();
        escritura.accept(new Resultado<Void>() {
            @Override
            public void ok(Void v) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado = false;
                habilitar();
                if (alOk != null) {
                    alOk.run();
                } else {
                    Snackbar.make(findViewById(R.id.main), aviso, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void error(Exception e) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ocupado = false;
                eliminando = false;
                habilitar();
                Snackbar.make(findViewById(R.id.main), UsuariosFirestore.mensajeError(EnvioDetalleActivity.this, e), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void confirmarCancelar() {
        cerrarDialogo();
        dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detalle_cancelar_titulo)
                .setMessage(R.string.detalle_cancelar_mensaje)
                .setNegativeButton(R.string.accion_volver, null)
                .setPositiveButton(R.string.detalle_cancelar_confirmar,
                        (d, w) -> enviar(r -> EnviosFirestore.cancelar(this, envio, r), R.string.detalle_cancelado))
                .show();
    }

    private void confirmarEliminar() {
        cerrarDialogo();
        dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detalle_eliminar_titulo)
                .setMessage(R.string.detalle_eliminar_mensaje)
                .setNegativeButton(R.string.accion_volver, null)
                .setPositiveButton(R.string.detalle_eliminar_confirmar,
                        (d, w) -> {
                            eliminando = true;
                            enviar(r -> EnviosFirestore.eliminar(this, envio, r), R.string.detalle_eliminado_ok, () -> {
                                Toast.makeText(this, R.string.detalle_eliminado_ok, Toast.LENGTH_LONG).show();
                                finish();
                            });
                        })
                .show();
    }
}
