package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.model.Producto;
import org.springframework.stereotype.Service;

@Service
public class CalculoPrecioService {

    private static final double TOLERANCIA_DECIMAL = 0.01;

    /**
     * Determina el precio unitario basado en el tipo de cliente.
     */
    public Double determinarPrecioUnitario(Producto producto, TipoCliente tipoCliente) {
        return (tipoCliente == TipoCliente.MAYORISTA)
                ? producto.getPrecioMayorista()
                : producto.getPrecioDetalle();
    }

    /**
     * Calcula el monto de un descuento, ya sea porcentaje o fijo.
     * @param precioBase El precio sobre el cual aplicar el descuento (ej.: precio unitario o total bruto)
     * @param valorDescuento El valor del request (ej.: 10 para 10% o 100 para $100)
     * @param tipoDescuento PORCENTAJE o NUMERICO
     * @return El monto final del descuento en dinero.
     */
    public Double calcularMontoDescuento(Double precioBase, Double valorDescuento, TipoDescuento tipoDescuento) {
        if (valorDescuento == null || tipoDescuento == null) {
            return 0.0;
        }

        if (tipoDescuento == TipoDescuento.PORCENTAJE) {
            if (valorDescuento < 0 || valorDescuento > 100) {
                throw new BusinessException("El porcentaje de descuento debe estar entre 0 y 100.");
            }
            return precioBase * (valorDescuento / 100.0);
        } else { // NUMÉRICO
            if (valorDescuento < 0) {
                throw new BusinessException("El descuento numérico no puede ser negativo.");
            }
            return valorDescuento;
        }
    }

    /**
     * Valida que un descuento no sea mayor que su base.
     */
    public void validarDescuento(Double montoDescuento, Double precioBase, String mensajeError) {
        // Usamos una pequeña tolerancia para problemas de decimales
        if (montoDescuento > (precioBase + TOLERANCIA_DECIMAL)) {
            throw new BusinessException(mensajeError);
        }
    }
}