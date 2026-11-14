package com.example.tortugaturbo;

import androidx.annotation.StringRes;

public enum Servicio {
    ESTANDAR(R.string.servicio_estandar),
    EXPRESS(R.string.servicio_express);

    private final int etiqueta;

    Servicio(@StringRes int etiqueta) {
        this.etiqueta = etiqueta;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
