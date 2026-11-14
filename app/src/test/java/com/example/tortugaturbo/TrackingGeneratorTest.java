package com.example.tortugaturbo;

import static com.example.tortugaturbo.TrackingGenerator.*;
import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

public class TrackingGeneratorTest {
    @Test public void formatoCorrecto() { assertTrue(generar(new Random(7)).matches("^TT-\\d{9}-\\d$")); }

    @Test public void generadosSonValidos() {
        for (int seed = 0; seed < 1000; seed++) {
            assertTrue(esValido(generar(new Random(seed))));
        }
    }

    @Test public void detectaUnDigitoCambiado() {
        String t = generar(new Random(3));
        char c = t.charAt(5);
        char otro = c == '9' ? '0' : (char) (c + 1);
        assertFalse(esValido(t.substring(0, 5) + otro + t.substring(6)));
    }

    @Test public void ejemploConocido() { assertTrue(esValido("TT-482913057-6")); }
    @Test public void normalizaEspaciosYMinusculas() { assertEquals("TT-482913057-6", normalizar("  tt-482913057-6 ")); }
    @Test public void rechazaBasura() { assertFalse(esValido(null)); assertFalse(esValido("")); assertFalse(esValido("TT-12-3")); }
}
