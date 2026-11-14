package com.example.tortugaturbo;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

public class Usuario {
    public static final String ROL_CLIENTE = "CLIENTE";
    public static final String ROL_OPERADOR = "OPERADOR";

    private String uid;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String rol;
    private boolean activo;
    private Timestamp creadoEn;

    public Usuario() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @ServerTimestamp
    public Timestamp getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Timestamp creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Exclude
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean esOperador() {
        return ROL_OPERADOR.equals(rol);
    }

    public static String iniciales(Usuario u) {
        String n = u.nombre == null ? "" : u.nombre.trim();
        String a = u.apellido == null ? "" : u.apellido.trim();
        return ((n.isEmpty() ? "" : n.substring(0, 1)) + (a.isEmpty() ? "" : a.substring(0, 1))).toUpperCase();
    }
}
