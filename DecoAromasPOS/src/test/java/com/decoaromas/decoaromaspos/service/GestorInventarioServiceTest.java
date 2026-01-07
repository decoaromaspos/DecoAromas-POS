package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.venta.DetalleVentaRequest;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorInventarioServiceTest {

    @Mock(lenient = true)
    private ProductoRepository productoRepository;
    @Mock(lenient = true)
    private MovimientoInventarioService movimientoService;
    @Mock(lenient = true)
    private UsuarioService usuarioService;
    @InjectMocks
    private GestorInventarioService gestorInventarioService;
    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {

        producto = new Producto();
        producto.setProductoId(1L);
        producto.setNombre("Producto Test");
        producto.setStock(10);

        usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setNombre("Usuario Test");

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioService.obtenerUsuarioRealPorId(1L)).thenReturn(usuario);
    }

    @Test
    @DisplayName("Test para validar el stock disponible, stock suficiente no lanza excepcion")
    void validarStockDisponible_conStockSuficiente_noLanzaExcepcion() {
        DetalleVentaRequest detalle = crearDetalleVentaRequest(1L, 5, 0.0, TipoDescuento.VALOR);
        assertDoesNotThrow(() -> gestorInventarioService.validarStockDisponible(List.of(detalle)));
        verify(productoRepository).findById(1L);
    }


    @Test
    @DisplayName("Test para registrar stock incial, cantida positiva")
    void registrarStockInicial_conCantidadPositiva_creaMovimiento() {
        gestorInventarioService.registrarStockInicial(producto, 5, 1L);
        verify(movimientoService).crearMovimientoInventario(
                eq(producto),
                eq(usuario),
                eq(5),
                eq(TipoMovimiento.ENTRADA),
                eq(MotivoMovimiento.PRODUCCION)
        );
    }

    @Test
    @DisplayName("Test para registrar stock incial, cantida cero no hace cambios")
    void registrarStockInicial_conCantidadCero_noHaceNada() {
        gestorInventarioService.registrarStockInicial(producto, 0, 1L);
        verifyNoInteractions(movimientoService);
    }

    @Test
    @DisplayName("Test para registrar salida de stock suficiente, actualiza el stock")
    void registrarSalidaDeStock_conStockSuficiente_retornaMovimientoYActualizaStock() {
        int cantidad = 3;
        MovimientoInventario movimiento = gestorInventarioService.registrarSalidaDeStock(producto, cantidad, usuario);
        assertEquals(7, producto.getStock()); // 10 - 3
        assertEquals(cantidad, movimiento.getCantidad());
        assertEquals(TipoMovimiento.SALIDA, movimiento.getTipo());
        assertEquals(MotivoMovimiento.VENTA, movimiento.getMotivo());
        assertEquals(usuario, movimiento.getUsuario());
        assertEquals(producto, movimiento.getProducto());
    }

    @Test
    @DisplayName("Test para registrar salida de stock insuficiente, lanza excepcion")
    void registrarSalidaDeStock_conStockInsuficiente_lanzaExcepcion() {
        int cantidad = 15;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gestorInventarioService.registrarSalidaDeStock(producto, cantidad, usuario));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Test para actualizar stock absoluto,sin cambio, retorna lista de movimiento")
    void actualizarStockAbsoluto_sinCambio_retornaProductoSinLlamarMovimiento() {
        Producto resultado = gestorInventarioService.actualizarStockAbsoluto(1L, producto.getStock(), usuario.getUsuarioId());
        assertEquals(producto, resultado);
        verifyNoInteractions(movimientoService);
    }

    @Test
    @DisplayName("Test para actualizar stock absoluto, con entrada, crea el movimiento de entrada")
    void actualizarStockAbsoluto_conEntrada_creaMovimientoEntrada() {
        Producto resultado = gestorInventarioService.actualizarStockAbsoluto(1L, 15, usuario.getUsuarioId());
        assertEquals(15, resultado.getStock());
        verify(movimientoService).crearMovimientoInventario(
                eq(producto),
                eq(usuario),
                eq(5),
                eq(TipoMovimiento.ENTRADA),
                eq(MotivoMovimiento.CORRECCION)
        );
    }

    @Test
    @DisplayName("Test para actualizar stock absoluto, con salida, crea el movimiento de salida")
    void actualizarStockAbsoluto_conSalida_creaMovimientoSalida() {
        Producto resultado = gestorInventarioService.actualizarStockAbsoluto(1L, 5, usuario.getUsuarioId());
        assertEquals(5, resultado.getStock());
        verify(movimientoService).crearMovimientoInventario(
                eq(producto),
                eq(usuario),
                eq(5),
                eq(TipoMovimiento.SALIDA),
                eq(MotivoMovimiento.CORRECCION)
        );
    }

    @Test
    @DisplayName("Test para registro manual de movimiento, con entrada valida, actualiza el stock y el movimiento es creado")
    void registrarMovimientoManual_conEntrada_actualizaStockYcreaMovimiento() {
        Producto resultado = gestorInventarioService.registrarMovimientoManual(1L, 5, TipoMovimiento.ENTRADA, MotivoMovimiento.AJUSTE_VENTA, usuario.getUsuarioId());
        assertEquals(15, resultado.getStock());
        verify(movimientoService).crearMovimientoInventario(
                eq(producto),
                eq(usuario),
                eq(5),
                eq(TipoMovimiento.ENTRADA),
                eq(MotivoMovimiento.AJUSTE_VENTA)
        );
    }

    @Test
    @DisplayName("Test para registro manual de movimiento, con salida valida, el stock es insuficiente y lanza excpecion")
    void registrarMovimientoManual_conSalida_stockInsuficiente_lanzaExcepcion() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gestorInventarioService.registrarMovimientoManual(1L, 20, TipoMovimiento.SALIDA, MotivoMovimiento.AJUSTE_VENTA, usuario.getUsuarioId()));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Test para registro manual de movimiento, con salida valida, se actualiza el stock")
    void registrarMovimientoManual_conSalida_valido_actualizaStock() {
        Producto resultado = gestorInventarioService.registrarMovimientoManual(1L, 5, TipoMovimiento.SALIDA, MotivoMovimiento.AJUSTE_VENTA, usuario.getUsuarioId());
        assertEquals(5, resultado.getStock());
        verify(movimientoService).crearMovimientoInventario(
                eq(producto),
                eq(usuario),
                eq(5),
                eq(TipoMovimiento.SALIDA),
                eq(MotivoMovimiento.AJUSTE_VENTA)
        );
    }

    @Test
    @DisplayName("Test para guardar movimientps, delega llamada al servicio")
    void guardarMovimientos_delegaLlamadaAlServicio() {
        List<MovimientoInventario> movimientos = List.of(
                MovimientoInventario.builder().cantidad(1).build(),
                MovimientoInventario.builder().cantidad(2).build()
        );

        gestorInventarioService.guardarMovimientos(movimientos);

        verify(movimientoService).guardarListaMovimientos(movimientos);
    }

    private DetalleVentaRequest crearDetalleVentaRequest(Long productoId, Integer cantidad, Double valor, TipoDescuento tipoDescuento) {
        try {
            Constructor<DetalleVentaRequest> constructor = DetalleVentaRequest.class.getDeclaredConstructor(
                    Long.class, Integer.class, Double.class, TipoDescuento.class);
            constructor.setAccessible(true);
            return constructor.newInstance(productoId, cantidad, valor, tipoDescuento);
        } catch (Exception e) {
            throw new RuntimeException("Error creando DetalleVentaRequest con reflexión", e);
        }
    }


    @Test
    @DisplayName("Test para validar stock disponible, con stock insuficiente lanza excepcion")
    void validarStockDisponible_conStockInsuficiente_lanzaBusinessException() {
        DetalleVentaRequest detalle = crearDetalleVentaRequest(1L, 15, 0.0, TipoDescuento.VALOR);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> gestorInventarioService.validarStockDisponible(List.of(detalle)));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Test para obtener producto por ID, si no existe lanza excepcion")
    void obtenerProductoRealPorId_noExiste_lanzaResourceNotFoundException() {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> {
                    DetalleVentaRequest d = crearDetalleVentaRequest(999L, 1, 0.0, TipoDescuento.VALOR);
                    gestorInventarioService.validarStockDisponible(List.of(d));
                });

        assertTrue(ex.getMessage().contains("No existe producto con id"));
    }
}
