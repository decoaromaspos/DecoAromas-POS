package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.caja.PagoPorMedioDTO;
import com.decoaromas.decoaromaspos.dto.reportes.MedioPagoTotalDTO;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {

    @Query("SELECT pv.medioPago as medioPago, SUM(pv.monto) as total " +
            "FROM PagoVenta pv " +
            "WHERE (CAST(:fechaInicio as timestamp) IS NULL OR pv.venta.fecha >= :fechaInicio) " +
            "AND (CAST(:fechaFin as timestamp) IS NULL OR pv.venta.fecha <= :fechaFin) " +
            "AND (:tipoCliente IS NULL OR pv.venta.tipoCliente = :tipoCliente) " +
            "GROUP BY pv.medioPago")
    List<MedioPagoTotalDTO> findTotalPorMedioPago(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente
    );

    /**
     * Calcula la suma total de pagos agrupados por medio de pago
     * para una caja específica.
     * @param cajaId El ID de la caja a consultar.
     * @return Una lista de DTOs con el MedioPago y el total sumado.
     */
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.caja.PagoPorMedioDTO(p.medioPago, SUM(p.monto)) " +
            "FROM PagoVenta p " +
            "WHERE p.venta.caja.cajaId = :cajaId " +
            "GROUP BY p.medioPago")
    List<PagoPorMedioDTO> sumTotalesByCajaId(@Param("cajaId") Long cajaId);
}