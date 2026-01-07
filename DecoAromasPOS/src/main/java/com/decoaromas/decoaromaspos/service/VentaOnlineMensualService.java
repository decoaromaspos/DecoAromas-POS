package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualRequest;
import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.VentaOnlineMensualMapper;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import com.decoaromas.decoaromaspos.repository.VentaOnlineMensualRepository;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Servicio para gestionar los registros de ventas online mensuales.
 * Esta entidad parece ser un resumen o consolidado manual de las ventas
 * totales (detalle y mayorista) para un mes y año específicos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VentaOnlineMensualService {

    private final VentaOnlineMensualRepository ventaOnlineMensualRepository;
    private final VentaOnlineMensualMapper ventaOnlineMensualMapper;

    /**
     * Obtiene una lista de todos los registros de ventas online mensuales.
     * @return Lista de VentaOnlineMensualResponse.
     */
    @Transactional(readOnly = true)
    public List<VentaOnlineMensualResponse> listarVentasOnlineMensuales() {
        return mapToList(ventaOnlineMensualRepository.findAll());
    }

    /**
     * Obtiene todos los registros de ventas online de un año específico.
     * @param anio Integer del año a buscar.
     * @return Lista de VentaOnlineMensualResponse para ese año.
     */
    @Transactional(readOnly = true)
    public List<VentaOnlineMensualResponse> obtenerVentasOnlineMensualesPorAnio(Integer anio) {
        return mapToList(ventaOnlineMensualRepository.findByAnio(anio));
    }

    /**
     * Obtiene un registro de venta online mensual por su ID único.
     * @param id El ID del registro.
     * @return VentaOnlineMensualResponse.
     * @throws ResourceNotFoundException si no se encuentra el registro.
     */
    @Transactional(readOnly = true)
    public VentaOnlineMensualResponse obtenerVentaOnlineMensualPorId(Long id) {
        return ventaOnlineMensualMapper.toResponse(obtenerVentaOnlineMensualRealPorId(id));
    }

    /**
     * Obtiene un registro de venta online mensual por su clave única (Año y Mes).
     * @param anio Integer del año a buscar.
     * @param mes Integer del mes a buscar.
     * @return VentaOnlineMensualResponse.
     * @throws ResourceNotFoundException si no se encuentra el registro para ese mes/año.
     */
    @Transactional(readOnly = true)
    public VentaOnlineMensualResponse obtenerVentaOnlineMensualPorAnioMes(Integer anio, Integer mes) {
        return ventaOnlineMensualMapper.toResponse(obtenerVentaOnlineMensualRealPorAnioMes(anio, mes));
    }

    /**
     * Crea un nuevo registro de venta online mensual.
     * Falla si ya existe un registro para el mismo mes y año o si la fecha es futura.
     * @param request DTO con el año, mes y los totales.
     * @return VentaOnlineMensualResponse del registro creado.
     * @throws ExistsRegisterException si ya existe un registro para ese mes/año.
     * @throws IllegalArgumentException si se intenta crear un registro para un mes futuro.
     */
    public VentaOnlineMensualResponse crearVentaOnlineMensual(VentaOnlineMensualRequest request) {
        validarFecha(request.getAnio(), request.getMes());  // Validación de fecha futura
        // Verificar que no exista registro para el mes y año a crear
        ventaOnlineMensualRepository.findByAnioAndMes(request.getAnio(), request.getMes())
                .ifPresent(existingVenta -> {
                    throw new ExistsRegisterException("Ya existe registro para el mes " + request.getMes() + " y año " + request.getAnio());
                });

        VentaOnlineMensual ventaOnline = VentaOnlineMensual.builder()
                .anio(request.getAnio())
                .mes(request.getMes())
                .totalDetalle(request.getTotalDetalle())
                .totalMayorista(request.getTotalMayorista())
                .fechaIngreso(DateUtils.obtenerFechaHoraActual())
                .build();

        return ventaOnlineMensualMapper.toResponse(ventaOnlineMensualRepository.save(ventaOnline));
    }

    /**
     * Actualiza los totales (detalle y mayorista) de un registro existente,
     * buscándolo por año y mes.
     * @param request DTO con el año, mes y los nuevos totales.
     * @return VentaOnlineMensualResponse del registro actualizado.
     * @throws ResourceNotFoundException si no se encuentra un registro para ese mes/año.
     */
    public VentaOnlineMensualResponse actualizarVentaOnlineMensual(VentaOnlineMensualRequest request) {
        validarFecha(request.getAnio(), request.getMes());

        // Busca el registro existente por la clave de negocio (año y mes)
        VentaOnlineMensual existente = obtenerVentaOnlineMensualRealPorAnioMes(request.getAnio(), request.getMes());

        // Actualiza los campos
        existente.setTotalDetalle(request.getTotalDetalle());
        existente.setTotalMayorista(request.getTotalMayorista());
        existente.setFechaIngreso(DateUtils.obtenerFechaHoraActual());

        return ventaOnlineMensualMapper.toResponse(ventaOnlineMensualRepository.save(existente));
    }

    /**
     * Elimina físicamente un registro de venta online mensual por su ID.
     * @param id El ID del registro a eliminar.
     * @throws ResourceNotFoundException si no se encuentra el registro.
     */
    public void eliminarVentaOnlineMensual(Long id) {
        VentaOnlineMensual ventaOnlineMensual = obtenerVentaOnlineMensualRealPorId(id);
        ventaOnlineMensualRepository.delete(ventaOnlineMensual);
    }


    // ---- lógica privada ----
    /**
     * Validación de que la fecha no sea futura y que el año sea igual o superior a 2023.
     * @param anio Año a validar
     * @param mes Mes a validar
     */
    private void validarFecha(Integer anio, Integer mes) {
        // Validación de antigüedad
        if (anio < 2023) {
            throw new IllegalArgumentException("No se permiten registros anteriores al año 2023.");
        }

        // Validación de fecha futura
        YearMonth fechaIngresada = YearMonth.of(anio, mes);
        YearMonth fechaActual = YearMonth.now();

        if (fechaIngresada.isAfter(fechaActual)) {
            throw new IllegalArgumentException("No se pueden registrar ventas para un mes futuro (" + anio + "-" + mes + ")");
        }
    }

    /**
     * Helper interno para obtener la entidad VentaOnlineMensual por Año y Mes.
     * @param anio El año (ej.: 2024).
     * @param mes  El mes (ej.: 1 para enero).
     * @return La entidad VentaOnlineMensual.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    private VentaOnlineMensual obtenerVentaOnlineMensualRealPorAnioMes(Integer anio, Integer mes) {
        return ventaOnlineMensualRepository.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro para el mes " + mes + " del año " + anio));
    }

    /**
     * Helper interno para obtener la entidad VentaOnlineMensual por ID.
     * @param id El ID del registro.
     * @return La entidad VentaOnlineMensual.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    private VentaOnlineMensual obtenerVentaOnlineMensualRealPorId(Long id) {
        return ventaOnlineMensualRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe venta online mensual con id " + id));
    }

    /**
     * Helper privado para mapear listas de entidades a DTOs.
     * @param ventasOnlineMensual Lista de entidades.
     * @return Lista de VentaOnlineMensualResponse.
     */
    private List<VentaOnlineMensualResponse> mapToList(List<VentaOnlineMensual> ventasOnlineMensual) {
        return ventasOnlineMensual.stream().map(ventaOnlineMensualMapper::toResponse).toList();
    }
}
