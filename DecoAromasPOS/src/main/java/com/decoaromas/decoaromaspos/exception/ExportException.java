package com.decoaromas.decoaromaspos.exception;

/**
 * Excepción personalizada para errores durante la generación de archivos (CSV, PDF, etc.).
 */
public class ExportException extends RuntimeException {
    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}