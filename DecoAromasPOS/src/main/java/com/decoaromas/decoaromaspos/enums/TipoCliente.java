package com.decoaromas.decoaromaspos.enums;

import lombok.Getter;

@Getter
public enum TipoCliente {
    MAYORISTA("Mayorista"),
    DETALLE("Detalle");

    private final String nombreParaUi;

    TipoCliente(String nombreParaUi) {
        this.nombreParaUi = nombreParaUi;
    }
}
