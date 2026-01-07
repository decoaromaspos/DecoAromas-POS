package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoFilterDTO;
import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoInventarioResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.MovimientoInventarioMapper;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository mockRepo;
    @Mock
    private MovimientoInventarioMapper movMapper;
    @InjectMocks
    private MovimientoInventarioService service;
    private MovimientoInventario movimiento;
    private MovimientoInventarioResponse response;

    @BeforeEach
    void setup() {
        // --- Usuario de prueba ---
        Usuario u = new Usuario();
        u.setUsuarioId(1L);
        u.setNombre("Juan");
        u.setApellido("Perez");
        u.setUsername("juanp");

        // --- Producto de prueba ---
        Producto p = new Producto();
        p.setProductoId(10L);
        p.setNombre("Velas Color 4x1000");

        // --- Movimiento de inventario ---
        movimiento = new MovimientoInventario();
        movimiento.setMovimientoId(1L);
        movimiento.setUsuario(u);
        movimiento.setProducto(p);
        movimiento.setCantidad(5);
        movimiento.setMotivo(MotivoMovimiento.AJUSTE_VENTA);
        movimiento.setTipo(TipoMovimiento.ENTRADA);
        movimiento.setFecha(ZonedDateTime.now());

        response = MovimientoInventarioResponse.builder()
                .movimientoId(1L)
                .fecha(movimiento.getFecha())
                .tipo(movimiento.getTipo())
                .motivo(movimiento.getMotivo())
                .cantidad(movimiento.getCantidad())
                .productoId(p.getProductoId())
                .productoNombre(p.getNombre())
                .usuarioId(u.getUsuarioId())
                .nombreCompleto(u.getNombre() + " " + u.getApellido())
                .username(u.getUsername())
                .build();
    }

    @Test
    @DisplayName("Test para listar movimientos")
    void listarMovimientos_deberiaDevolverTodos() {
        when(mockRepo.findAll()).thenReturn(List.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var result = service.listarMovimientos();

        assertEquals(1, result.size());
        assertEquals("Juan Perez", result.get(0).getNombreCompleto());
        verify(mockRepo).findAll();
    }

    @Test
    @DisplayName("Test para obtener movimientos por fecha")
    void obtenerMovimientosPorFecha_deberiaFiltrarPorDia() {
        when(mockRepo.findByFechaBetween(any(), any())).thenReturn(List.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var lista = service.obtenerMovimientosPorFecha(LocalDate.now());

        assertEquals(1, lista.size());
        assertEquals("Velas Color 4x1000", lista.get(0).getProductoNombre());
        verify(mockRepo).findByFechaBetween(any(), any());
    }

    @Test
    @DisplayName("Test obtener movimientos por producto ID")
    void obtenerMovimientosPorProductoId_deberiaRetornarLista() {
        when(mockRepo.findByProducto_ProductoId(10L)).thenReturn(List.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var lista = service.obtenerMovimientosPorProductoId(10L);

        assertEquals(1, lista.size());
        assertEquals("Velas Color 4x1000", lista.get(0).getProductoNombre());
        verify(mockRepo).findByProducto_ProductoId(10L);
    }

    @Test
    @DisplayName("Test obtener movimientos por motivo o causal")
    void obtenerMovimientosPorMotivo_deberiaRetornarFiltrados() {
        when(mockRepo.findByMotivo(MotivoMovimiento.AJUSTE_VENTA)).thenReturn(List.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var lista = service.obtenerMovimientosPorMotivo(MotivoMovimiento.AJUSTE_VENTA);

        assertEquals(1, lista.size());
        assertEquals(MotivoMovimiento.AJUSTE_VENTA, lista.get(0).getMotivo());
        verify(mockRepo).findByMotivo(MotivoMovimiento.AJUSTE_VENTA);
    }

    @Test
    @DisplayName("Test para obtener movimientos por ID del usuario")
    void obtenerMovimientosPorIdUsuario_deberiaRetornarCorrectos() {
        when(mockRepo.findByUsuario_UsuarioId(1L)).thenReturn(List.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var lista = service.obtenerMovimientosPorIdUsuario(1L);

        assertEquals(1, lista.size());
        assertEquals("Juan Perez", lista.get(0).getNombreCompleto());
        verify(mockRepo).findByUsuario_UsuarioId(1L);
    }

    @Test
    @DisplayName("Test para obtener movimiento por ID, debe retornarlos en caso de coincidencias")
    void obtenerMovimientoPorId_existente_deberiaRetornarResponse() {
        when(mockRepo.findById(1L)).thenReturn(Optional.of(movimiento));
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        var res = service.obtenerMovimientoPorId(1L);

        assertEquals("Juan Perez", res.getNombreCompleto());
        verify(mockRepo).findById(1L);
    }

    @Test
    @DisplayName("Test para obtener movimientos por ID, en caso de no existencia lanza error")
    void obtenerMovimientoPorId_inexistente_deberiaLanzarExcepcion() {
        when(mockRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMovimientoPorId(99L));
    }

    @Test
    @DisplayName("Test para crear movimiento de inventario, debe guardarlo correctamente")
    void crearMovimientoInventario_deberiaGuardarCorrectamente() {
        when(mockRepo.save(any(MovimientoInventario.class))).thenReturn(movimiento);
        when(movMapper.toResponse(any(MovimientoInventario.class))).thenReturn(response);

        var res = service.crearMovimientoInventario(movimiento.getProducto(), movimiento.getUsuario(),
                5, TipoMovimiento.SALIDA, MotivoMovimiento.VENTA);

        assertEquals("Juan Perez", res.getNombreCompleto());
        verify(mockRepo).save(any(MovimientoInventario.class));
    }

    @Test
    @DisplayName("Test para guardar una lista de movimientos, debe guardar la lista")
    void guardarListaMovimientos_deberiaGuardarLista() {
        when(mockRepo.saveAll(anyList())).thenReturn(List.of(movimiento));

        var result = service.guardarListaMovimientos(List.of(movimiento));

        assertEquals(1, result.size());
        verify(mockRepo).saveAll(anyList());
    }

    @Test
    @DisplayName("Test para obtener los movimientos segun filtro")
    void getMovimientosFiltrados_deberiaLlamarRepositorio() {
        Page<MovimientoInventario> page = new PageImpl<>(List.of(movimiento));
        when(mockRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        MovimientoFilterDTO filterDTO = new MovimientoFilterDTO();
        filterDTO.setMotivo(MotivoMovimiento.AJUSTE_VENTA);
        filterDTO.setTipo(TipoMovimiento.ENTRADA);
        filterDTO.setUsuarioId(1L);

        var res = service.getMovimientosFiltrados(0, 10, "fecha", LocalDate.now().minusDays(1),
                LocalDate.now(), filterDTO);

        assertNotNull(res);
        assertFalse(res.getContent().isEmpty());
        verify(mockRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para obtener los movimientos segun paginacion")
    void obtenerMovimientosPaginados_deberiaRetornarPaginaCorrecta() {
        Page<MovimientoInventario> pageMock = new PageImpl<>(List.of(movimiento));
        when(mockRepo.findAll(any(Pageable.class))).thenReturn(pageMock);
        when(movMapper.toResponse(movimiento)).thenReturn(response);

        PaginacionResponse<MovimientoInventarioResponse> result =
                service.obtenerMovimientosPaginados(0, 10, "fecha");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(mockRepo).findAll(any(Pageable.class));
    }
}
