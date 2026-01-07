package com.decoaromas.decoaromaspos.enums;

import lombok.Getter;

@Getter
public enum MedioPago {
    MERCADO_PAGO("Mercado Pago"),
    BCI("Bci"),
    TRANSFERENCIA("Transferencia"),
    EFECTIVO("Efectivo"),
    BOTON_DE_PAGO("Botón De Pago"),
    POST("Post");

    private final String nombreParaUi;

    MedioPago(String nombreParaUi) {
        this.nombreParaUi = nombreParaUi;
    }
}
