package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoFilterDTO;
import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoInventarioResponse;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.MovimientoInventarioMapper;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.MovimientoInventarioRepository;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import com.decoaromas.decoaromaspos.utils.MovimientoSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Servicio para la gestión (principalmente lectura y registro)
 * de la entidad MovimientoInventario.
 * Este servicio NO actualiza el stock, solo lo registra.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movRepository;
    private final MovimientoInventarioMapper movMapper;

    /**
     * Obtiene una lista de todos los movimientos de inventario.
     * @return Lista de MovimientoInventarioResponse.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listarMovimientos() {
        return mapToList(movRepository.findAll());
    }

    /**
     * Obtiene una lista paginada de todos los movimientos de inventario.
     * @param page   Número de página (base 0).
     * @param size   Tamaño de la página.
     * @param sortBy Campo por el cual ordenar.
     * @return PaginacionResponse con los movimientos.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<MovimientoInventarioResponse> obtenerMovimientosPaginados(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        Page<MovimientoInventario> movsPage = movRepository.findAll(pageable);
        Page<MovimientoInventarioResponse> responsePage = movsPage.map(movMapper::toResponse);

        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Obtiene movimientos paginados y filtrados por un rango de fechas, tipo, motivo o usuario.
     * @param page        Número de página.
     * @param size        Tamaño de la página.
     * @param sortBy      Campo de ordenamiento.
     * @param fechaInicio Fecha de inicio del rango (opcional).
     * @param fechaFin    Fecha de fin del rango (opcional).
     * @param dto         DTO para filtros de movimientos.
     * @return PaginacionResponse con los movimientos filtrados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<MovimientoInventarioResponse> getMovimientosFiltrados(
            int page, int size, String sortBy,
            LocalDate fechaInicio, LocalDate fechaFin, MovimientoFilterDTO dto) {

        ZonedDateTime startOfDay = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime endOfDay = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        Specification<MovimientoInventario> filtros = MovimientoSpecification.conFiltros(startOfDay, endOfDay, dto);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<MovimientoInventario> movsPage = movRepository.findAll(filtros, pageable);
        Page<MovimientoInventarioResponse> responsePage = movsPage.map(movMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }


    /**
     * Obtiene todos los movimientos ocurridos en una fecha específica.
     * @param fecha La fecha a consultar.
     * @return Lista de MovimientoInventarioResponse.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> obtenerMovimientosPorFecha(LocalDate fecha) {
        ZonedDateTime startOfDay = DateUtils.obtenerInicioDiaSegunFecha(fecha);
        ZonedDateTime endOfDay = DateUtils.obtenerFinDiaSegunFecha(fecha);

        return mapToList(movRepository.findByFechaBetween(startOfDay, endOfDay));
    }

    /**
     * Obtiene todos los movimientos de un producto específico.
     * @param id Id del producto.
     * @return Lista de MovimientoInventarioResponse.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> obtenerMovimientosPorProductoId(Long id) {
        return mapToList(movRepository.findByProducto_ProductoId(id));
    }

    /**
     * Obtiene todos los movimientos con un motivo específico.
     * @param motivo El motivo (VENTA, CORRECCION, etc.).
     * @return Lista de MovimientoInventarioResponse.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> obtenerMovimientosPorMotivo(MotivoMovimiento motivo) {
        return mapToList(movRepository.findByMotivo(motivo));
    }

    /**
     * Obtiene todos los movimientos realizados por un usuario específico.
     * @param idUsuario ID del usuario.
     * @return Lista de MovimientoInventarioResponse.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> obtenerMovimientosPorIdUsuario(Long idUsuario) {
        return mapToList(movRepository.findByUsuario_UsuarioId(idUsuario));
    }

    /**
     * Obtiene un movimiento de inventario por su ID.
     * @param id Id del movimiento.
     * @return MovimientoInventarioResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public MovimientoInventarioResponse obtenerMovimientoPorId(Long id) {
        return movMapper.toResponse(obtenerMovimientoRealPorId(id));
    }


    /**
     * Crea y guarda un único movimiento de inventario.
     * Este método es llamado por servicios superiores (como GestorInventarioService, que ya han resuelto las entidades.
     * No se requiere EndPoint para crear movimiento, estos se hacen automáticamente en la venta y al actualizar stock
     * @param producto La entidad Producto afectada.
     * @param usuario  La entidad Usuario que realiza la acción.
     * @param cantidad La cantidad de stock movida.
     * @param tipo     ENTRADA o SALIDA.
     * @param motivo   La razón del movimiento (VENTA, AJUSTE, etc.).
     * @return MovimientoInventarioResponse del movimiento creado.
     */
    public MovimientoInventarioResponse crearMovimientoInventario(Producto producto, Usuario usuario, int cantidad, TipoMovimiento tipo, MotivoMovimiento motivo) {
        MovimientoInventario mov = MovimientoInventario.builder()
                .fecha(DateUtils.obtenerFechaHoraActual())
                .tipo(tipo)
                .motivo(motivo)
                .cantidad(cantidad)
                .usuario(usuario)
                .producto(producto)
                .build();
        return movMapper.toResponse(movRepository.save(mov));
    }


    /**
     * Guarda una lista de movimientos de inventario (ej: desde una Venta).
     * Método es optimizado para inserciones en batch.
     * @param movimientos Lista de entidades MovimientoInventario (ya construidas).
     * @return La lista de movimientos guardados.
     */
    public List<MovimientoInventario> guardarListaMovimientos(List<MovimientoInventario> movimientos) {
        return movRepository.saveAll(movimientos);
    }


    // --- MÉTODOS PRIVADOS ---

    /**
     * Helper interno para obtener la entidad MovimientoInventario por ID.
     * @param id Id del movimiento.
     * @return La entidad MovimientoInventario.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    private MovimientoInventario obtenerMovimientoRealPorId(Long id) {
        return movRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento " + id + " no encontrado"));
    }

    /**
     * Helper privado para mapear listas de entidades a DTOs.
     * @param movimientos Lista de entidades.
     * @return Lista de DTOs (MovimientoInventarioResponse).
     */
    private List<MovimientoInventarioResponse> mapToList(List<MovimientoInventario> movimientos) {
        return movimientos.stream()
                .map(movMapper::toResponse)
                .toList();
    }
}