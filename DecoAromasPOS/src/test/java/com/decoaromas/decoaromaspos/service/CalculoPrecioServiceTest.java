package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalculoPrecioServiceTest {

    private CalculoPrecioService calculoPrecioService;
    private Producto producto;

    @BeforeEach
    void setUp() {
        calculoPrecioService = new CalculoPrecioService();

        producto = new Producto();
        producto.setPrecioMayorista(80.0);
        producto.setPrecioDetalle(100.0);
    }

    @Test
    @DisplayName("Test para eliminar precio unitario mayorista")
    void determinarPrecioUnitario_Mayorista_DeberiaRetornarPrecioMayorista() {
        Double precio = calculoPrecioService.determinarPrecioUnitario(producto, TipoCliente.MAYORISTA);
        assertThat(precio).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Test para determinar precio unitario al detalle")
    void determinarPrecioUnitario_Detalle_DeberiaRetornarPrecioDetalle() {
        Double precio = calculoPrecioService.determinarPrecioUnitario(producto, TipoCliente.DETALLE);
        assertThat(precio).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Test para calcular umonto descuento, porcentaje valido")
    void calcularMontoDescuento_PorcentajeValido_DeberiaCalcularCorrectamente() {
        Double descuento = calculoPrecioService.calcularMontoDescuento(200.0, 10.0, TipoDescuento.PORCENTAJE);
        assertThat(descuento).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Test para calcular monto descuento, porcentaje negativo")
    void calcularMontoDescuento_PorcentajeNegativo_DeberiaLanzarExcepcion() {
        assertThatThrownBy(() ->
                calculoPrecioService.calcularMontoDescuento(200.0, -1.0, TipoDescuento.PORCENTAJE)
        ).isInstanceOf(BusinessException.class)
                .hasMessage("El porcentaje de descuento debe estar entre 0 y 100.");
    }

    @Test
    @DisplayName("Test para calcular monto descuento, porcentaje mayor al concedido")
    void calcularMontoDescuento_PorcentajeMayorA100_DeberiaLanzarExcepcion() {
        assertThatThrownBy(() ->
                calculoPrecioService.calcularMontoDescuento(200.0, 101.0, TipoDescuento.PORCENTAJE)
        ).isInstanceOf(BusinessException.class)
                .hasMessage("El porcentaje de descuento debe estar entre 0 y 100.");
    }

    @Test
    @DisplayName("Test para calcular monto descuento, descuento numerico valido")
    void calcularMontoDescuento_DescuentoNumericoValido_DeberiaRetornarValor() {
        Double descuento = calculoPrecioService.calcularMontoDescuento(200.0, 15.0, TipoDescuento.VALOR);
        assertThat(descuento).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Test para calcular monto descuento, descuento numerico negativo")
    void calcularMontoDescuento_DescuentoNumericoNegativo_DeberiaLanzarExcepcion() {
        assertThatThrownBy(() ->
                calculoPrecioService.calcularMontoDescuento(200.0, -5.0, TipoDescuento.VALOR)
        ).isInstanceOf(BusinessException.class)
                .hasMessage("El descuento numérico no puede ser negativo.");
    }

    @Test
    @DisplayName("Test para calcular monto descuento, valor por tipo nulo")
    void calcularMontoDescuento_NullValorOTipo_DeberiaRetornarCero() {
        Double descuento1 = calculoPrecioService.calcularMontoDescuento(200.0, null, TipoDescuento.VALOR);
        Double descuento2 = calculoPrecioService.calcularMontoDescuento(200.0, 10.0, null);
        assertThat(descuento1).isEqualTo(0.0);
        assertThat(descuento2).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Test para validar descuento, monto menor al precio base")
    void validarDescuento_MontoMenorQuePrecioBase_NoLanzaExcepcion() {
        // Usamos assertThatCode para validar explícitamente que no hay excepciones
        assertThatCode(() ->
                calculoPrecioService.validarDescuento(50.0, 100.0, "Error: descuento mayor")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test validar descuento, monto igual al precio base")
    void validarDescuento_MontoIgualPrecioBase_NoLanzaExcepcion() {
        // Usamos assertThatCode para validar explícitamente que no hay excepciones
        assertThatCode(() ->
                calculoPrecioService.validarDescuento(100.0, 100.0, "Error: descuento mayor")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test para validar descuento, monto mayor al precio base")
    void validarDescuento_MontoMayorQuePrecioBase_LanzaExcepcion() {
        assertThatThrownBy(() ->
                calculoPrecioService.validarDescuento(101.0, 100.0, "Error: descuento mayor")
        ).isInstanceOf(BusinessException.class)
                .hasMessage("Error: descuento mayor");
    }
}
