package com.example.tortugaturbo;

public enum MotivoIncidente {
    AUSENTE(R.string.motivo_ausente),
    DIRECCION(R.string.motivo_direccion),
    CLIMA(R.string.motivo_clima);

    private final int texto;

    MotivoIncidente(int texto) {
        this.texto = texto;
    }

    public int getTexto() {
        return texto;
    }
}
