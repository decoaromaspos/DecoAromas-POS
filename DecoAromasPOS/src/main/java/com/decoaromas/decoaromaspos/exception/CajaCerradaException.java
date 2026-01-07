package com.decoaromas.decoaromaspos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "No existe una caja abierta para realizar la venta")
public class CajaCerradaException extends RuntimeException {
    public CajaCerradaException(String message) {
        super(message);
    }
}