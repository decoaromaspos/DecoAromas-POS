package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.venta.DatosPagoProcesado;
import com.decoaromas.decoaromaspos.dto.venta.PagoRequest;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private final PagoService pagoService = new PagoService();

    @Test
    @DisplayName("Test para procesar pagos validos, no hay vuelto")
    void procesarPagos_valido_sinVuelto() {
        PagoRequest pago1 = new PagoRequest();
        pago1.setMedioPago(MedioPago.EFECTIVO);
        pago1.setMonto(50.0);

        PagoRequest pago2 = new PagoRequest();
        pago2.setMedioPago(MedioPago.BCI);
        pago2.setMonto(50.0);

        List<PagoRequest> pagos = Arrays.asList(pago1, pago2);

        double totalNeto = 100.0;

        DatosPagoProcesado resultado = pagoService.procesarPagos(pagos, totalNeto);

        assertNotNull(resultado);
        assertEquals(0.0, resultado.getVuelto(), 0.001);
        assertEquals(2, resultado.getPagosEntidad().size());

        double suma = resultado.getPagosEntidad().stream().mapToDouble(PagoVenta::getMonto).sum();
        assertEquals(100.0, suma, 0.001);

    }

    @Test
    @DisplayName("Test para procesar pagos validos, con vuelto")
    void procesarPagos_valido_conVuelto() {
        PagoRequest pago = new PagoRequest();
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setMonto(120.0);

        List<PagoRequest> pagos = Arrays.asList(pago);

        double totalNeto = 100.0;

        DatosPagoProcesado resultado = pagoService.procesarPagos(pagos, totalNeto);

        assertNotNull(resultado);
        assertEquals(20.0, resultado.getVuelto(), 0.001);
        assertEquals(1, resultado.getPagosEntidad().size());
    }

    @Test
    @DisplayName("Test para procesar pagos validos, monto insuficiente, lanza excepcion")
    void procesarPagos_montoNoSuficiente_deberiaLanzar() {
        PagoRequest pago1 = new PagoRequest();
        pago1.setMedioPago(MedioPago.EFECTIVO);
        pago1.setMonto(30.0);

        PagoRequest pago2 = new PagoRequest();
        pago2.setMedioPago(MedioPago.BCI);
        pago2.setMonto(30.0);

        List<PagoRequest> pagos = Arrays.asList(pago1, pago2);

        double totalNeto = 100.0;

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.procesarPagos(pagos, totalNeto));

        assertTrue(ex.getMessage().contains("Pago insuficiente"));
    }

    @Test
    @DisplayName("Test para procesar pagos validos, vuelto decimal redondeado a cero")
    void procesarPagos_vueltoDecimal_redondeadoACero() {
        PagoRequest pago1 = new PagoRequest();
        pago1.setMedioPago(MedioPago.TRANSFERENCIA);
        pago1.setMonto(30.0);

        PagoRequest pago2 = new PagoRequest();
        pago2.setMedioPago(MedioPago.BCI);
        pago2.setMonto(30.0);

        List<PagoRequest> pagos = Arrays.asList(pago1, pago2);

        double totalNeto = 30.01;

        DatosPagoProcesado resultado = pagoService.procesarPagos(pagos, totalNeto);

        assertNotNull(resultado);
        assertEquals(0.0, resultado.getVuelto(), 0.001);
        assertEquals(2, resultado.getPagosEntidad().size());
    }

    @Test
    @DisplayName("Test para procesar pagos validos (lista), si entrega una lista vacia, lanza expecion")
    void procesarPagos_listaPagosVacia_deberiaLanzar() {
        List<PagoRequest> pagos = List.of();

        double totalNeto = 100.0;

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.procesarPagos(pagos, totalNeto));

        assertTrue(ex.getMessage().contains("al menos un método de pago"));
    }

    @Test
    @DisplayName("Test para procesar pagos validos (lista nula), si la lista de pagos es nula, debe lanzar excepcion")
    void procesarPagos_listaPagosNull_deberiaLanzar() {
        double totalNeto = 100.0;

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.procesarPagos(null, totalNeto));

        assertTrue(ex.getMessage().contains("al menos un método de pago"));
    }
}
