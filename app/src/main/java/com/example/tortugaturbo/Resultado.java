package com.example.tortugaturbo;

public interface Resultado<T> {
    void ok(T valor);

    void error(Exception e);
}
