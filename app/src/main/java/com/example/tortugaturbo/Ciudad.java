package com.example.tortugaturbo;

import androidx.annotation.StringRes;

public enum Ciudad {
    BUENOS_AIRES(-34.6037, -58.3816, R.string.ciudad_buenos_aires),
    LA_PLATA(-34.9205, -57.9536, R.string.ciudad_la_plata),
    MAR_DEL_PLATA(-38.0055, -57.5426, R.string.ciudad_mar_del_plata),
    ROSARIO(-32.9442, -60.6505, R.string.ciudad_rosario),
    CORDOBA(-31.4201, -64.1888, R.string.ciudad_cordoba),
    MENDOZA(-32.8895, -68.8458, R.string.ciudad_mendoza),
    TUCUMAN(-26.8083, -65.2176, R.string.ciudad_tucuman),
    SALTA(-24.7821, -65.4232, R.string.ciudad_salta),
    NEUQUEN(-38.9516, -68.0591, R.string.ciudad_neuquen),
    BARILOCHE(-41.1335, -71.3103, R.string.ciudad_bariloche),
    USHUAIA(-54.8019, -68.3030, R.string.ciudad_ushuaia);

    private final double lat;
    private final double lng;
    private final int etiqueta;

    Ciudad(double lat, double lng, @StringRes int etiqueta) {
        this.lat = lat;
        this.lng = lng;
        this.etiqueta = etiqueta;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @StringRes
    public int getEtiqueta() {
        return etiqueta;
    }
}
