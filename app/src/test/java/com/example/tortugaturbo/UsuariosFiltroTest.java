package com.example.tortugaturbo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UsuariosFiltroTest {

    private static Usuario u(String nombre, String apellido, String email, String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    @Test
    public void acentos() {
        Usuario jose = u("José", "Pérez", "jp@mail.com", Usuario.ROL_CLIENTE, true);
        assertTrue(UsuariosAdapter.coincide(jose, "jose", UsuariosAdapter.FILTRO_TODOS));
        assertTrue(UsuariosAdapter.coincide(jose, "PEREZ", UsuariosAdapter.FILTRO_TODOS));
        assertFalse(UsuariosAdapter.coincide(jose, "ana", UsuariosAdapter.FILTRO_TODOS));
    }

    @Test
    public void email() {
        Usuario x = u("Ana", "Gómez", "ana.g@mail.com", Usuario.ROL_CLIENTE, true);
        assertTrue(UsuariosAdapter.coincide(x, "g@mail", UsuariosAdapter.FILTRO_TODOS));
        assertTrue(UsuariosAdapter.coincide(x, "", UsuariosAdapter.FILTRO_TODOS));
    }

    @Test
    public void filtros() {
        Usuario op = u("Ana", "Gómez", "a@m.com", Usuario.ROL_OPERADOR, true);
        Usuario off = u("Luis", "Díaz", "l@m.com", Usuario.ROL_CLIENTE, false);
        assertTrue(UsuariosAdapter.coincide(op, "", UsuariosAdapter.FILTRO_OPERADORES));
        assertFalse(UsuariosAdapter.coincide(off, "", UsuariosAdapter.FILTRO_OPERADORES));
        assertTrue(UsuariosAdapter.coincide(off, "", UsuariosAdapter.FILTRO_DESACTIVADOS));
        assertFalse(UsuariosAdapter.coincide(op, "", UsuariosAdapter.FILTRO_DESACTIVADOS));
        assertTrue(UsuariosAdapter.coincide(off, "", UsuariosAdapter.FILTRO_CLIENTES));
        assertFalse(UsuariosAdapter.coincide(op, "", UsuariosAdapter.FILTRO_CLIENTES));
    }
}
