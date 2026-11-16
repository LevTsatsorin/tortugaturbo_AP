package com.example.tortugaturbo;

public enum EstadoEnvio {
    CREADO(R.string.estado_creado, R.string.estado_evento_creado, R.color.estado_creado, 1),
    RETIRADO(R.string.estado_retirado, R.string.estado_evento_retirado, R.color.estado_retirado, 2),
    EN_CENTRO_ORIGEN(R.string.estado_en_centro_origen, R.string.estado_evento_en_centro_origen, R.color.estado_en_centro_origen, 3),
    EN_TRANSITO(R.string.estado_en_transito, R.string.estado_evento_en_transito, R.color.estado_en_transito, 4),
    EN_CENTRO_DESTINO(R.string.estado_en_centro_destino, R.string.estado_evento_en_centro_destino, R.color.estado_en_centro_destino, 5),
    EN_REPARTO(R.string.estado_en_reparto, R.string.estado_evento_en_reparto, R.color.estado_en_reparto, 6),
    INTENTO_FALLIDO(R.string.estado_intento_fallido, R.string.estado_evento_intento_fallido, R.color.estado_intento_fallido, 6),
    ENTREGADO(R.string.estado_entregado, R.string.estado_evento_entregado, R.color.estado_entregado, 7),
    CANCELADO(R.string.estado_cancelado, R.string.estado_evento_cancelado, R.color.estado_cancelado, 0);

    private final int etiqueta;
    private final int eventoTexto;
    private final int color;
    private final int paso;

    EstadoEnvio(int etiqueta, int eventoTexto, int color, int paso) {
        this.etiqueta = etiqueta;
        this.eventoTexto = eventoTexto;
        this.color = color;
        this.paso = paso;
    }

    public int getEtiqueta() {
        return etiqueta;
    }

    public int getEventoTexto() {
        return eventoTexto;
    }

    public int getColor() {
        return color;
    }

    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    public int getPaso() {
        return paso;
    }
}
