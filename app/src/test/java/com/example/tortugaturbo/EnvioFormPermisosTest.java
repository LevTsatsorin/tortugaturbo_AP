package com.example.tortugaturbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EnvioFormPermisosTest {

    @Test
    public void clienteEditaDestinatarioSoloAntesDelReparto() {
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            boolean permitido = estado == EstadoEnvio.CREADO || estado == EstadoEnvio.RETIRADO
                    || estado == EstadoEnvio.EN_CENTRO_ORIGEN || estado == EstadoEnvio.EN_TRANSITO
                    || estado == EstadoEnvio.EN_CENTRO_DESTINO;
            assertEquals(estado.name(), permitido, EnvioFormActivity.puedeEditar(estado, false, true));
        }
    }

    @Test
    public void clienteNuncaEditaEnviosAjenos() {
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            assertFalse(EnvioFormActivity.puedeEditar(estado, false, false));
        }
    }

    @Test
    public void operadorEditaCualquierEnvioNoFinal() {
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            boolean permitido = estado != EstadoEnvio.ENTREGADO && estado != EstadoEnvio.CANCELADO;
            assertEquals(estado.name(), permitido, EnvioFormActivity.puedeEditar(estado, true, false));
            assertEquals(estado.name(), permitido, EnvioFormActivity.puedeEditarRuta(estado, true));
        }
    }

    @Test
    public void clienteEditaPaqueteSoloEnCreado() {
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            assertEquals(estado.name(), estado == EstadoEnvio.CREADO,
                    EnvioFormActivity.puedeEditarRuta(estado, false));
        }
    }

    @Test
    public void estadoDesconocidoNoPermiteEditar() {
        assertFalse(EnvioFormActivity.puedeEditar(null, true, true));
        assertFalse(EnvioFormActivity.puedeEditarRuta(null, true));
        assertTrue(EnvioFormActivity.puedeEditar(EstadoEnvio.CREADO, false, true));
    }
}
