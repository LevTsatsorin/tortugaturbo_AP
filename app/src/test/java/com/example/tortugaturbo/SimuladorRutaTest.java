package com.example.tortugaturbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class SimuladorRutaTest {

    @Test
    public void caminoCompletoLlegaAEntregado() {
        for (long seed = 0; seed < 50; seed++) {
            Random random = new Random(seed);
            EstadoEnvio estado = EstadoEnvio.CREADO;
            boolean intento = false;
            int pasos = 0;
            while (estado != EstadoEnvio.ENTREGADO && pasos <= 8) {
                SimuladorRuta.Paso paso = SimuladorRuta.siguiente(estado, intento, random);
                assertNotNull(paso);
                estado = paso.estado;
                if (estado == EstadoEnvio.INTENTO_FALLIDO) {
                    intento = true;
                }
                pasos++;
            }
            assertEquals(EstadoEnvio.ENTREGADO, estado);
            assertTrue(pasos <= 8);
        }
    }

    @Test
    public void incidenteComoMaximoUnaVez() {
        int conIncidente = 0;
        for (long seed = 0; seed < 1000; seed++) {
            Random random = new Random(seed * 2654435761L);
            EstadoEnvio estado = EstadoEnvio.CREADO;
            boolean intento = false;
            int incidentes = 0;
            while (true) {
                SimuladorRuta.Paso paso = SimuladorRuta.siguiente(estado, intento, random);
                if (paso == null) {
                    break;
                }
                estado = paso.estado;
                if (estado == EstadoEnvio.INTENTO_FALLIDO) {
                    intento = true;
                    incidentes++;
                }
            }
            assertTrue(incidentes <= 1);
            conIncidente += incidentes;
        }
        assertTrue(conIncidente >= 200 && conIncidente <= 400);
    }

    @Test
    public void enRepartoConIncidentePrevioEntrega() {
        assertEquals(EstadoEnvio.ENTREGADO,
                SimuladorRuta.siguiente(EstadoEnvio.EN_REPARTO, true, new Random(1)).estado);
    }

    @Test
    public void intentoFallidoVuelveAReparto() {
        assertEquals(EstadoEnvio.EN_REPARTO,
                SimuladorRuta.siguiente(EstadoEnvio.INTENTO_FALLIDO, true, new Random(1)).estado);
    }

    @Test
    public void estadosFinalesNoTienenSiguiente() {
        Random random = new Random(1);
        assertNull(SimuladorRuta.siguiente(EstadoEnvio.ENTREGADO, false, random));
        assertNull(SimuladorRuta.siguiente(EstadoEnvio.CANCELADO, false, random));
    }

    @Test
    public void demorasEnRango() {
        Random random = new Random(7);
        for (int i = 0; i < 1000; i++) {
            int express = SimuladorRuta.demoraSegundos(Servicio.EXPRESS, random);
            int estandar = SimuladorRuta.demoraSegundos(Servicio.ESTANDAR, random);
            assertTrue(express >= 30 && express <= 120);
            assertTrue(estandar >= 60 && estandar <= 300);
        }
    }

    @Test
    public void motivoSoloEnIncidente() {
        for (long seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            for (EstadoEnvio estado : EstadoEnvio.values()) {
                SimuladorRuta.Paso paso = SimuladorRuta.siguiente(estado, false, random);
                if (paso != null) {
                    assertEquals(paso.estado == EstadoEnvio.INTENTO_FALLIDO, paso.motivo != null);
                }
            }
        }
    }

    @Test
    public void ciudadEventoSegunTabla() {
        assertEquals("A", SimuladorRuta.ciudadEvento(EstadoEnvio.CREADO, "A", "B"));
        assertEquals("A", SimuladorRuta.ciudadEvento(EstadoEnvio.RETIRADO, "A", "B"));
        assertEquals("A", SimuladorRuta.ciudadEvento(EstadoEnvio.EN_CENTRO_ORIGEN, "A", "B"));
        assertNull(SimuladorRuta.ciudadEvento(EstadoEnvio.EN_TRANSITO, "A", "B"));
        assertNull(SimuladorRuta.ciudadEvento(EstadoEnvio.CANCELADO, "A", "B"));
        assertEquals("B", SimuladorRuta.ciudadEvento(EstadoEnvio.EN_CENTRO_DESTINO, "A", "B"));
        assertEquals("B", SimuladorRuta.ciudadEvento(EstadoEnvio.EN_REPARTO, "A", "B"));
        assertEquals("B", SimuladorRuta.ciudadEvento(EstadoEnvio.INTENTO_FALLIDO, "A", "B"));
        assertEquals("B", SimuladorRuta.ciudadEvento(EstadoEnvio.ENTREGADO, "A", "B"));
    }
}
