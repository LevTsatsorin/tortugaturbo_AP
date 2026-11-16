package com.example.tortugaturbo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RegistroValidacionTest {

    @Test
    public void nombre() {
        assertTrue(RegistroActivity.esNombreValido("José María"));
        assertTrue(RegistroActivity.esNombreValido("  Ana "));
        assertFalse(RegistroActivity.esNombreValido("A"));
        assertFalse(RegistroActivity.esNombreValido("Ana3"));
        assertFalse(RegistroActivity.esNombreValido("a".repeat(31)));
        assertFalse(RegistroActivity.esNombreValido(""));
        assertFalse(RegistroActivity.esNombreValido("   "));
        assertFalse(RegistroActivity.esNombreValido(null));
    }

    @Test
    public void telefono() {
        assertTrue(RegistroActivity.esTelefonoValido("12345678"));
        assertTrue(RegistroActivity.esTelefonoValido("123456789012345"));
        assertFalse(RegistroActivity.esTelefonoValido("1234567"));
        assertFalse(RegistroActivity.esTelefonoValido("1234567890123456"));
        assertFalse(RegistroActivity.esTelefonoValido("1234567a"));
        assertFalse(RegistroActivity.esTelefonoValido(null));
    }

    @Test
    public void password() {
        assertTrue(RegistroActivity.esPasswordValida("abc123"));
        assertFalse(RegistroActivity.esPasswordValida("abcdef"));
        assertFalse(RegistroActivity.esPasswordValida("123456"));
        assertFalse(RegistroActivity.esPasswordValida("ab12"));
        assertFalse(RegistroActivity.esPasswordValida(null));
    }
}
