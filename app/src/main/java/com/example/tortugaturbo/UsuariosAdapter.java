package com.example.tortugaturbo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.Holder> {

    public interface Click {
        void alTocar(Usuario u);
    }

    public static final int FILTRO_TODOS = 0;
    public static final int FILTRO_CLIENTES = 1;
    public static final int FILTRO_OPERADORES = 2;
    public static final int FILTRO_DESACTIVADOS = 3;

    private final List<Usuario> todos = new ArrayList<>();
    private final List<Usuario> items = new ArrayList<>();
    private final Click click;

    public UsuariosAdapter(Click click) {
        this.click = click;
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    static boolean coincide(Usuario u, String consulta, int filtro) {
        switch (filtro) {
            case FILTRO_CLIENTES:
                if (u.esOperador()) {
                    return false;
                }
                break;
            case FILTRO_OPERADORES:
                if (!u.esOperador()) {
                    return false;
                }
                break;
            case FILTRO_DESACTIVADOS:
                if (u.isActivo()) {
                    return false;
                }
                break;
            default:
                break;
        }
        String q = normalizar(consulta);
        return q.isEmpty()
                || normalizar(u.getNombre()).contains(q)
                || normalizar(u.getApellido()).contains(q)
                || normalizar(u.getEmail()).contains(q);
    }

    public void setTodos(List<Usuario> nuevos) {
        todos.clear();
        todos.addAll(nuevos);
    }

    public int filtrar(String consulta, int filtro) {
        items.clear();
        for (Usuario u : todos) {
            if (coincide(u, consulta, filtro)) {
                items.add(u);
            }
        }
        notifyDataSetChanged();
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Usuario u = items.get(position);
        Context c = h.itemView.getContext();
        h.nombre.setText(u.getNombreCompleto());
        h.avatar.setText(Usuario.iniciales(u));
        h.detalle.setText(u.getEmail());
        h.chipRol.setText(u.esOperador() ? R.string.perfil_rol_operador : R.string.perfil_rol_cliente);
        h.chipActivo.setText(u.isActivo() ? R.string.usuarios_activa : R.string.usuarios_desactivada);
        h.chipActivo.setChipBackgroundColorResource(u.isActivo() ? R.color.chip_activa_fondo : R.color.chip_desactivada_fondo);
        h.chipActivo.setTextColor(c.getColor(u.isActivo() ? R.color.tortuga_verde : R.color.peligro));
        Usuario yo = Sesion.actual();
        h.tu.setVisibility(yo != null && u.getUid() != null && u.getUid().equals(yo.getUid()) ? View.VISIBLE : View.GONE);
        h.itemView.setAlpha(u.isActivo() ? 1f : 0.6f);
        h.itemView.setOnClickListener((View v) -> click.alTocar(u));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView detalle;
        final TextView avatar;
        final TextView tu;
        final Chip chipRol;
        final Chip chipActivo;

        Holder(View v) {
            super(v);
            nombre = v.findViewById(R.id.textoNombre);
            detalle = v.findViewById(R.id.textoDetalle);
            avatar = v.findViewById(R.id.avatar);
            tu = v.findViewById(R.id.textoTu);
            chipRol = v.findViewById(R.id.chipRol);
            chipActivo = v.findViewById(R.id.chipActivo);
        }
    }
}
