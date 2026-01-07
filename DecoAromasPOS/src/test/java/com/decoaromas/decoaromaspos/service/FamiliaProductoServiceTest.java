package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.familia.*;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.FamiliaProductoMapper;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import com.decoaromas.decoaromaspos.repository.FamiliaProductoRepository;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamiliaProductoServiceTest {

    @Mock
    private FamiliaProductoRepository familiaProductoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private FamiliaProductoMapper familiaMapper;

    @InjectMocks
    private FamiliaProductoService familiaProductoService;

    @Test
    @DisplayName("Test para listar familia de productos, devuelve lista")
    void listarFamiliasProductos_deberiaRetornarLista() {
        List<FamiliaProducto> lista = List.of(
                new FamiliaProducto(1L, "Familia1", false),
                new FamiliaProducto(2L, "Familia2", false)
        );

        when(familiaProductoRepository.findAll()).thenReturn(lista);

        // Mock mapper devuelve un FamiliaResponse con datos correctos
        when(familiaMapper.toResponse(any())).thenAnswer(invocation -> {
            FamiliaProducto fam = invocation.getArgument(0);
            return FamiliaResponse.builder()
                    .familiaId(fam.getFamiliaId())
                    .nombre(fam.getNombre())
                    .isDeleted(fam.getIsDeleted())
                    .build();
        });

        List<FamiliaResponse> result = familiaProductoService.listarFamiliasProductos();

        assertEquals(2, result.size());
        assertEquals("Familia1", result.get(0).getNombre());
        verify(familiaProductoRepository).findAll();
    }

    @Test
    @DisplayName("Test para listar familia de productos activos, devuelve lista")
    void listarFamiliasProductosActivos_deberiaRetornarLista() {
        List<FamiliaProducto> lista = List.of(
                new FamiliaProducto(1L, "Familia1", false),
                new FamiliaProducto(2L, "Familia2", false)
        );

        when(familiaProductoRepository.findAllByIsDeletedIsFalse()).thenReturn(lista);

        // Mock mapper devuelve un FamiliaResponse con datos correctos
        when(familiaMapper.toResponse(any())).thenAnswer(invocation -> {
            FamiliaProducto fam = invocation.getArgument(0);
            return FamiliaResponse.builder()
                    .familiaId(fam.getFamiliaId())
                    .nombre(fam.getNombre())
                    .isDeleted(fam.getIsDeleted())
                    .build();
        });

        List<FamiliaResponse> result = familiaProductoService.listarFamiliasActivos();

        assertEquals(2, result.size());
        assertEquals("Familia1", result.get(0).getNombre());
        verify(familiaProductoRepository).findAllByIsDeletedIsFalse();
    }

    @Test
    @DisplayName("Test para crear familia con nombre disponible, debe crearla")
    void crearFamilia_nombreDisponible_deberiaCrearYRetornar() {
        FamiliaRequest request = mock(FamiliaRequest.class);
        when(request.getNombre()).thenReturn("Nueva Familia");

        // *** IMPORTANTE: findByNombreIgnoreCase devuelve List<FamiliaProducto> ***
        when(familiaProductoRepository.findByNombreIgnoreCase("Nueva Familia")).thenReturn(Collections.emptyList());

        when(familiaProductoRepository.save(any(FamiliaProducto.class))).thenAnswer(invocation -> {
            FamiliaProducto f = invocation.getArgument(0);
            f.setFamiliaId(1L);
            return f;
        });

        when(familiaMapper.toResponse(any())).thenAnswer(invocation -> {
            FamiliaProducto fam = invocation.getArgument(0);
            return FamiliaResponse.builder()
                    .familiaId(fam.getFamiliaId())
                    .nombre(fam.getNombre())
                    .isDeleted(fam.getIsDeleted())
                    .build();
        });

        FamiliaResponse response = familiaProductoService.crearFamilia(request);

        assertNotNull(response);
        assertEquals("Nueva Familia", response.getNombre());
        verify(familiaProductoRepository).save(any(FamiliaProducto.class));
    }

    @Test
    @DisplayName("Test para crear familia, el nombre no esta disponible, debe lanzar excepcion")
    void crearFamilia_nombreExistente_deberiaLanzarException() {
        FamiliaRequest request = mock(FamiliaRequest.class);
        when(request.getNombre()).thenReturn("Familia Existente");

        when(familiaProductoRepository.findByNombreIgnoreCase("Familia Existente"))
                .thenReturn(List.of(new FamiliaProducto()));

        ExistsRegisterException exception = assertThrows(ExistsRegisterException.class,
                () -> familiaProductoService.crearFamilia(request));

        assertTrue(exception.getMessage().contains("Ya existe una familia (activa o inactiva) con nombre " + request.getNombre() + ". Ingrese otro."));
    }

    @Test
    @DisplayName("Test para actualizar el nombre de familia de producto, sin producto existente, debe lanzar excepcion")
    void actualizarFamilia_productoNoExiste_deberiaLanzarResourceNotFound() {
        when(familiaProductoRepository.findById(1L)).thenReturn(Optional.empty());

        FamiliaRequest request = mock(FamiliaRequest.class);
        when(request.getNombre()).thenReturn("Nuevo");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> familiaProductoService.actualizarFamiliaProducto(1L, request));

        assertTrue(exception.getMessage().contains("No existe familia"));
    }

    @Test
    @DisplayName("Test para actualizar el nombre de familia de producto, debe actualizarlo")
    void actualizarFamilia_nombreNuevoDisponible_deberiaActualizar() {
        FamiliaProducto familiaExistente = new FamiliaProducto(1L, "Viejo Nombre", false);

        FamiliaRequest request = mock(FamiliaRequest.class);
        when(request.getNombre()).thenReturn("Nuevo Nombre");

        when(familiaProductoRepository.findById(1L)).thenReturn(Optional.of(familiaExistente));
        when(familiaProductoRepository.findByNombreIgnoreCase("Nuevo Nombre")).thenReturn(Collections.emptyList());
        when(familiaProductoRepository.save(any(FamiliaProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(familiaMapper.toResponse(any())).thenAnswer(invocation -> {
            FamiliaProducto fam = invocation.getArgument(0);
            return FamiliaResponse.builder()
                    .familiaId(fam.getFamiliaId())
                    .nombre(fam.getNombre())
                    .isDeleted(fam.getIsDeleted())
                    .build();
        });

        FamiliaResponse response = familiaProductoService.actualizarFamiliaProducto(1L, request);

        assertEquals("Nuevo Nombre", response.getNombre());
        verify(familiaProductoRepository).save(familiaExistente);
    }

    @Test
    @DisplayName("Test para cambiar el estado familia de producto, debe cambiar estado")
    void cambiarEstadoEliminadoFamiliaProducto_deberiaCambiarEstado() {
        FamiliaProducto familia = new FamiliaProducto(1L, "Nombre", false);

        when(familiaProductoRepository.findById(1L)).thenReturn(Optional.of(familia));
        when(familiaProductoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(familiaMapper.toResponse(any())).thenAnswer(invocation -> {
            FamiliaProducto f = invocation.getArgument(0);
            return FamiliaResponse.builder()
                    .familiaId(f.getFamiliaId())
                    .nombre(f.getNombre())
                    .isDeleted(f.getIsDeleted())
                    .build();
        });

        FamiliaResponse resp = familiaProductoService.cambiarEstadoEliminadoFamiliaProducto(1L, true);

        assertTrue(familia.getIsDeleted());
        assertEquals("Nombre", resp.getNombre());
        verify(familiaProductoRepository).save(familia);
    }

    @Test
    @DisplayName("Test para eliminar familia de producto, con productos asociados, debe lanzar excepcion")
    void eliminarFamiliaProducto_conProductosAsociados_deberiaLanzarException() {
        lenient().when(familiaProductoRepository.findById(1L)).thenReturn(Optional.of(new FamiliaProducto(1L, "Familia", false)));
        when(productoRepository.countByFamilia_FamiliaId(1L)).thenReturn(2);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> familiaProductoService.eliminarFamiliaProducto(1L));

        assertTrue(ex.getMessage().contains("No se puede eliminar la familia"));
    }


    @Test
    @DisplayName("Test para eliminar familia de producto, sin productos asociados, debe eliminarse")
    void eliminarFamiliaProducto_sinProductosAsociados_deberiaEliminar() {
        FamiliaProducto familia = new FamiliaProducto(1L, "Nombre", false);
        when(productoRepository.countByFamilia_FamiliaId(1L)).thenReturn(0);
        when(familiaProductoRepository.findById(1L)).thenReturn(Optional.of(familia));
        familiaProductoService.eliminarFamiliaProducto(1L);
        verify(familiaProductoRepository).delete(familia);
    }

    @Test
    @DisplayName("Test para verificar disponibilidad de nombre, nombre disponible, debe retornar disponibilidad")
    void checkNombreAvailability_nombreDisponible_deberiaRetornarDisponible() {
        when(familiaProductoRepository.findByNombreIgnoreCase("Disponible")).thenReturn(Collections.emptyList());
        AvailabilityResponse response = familiaProductoService.checkNombreAvailability("Disponible");
        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("Test para verificar disponibilidad de nombre, nombre nulo no disponible debe lanzar excepcion")
    void checkNombreAvailability_nombreNulo_noDisponible_false() {
        AvailabilityResponse response = familiaProductoService.checkNombreAvailability(null);
        assertFalse(response.isAvailable());
        assertEquals("El nombre es obligatorio y no puede estar vacío.", response.getMessage());
    }

    @Test
    @DisplayName("Test para verificar disponibilidad de nombre, nombre no disponible debe lanzar excepcion")
    void checkNombreAvailability_nombreNoDisponible_deberiaRetornarNoDisponible() {
        when(familiaProductoRepository.findByNombreIgnoreCase("NoDisponible")).thenReturn(List.of(new FamiliaProducto()));
        AvailabilityResponse response = familiaProductoService.checkNombreAvailability("NoDisponible");
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("Test para obtener familia por filtro, paginado")
    void getFamiliasFiltradas_deberiaRetornarPaginacion() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre").ascending());
        FamiliaCantidadProductosResponse dto = new FamiliaCantidadProductosResponse(1L, "Familia1", false, 5L);
        Page<FamiliaCantidadProductosResponse> page = new PageImpl<>(List.of(dto));
        when(familiaProductoRepository.findAllWithFiltersAndProductCount("Familia", false, pageable))
                .thenReturn(page);

        PaginacionResponse<FamiliaCantidadProductosResponse> result =
                familiaProductoService.getFamiliasFiltradas(0, 10, "nombre", "Familia", false);
        assertEquals(1, result.getContent().size());
        assertEquals("Familia1", result.getContent().get(0).getNombre());
    }
}

