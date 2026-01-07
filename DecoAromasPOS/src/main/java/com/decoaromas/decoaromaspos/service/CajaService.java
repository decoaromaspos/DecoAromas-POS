package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.caja.AbrirCajaRequest;
import com.decoaromas.decoaromaspos.dto.caja.CajaResponse;
import com.decoaromas.decoaromaspos.dto.caja.CajaResumenResponse;
import com.decoaromas.decoaromaspos.dto.caja.PagoPorMedioDTO;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.CajaMapper;
import com.decoaromas.decoaromaspos.model.Caja;
import com.decoaromas.decoaromaspos.repository.CajaRepository;
import com.decoaromas.decoaromaspos.repository.PagoVentaRepository;
import com.decoaromas.decoaromaspos.repository.VentaRepository;
import com.decoaromas.decoaromaspos.utils.CajaSpecification;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de Cajas (apertura, cierre, consultas).
 * Orquesta las operaciones de caja y calcula los resúmenes de ventas
 * consultando directamente los repositorios de pagos y ventas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CajaService {

    private final CajaRepository cajaRepository;
    private final UsuarioService usuarioService;
    private final VentaRepository ventaRepository; // Añadida para sumar vueltos
    private final PagoVentaRepository pagoVentaRepository; // Añadido para sumar pagos
    private final CajaMapper cajaMapper;

    /**
     * Obtiene una lista de todas las cajas.
     * @return Lista de CajaResponse.
     */
    @Transactional(readOnly = true)
    public List<CajaResponse> listarCajas() {
        return cajaRepository.findAll().stream()
                .map(cajaMapper::toResponse)
                .toList();
    }

    /**
     * Obtiene una caja por su ID.
     * @param id El ID de la caja.
     * @return CajaResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaPorId(Long id) {
        return cajaMapper.toResponse(obtenerCajaRealPorId(id));
    }

    /**
     * Obtiene la caja que se encuentra actualmente en estado ABIERTA.
     * @return CajaResponse.
     * @throws ResourceNotFoundException si no hay ninguna caja abierta.
     */
    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaAbierta() {
        return cajaMapper.toResponse(obtenerCajaRealAbierta());
    }


    /**
     * Abre una nueva caja con un monto inicial.
     * Falla si ya existe otra caja abierta.
     * @param request DTO con el efectivo de apertura y el ID de usuario.
     * @return CajaResponse de la caja recién abierta.
     * @throws BusinessException si ya existe una caja abierta.
     */
    public CajaResponse abrirCaja(AbrirCajaRequest request) {
        // Validación que no existe caja abierta
        boolean existeCajaAbierta = cajaRepository.existsByEstado(EstadoCaja.ABIERTA);
        if (existeCajaAbierta) {
            throw new BusinessException("Ya existe una caja abierta. Debe cerrarla antes de abrir una nueva.");
        }

        Caja caja = Caja.builder()
                .fechaApertura(DateUtils.obtenerFechaHoraActual())
                .efectivoApertura(request.getEfectivoApertura())
                .estado(EstadoCaja.ABIERTA)
                // Dependencia de UsuarioService justificada por validación de existencia y activo
                .usuario(usuarioService.obtenerUsuarioRealPorId(request.getUsuarioId()))
                .build();

        return cajaMapper.toResponse(cajaRepository.save(caja));
    }

    /**
     * Cierra la caja actualmente abierta.
     * Calcula el resumen de pagos, lo compara con el efectivo real contado
     * y registra la diferencia si existe.
     *
     * @param efectivoRealContado El monto de efectivo contado físicamente.
     * @return CajaResponse de la caja cerrada.
     * @throws ResourceNotFoundException si no hay caja abierta para cerrar.
     * @throws BusinessException si el efectivo de cierre es nulo.
     */
    public CajaResponse cerrarCaja(Double efectivoRealContado) {
        if (efectivoRealContado == null) {
            throw new BusinessException("Debe ingresar el efectivo real contado para cerrar la caja.");
        }

        Caja caja = obtenerCajaRealAbierta();
        CajaResumenResponse ventasResumen = calcularResumenCaja(caja);

        caja.setFechaCierre(DateUtils.obtenerFechaHoraActual());
        caja.setEstado(EstadoCaja.CERRADA);

        // Guardamos lo que el sistema dice que se vendió por cada medio (Totales Teóricos)
        caja.setMercadoPagoCierre(ventasResumen.getTotalMercadoPago());
        caja.setBciCierre(ventasResumen.getTotalBCI());
        caja.setBotonDePagoCierre(ventasResumen.getTotalBotonDePago());
        caja.setTransferenciaCierre(ventasResumen.getTotalTransferencia());
        caja.setPostCierre(ventasResumen.getTotalPost());

        // 1. Guardamos el EFECTIVO REAL contado por el usuario (la realidad física)
        caja.setEfectivoCierre(efectivoRealContado);

        // 2. Calculamos cuánto efectivo DEBERÍA haber: (Apertura + Ventas Efectivo - Vueltos)
        // ventasResumen.getTotalEfectivo() ya viene con (Efectivo Bruto - Vueltos) de calcularResumenCaja
        double efectivoEsperado = caja.getEfectivoApertura() + ventasResumen.getTotalEfectivo();

        // 3. Diferencia: Real - Esperado
        // Si da negativo: falta dinero (faltante). Si da positivo: sobra dinero (sobrante).
        double diferencia = efectivoRealContado - efectivoEsperado;

        // Usamos un umbral pequeño para evitar problemas de precisión decimal
        if (Math.abs(diferencia) < 0.01) {
            caja.setDiferenciaReal(0.0);
        } else {
            caja.setDiferenciaReal(diferencia);
        }

        return cajaMapper.toResponse(cajaRepository.save(caja));
    }

    /**
     * Obtiene el resumen de ventas (totales por medio de pago) de una caja específica por ID.
     *
     * @param cajaId Id de la caja.
     * @return CajaResumenResponse con los totales.
     */
    @Transactional(readOnly = true)
    public CajaResumenResponse obtenerResumenCajaById(Long cajaId) {
        Caja caja = obtenerCajaRealPorId(cajaId);
        return calcularResumenCaja(caja);
    }

    /**
     * Obtiene el resumen de ventas de la caja actualmente abierta.
     *
     * @return CajaResumenResponse con los totales.
     * @throws ResourceNotFoundException si la caja no está abierta.
     */
    @Transactional(readOnly = true)
    public CajaResumenResponse obtenerResumenCajaAbierta() {
        Caja caja = obtenerCajaRealAbierta(); // Validación de estado ABIERTA
        return calcularResumenCaja(caja);
    }

    /**
     * Elimina físicamente una caja (Hard delete).
     * ¡Advertencia! Esto puede fallar si hay ventas asociadas (FK constraint).
     * @param id Id de la caja a eliminar.
     */
    public void eliminarCaja(Long id) {
        Caja caja = obtenerCajaRealPorId(id);
        cajaRepository.delete(caja);
    }

    /**
     * Obtiene cajas paginadas y filtradas por fecha, estado, cuadratura o usuario.
     * @param page        Número de página.
     * @param size        Tamaño de página.
     * @param sortBy      Campo de ordenamiento.
     * @param fechaInicio Filtro de fecha (opcional).
     * @param fechaFin    Filtro de fecha (opcional).
     * @param estado      Filtro de estado (opcional).
     * @param cuadrada    Filtro de cuadratura (true=0, false!=0, null=todas).
     * @param usuarioId   Filtro por ID de usuario (opcional).
     * @return PaginacionResponse con las cajas filtradas.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<CajaResponse> getCajasFiltradas(int page, int size, String sortBy,
                                                              LocalDate fechaInicio, LocalDate fechaFin,
                                                              EstadoCaja estado, Boolean cuadrada, Long usuarioId) {
        ZonedDateTime startOfDay = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime endOfDay = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        Specification<Caja> filtros = CajaSpecification.conFiltros(startOfDay, endOfDay, estado, cuadrada, usuarioId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<Caja> cajasPage = cajaRepository.findAll(filtros, pageable);
        Page<CajaResponse> responsePage = cajasPage.map(cajaMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }


    // --- Lógica interna / Helpers ---

    /**
     * Helper para obtener la entidad Caja que está ABIERTA.
     * @return La entidad Caja.
     * @throws ResourceNotFoundException si no hay caja abierta.
     */
    public Caja obtenerCajaRealAbierta() {
        return cajaRepository.findByEstado(EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna caja abierta."));
    }

    /**
     * Helper para obtener la entidad Caja por ID.
     * @param id Id de la caja.
     * @return La entidad Caja.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    public Caja obtenerCajaRealPorId(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja con id " + id + " no encontrada"));
    }

    /**
     * Calcula el resumen de pagos de una caja de forma eficiente,
     * consultando directamente la base de datos en lugar de cargar todas las ventas.
     * @param caja La entidad Caja (abierta o cerrada).
     * @return CajaResumenResponse con los totales.
     */
    public CajaResumenResponse calcularResumenCaja(Caja caja) {
        // 1. Obtener los totales de pagos agrupados por medio de pago
        List<PagoPorMedioDTO> pagosAgrupados = pagoVentaRepository.sumTotalesByCajaId(caja.getCajaId());

        // 2. Obtener la suma total de vueltos dados en esa caja
        double totalVuelto = ventaRepository.sumVueltoByCajaIdCoalesce(caja.getCajaId());

        // 3. Convertir la lista a un Mapa para fácil acceso
        Map<MedioPago, Double> mapaPagos = pagosAgrupados.stream()
                .collect(Collectors.toMap(PagoPorMedioDTO::getMedioPago, PagoPorMedioDTO::getTotal));

        // 4. Obtener totales (con 0.0 si no hubo pagos de ese tipo)
        double totalEfectivoBruto = mapaPagos.getOrDefault(MedioPago.EFECTIVO, 0.0);
        double totalMercadoPago = mapaPagos.getOrDefault(MedioPago.MERCADO_PAGO, 0.0);
        double totalBCI = mapaPagos.getOrDefault(MedioPago.BCI, 0.0);
        double totalBotonDePago = mapaPagos.getOrDefault(MedioPago.BOTON_DE_PAGO, 0.0);
        double totalTransferencia = mapaPagos.getOrDefault(MedioPago.TRANSFERENCIA, 0.0);
        double totalPost = mapaPagos.getOrDefault(MedioPago.POST, 0.0);

        // 5. Calcular el efectivo NETO (Efectivo Recibido - Vueltos Dados)
        double totalEfectivoNeto = totalEfectivoBruto - totalVuelto;

        return new CajaResumenResponse(totalEfectivoNeto, totalMercadoPago, totalBCI, totalBotonDePago, totalTransferencia, totalPost);
    }
}
