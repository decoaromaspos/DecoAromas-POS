package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualRequest;
import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.VentaOnlineMensualMapper;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import com.decoaromas.decoaromaspos.repository.VentaOnlineMensualRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaOnlineMensualServiceTest {

    @Mock
    private VentaOnlineMensualRepository ventaOnlineMensualRepository;
    @Mock
    private VentaOnlineMensualMapper ventaOnlineMensualMapper;
    @InjectMocks
    private VentaOnlineMensualService ventaOnlineMensualService;
    private VentaOnlineMensual venta;
    private VentaOnlineMensualResponse response;
    private VentaOnlineMensualRequest request;

    @BeforeEach
    void setUp() {
        venta = VentaOnlineMensual.builder()
                .ventaOnlineMensualId(1L)
                .anio(2024)
                .mes(5)
                .totalDetalle(1000.0)
                .totalMayorista(500.0)
                .build();

        response = new VentaOnlineMensualResponse();
        response.setAnio(2024);
        response.setMes(5);
        response.setTotalDetalle(1000.0);
        response.setTotalMayorista(500.0);

        request = new VentaOnlineMensualRequest();
        request.setAnio(2024);
        request.setMes(5);
        request.setTotalDetalle(1000.0);
        request.setTotalMayorista(500.0);
    }

    @Test
    @DisplayName("Test para listar todas las ventas online mensuales")
    void listarVentasOnlineMensuales_DeberiaRetornarListaMapeada() {
        when(ventaOnlineMensualRepository.findAll()).thenReturn(List.of(venta));
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        List<VentaOnlineMensualResponse> result = ventaOnlineMensualService.listarVentasOnlineMensuales();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMes()).isEqualTo(5);
        verify(ventaOnlineMensualRepository).findAll();
    }

    @Test
    @DisplayName("Test para obtener ventas online por año")
    void obtenerVentasOnlineMensualesPorAnio_DeberiaRetornarListaDelAnio() {
        when(ventaOnlineMensualRepository.findByAnio(2024)).thenReturn(List.of(venta));
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        List<VentaOnlineMensualResponse> result = ventaOnlineMensualService.obtenerVentasOnlineMensualesPorAnio(2024);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnio()).isEqualTo(2024);
        verify(ventaOnlineMensualRepository).findByAnio(2024);
    }

    @Test
    @DisplayName("Test para obtener una venta online mensual por ID existente")
    void obtenerVentaOnlineMensualPorId_DeberiaRetornarElRegistro() {
        when(ventaOnlineMensualRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        ventaOnlineMensualService.obtenerVentaOnlineMensualPorId(1L);

        verify(ventaOnlineMensualRepository).findById(1L);
    }

    @Test
    @DisplayName("Test para obtener una venta online mensual por ID inexistente")
    void obtenerVentaOnlineMensualPorId_DeberiaLanzarExcepcionSiNoExiste() {
        when(ventaOnlineMensualRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaOnlineMensualService.obtenerVentaOnlineMensualPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe venta online mensual con id 99");
    }

    @Test
    @DisplayName("Test para obtener venta online mensual por año y mes existentes")
    void obtenerVentaOnlineMensualPorAnioMes_DeberiaRetornarElRegistro() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 5)).thenReturn(Optional.of(venta));
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        VentaOnlineMensualResponse result = ventaOnlineMensualService.obtenerVentaOnlineMensualPorAnioMes(2024, 5);

        assertThat(result.getMes()).isEqualTo(5);
        verify(ventaOnlineMensualRepository).findByAnioAndMes(2024, 5);
    }

    @Test
    @DisplayName("Test para obtener venta online mensual por año y mes inexistentes")
    void obtenerVentaOnlineMensualPorAnioMes_DeberiaLanzarExcepcionSiNoExiste() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaOnlineMensualService.obtenerVentaOnlineMensualPorAnioMes(2024, 7))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No se encontró registro para el mes 7 del año 2024");
    }

    @Test
    @DisplayName("Test para crear nueva venta online mensual si no existe")
    void crearVentaOnlineMensual_DeberiaGuardarNuevoRegistro() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 5)).thenReturn(Optional.empty());
        when(ventaOnlineMensualRepository.save(any(VentaOnlineMensual.class))).thenReturn(venta);
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        VentaOnlineMensualResponse result = ventaOnlineMensualService.crearVentaOnlineMensual(request);

        assertThat(result.getAnio()).isEqualTo(2024);
        verify(ventaOnlineMensualRepository).save(any(VentaOnlineMensual.class));
    }

    @Test
    @DisplayName("Test para crear venta online mensual existente debería lanzar excepción")
    void crearVentaOnlineMensual_DeberiaLanzarExcepcionSiYaExiste() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 5)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> ventaOnlineMensualService.crearVentaOnlineMensual(request))
                .isInstanceOf(ExistsRegisterException.class)
                .hasMessageContaining("Ya existe registro para el mes 5 y año 2024");
    }

    @Test
    @DisplayName("Test para actualizar venta online mensual existente")
    void actualizarVentaOnlineMensual_DeberiaActualizarTotales() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 5)).thenReturn(Optional.of(venta));
        when(ventaOnlineMensualRepository.save(venta)).thenReturn(venta);
        when(ventaOnlineMensualMapper.toResponse(venta)).thenReturn(response);

        request.setTotalDetalle(2000.0);
        request.setTotalMayorista(1000.0);

        VentaOnlineMensualResponse result = ventaOnlineMensualService.actualizarVentaOnlineMensual(request);

        assertThat(result.getTotalDetalle()).isEqualTo(1000.0); // El valor mapeado en response
        verify(ventaOnlineMensualRepository).save(venta);
    }

    @Test
    @DisplayName("Test para actualizar venta online mensual inexistente")
    void actualizarVentaOnlineMensual_DeberiaLanzarExcepcionSiNoExiste() {
        when(ventaOnlineMensualRepository.findByAnioAndMes(2024, 5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaOnlineMensualService.actualizarVentaOnlineMensual(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Test para eliminar venta online mensual existente")
    void eliminarVentaOnlineMensual_DeberiaEliminarRegistro() {
        when(ventaOnlineMensualRepository.findById(1L)).thenReturn(Optional.of(venta));

        ventaOnlineMensualService.eliminarVentaOnlineMensual(1L);

        verify(ventaOnlineMensualRepository).delete(venta);
    }

    @Test
    @DisplayName("Test para eliminar venta online mensual inexistente")
    void eliminarVentaOnlineMensual_DeberiaLanzarExcepcionSiNoExiste() {
        when(ventaOnlineMensualRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaOnlineMensualService.eliminarVentaOnlineMensual(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe venta online mensual con id 1");
    }
}
