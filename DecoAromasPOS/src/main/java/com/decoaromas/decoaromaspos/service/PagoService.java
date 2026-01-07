package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.venta.DatosPagoProcesado;
import com.decoaromas.decoaromaspos.dto.venta.PagoRequest;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PagoService {

    private static final double TOLERANCIA_DECIMAL = 0.01;

    /**
     * Procesa y valida la lista de pagos de un request contra el total neto.
     * Devuelve las entidades PagoVenta listas y el vuelto calculado.
     */
    public DatosPagoProcesado procesarPagos(List<PagoRequest> pagosRequest, double totalNeto) {
        // Validación: lista de pagos no vacía.
        if (pagosRequest == null || pagosRequest.isEmpty()) {
            throw new BusinessException("Debe proporcionar al menos un método de pago.");
        }

        double totalPagadoEfectivo = 0.0;
        double totalPagadoOtrosMedios = 0.0;

        // Clasificar y sumar los pagos recibidos según efectivo u otros
        for (PagoRequest pagoDto : pagosRequest) {
            if (pagoDto.getMedioPago() == MedioPago.EFECTIVO) {
                totalPagadoEfectivo += pagoDto.getMonto();
            } else {
                totalPagadoOtrosMedios += pagoDto.getMonto();
            }
        }

        // Validar que el pago total sea suficiente para cubrir la venta.
        double totalPagado = totalPagadoEfectivo + totalPagadoOtrosMedios;
        if (totalPagado < totalNeto - TOLERANCIA_DECIMAL) {
            throw new BusinessException("Pago insuficiente. Monto pagado (" + totalPagado + ") es menor que el total (" + totalNeto + ").");
        }

        // Calcular el vuelto correctamente
        double vuelto = Math.max(0, totalPagado - totalNeto); // Asegura que el vuelto no sea negativo


        /*
          Verificamos si el vuelto calculado es MAYOR que el efectivo recibido.
          Si lo es, significa que el "vuelto" proviene de un pago electrónico,
          lo cual no es posible. En ese caso, el vuelto real es $0.
          Ej.: Total: 29351.52, Efectivo: 0, Transferencia: 29352, Vuelto calculado: 0.48
            (0.48 > 0 + 0.01) -> Verdadero. -> no hay vuelto real
         */
        if (vuelto > totalPagadoEfectivo + TOLERANCIA_DECIMAL) {
            vuelto = 0.0;
        }

        // Crear las entidades PagoVenta y asociarlas a la Venta.
        List<PagoVenta> pagosEntidad = new ArrayList<>();
        for (PagoRequest pagoDto : pagosRequest) {
            pagosEntidad.add(
                    PagoVenta.builder()
                        .medioPago(pagoDto.getMedioPago())
                        .monto(pagoDto.getMonto())
                        .build());
        }

        return new DatosPagoProcesado(pagosEntidad, vuelto);
    }
}