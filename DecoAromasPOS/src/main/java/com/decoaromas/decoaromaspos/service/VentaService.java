package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.venta.*;
import com.decoaromas.decoaromaspos.enums.*;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.CajaCerradaException;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.VentaMapper;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.*;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import com.decoaromas.decoaromaspos.utils.VentaSpecification;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio principal para la orquestación y gestión de Ventas.
 * Actúa como un orquestador que delega la lógica de negocio específica a otros servicios:
 * - {@link GestorInventarioService}: Para validación y modificación de stock.
 * - {@link CalculoPrecioService}: Para toda la lógica de precios y descuentos.
 * - {@link PagoService}: Para la validación de pagos mixtos.
 * - {@link ReceiptBuilderService}: Para formatear el recibo de texto.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {

    // --- Repositorios y Servicios de Entidad ---
    private final VentaRepository ventaRepository;
    private final UsuarioService usuarioService;
    private final CajaRepository cajaRepository;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final CotizacionRepository cotizacionRepository;

    // --- Servicios de Lógica de Negocio Extraídos ---
    private final GestorInventarioService gestorInventarioService;
    private final CalculoPrecioService calculoPrecioService;
    private final PagoService pagoService;
    private final ReceiptBuilderService receiptBuilderService;

    // --- Mappers y Utilidades ---
    private final VentaMapper ventaMapper;
    private final AvailabilityChecker checker;


    /**
     * Orquesta la creación de una nueva Venta.
     * 1. Validación de caja abierta y el stock disponible.
     * 2. Construye la Venta.
     * 3. Procesa detalles (cálculos) y genera movimientos de inventario.
     * 4. Calcula totales globales.
     * 5. Procesa y valida los pagos.
     * 6. Guarda la venta y los movimientos.
     * 7. Actualiza el estado de la cotización si aplica.
     * @param request DTO con todos los datos para la venta.
     * @return VentaResponse con la venta creada.
     * @throws IllegalStateException si no hay caja abierta.
     * @throws BusinessException si el stock es insuficiente o el pago es inválido.
     */
    public VentaResponse crearVenta(VentaRequest request) {
        // 1. Validaciones previas (fail-fast)
        Caja caja = cajaRepository.findByEstado(EstadoCaja.ABIERTA)
                .orElseThrow(() -> new CajaCerradaException("No hay ninguna caja abierta."));
        // Validar stock de todos los productos antes de ejecutar lógica
        gestorInventarioService.validarStockDisponible(request.getDetalles());

        // 2. Obtener entidades principales
        Usuario usuario = usuarioService.obtenerUsuarioRealPorId(request.getUsuarioId());
        Cliente cliente = (request.getClienteId() != null)
                ? clienteService.obtenerClienteRealPorId(request.getClienteId())
                : null;

        // 3. Construir la cabecera de la Venta
        Venta venta = buildVentaCabecera(request, usuario, caja, cliente);

        // 4. Procesar detalles (Cálculos, Costos). Crea movimientos de inventario
        List<MovimientoInventario> movimientos = new ArrayList<>();
        procesarDetallesVenta(venta, request.getDetalles(), usuario, movimientos);

        // 5. Calcular totales globales
        calcularTotalesGlobales(venta);

        // 6. Procesamiento y validación de pagos mixtos
        DatosPagoProcesado datosPago = pagoService.procesarPagos(
                request.getPagos(),
                venta.getTotalNeto()
        );
        venta.setVuelto(datosPago.getVuelto());
        datosPago.getPagosEntidad().forEach(venta::addPago); // Asocia los pagos a la venta

        // 7. Guardar venta
        Venta savedVenta = ventaRepository.save(venta);
        gestorInventarioService.guardarMovimientos(movimientos); // Delega el guardado

        // 8. Verificar si venta estaba asociada a cotización
        if (request.getCotizacionId() != null) {
            actualizarEstadoCotizacion(request.getCotizacionId());
        }

        return ventaMapper.toResponse(savedVenta);
    }

    /**
     * Verifica la disponibilidad de un número de documento (Boleta/Factura) para la UI.
     * @param numDocumento El número a verificar.
     * @return AvailabilityResponse indicando si está disponible.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkNumDocumentoAvailability(String numDocumento) {
        String cleanedNumDocumento = numDocumento.trim();
        return checker.check(
                () -> ventaRepository.existsByNumeroDocumentoIgnoreCase(numDocumento),"Número de documento", cleanedNumDocumento);
    }

    /**
     * Actualiza el documento (Tipo y Número) de una venta ya existente.
     * Validación de formato (B123 o F123) y la unicidad del número.
     * @param ventaId Id de la venta a actualizar.
     * @param request DTO con el nuevo tipo y número.
     * @return VentaResponse actualizada.
     * @throws BusinessException si el formato es inválido o el número ya existe.
     */
    public VentaResponse actualizarDocumento(Long ventaId, ActualizarDocumentoRequest request) {
        Venta venta = obtenerVentaRealPorId(ventaId);
        TipoDocumento nuevoTipo = request.getTipoDocumento();

        // Delegar toda la validación y limpieza a un método helper
        String numDocLimpio = validarYLimpiarNumeroDocumento(
                ventaId,
                request.getNumeroDocumento(),
                nuevoTipo
        );

        venta.setTipoDocumento(nuevoTipo);
        venta.setNumeroDocumento(numDocLimpio);

        Venta ventaGuardada = ventaRepository.save(venta);
        return ventaMapper.toResponse(ventaGuardada);
    }

    /**
     * Actualiza el cliente asociado a una venta.
     * @param ventaId Id de la venta a actualizar.
     * @param request DTO con el ID del nuevo cliente (puede ser null).
     * @return VentaResponse actualizada.
     */
    public VentaResponse actualizarCliente(Long ventaId, ActualizarClienteRequest request) {
        Venta venta = obtenerVentaRealPorId(ventaId);

        // Validar cliente
        Cliente nuevoCliente = (request.getClienteId() != null)
                ? clienteService.obtenerClienteRealPorId(request.getClienteId())
                : null;
        venta.setCliente(nuevoCliente);

        Venta ventaGuardada = ventaRepository.save(venta);
        return ventaMapper.toResponse(ventaGuardada);
    }

    /**
     * Obtiene una venta por su ID.
     * @param id Id de la venta.
     * @return VentaResponse.
     * @throws ResourceNotFoundException si la venta no existe.
     */
    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long id) {
        Venta venta = obtenerVentaRealPorId(id);
        return ventaMapper.toResponse(venta);
    }

    /**
     * Lista todas las ventas sin paginación.
     * @return Lista de VentaResponse.
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentas() {
        return ventaRepository.findAll().stream()
                .map(ventaMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene una lista paginada y filtrada de ventas.
     * @param page        Número de página (base 0).
     * @param size        Tamaño de la página.
     * @param sortBy      Campo de ordenamiento.
     * @param fechaInicio Filtro de fecha (opcional).
     * @param fechaFin    Filtro de fecha (opcional).
     * @param dto         Dto con filtros para Venta.
     * @return PaginacionResponse con las ventas filtradas.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<VentaResponse> getVentasFiltradas(
            int page, int size, String sortBy,
            LocalDate fechaInicio, LocalDate fechaFin,
            VentaFilterDTO dto) {

        ZonedDateTime startOfDay = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime endOfDay = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        Specification<Venta> filtros = VentaSpecification.conFiltros(startOfDay, endOfDay, dto);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<Venta> ventasPage = ventaRepository.findAll(filtros, pageable);
        Page<VentaResponse> responsePage = ventasPage.map(ventaMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Elimina una venta y revierte el stock asociado.
     * Delega la lógica de reposición de stock a {@link GestorInventarioService}.
     * @param id        Id de la venta a eliminar.
     * @param usuarioId Id del usuario que realiza la eliminación (para el log de inventario).
     */
    public void eliminarVenta(Long id, Long usuarioId) {
        Venta venta = obtenerVentaRealPorId(id);

        // --- LÓGICA DE INVENTARIO ---
        // Se delega la reposición de stock y la creación de movimientos.
        for (DetalleVenta d : venta.getDetalles()) {
            gestorInventarioService.registrarMovimientoManual(
                    d.getProducto().getProductoId(),
                    d.getCantidad(),
                    TipoMovimiento.ENTRADA,
                    MotivoMovimiento.AJUSTE_VENTA,
                    usuarioId
            );
        }
        // No es necesario guardar movimientos manualmente, GestorInventarioService lo hace.

        ventaRepository.delete(venta);
    }

    /**
     * Calcula la ganancia total (suma de totalNeto) de todas las ventas realizadas en el mes actual.
     * @return La suma total de las ventas del mes. Devuelve 0.0 si no hay ventas.
     */
    @Transactional(readOnly = true)
    public Double getGananciasDelMesActual() {
        YearMonth mesActual = YearMonth.now(DateUtils.ZONE_ID_SANTIAGO);
        ZonedDateTime fechaInicio = mesActual.atDay(1).atStartOfDay(DateUtils.ZONE_ID_SANTIAGO);
        ZonedDateTime fechaFin = mesActual.plusMonths(1).atDay(1).atStartOfDay(DateUtils.ZONE_ID_SANTIAGO);

        return ventaRepository.sumTotalNetoByFechaBetween(fechaInicio, fechaFin).orElse(0.0);
    }

    /**
     * Calcula la ganancia total (suma de totalNeto) de todas las ventas realizadas en el día actual.
     * @return La suma total de las ventas del día. Devuelve 0.0 si no hay ventas.
     */
    @Transactional(readOnly = true)
    public Double getGananciasDelDiaActual() {
        ZonedDateTime inicioDia = DateUtils.obtenerInicioDiaSegunFecha(DateUtils.obtenerFechaActual());
        ZonedDateTime finDia = DateUtils.obtenerFinDiaSegunFecha(DateUtils.obtenerFechaActual());

        return ventaRepository.sumTotalNetoByFechaBetween(inicioDia, finDia).orElse(0.0);
    }


    // --- Lógica interna ---

    /**
     * Obtiene la entidad Venta por su ID.
     * Usado internamente y por otros servicios (ej.: CierreCajaService).
     * @param id Id de la venta.
     * @return La entidad Venta.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    public Venta obtenerVentaRealPorId(Long id) {
        return ventaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
    }

    /**
     * Helper privado para validar, limpiar y verificar la unicidad de un número de documento.
     * Es llamado por `actualizarDocumento`.
     * @param ventaIdExcluir Id de la venta actual (para excluirla de la validación de unicidad), o null si es una creación.
     * @param nuevoNumero    El número de documento (sucio) del request.
     * @param nuevoTipo      El tipo de documento del request.
     * @return El número de documento limpio y validado (ej.: "B12345").
     * @throws BusinessException Si la validación falla.
     */
    private String validarYLimpiarNumeroDocumento(Long ventaIdExcluir, String nuevoNumero, TipoDocumento nuevoTipo) {
        if (nuevoNumero == null || nuevoNumero.trim().isEmpty()) {
            throw new BusinessException("El número de documento no puede estar vacío.");
        }
        // Validar formato
        String numDocLimpio = validarFormatoNumeroDocumento(nuevoNumero, nuevoTipo);

        // Validar Unicidad. Número no repetido.
        Optional<Venta> ventaExistente = ventaRepository.findByNumeroDocumento(numDocLimpio);

        // Comprobar si existe Y (lógicamente) no es la misma venta que estamos editando.
        if (ventaExistente.isPresent() && !ventaExistente.get().getVentaId().equals(ventaIdExcluir)) {
            throw new ExistsRegisterException("El número de documento '" + numDocLimpio + "' ya está asignado a otra venta.");
        }

        return numDocLimpio;
    }

    /**
     * Helper privado que valida si el formato de número de documento es efectivamente válido
     * @param nuevoNumero El número de documento (sucio) del request.
     * @param nuevoTipo   El tipo de documento del request.
     * @return El número de documento validado
     * @throws BusinessException Si el formato es inválido
     */
    private static String validarFormatoNumeroDocumento(String nuevoNumero, TipoDocumento nuevoTipo) {
        String numDocLimpio = nuevoNumero.trim().toUpperCase();

        // Validar formato (B o F, seguido de números)
        if (!numDocLimpio.matches("^[BF]\\d+$")) {
            throw new BusinessException("Formato de documento inválido. Debe ser B (ej: B12345) o F (ej: F54321).");
        }

        // Validar coherencia (BOLETA con B, FACTURA con F)
        if (nuevoTipo == TipoDocumento.BOLETA && !numDocLimpio.startsWith("B")) {
            throw new BusinessException("Tipo BOLETA seleccionado, pero el número no empieza con 'B'.");
        }
        if (nuevoTipo == TipoDocumento.FACTURA && !numDocLimpio.startsWith("F")) {
            throw new BusinessException("Tipo FACTURA seleccionado, pero el número no empieza con 'F'.");
        }
        return numDocLimpio;
    }

    /**
     * Helper privado para construir la entidad Venta (cabecera) desde el Request.
     */
    private Venta buildVentaCabecera(VentaRequest request, Usuario usuario, Caja caja, Cliente cliente) {
        Venta venta = new Venta();
        venta.setFecha(DateUtils.obtenerFechaHoraActual());
        venta.setTipoCliente(request.getTipoCliente());
        venta.setValorDescuentoGlobal(request.getValorDescuentoGlobal());
        venta.setTipoDescuentoGlobal(request.getTipoDescuentoGlobal());
        venta.setTipoDocumento(request.getTipoDocumento());
        venta.setUsuario(usuario);
        venta.setCaja(caja);
        venta.setCliente(cliente);
        return venta;
    }

    /**
     * Helper privado para calcular los descuentos globales y el total neto.
     * Lógica secuencial de descuentos y redondeo (Math.ceil) del total neto.
     * Modifica la entidad Venta por referencia.
     */
    private void calcularTotalesGlobales(Venta venta) {
        // 1. Obtener el subtotal DESPUÉS de descuentos unitarios.
        double totalDescuentosUnitarios = (venta.getTotalDescuentosUnitarios() != null) ? venta.getTotalDescuentosUnitarios() : 0.0;
        double subtotalNetoUnitario = venta.getTotalBruto() - totalDescuentosUnitarios;
        // Ejemplo: $7900 - $79 = $7821

        // 2. Calcular el descuento global sobre ESE subtotal (lógica secuencial).
        double montoDescuentoGlobal = calculoPrecioService.calcularMontoDescuento(
                subtotalNetoUnitario, // <-- BASE CORREGIDA
                venta.getValorDescuentoGlobal(),
                venta.getTipoDescuentoGlobal()
        );

        calculoPrecioService.validarDescuento(montoDescuentoGlobal, subtotalNetoUnitario,
                "El descuento global no puede ser mayor al total después de descuentos unitarios.");

        // 4. Guardar los totales (todavía con decimales)
        venta.setMontoDescuentoGlobalCalculado(montoDescuentoGlobal); // Ej.: 234.63

        double totalDescuentoTotal = totalDescuentosUnitarios + montoDescuentoGlobal; // Ej.: 79 + 234.63 = 313.63
        venta.setTotalDescuentoTotal(totalDescuentoTotal);

        // 5. Calcular el Neto Final (con decimales)
        double totalNetoConDecimales = venta.getTotalBruto() - totalDescuentoTotal; // Ej.: 7900 - 313.63 = 7586.37

        // 6. REDONDEAR el total neto final (CLP no tiene decimales)
        // Usamos Math.ceil() para (redondeo hacia arriba).
        double totalNetoRedondeado = Math.ceil(totalNetoConDecimales); // Ej.: 7587.0

        venta.setTotalNeto(totalNetoRedondeado);
    }

    /**
     * Helper privado para procesar los detalles de la venta.
     * Modifica la Venta por referencia (añade detalles, costo, bruto)
     * y la lista de Movimientos por referencia.
     */
    private void procesarDetallesVenta(Venta venta, List<DetalleVentaRequest> detallesRequest, Usuario usuario, List<MovimientoInventario> movimientos) {
        double totalBrutoAcumulado = 0.0;
        double costoGeneralAcumulado = 0.0;
        double totalDescuentosUnitariosAcumulado = 0.0;

        for (DetalleVentaRequest d : detallesRequest) {
            // La validación de stock ya se hizo. Aquí solo procesamos.
            Producto producto = productoService.obtenerProductoRealPorId(d.getProductoId());

            costoGeneralAcumulado += (producto.getCosto() != null) ? (d.getCantidad() * producto.getCosto()) : 0;

            Double precioUnitario = calculoPrecioService.determinarPrecioUnitario(producto, venta.getTipoCliente());

            // Lógica de cálculo de descuentos unitarios
            double montoDescuentoPorUnidad = calculoPrecioService.calcularMontoDescuento(
                    precioUnitario, d.getValorDescuentoUnitario(), d.getTipoDescuentoUnitario());

            // Validación para que el descuento no supere el precio
            calculoPrecioService.validarDescuento(montoDescuentoPorUnidad, precioUnitario,
                    "El descuento no puede ser mayor al precio del producto.");

            // 2. Calcular totales de la LÍNEA
            double subtotalBrutoLinea = precioUnitario * d.getCantidad();
            double montoDescuentoTotalLinea = montoDescuentoPorUnidad * d.getCantidad();
            double subtotalNetoLinea = subtotalBrutoLinea - montoDescuentoTotalLinea; // Este es el valor que antes llamabas "subtotal"

            DetalleVenta detalle = DetalleVenta.builder()
                    .producto(producto)
                    .codigoBarras(producto.getCodigoBarras())
                    .cantidad(d.getCantidad())
                    .precioUnitario(precioUnitario)
                    .valorDescuentoUnitario(d.getValorDescuentoUnitario())
                    .tipoDescuentoUnitario(d.getTipoDescuentoUnitario())
                    .subtotalBruto(subtotalBrutoLinea)                  // (precio * cant)
                    .montoDescuentoUnitarioCalculado(montoDescuentoTotalLinea) // (descuento * cant)
                    .subtotal(subtotalNetoLinea)                        // (bruto - descuento)
                    .build();

            venta.addDetalle(detalle);

            // 4. Acumular totales para la Venta
            totalBrutoAcumulado += subtotalBrutoLinea; // Se suma el bruto
            totalDescuentosUnitariosAcumulado += montoDescuentoTotalLinea;

            // Lógica de inventario. Registrar movimiento y restar stock
            MovimientoInventario mov = gestorInventarioService
                    .registrarSalidaDeStock(producto, d.getCantidad(), usuario);
            movimientos.add(mov);
        }

        // 5. Asignar los totales acumulados a la Venta
        venta.setTotalBruto(totalBrutoAcumulado);
        venta.setTotalDescuentosUnitarios(totalDescuentosUnitariosAcumulado);
        venta.setCostoGeneral(costoGeneralAcumulado);
    }

    /**
     * Helper para actualizar el estado de una cotización a 'CONVERTIDA'.
     * @param cotizacionId Id de la cotización a actualizar.
     */
    private void actualizarEstadoCotizacion(Long cotizacionId) {
        // Buscar la cotización
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElse(null); // No lanzar excepción si no se encuentra

        if (cotizacion != null && cotizacion.getEstado() != EstadoCotizacion.CONVERTIDA) {
            // Se marca como convertida para sacarla de la lista de pendientes
            cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
            cotizacionRepository.save(cotizacion);
        }
        // Si es null, la venta se crea de todas formas, pero no se asocia
    }


    // --- LÓGICA DE FORMATEO DE RECIBO (DELEGADA) ---

    /**
     * Construye las líneas de texto a imprimir para un comprobante simple (ticket).
     * Delega la lógica de formato a {@link ReceiptBuilderService}.
     * @param venta La entidad Venta a imprimir.
     * @return Una lista de strings, donde cada string es una línea del recibo.
     */
    public List<String> buildReceiptLines(Venta venta) {
        return receiptBuilderService.buildReceiptLines(venta);
    }
}
