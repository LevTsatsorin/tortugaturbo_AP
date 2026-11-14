package com.example.tortugaturbo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class EnviosAdapter extends RecyclerView.Adapter<EnviosAdapter.Holder> {

    public interface Click {
        void alTocar(Envio e);
    }

    private final List<Envio> items = new ArrayList<>();
    private final Click click;

    public EnviosAdapter(Click click) {
        this.click = click;
    }

    public void setItems(List<Envio> lista) {
        items.clear();
        items.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_envio, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Envio e = items.get(position);
        Context c = h.itemView.getContext();
        h.tracking.setText(e.getTracking());
        h.ruta.setText(c.getString(R.string.home_ruta,
                Notificador.etiquetaCiudad(c, e.getOrigen()), Notificador.etiquetaCiudad(c, e.getDestino())));
        boolean oper = Sesion.esOperador() && e.getClienteNombre() != null;
        h.cliente.setVisibility(oper ? View.VISIBLE : View.GONE);
        if (oper) {
            h.cliente.setText(c.getString(R.string.home_cliente, e.getClienteNombre()));
        }
        EstadoEnvio estado;
        try {
            estado = e.getEstadoEnum();
        } catch (IllegalArgumentException ex) {
            estado = null;
        }
        if (estado == null) {
            h.estado.setVisibility(View.GONE);
            h.proximo.setVisibility(View.GONE);
            h.progreso.setProgress(0);
            h.franja.setBackgroundColor(0);
        } else {
            int color = ContextCompat.getColor(c, estado.getColor());
            h.estado.setVisibility(View.VISIBLE);
            h.estado.setText(estado.getEtiqueta());
            h.estado.setTextColor(color);
            h.estado.setChipBackgroundColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0x33)));
            h.progreso.setProgress(estado.getPaso());
            h.progreso.setIndicatorColor(color);
            h.franja.setBackgroundColor(color);
            String prox = Notificador.textoProximo(c, e, estado);
            h.proximo.setVisibility(prox == null ? View.GONE : View.VISIBLE);
            h.proximo.setText(prox);
        }
        h.itemView.setOnClickListener(v -> click.alTocar(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tracking;
        final TextView ruta;
        final TextView cliente;
        final Chip estado;
        final TextView proximo;
        final LinearProgressIndicator progreso;
        final View franja;

        Holder(@NonNull View v) {
            super(v);
            tracking = v.findViewById(R.id.itemTracking);
            ruta = v.findViewById(R.id.itemRuta);
            cliente = v.findViewById(R.id.itemCliente);
            estado = v.findViewById(R.id.itemEstado);
            proximo = v.findViewById(R.id.itemProximo);
            progreso = v.findViewById(R.id.itemProgreso);
            franja = v.findViewById(R.id.itemFranja);
        }
    }
}
