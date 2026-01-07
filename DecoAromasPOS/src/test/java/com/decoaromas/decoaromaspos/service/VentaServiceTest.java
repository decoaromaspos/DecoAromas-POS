package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.venta.*;
import com.decoaromas.decoaromaspos.enums.*;
import com.decoaromas.decoaromaspos.exception.CajaCerradaException;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.VentaMapper;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.*;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private CajaRepository cajaRepository;
    @Mock private ClienteService clienteService;
    @Mock private ProductoService productoService;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private GestorInventarioService gestorInventarioService;
    @Mock private CalculoPrecioService calculoPrecioService;
    @Mock private PagoService pagoService;
    @Mock private ReceiptBuilderService receiptBuilderService;
    @Mock private VentaMapper ventaMapper;
    @Mock private AvailabilityChecker checker;
    @InjectMocks
    private VentaService ventaService;
    private Usuario usuario;
    private Caja caja;
    private Cliente cliente;
    private Venta venta;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsuarioId(1L);
        caja = new Caja();
        caja.setCajaId(1L);
        cliente = new Cliente();
        cliente.setClienteId(1L);
        venta = new Venta();
        venta.setVentaId(1L);
        venta.setTotalBruto(100.0);
        venta.setTipoDocumento(TipoDocumento.BOLETA);
    }

    @Test
    @DisplayName("Test revisar la disponibilidad del numero de un documento, debe devulver su disponibilidad y numero")
    void checkNumDocumentoAvailability_deberiaRetornarCheckResponse() {
        when(checker.check(any(), anyString(), anyString()))
                .thenReturn(new AvailabilityResponse(true, "Disponible"));

        AvailabilityResponse resp = ventaService.checkNumDocumentoAvailability("B123");
        assertTrue(resp.isAvailable());
    }

    @Test
    @DisplayName("Test actualizar un documento")
    void actualizarDocumento_deberiaActualizarCorrectamente() {
        ActualizarDocumentoRequest req = new ActualizarDocumentoRequest();
        req.setTipoDocumento(TipoDocumento.BOLETA);
        req.setNumeroDocumento("B123");

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaRepository.findByNumeroDocumento("B123")).thenReturn(Optional.empty());
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ventaMapper.toResponse(any())).thenReturn(new VentaResponse());

        VentaResponse result = ventaService.actualizarDocumento(1L, req);
        assertNotNull(result);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Test para actualizar un documento con número repetido, debe lanzar ExistsRegisterException")
    void actualizarDocumento_conNumeroRepetidoDebeLanzarExcepcion() {

        ActualizarDocumentoRequest req = new ActualizarDocumentoRequest();
        req.setTipoDocumento(TipoDocumento.BOLETA);
        req.setNumeroDocumento("B123");

        venta.setVentaId(1L); // venta actual

        Venta otraVenta = new Venta();
        otraVenta.setVentaId(2L); // venta distinta con mismo número

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        when(ventaRepository.findByNumeroDocumento("B123"))
                .thenReturn(Optional.of(otraVenta));

        ExistsRegisterException ex = assertThrows(
                ExistsRegisterException.class,
                () -> ventaService.actualizarDocumento(1L, req)
        );

        assertEquals(
                "El número de documento 'B123' ya está asignado a otra venta.",
                ex.getMessage()
        );
    }


    @Test
    @DisplayName("Test para actualizar el cliente, debe actualizarlo")
    void actualizarCliente_deberiaActualizarCliente() {
        ActualizarClienteRequest req = new ActualizarClienteRequest();
        req.setClienteId(1L);

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(clienteService.obtenerClienteRealPorId(1L)).thenReturn(cliente);
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ventaMapper.toResponse(any())).thenReturn(new VentaResponse());

        VentaResponse result = ventaService.actualizarCliente(1L, req);
        assertNotNull(result);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Test para obtener una venta, debe devolverla")
    void obtenerVenta_deberiaRetornarVentaResponse() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaMapper.toResponse(any())).thenReturn(new VentaResponse());

        VentaResponse result = ventaService.obtenerVenta(1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test para obtener una venta, sino existe lanza excepcion")
    void obtenerVenta_noExistenteDebeLanzarExcepcion() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ventaService.obtenerVenta(1L));
    }

    @Test
    @DisplayName("Test para listar las ventas, debe devolver una lista")
    void listarVentas_deberiaRetornarListaDeVentas() {
        when(ventaRepository.findAll()).thenReturn(List.of(venta));
        when(ventaMapper.toResponse(any())).thenReturn(new VentaResponse());
        List<VentaResponse> result = ventaService.listarVentas();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test para obtener las ventas filtradas, debe devolver la pagina")
    void getVentasFiltradas_deberiaRetornarPagina() {
        Page<Venta> ventasPage = new PageImpl<>(List.of(venta));
        when(ventaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(ventasPage);
        when(ventaMapper.toResponse(any())).thenReturn(new VentaResponse());

        PaginacionResponse<VentaResponse> resp = ventaService.getVentasFiltradas(
                0, 10, "fecha", null, null, null);

        assertNotNull(resp);
        assertEquals(1, resp.getContent().size());
    }

    @Test
    @DisplayName("Test para eliminar una venta, debe eliminar y registrar entradas")
    void eliminarVenta_deberiaEliminarYRegistrarEntradas() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(new Producto());
        detalle.setCantidad(2);
        venta.setDetalles(List.of(detalle));

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        ventaService.eliminarVenta(1L, 1L);
        verify(ventaRepository).delete(venta);
        verify(gestorInventarioService, atLeastOnce())
                .registrarMovimientoManual(any(), anyInt(), any(), any(), anyLong());

    }

    @Test
    @DisplayName("Test para obtener ganancias del mes actual")
    void getGananciasDelMesActual_deberiaRetornarSuma() {
        when(ventaRepository.sumTotalNetoByFechaBetween(any(), any())).thenReturn(Optional.of(500.0));
        Double result = ventaService.getGananciasDelMesActual();
        assertEquals(500.0, result);
    }

    @Test
    @DisplayName("Test para obtener ganancias del dia actual")
    void getGananciasDelDiaActual_deberiaRetornarSuma() {
        when(ventaRepository.sumTotalNetoByFechaBetween(any(), any())).thenReturn(Optional.of(100.0));
        Double result = ventaService.getGananciasDelDiaActual();
        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Test para construir lineas, debe devolver el recibo")
    void buildReceiptLines_deberiaDevolverLineasDeRecibo() {
        when(receiptBuilderService.buildReceiptLines(any())).thenReturn(List.of("LINEA1"));
        List<String> result = ventaService.buildReceiptLines(venta);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test para crear venta, debe crear correctamente")
    void crearVenta_deberiaCrearVentaCorrectamente() {
        DetalleVentaRequest detalle = DetalleVentaRequest.builder()
                .productoId(1L)
                .cantidad(2)
                .valorDescuentoUnitario(0.0)
                .tipoDescuentoUnitario(TipoDescuento.VALOR)
                .build();

        PagoRequest pago = new PagoRequest();
        pago.setMedioPago(MedioPago.MERCADO_PAGO);
        pago.setMonto(200.0);

        VentaRequest request = VentaRequest.builder()
                .usuarioId(1L)
                .tipoCliente(TipoCliente.DETALLE)
                .detalles(List.of(detalle))
                .pagos(List.of(pago))
                .build();

        when(cajaRepository.findByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(caja));
        when(usuarioService.obtenerUsuarioRealPorId(1L)).thenReturn(usuario);
        when(productoService.obtenerProductoRealPorId(anyLong())).thenReturn(new Producto());
        when(calculoPrecioService.determinarPrecioUnitario(any(), any())).thenReturn(50.0);

        PagoVenta pagoVenta = new PagoVenta();
        when(pagoService.procesarPagos(any(), anyDouble()))
                .thenReturn(new DatosPagoProcesado(List.of(pagoVenta), 0.0));

        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse response = new VentaResponse();
        when(ventaMapper.toResponse(any())).thenReturn(response);

        VentaResponse result = ventaService.crearVenta(request);
        assertNotNull(result);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Test para crear venta con cliente, debe crear correctamente")
    void crearVenta_conCliente_deberiaCrearVentaCorrectamente() {
        DetalleVentaRequest detalle = DetalleVentaRequest.builder()
                .productoId(1L)
                .cantidad(2)
                .valorDescuentoUnitario(0.0)
                .tipoDescuentoUnitario(TipoDescuento.VALOR)
                .build();

        PagoRequest pago = new PagoRequest();
        pago.setMedioPago(MedioPago.MERCADO_PAGO);
        pago.setMonto(200.0);

        VentaRequest request = VentaRequest.builder()
                .usuarioId(1L)
                .clienteId(1L)
                .tipoCliente(TipoCliente.DETALLE)
                .detalles(List.of(detalle))
                .pagos(List.of(pago))
                .build();

        when(cajaRepository.findByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(caja));
        when(productoService.obtenerProductoRealPorId(anyLong())).thenReturn(new Producto());
        when(calculoPrecioService.determinarPrecioUnitario(any(), any())).thenReturn(50.0);

        PagoVenta pagoVenta = new PagoVenta();
        when(pagoService.procesarPagos(any(), anyDouble()))
                .thenReturn(new DatosPagoProcesado(List.of(pagoVenta), 0.0));

        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse response = new VentaResponse();
        when(ventaMapper.toResponse(any())).thenReturn(response);

        VentaResponse result = ventaService.crearVenta(request);
        assertNotNull(result);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Test para crear venta, sin caja abierta debe lanzar CajaCerradaException")
    void crearVenta_sinCajaAbiertaDebeLanzarExcepcion() {

        when(cajaRepository.findByEstado(EstadoCaja.ABIERTA))
                .thenReturn(Optional.empty());

        VentaRequest request = VentaRequest.builder()
                .usuarioId(1L)
                .tipoCliente(TipoCliente.DETALLE)
                .detalles(List.of())
                .pagos(List.of())
                .build();

        CajaCerradaException ex = assertThrows(
                CajaCerradaException.class,
                () -> ventaService.crearVenta(request)
        );

        assertEquals("No hay ninguna caja abierta.", ex.getMessage());
    }

}