package com.example.tortugaturbo;

import static com.example.tortugaturbo.Ciudad.*;
import static com.example.tortugaturbo.Servicio.*;
import static com.example.tortugaturbo.Tarifador.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class TarifadorTest {
    @Test public void buenosAiresCordobaEntre800y900() { int km = distanciaKm(BUENOS_AIRES, CORDOBA); assertTrue(km >= 800 && km <= 900); }
    @Test public void distanciaSimetrica() { assertEquals(distanciaKm(SALTA, USHUAIA), distanciaKm(USHUAIA, SALTA)); }
    @Test(expected = IllegalArgumentException.class) public void mismaCiudadFalla() { distanciaKm(ROSARIO, ROSARIO); }
    @Test public void expressMasCaroQueEstandar() { assertTrue(precio(2, 500, EXPRESS) > precio(2, 500, ESTANDAR)); }
    @Test public void precioRedondeadoA10() { assertEquals(0, precio(1.3, 777, ESTANDAR) % 10, 0.0001); }
    @Test public void precioFormula() { assertEquals(3700, precio(1, 600, ESTANDAR), 0.0001); }
    @Test public void diasEstandar() { assertEquals(3, diasEstimados(800, ESTANDAR)); }
    @Test public void diasExpressMinimoUno() { assertEquals(1, diasEstimados(50, EXPRESS)); }
    @Test public void pesoConComa() { assertEquals(1.5, parsearPeso(" 1,5 "), 0.0001); }
    @Test public void pesoInvalido() { assertNull(parsearPeso("abc")); assertNull(parsearPeso("0")); assertNull(parsearPeso("31")); assertNull(parsearPeso("1e1")); assertNull(parsearPeso("5f")); assertNull(parsearPeso("0x1p3")); assertNull(parsearPeso("+5")); }
}
