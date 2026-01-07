package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.aroma.AromaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.dto.aroma.AromaRequest;
import com.decoaromas.decoaromaspos.dto.aroma.AromaResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.AromaMapper;
import com.decoaromas.decoaromaspos.model.Aroma;
import com.decoaromas.decoaromaspos.repository.AromaRepository;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AromaServiceTest {

    @Mock
    private AromaRepository aromaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AromaMapper aromaMapper;
    @InjectMocks
    private AromaService aromaService;
    private Aroma aroma;
    private AromaResponse aromaResponse;

    @BeforeEach
    void setUp() {
        aroma = Aroma.builder()
                .aromaId(1L)
                .nombre("Vainilla")
                .isDeleted(false)
                .build();
        aromaResponse = AromaResponse.builder()
                .aromaId(1L)
                .nombre("Vainilla")
                .build();
    }

    @Test
    @DisplayName("Test para listar aromas")
    void listarAromas_deberiaRetornarListaDeAromas() {
        when(aromaRepository.findAll()).thenReturn(List.of(aroma));
        when(aromaMapper.toResponse(aroma)).thenReturn(aromaResponse);

        List<AromaResponse> result = aromaService.listarAromas();

        assertEquals(1, result.size());
        assertEquals("Vainilla", result.get(0).getNombre());
        verify(aromaRepository).findAll();
    }

    @Test
    @DisplayName("Test para listar aromas activos")
    void listarAromasActivos_deberiaRetornarSoloActivos() {
        when(aromaRepository.findAllByIsDeletedIsFalse()).thenReturn(List.of(aroma));
        when(aromaMapper.toResponse(aroma)).thenReturn(aromaResponse);

        List<AromaResponse> result = aromaService.listarAromasActivos();

        assertEquals(1, result.size());
        assertEquals("Vainilla", result.get(0).getNombre());
        verify(aromaRepository).findAllByIsDeletedIsFalse();
    }

    @Test
    @DisplayName("Test para buscar aroma por nombre parcial")
    void buscarAromaPorNombreParcial_deberiaRetornarCoincidencias() {
        when(aromaRepository.findByNombreContainingIgnoreCase("vain")).thenReturn(List.of(aroma));
        when(aromaMapper.toResponse(aroma)).thenReturn(aromaResponse);

        List<AromaResponse> result = aromaService.buscarAromaPorNombreParcial("vain");

        assertEquals(1, result.size());
        assertEquals("Vainilla", result.get(0).getNombre());
    }

    @Test
    @DisplayName("Test para obtener aroma por ID")
    void obtenerAromaPorId_existente_deberiaRetornarAromaResponse() {
        when(aromaRepository.findById(1L)).thenReturn(Optional.of(aroma));
        when(aromaMapper.toResponse(any(Aroma.class))).thenReturn(aromaResponse);

        AromaResponse result = aromaService.obtenerAromaPorId(1L);

        assertNotNull(result);
        assertEquals("Vainilla", result.getNombre());
        verify(aromaRepository).findById(1L);
    }

    @Test
    @DisplayName("Test para obtener un aroma por ID, si no existe lanza excepcion")
    void obtenerAromaPorId_noExistente_deberiaLanzarExcepcion() {
        when(aromaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aromaService.obtenerAromaPorId(99L));
    }

    @Test
    @DisplayName("Test para crear aroma, con nombre disponible")
    void crearAroma_nombreDisponible_creaYDevuelveAroma() {
        AromaRequest request = new AromaRequest();
        request.setNombre("Lavanda");

        when(aromaRepository.findByNombreIgnoreCase("Lavanda")).thenReturn(Collections.emptyList());
        when(aromaRepository.save(any(Aroma.class))).thenAnswer(invocation -> {
            Aroma a = invocation.getArgument(0);
            a.setAromaId(2L); // simular id generado
            return a;
        });
        when(aromaMapper.toResponse(any(Aroma.class)))
                .thenAnswer(invocation -> {
                    Aroma a = invocation.getArgument(0);
                    return new AromaResponse(a.getAromaId(), a.getNombre(), a.getIsDeleted());
                });

        AromaResponse result = aromaService.crearAroma(request);

        assertNotNull(result);
        assertEquals("Lavanda", result.getNombre());
        verify(aromaRepository).save(any(Aroma.class));
    }

    @Test
    @DisplayName("Test para crear aroma, si el nombre no esta disponible lanza excepcion")
    void crearAroma_nombreNoDisponible_lanzaExcepcion() {
        AromaRequest request = new AromaRequest();
        request.setNombre("Vainilla");

        when(aromaRepository.findByNombreIgnoreCase("Vainilla")).thenReturn(List.of(aroma));

        assertThrows(ExistsRegisterException.class, () -> aromaService.crearAroma(request));
        verify(aromaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test para actualizar aroma con nombre disponible (nuevo nombre)")
    void actualizarAroma_nombreNuevoDisponible_actualiza() {
        AromaRequest request = new AromaRequest();
        request.setNombre("Lavanda");

        when(aromaRepository.findById(1L)).thenReturn(Optional.of(aroma));
        when(aromaRepository.findByNombreIgnoreCase("Lavanda")).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            Aroma a = invocation.getArgument(0);
            a.setNombre("Lavanda");
            return a;
        }).when(aromaRepository).save(aroma);

        when(aromaMapper.toResponse(any(Aroma.class))).thenAnswer(invocation -> {
            Aroma a = invocation.getArgument(0);
            return new AromaResponse(a.getAromaId(), a.getNombre(), a.getIsDeleted());
        });
        AromaResponse result = aromaService.actualizarAroma(1L, request);
        assertEquals("Lavanda", result.getNombre());
        verify(aromaRepository).save(aroma);
    }

    @Test
    @DisplayName("Test para actualizar un aroma, si el nombre ya existe lanza excepcion")
    void actualizarAroma_nombreExistente_lanzaExcepcion() {
        AromaRequest request = new AromaRequest();
        request.setNombre("Rosa");
        when(aromaRepository.findById(1L)).thenReturn(Optional.of(aroma));
        when(aromaRepository.findByNombreIgnoreCase("Rosa")).thenReturn(List.of(new Aroma()));
        assertThrows(ExistsRegisterException.class, () -> aromaService.actualizarAroma(1L, request));
    }

    @Test
    @DisplayName("Test para cambiar el estado eliminado de un aroma")
    void cambiarEstadoEliminadoAroma_deberiaActualizarEstado() {
        when(aromaRepository.findById(1L)).thenReturn(Optional.of(aroma));
        doAnswer(invocation -> {
            return invocation.getArgument(0);
        }).when(aromaRepository).save(any(Aroma.class));
        when(aromaMapper.toResponse(any(Aroma.class))).thenReturn(aromaResponse);
        aromaService.cambiarEstadoEliminadoAroma(1L, true);
        assertTrue(aroma.getIsDeleted());
        verify(aromaRepository).save(aroma);
    }

    @Test
    @DisplayName("Test para eliminar un aroma con productos asociados")
    void eliminarAroma_conProductosAsociados_lanzaExcepcion() {
        when(productoRepository.countByAroma_AromaId(1L)).thenReturn(3);
        when(aromaRepository.findById(1L)).thenReturn(Optional.of(aroma));
        assertThrows(DataIntegrityViolationException.class, () -> aromaService.eliminarAroma(1L));
        verify(aromaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Test para eliminar aroma sin productos asociados")
    void eliminarAroma_sinProductos_eliminaCorrectamente() {
        when(productoRepository.countByAroma_AromaId(1L)).thenReturn(0);
        AromaService spyService = Mockito.spy(aromaService);
        doReturn(aroma).when(spyService).obtenerAromaRealPorId(1L);
        spyService.eliminarAroma(1L);
        verify(productoRepository).countByAroma_AromaId(1L);
        verify(aromaRepository).delete(aroma);
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad del nombre de un aroma")
    void checkNombreAvailability_disponible_true() {
        when(aromaRepository.findByNombreIgnoreCase("Rosa")).thenReturn(Collections.emptyList());

        AvailabilityResponse response = aromaService.checkNombreAvailability("Rosa");

        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad del nombre de aroma, cuando es nulo, no disponible")
    void checkNombreAvailability_nombreNulo_noDisponible_false() {
        AvailabilityResponse response = aromaService.checkNombreAvailability(null);

        assertFalse(response.isAvailable());
        assertEquals("El nombre es obligatorio y no puede estar vacío.", response.getMessage());
    }


    @Test
    @DisplayName("Test para verificar la disponibilidad del nombre de un aroma, para no disponible")
    void checkNombreAvailability_noDisponible_false() {
        when(aromaRepository.findByNombreIgnoreCase("Vainilla")).thenReturn(List.of(aroma));
        AvailabilityResponse response = aromaService.checkNombreAvailability("Vainilla");
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("Test para obtener aromas segund filtro de pagina")
    void getAromasFiltrados_deberiaRetornarPaginacionResponse() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nombre").ascending());
        Page<AromaCantidadProductosResponse> page = new PageImpl<>(List.of(
                new AromaCantidadProductosResponse(1L, "Vainilla", false, 5L)
        ));
        when(aromaRepository.findAllWithFiltersAndProductCount(eq("vain"), eq(false), any(Pageable.class))).thenReturn(page);

        PaginacionResponse<AromaCantidadProductosResponse> result =
                aromaService.getAromasFiltrados(0, 10, "nombre", "vain", false);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }
}
