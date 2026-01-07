package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionRequest;
import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionResponse;
import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionUpdateEstado;
import com.decoaromas.decoaromaspos.dto.cotizacion.DetalleCotizacionRequest;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.EstadoCotizacion;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.CotizacionMapper;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.CotizacionRepository;
import com.decoaromas.decoaromaspos.utils.CotizacionSpecification;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de las Cotizaciones.
 * Se encarga de crear, actualizar, eliminar y consultar cotizaciones,
 * delegando los cálculos de precios a CalculoPrecioService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CotizacionMapper cotizacionMapper;
    private final CalculoPrecioService calculoPrecioService;

    /**
     * Orquesta la creación de una nueva cotización.
     * 1. Valida entidades (Usuario, Cliente).
     * 2. Construye la cabecera.
     * 3. Procesa los detalles y sus precios.
     * 4. Calcula totales y descuentos globales.
     * 5. Guarda en la base de datos.
     *
     * @param request DTO con la información para crear la cotización.
     * @return CotizacionResponse con los datos de la cotización creada.
     */
    public CotizacionResponse crearCotizacion(CotizacionRequest request) {
        // 1. Obtener entidades principales
        Usuario usuario = usuarioService.obtenerUsuarioRealPorId(request.getUsuarioId());
        Cliente cliente = (request.getClienteId() != null)
                ? clienteService.obtenerClienteRealPorId(request.getClienteId())
                : null;

        // 2. Construir la cabecera de la cotización
        Cotizacion cotizacion = buildCotizacionCabecera(request, usuario, cliente);

        // 3. Procesar y agregar detalles
        procesarDetalles(cotizacion, request.getDetalles(), request.getTipoCliente());

        // 4. Calcular totales y descuentos globales.
        calcularTotalesGlobales(cotizacion);

        Cotizacion savedCotizacion = cotizacionRepository.save(cotizacion);
        return cotizacionMapper.toResponse(savedCotizacion);
    }


    /**
     * Elimina físicamente una cotización de la base de datos.
     * Nota: Considerar un borrado lógico (actualizar estado a CANCELADO)
     * para mantener el historial.
     *
     * @param id El ID de la cotización a eliminar.
     */
    public void eliminarCotizacion(Long id) {
        Cotizacion cot = obtenerCotizacionById(id); // Valida si existe
        cotizacionRepository.delete(cot);
    }


    /**
     * Actualiza el estado de una cotización existente (Ej: PENDIENTE -> APROBADA).
     *
     * @param request DTO que contiene el ID de la cotización y el nuevo estado.
     * @return CotizacionResponse con la cotización actualizada.
     */
    public CotizacionResponse actualizarEstadoCotizacion(CotizacionUpdateEstado request) {
        Cotizacion cot = obtenerCotizacionById(request.getCotizacionId());
        cot.setEstado(request.getEstado());
        Cotizacion savedCot = cotizacionRepository.save(cot);
        return cotizacionMapper.toResponse(savedCot);
    }

    /**
     * Obtiene una lista de todas las cotizaciones sin paginación ni filtros.
     *
     * @return Lista de CotizacionResponse.
     */
    public List<CotizacionResponse> listarCotizaciones() {
        return cotizacionRepository.findAll().stream()
                .map(cotizacionMapper::toResponse)
                .toList();
    }

    /**
     * Busca cotizaciones usando paginación y filtros dinámicos.
     *
     * @param page         Número de página (base 0).
     * @param size         Tamaño de la página.
     * @param sortBy       Campo por el cual ordenar (ej: "fechaEmision").
     * @param fechaInicio  Filtro de fecha (opcional).
     * @param fechaFin     Filtro de fecha (opcional).
     * @param tipoCliente  Filtro de tipo de cliente (opcional).
     * @param minTotalNeto Filtro de monto mínimo (opcional).
     * @param maxTotalNeto Filtro de monto máximo (opcional).
     * @param usuarioId    Filtro por ID de usuario (opcional).
     * @param clienteId    Filtro por ID de cliente (opcional).
     * @return PaginacionResponse con la lista de cotizaciones y detalles de paginación.
     */
    public PaginacionResponse<CotizacionResponse> getCotizacionesFiltradas(
            int page, int size, String sortBy,
            LocalDate fechaInicio, LocalDate fechaFin,
            TipoCliente tipoCliente, Double minTotalNeto, Double maxTotalNeto,
            Long usuarioId, Long clienteId) {

        ZonedDateTime startOfDay = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime endOfDay = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        // Se utiliza CotizacionSpecification para construir la consulta dinámica
        Specification<Cotizacion> filtros = CotizacionSpecification.conFiltros(
                startOfDay, endOfDay, tipoCliente, minTotalNeto, maxTotalNeto, usuarioId, clienteId
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<Cotizacion> cotizacionesPage = cotizacionRepository.findAll(filtros, pageable);
        Page<CotizacionResponse> responsePage = cotizacionesPage.map(cotizacionMapper::toResponse);

        return PaginacionMapper.mapToResponse(responsePage);
    }





    // --- Lógica interna ---

    /**
     * Busca una cotización por su ID.
     * Método de utilidad interno (o público) para validaciones.
     *
     * @param id El ID de la cotización.
     * @return La entidad Cotizacion.
     * @throws ResourceNotFoundException si la cotización no existe.
     */
    public Cotizacion obtenerCotizacionById(Long id) {
        return cotizacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada"));
    }

    /**
     * Helper: Construye el objeto Cotizacion (cabecera) a partir del Request.
     * No incluye detalles ni totales.
     *
     * @param request El DTO de entrada.
     * @param usuario La entidad Usuario asociada.
     * @param cliente La entidad Cliente asociada (puede ser null).
     * @return La entidad Cotizacion inicializada.
     */
    private Cotizacion buildCotizacionCabecera(CotizacionRequest request, Usuario usuario, Cliente cliente) {
        return Cotizacion.builder()
                .fechaEmision(DateUtils.obtenerFechaHoraActual())
                .estado(EstadoCotizacion.PENDIENTE) // Estado inicial
                .tipoCliente(request.getTipoCliente())
                .valorDescuentoGlobal(request.getValorDescuentoGlobal())
                .tipoDescuentoGlobal(request.getTipoDescuentoGlobal())
                .usuario(usuario)
                .cliente(cliente)
                .detalles(new ArrayList<>()) // Inicializar la lista para los detalles
                .build();
    }

    /**
     * Helper: Calcula el descuento global y el total neto final de la cotización.
     * Modifica el objeto Cotizacion por referencia.
     *
     * @param cotizacion La cotización que se está procesando (debe tener TotalBruto ya calculado).
     */
    private void calcularTotalesGlobales(Cotizacion cotizacion) {
        // Delega el cálculo del monto al servicio experto
        double montoDescuentoGlobal = calculoPrecioService.calcularMontoDescuento(
                cotizacion.getTotalBruto(),
                cotizacion.getValorDescuentoGlobal(),
                cotizacion.getTipoDescuentoGlobal()
        );
        // Delega la validación
        calculoPrecioService.validarDescuento(montoDescuentoGlobal, cotizacion.getTotalBruto(),
                "El descuento global no puede ser mayor al total bruto.");

        cotizacion.setMontoDescuentoGlobalCalculado(montoDescuentoGlobal);
        cotizacion.setTotalNeto(cotizacion.getTotalBruto() - montoDescuentoGlobal);
    }

    /**
     * Helper: Procesa la lista de DetalleCotizacionRequest, calcula sus precios
     * y subtotales, y los añade a la entidad Cotizacion.
     * Modifica el objeto Cotizacion por referencia (añade detalles, TotalBruto y CostoGeneral).
     *
     * @param cotizacion      La cotización que se está procesando.
     * @param detallesRequest La lista de DTOs de detalles del request.
     * @param tipoCliente     El tipo de cliente para determinar el precio.
     */
    private void procesarDetalles(Cotizacion cotizacion, List<DetalleCotizacionRequest> detallesRequest, TipoCliente tipoCliente) {
        double totalBruto = 0.0;
        double costoGeneral = 0.0;

        for (DetalleCotizacionRequest d : detallesRequest) {
            Producto producto = productoService.obtenerProductoRealPorId(d.getProductoId());
            costoGeneral += (producto.getCosto() != null) ? (d.getCantidad() * producto.getCosto()) : 0;

            // Determinar precio según tipo de cliente (MAYORISTA O DETALLE)
            Double precioUnitario = calculoPrecioService.determinarPrecioUnitario(producto, tipoCliente);

            // Lógica delegada de cálculo de descuentos unitario
            double montoDescuentoUnitario = calculoPrecioService.calcularMontoDescuento(
                    precioUnitario, d.getValorDescuentoUnitario(), d.getTipoDescuentoUnitario());

            calculoPrecioService.validarDescuento(montoDescuentoUnitario, precioUnitario,
                    "El descuento no puede ser mayor al precio del producto.");

            double precioFinalUnitario = precioUnitario - montoDescuentoUnitario;
            double subtotal = precioFinalUnitario * d.getCantidad();

            DetalleCotizacion detalle = DetalleCotizacion.builder()
                    .producto(producto)
                    .codigoBarras(producto.getCodigoBarras())
                    .cantidad(d.getCantidad())
                    .precioUnitario(precioUnitario)
                    .valorDescuentoUnitario(d.getValorDescuentoUnitario())
                    .tipoDescuentoUnitario(d.getTipoDescuentoUnitario())
                    .subtotal(subtotal)
                    .build();

            cotizacion.addDetalle(detalle); // Asocia el detalle con la cotización
            totalBruto += subtotal;
        }

        cotizacion.setTotalBruto(totalBruto);
        cotizacion.setCostoGeneral(costoGeneral);
    }
}