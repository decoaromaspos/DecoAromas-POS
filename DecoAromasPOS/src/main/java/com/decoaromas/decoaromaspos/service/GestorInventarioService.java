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
import com.decoaromas.decoaromaspos.utils.DateUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio experto en la gestión de inventario y stock.
 * Es el ÚNICO responsable de modificar el stock de un producto
 * y de registrar el {@link MovimientoInventario} correspondiente.
 */
@Service
@RequiredArgsConstructor
public class GestorInventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioService movimientoService;
    private final UsuarioService usuarioService;

    /**
     * Valida si hay stock disponible para una lista de productos y cantidades.
     * Usado por VentaService antes de confirmar una venta.
     * @param detalles Lista de DTOs con productoId y cantidad.
     * @throws BusinessException si algún producto no tiene stock suficiente.
     */
    @Transactional(readOnly = true)
    public void validarStockDisponible(List<DetalleVentaRequest> detalles) {
        for (DetalleVentaRequest d : detalles) {
            // Se usa el método 'Real' de ProductoService para obtener la entidad
            Producto producto = obtenerProductoRealPorId(d.getProductoId());
            if (producto.getStock() < d.getCantidad()) {
                throw new BusinessException("Stock insuficiente para producto " + producto.getNombre());
            }
        }
    }


    /**
     * Registra el stock inicial de un producto (ej.: al crearlo).
     * Crea un movimiento de ENTRADA por PRODUCCION.
     * @param producto  La entidad Producto.
     * @param cantidad  La cantidad inicial.
     * @param usuarioId El ID del usuario que registra.
     */
    @Transactional
    public void registrarStockInicial(Producto producto, int cantidad, Long usuarioId) {
        if (cantidad <= 0) return;

        // 1. Obtener entidad Usuario
        Usuario usuario = usuarioService.obtenerUsuarioRealPorId(usuarioId);

        // 2. Llamar al servicio de movimiento para crear y guardar
        movimientoService.crearMovimientoInventario(
                producto,
                usuario,
                cantidad,
                TipoMovimiento.ENTRADA,
                MotivoMovimiento.PRODUCCION
        );
    }


    /**
     * Registra una salida de stock por VENTA.
     * Actualiza el stock del producto (descontar) y prepara el movimiento de inventario.
     * @param producto La entidad Producto a modificar.
     * @param cantidad La cantidad a descontar.
     * @param usuario  El usuario que realiza la venta.
     * @return El MovimientoInventario creado (para guardar en batch).
     */
    @Transactional
    public MovimientoInventario registrarSalidaDeStock(Producto producto, int cantidad, Usuario usuario) {
        int stockAnterior = producto.getStock() != null ? producto.getStock() : 0;
        if (stockAnterior < cantidad) {
            throw new BusinessException("Stock insuficiente para " + producto.getNombre());
        }
        producto.setStock(stockAnterior - cantidad); // El producto se guarda por la transacción de VentaService

        // Crea el movimiento para ser guardado después (por VentaService en batch)
        return MovimientoInventario.builder()
                .fecha(DateUtils.obtenerFechaHoraActual())
                .tipo(TipoMovimiento.SALIDA)
                .motivo(MotivoMovimiento.VENTA)
                .cantidad(cantidad)
                .usuario(usuario)
                .producto(producto)
                .build();
    }

    /**
     * Actualiza el stock de un producto a una cantidad absoluta (ej.: conteo de inventario).
     * Calcula la diferencia y crea un movimiento de ENTRADA o SALIDA por CORRECCION.
     * @param idProducto     ID del producto.
     * @param nuevaCantidad  La nueva cantidad total de stock.
     * @param usuarioId      ID del usuario que realiza el ajuste.
     * @return El Producto actualizado y guardado.
     */
    public Producto actualizarStockAbsoluto(Long idProducto, int nuevaCantidad, Long usuarioId) {
        Producto producto = obtenerProductoRealPorId(idProducto);
        Usuario usuario = usuarioService.obtenerUsuarioRealPorId(usuarioId);

        int stockAnterior = producto.getStock() != null ? producto.getStock() : 0;
        int diferencia = nuevaCantidad - stockAnterior;

        if (diferencia == 0) {
            return producto; // No hay cambios
        }

        producto.setStock(nuevaCantidad);

        // Llamar al servicio de movimiento con las entidades resueltas
        movimientoService.crearMovimientoInventario(
                producto,
                usuario,
                Math.abs(diferencia),
                diferencia > 0 ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA,
                MotivoMovimiento.CORRECCION
        );

        // El guardado del producto es implícito por @Transactional y la sesión de Hibernate (dirty-checking)
        return producto;
    }


    /**
     * Registra un movimiento de stock manual (ENTRADA o SALIDA) por motivos
     * como VENTA, AJUSTE_VENTA, PRODUCCION, NUEVO_STOCK, CORRECCION.
     * @param idProducto ID del producto.
     * @param cantidad   Cantidad a mover.
     * @param tipo       ENTRADA o SALIDA.
     * @param motivo     El motivo del movimiento.
     * @param usuarioId  ID del usuario.
     * @return El Producto actualizado.
     */
    @Transactional
    public Producto registrarMovimientoManual(Long idProducto, int cantidad, TipoMovimiento tipo, MotivoMovimiento motivo, Long usuarioId) {
        Producto producto = obtenerProductoRealPorId(idProducto);
        Usuario usuario = usuarioService.obtenerUsuarioRealPorId(usuarioId);

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;

        if (tipo == TipoMovimiento.SALIDA && stockActual < cantidad) {
            throw new BusinessException("Stock insuficiente para realizar la salida manual.");
        }

        int nuevoStock = (tipo == TipoMovimiento.ENTRADA) ? stockActual + cantidad : stockActual - cantidad;
        producto.setStock(nuevoStock);

        // Llamar al servicio de movimiento con las entidades resueltas
        movimientoService.crearMovimientoInventario(
                producto,
                usuario,
                cantidad,
                tipo,
                motivo
        );

        // El guardado del producto es implícito por @Transactional
        return producto;
    }

    /**
     * Guarda una lista de movimientos generada por una venta.
     * @param movimientos Lista de entidades MovimientoInventario.
     */
    public void guardarMovimientos(List<MovimientoInventario> movimientos) {
        movimientoService.guardarListaMovimientos(movimientos);
    }


    // --- HELPER INTERNO ---
    /**
     * Helper privado para obtener la entidad Producto directamente desde el repositorio.
     * @param id El ID del producto.
     * @return La entidad Producto.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    private Producto obtenerProductoRealPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe producto con id " + id));
    }
}
