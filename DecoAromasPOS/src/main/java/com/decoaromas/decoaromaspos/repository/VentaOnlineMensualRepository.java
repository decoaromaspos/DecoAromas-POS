package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.reportes.VentaMensualDTO;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaOnlineMensualRepository extends JpaRepository<VentaOnlineMensual, Long> {

    List<VentaOnlineMensual> findByAnio(Integer anio);
    Optional<VentaOnlineMensual> findByAnioAndMes(Integer anio, Integer mes);

    List<VentaOnlineMensual> findByAnioOrderByMesAsc(int anio);


    // Filtros suma ganancias online según año y mes (opcional)
    @Query("SELECT SUM(v.totalDetalle) FROM VentaOnlineMensual v WHERE v.anio = :anio " +
            "AND (:mes IS NULL OR v.mes = :mes)")
    Double sumTotalDetalleByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("SELECT SUM(v.totalMayorista) FROM VentaOnlineMensual v WHERE v.anio = :anio " +
            "AND (:mes IS NULL OR v.mes = :mes)")
    Double sumTotalMayoristaByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("SELECT SUM(v.totalDetalle + v.totalMayorista) FROM VentaOnlineMensual v WHERE v.anio = :anio " +
            "AND (:mes IS NULL OR v.mes = :mes)")
    Double sumTotalGeneralByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);




    /**
     * Calcula la suma del total de ventas online por mes, aplicando un filtro opcional por tipo de cliente.
     * @param anio El año para el reporte.
     * @param tipoCliente El tipo de cliente (DETALLE, MAYORISTA). Si es nulo, se suman ambos.
     * @return Una lista de VentaMensualDTO con el mes y el total calculado.
     */
    @Query("SELECT v.mes as mes, " +
            "SUM(CASE " +
            "      WHEN :tipoCliente = 'DETALLE' THEN v.totalDetalle " +
            "      WHEN :tipoCliente = 'MAYORISTA' THEN v.totalMayorista " +
            "      WHEN :tipoCliente IS NULL THEN v.totalDetalle + v.totalMayorista " +
            "      ELSE 0 " +
            "    END) as total " +
            "FROM VentaOnlineMensual v " +
            "WHERE v.anio = :anio " +
            "GROUP BY v.mes " +
            "ORDER BY v.mes ASC")
    List<VentaMensualDTO> findTotalVentasOnlinePorMes(
            @Param("anio") int anio,
            @Param("tipoCliente") String tipoCliente
    );


    @Query("SELECT " +
            "SUM(CASE " +
            "  WHEN :tipoCliente = 'DETALLE' THEN v.totalDetalle " +
            "  WHEN :tipoCliente = 'MAYORISTA' THEN v.totalMayorista " +
            "  ELSE (v.totalDetalle + v.totalMayorista) " +
            "END) " +
            "FROM VentaOnlineMensual v " +
            "WHERE (v.anio > :anioInicio OR (v.anio = :anioInicio AND v.mes >= :mesInicio)) " +
            "AND (v.anio < :anioFin OR (v.anio = :anioFin AND v.mes <= :mesFin))")
    Double sumVentasOnlineByRango(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("tipoCliente") String tipoClienteStr);
}
