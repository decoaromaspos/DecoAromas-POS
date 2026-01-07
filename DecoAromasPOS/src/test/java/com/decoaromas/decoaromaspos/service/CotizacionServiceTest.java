package com.decoaromas.decoaromaspos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.decoaromas.decoaromaspos.dto.cotizacion.*;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.*;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.CotizacionMapper;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.CotizacionRepository;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository cotizacionRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private ClienteService clienteService;
    @Mock
    private ProductoService productoService;
    @Mock
    private CotizacionMapper cotizacionMapper;
    @Mock
    private CalculoPrecioService calculoPrecioService;
    @InjectMocks
    private CotizacionService cotizacionService;
    private Usuario usuario;
    private Cliente cliente;
    private Producto producto;
    private DetalleCotizacionRequest detalleRequest;
    private CotizacionRequest cotizacionRequest;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().usuarioId(1L).nombre("Usuario Test").build();
        cliente = Cliente.builder().clienteId(1L).nombre("Cliente Test").build();
        producto = Producto.builder().productoId(1L).nombre("Producto Test").costo(10.0).codigoBarras("123456789").build();

        detalleRequest = DetalleCotizacionRequest.builder()
                .productoId(producto.getProductoId())
                .cantidad(2)
                .valorDescuentoUnitario(0.0)
                .tipoDescuentoUnitario(TipoDescuento.PORCENTAJE)
                .build();

        cotizacionRequest = CotizacionRequest.builder()
                .usuarioId(usuario.getUsuarioId())
                .clienteId(cliente.getClienteId())
                .tipoCliente(TipoCliente.DETALLE)
                .valorDescuentoGlobal(0.0)
                .tipoDescuentoGlobal(TipoDescuento.PORCENTAJE)
                .detalles(List.of(detalleRequest))
                .build();
    }

    @Test
    @DisplayName("Test para creear cotizacion existosa")
    void crearCotizacion_exitoso_deberiaCrearYDevolverCotizacion() {
        when(usuarioService.obtenerUsuarioRealPorId(usuario.getUsuarioId())).thenReturn(usuario);
        when(clienteService.obtenerClienteRealPorId(cliente.getClienteId())).thenReturn(cliente);
        when(productoService.obtenerProductoRealPorId(producto.getProductoId())).thenReturn(producto);
        when(calculoPrecioService.determinarPrecioUnitario(any(), eq(TipoCliente.DETALLE))).thenReturn(20.0);
        when(calculoPrecioService.calcularMontoDescuento(anyDouble(), anyDouble(), any())).thenReturn(0.0);
        doNothing().when(calculoPrecioService).validarDescuento(anyDouble(), anyDouble(), anyString());
        when(calculoPrecioService.calcularMontoDescuento(anyDouble(), anyDouble(), any())).thenReturn(0.0);
        Cotizacion cotizacionGuardada = Cotizacion.builder()
                .cotizacionId(1L)
                .fechaEmision(DateUtils.obtenerFechaHoraActual())
                .estado(EstadoCotizacion.PENDIENTE)
                .tipoCliente(TipoCliente.DETALLE)
                .valorDescuentoGlobal(0.0)
                .tipoDescuentoGlobal(TipoDescuento.PORCENTAJE)
                .totalBruto(40.0)
                .totalNeto(40.0)
                .detalles(new ArrayList<>())
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionGuardada);
        CotizacionResponse responseMock = new CotizacionResponse();
        responseMock.setCotizacionId(1L);
        when(cotizacionMapper.toResponse(any(Cotizacion.class))).thenReturn(responseMock);
        CotizacionResponse resultado = cotizacionService.crearCotizacion(cotizacionRequest);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getCotizacionId());

        verify(usuarioService).obtenerUsuarioRealPorId(usuario.getUsuarioId());
        verify(clienteService).obtenerClienteRealPorId(cliente.getClienteId());
        verify(productoService).obtenerProductoRealPorId(producto.getProductoId());
        verify(calculoPrecioService, atLeastOnce()).calcularMontoDescuento(anyDouble(), anyDouble(), any());
        verify(cotizacionRepository).save(any(Cotizacion.class));
        verify(cotizacionMapper).toResponse(any(Cotizacion.class));
    }

    @Test
    @DisplayName("Test para eliminar cotizazcion existente")
    void eliminarCotizacion_existente_deberiaEliminar() {
        Cotizacion cotizacion = Cotizacion.builder().cotizacionId(1L).build();
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));
        cotizacionService.eliminarCotizacion(1L);
        verify(cotizacionRepository).delete(cotizacion);
    }

    @Test
    @DisplayName("Test para eliminar una cotizacion, si no existe lanza excepcion")
    void eliminarCotizacion_noExistente_deberiaLanzarExcepcion() {
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cotizacionService.eliminarCotizacion(1L));
        verify(cotizacionRepository, never()).delete(ArgumentMatchers.<Cotizacion>any());
    }

    @Test
    @DisplayName("Test actualizar el estado de una cotizacion, si existe lo actualiza")
    void actualizarEstadoCotizacion_existente_deberiaActualizarYDevolver() {
        Cotizacion cot = Cotizacion.builder()
                .cotizacionId(1L)
                .estado(EstadoCotizacion.PENDIENTE)
                .build();
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cot));
        CotizacionUpdateEstado request = new CotizacionUpdateEstado();
        request.setCotizacionId(1L);
        request.setEstado(EstadoCotizacion.CONVERTIDA);

        Cotizacion cotGuardada = Cotizacion.builder()
                .cotizacionId(1L)
                .estado(EstadoCotizacion.CONVERTIDA)
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotGuardada);
        when(cotizacionMapper.toResponse(cotGuardada)).thenReturn(new CotizacionResponse());
        CotizacionResponse response = cotizacionService.actualizarEstadoCotizacion(request);
        assertNotNull(response);
        verify(cotizacionRepository).save(any(Cotizacion.class));
        verify(cotizacionMapper).toResponse(any());
    }

    @Test
    @DisplayName("Test actualizar el estado de una cotizacion, si no existe lanza excepcion")
    void actualizarEstadoCotizacion_noExistente_deberiaLanzarExcepcion() {
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.empty());
        CotizacionUpdateEstado request = new CotizacionUpdateEstado();
        request.setCotizacionId(1L);
        request.setEstado(EstadoCotizacion.CONVERTIDA); // Estado válido
        assertThrows(ResourceNotFoundException.class, () -> cotizacionService.actualizarEstadoCotizacion(request));
    }

    @Test
    @DisplayName("Test para listar cotizaciones, debe retornar una lista")
    void listarCotizaciones_deberiaDevolverLista() {
        Cotizacion cot1 = Cotizacion.builder().cotizacionId(1L).build();
        Cotizacion cot2 = Cotizacion.builder().cotizacionId(2L).build();
        when(cotizacionRepository.findAll()).thenReturn(List.of(cot1, cot2));
        when(cotizacionMapper.toResponse(any())).thenReturn(new CotizacionResponse());
        List<CotizacionResponse> resultado = cotizacionService.listarCotizaciones();
        assertEquals(2, resultado.size());
        verify(cotizacionRepository).findAll();
        verify(cotizacionMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("Test obtener cotizacion por filtro, paginado")
    void getCotizacionesFiltradas_deberiaRetornarPaginacion() {
        LocalDate fechaInicio = LocalDate.now().minusDays(10);
        LocalDate fechaFin = LocalDate.now();
        ZonedDateTime startOfDay = ZonedDateTime.now().minusDays(10);
        ZonedDateTime endOfDay = ZonedDateTime.now();
        Page<Cotizacion> cotPage = new PageImpl<>(List.of(
                Cotizacion.builder().cotizacionId(1L).build(),
                Cotizacion.builder().cotizacionId(2L).build()
        ));

        when(cotizacionRepository.findAll(
                ArgumentMatchers.<Specification<Cotizacion>>any(),
                any(Pageable.class))
        ).thenReturn(cotPage);
        when(cotizacionMapper.toResponse(any())).thenAnswer(invocation -> {
            Cotizacion c = invocation.getArgument(0);
            CotizacionResponse r = new CotizacionResponse();
            r.setCotizacionId(c.getCotizacionId());
            return r;
        });

        PaginacionResponse<CotizacionResponse> response = cotizacionService.getCotizacionesFiltradas(
                0, 10, "fechaEmision",
                fechaInicio, fechaFin,
                TipoCliente.DETALLE,
                null, null,
                1L, 1L);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getCotizacionId());
        assertEquals(2L, response.getContent().get(1).getCotizacionId());
        verify(cotizacionMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("Test obtener cotizacion por ID existente, debe retornar entidad")
    void obtenerCotizacionById_existente_deberiaRetornarEntidad() {
        Cotizacion cotizacion = Cotizacion.builder().cotizacionId(1L).build();
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));
        Cotizacion resultado = cotizacionService.obtenerCotizacionById(1L);
        assertEquals(1L, resultado.getCotizacionId());
        verify(cotizacionRepository).findById(1L);
    }

    @Test
    @DisplayName("Test obtener cotizacion por ID si no existe, debe retornar excepcion")
    void obtenerCotizacionById_noExistente_deberiaLanzarExcepcion() {
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cotizacionService.obtenerCotizacionById(1L));
    }
}
