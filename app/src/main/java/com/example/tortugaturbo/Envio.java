package com.example.tortugaturbo;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Envio {
    private String tracking;
    private String clienteUid;
    private String clienteNombre;
    private String destinatarioNombre;
    private String destinatarioTelefono;
    private String direccion;
    private String origen;
    private String destino;
    private double pesoKg;
    private String servicio;
    private String descripcion;
    private int distanciaKm;
    private double precio;
    private int diasEstimados;
    private String estado;
    private boolean pausado;
    private boolean intentoFallido;
    private Timestamp proximoPasoEn;
    private List<EventoEnvio> eventos = new ArrayList<>();
    private Timestamp creadoEn;
    private Timestamp actualizadoEn;

    public Envio() {
    }

    public String getTracking() {
        return tracking;
    }

    public void setTracking(String tracking) {
        this.tracking = tracking;
    }

    public String getClienteUid() {
        return clienteUid;
    }

    public void setClienteUid(String clienteUid) {
        this.clienteUid = clienteUid;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getDestinatarioNombre() {
        return destinatarioNombre;
    }

    public void setDestinatarioNombre(String destinatarioNombre) {
        this.destinatarioNombre = destinatarioNombre;
    }

    public String getDestinatarioTelefono() {
        return destinatarioTelefono;
    }

    public void setDestinatarioTelefono(String destinatarioTelefono) {
        this.destinatarioTelefono = destinatarioTelefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(int distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getDiasEstimados() {
        return diasEstimados;
    }

    public void setDiasEstimados(int diasEstimados) {
        this.diasEstimados = diasEstimados;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isPausado() {
        return pausado;
    }

    public void setPausado(boolean pausado) {
        this.pausado = pausado;
    }

    public boolean isIntentoFallido() {
        return intentoFallido;
    }

    public void setIntentoFallido(boolean intentoFallido) {
        this.intentoFallido = intentoFallido;
    }

    public Timestamp getProximoPasoEn() {
        return proximoPasoEn;
    }

    public void setProximoPasoEn(Timestamp proximoPasoEn) {
        this.proximoPasoEn = proximoPasoEn;
    }

    public List<EventoEnvio> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoEnvio> eventos) {
        this.eventos = eventos;
    }

    public Timestamp getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Timestamp creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Timestamp getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Timestamp actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    @Exclude
    public EstadoEnvio getEstadoEnum() {
        return estado == null ? null : EstadoEnvio.valueOf(estado);
    }

    @Exclude
    public Ciudad getOrigenEnum() {
        return origen == null ? null : Ciudad.valueOf(origen);
    }

    @Exclude
    public Ciudad getDestinoEnum() {
        return destino == null ? null : Ciudad.valueOf(destino);
    }

    @Exclude
    public Servicio getServicioEnum() {
        return servicio == null ? null : Servicio.valueOf(servicio);
    }
}
