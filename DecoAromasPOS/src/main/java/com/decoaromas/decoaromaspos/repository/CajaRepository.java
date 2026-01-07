package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import com.decoaromas.decoaromaspos.model.Caja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja,Long>, JpaSpecificationExecutor<Caja> {
    Optional<Caja> findByEstado(EstadoCaja estado);

    boolean existsByEstado(EstadoCaja estado);

    // Busca cajas cerradas sin cuadrar
    @Query("SELECT c.cajaId as cajaId, c.usuario.username as usuario, c.fechaCierre as fechaCierre, c.diferenciaReal as diferencia " +
            "FROM Caja c " +
            "WHERE c.diferenciaReal != 0 AND c.estado = 'CERRADA' " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM c.fechaCierre) = :anio) " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM c.fechaCierre) = :mes) " +
            "ORDER BY c.fechaCierre DESC")
    List<DescuadreCajaDTO> findDescuadres(@Param("anio") Integer anio, @Param("mes") Integer mes);

    // Busca cajas cerradas que tengan una diferencia (descuadre).
    @Query(value = "SELECT c.cajaId as cajaId, c.usuario.username as usuario, c.fechaCierre as fechaCierre, c.diferenciaReal as diferencia " +
            "FROM Caja c " +
            "WHERE c.diferenciaReal != 0 AND c.estado = 'CERRADA' " +
            "AND (:anio IS NULL OR EXTRACT(YEAR FROM c.fechaCierre) = :anio) " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM c.fechaCierre) = :mes)",

            countQuery = "SELECT count(c) FROM Caja c " + // Query de conteo
                    "WHERE c.diferenciaReal != 0 AND c.estado = 'CERRADA' " +
                    "AND (:anio IS NULL OR EXTRACT(YEAR FROM c.fechaCierre) = :anio) " +
                    "AND (:mes IS NULL OR EXTRACT(MONTH FROM c.fechaCierre) = :mes)")
    Page<DescuadreCajaDTO> findDescuadresPaginado(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            Pageable pageable
    );

    /**
     * Calcula la suma total de 'vuelto' dado en todas las ventas de una caja específica.
     * @param cajaId El ID de la caja.
     * @return Un Optional<Double> con la suma total de vueltos.
     */
    @Query("SELECT SUM(v.vuelto) FROM Venta v WHERE v.caja.cajaId = :cajaId")
    Optional<Double> sumVueltoByCajaId(@Param("cajaId") Long cajaId);


    /**
     * GRÁFICO 1: Desempeño de Descuadres por Usuario.
     * Obtiene la suma del valor absoluto de las diferencias de caja, agrupadas por usuario.
     * Solo incluye cajas CON diferencia.
     */
    @Query("SELECT NEW com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaPorNombreDTO(u.username, SUM(ABS(c.diferenciaReal))) " +
            "FROM Caja c JOIN c.usuario u " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR c.fechaCierre >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp) IS NULL OR c.fechaCierre <= :fechaFin) " +
            "AND c.diferenciaReal != 0 " +
            "GROUP BY u.username " +
            "ORDER BY SUM(ABS(c.diferenciaReal)) DESC")
    List<VentaAgrupadaPorNombreDTO> findDescuadresPorUsuario(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin
    );

    /**
     * GRÁFICO 2: Distribución de Métodos de Pago (Cierre).
     * Obtiene la suma total de cada método de pago registrado en los cierres de caja.
     */
    @Query("SELECT NEW com.decoaromas.decoaromaspos.dto.reportes.MetodosPagoCierreDTO(" +
            "SUM(c.efectivoCierre), " +
            "SUM(c.mercadoPagoCierre), " +
            "SUM(c.bciCierre), " +
            "SUM(c.botonDePagoCierre), " +
            "SUM(c.transferenciaCierre), " +
            "SUM(c.postCierre)) " +
            "FROM Caja c " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR c.fechaCierre >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp)  IS NULL OR c.fechaCierre <= :fechaFin) ")
    MetodosPagoCierreDTO findTotalesMetodosPagoCierre(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin
    );

    /**
     * GRÁFICO 3: Tendencia de Descuadres (para comparar con Ventas).
     * Obtiene la suma neta (positiva o negativa) de las diferencias de caja por mes.
     */
    @Query("SELECT MONTH(c.fechaCierre) AS mes, SUM(c.diferenciaReal) AS total " +
            "FROM Caja c " +
            "WHERE YEAR(c.fechaCierre) = :anio " +
            "AND c.fechaCierre IS NOT NULL " +
            "GROUP BY MONTH(c.fechaCierre)")
    List<VentaMensualDTO> findTotalDescuadresPorMes(@Param("anio") Integer anio);


    /**
     * Obtiene los KPIs para la pestaña de operaciones (Descuadre Neto, Absoluto y Conteo)
     * basado en un año y un mes opcional.
     */
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.OperacionesKpiDTO(" +
            "  COALESCE(SUM(c.diferenciaReal), 0.0), " +
            "  COALESCE(SUM(ABS(c.diferenciaReal)), 0.0), " +
            "  COUNT(c.cajaId)) " +
            "FROM Caja c " +
            "WHERE c.diferenciaReal != 0 " +
            "AND YEAR(c.fechaCierre) = :anio " +
            "AND (:mes IS NULL OR MONTH(c.fechaCierre) = :mes)")
    OperacionesKpiDTO findOperacionesKpis(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes
    );

    // Obtener descuadre neto por rango de fechas
    @Query("SELECT COALESCE(SUM(c.diferenciaReal), 0.0) " +
            "FROM Caja c " +
            "WHERE c.fechaCierre >= :fechaInicio " +
            "AND c.fechaCierre <= :fechaFin")
    Double getDescuadreNetoPorRango(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin
    );
}
