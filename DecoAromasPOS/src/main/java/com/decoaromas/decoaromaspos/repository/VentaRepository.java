package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.Venta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta,Long>, JpaSpecificationExecutor<Venta> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "cliente"})
    List<Venta> findAll(Specification<Venta> spec);

    @Query("SELECT SUM(v.vuelto) FROM Venta v WHERE v.caja.cajaId = :cajaId")
    Optional<Double> sumVueltoByCajaId(@Param("cajaId") Long cajaId);
    // Usamos COALESCE para que si la suma es NULL, la base de datos devuelva 0.0 directamente
    @Query("SELECT COALESCE(SUM(v.vuelto), 0.0) FROM Venta v WHERE v.caja.cajaId = :cajaId")
    Double sumVueltoByCajaIdCoalesce(@Param("cajaId") Long cajaId);

    Optional<Venta> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoIgnoreCase(String numeroDocumento);


    /**
     * Calcula la suma del totalNeto de todas las ventas dentro de un rango de fechas.
     * La fecha de fin es exclusiva (menor que).
     * @param fechaInicio La fecha y hora de inicio (inclusiva).
     * @param fechaFin    La fecha y hora de fin (exclusiva).
     * @return Un Optional<Double> con la suma total. Estará vacío si no hay ventas en el rango.
     */
    @Query("SELECT SUM(v.totalNeto) FROM Venta v WHERE v.fecha >= :fechaInicio AND v.fecha < :fechaFin")
    Optional<Double> sumTotalNetoByFechaBetween(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin
    );



    // Obtener total de ventas por mes según año y tipo de cliente
    @Query("SELECT EXTRACT(MONTH FROM v.fecha) as mes, SUM(v.totalNeto) as total " +
            "FROM Venta v " +
            "WHERE EXTRACT(YEAR FROM v.fecha) = :anio " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY EXTRACT(MONTH FROM v.fecha) " +
            "ORDER BY EXTRACT(MONTH FROM v.fecha) ")
    List<VentaMensualDTO> findTotalVentasEnTiendaPorMes(
            @Param("anio") int anio,
            @Param("tipoCliente") TipoCliente tipoCliente
    );

    // Obtener total de ventas por mes según año y tipo de cliente
    @Query("SELECT EXTRACT(MONTH FROM v.fecha) as mes, SUM(v.totalNeto) as total " +
            "FROM Venta v " +
            "WHERE EXTRACT(YEAR FROM v.fecha) = :anio " +
            "GROUP BY EXTRACT(MONTH FROM v.fecha) " +
            "ORDER BY EXTRACT(MONTH FROM v.fecha) ")
    List<VentaMensualDTO> findTotalVentasEnTiendaPorMes(@Param("anio") int anio);


    // Obtener Utilidad mensual según año
    @Query("SELECT EXTRACT(MONTH FROM v.fecha) as mes, " +
            "SUM(v.totalNeto) as totalIngresos, " +
            "SUM(v.costoGeneral) as totalCostos, " +
            "(SUM(v.totalNeto) - SUM(v.costoGeneral)) as totalUtilidad " +
            "FROM Venta v " +
            "WHERE EXTRACT(YEAR FROM v.fecha) = :anio " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY EXTRACT(MONTH FROM v.fecha) " +
            "ORDER BY EXTRACT(MONTH FROM v.fecha)")
    List<UtilidadMensualDTO> findUtilidadMensualPorAnio(@Param("anio") int anio, @Param("tipoCliente") TipoCliente tipoCliente);


    // Obtener total de ganancias de ventas según tipo de cliente
    @Query("SELECT v.tipoCliente as tipoCliente, SUM(v.totalNeto) as total " +
            "FROM Venta v " +
            "WHERE (:anio IS NULL OR EXTRACT(YEAR FROM v.fecha) = :anio) " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM v.fecha) = :mes) " +
            "GROUP BY v.tipoCliente")
    List<VentaPorTipoClienteDTO> findTotalPorTipoCliente(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes
    );


    // Filtros suma de ganancias según año y mes (opcional/nulo)
    @Query("SELECT SUM(v.totalNeto) FROM Venta v WHERE EXTRACT(YEAR FROM v.fecha) = :anio " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM v.fecha) = :mes)")
    Double sumTotalGeneralByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("SELECT SUM(v.totalNeto) FROM Venta v WHERE v.cliente.tipo = 'DETALLE' " +
            "AND EXTRACT(YEAR FROM v.fecha) = :anio " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM v.fecha) = :mes)")
    Double sumTotalDetalleByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);

    @Query("SELECT SUM(v.totalNeto) FROM Venta v WHERE v.cliente.tipo = 'MAYORISTA' " +
            "AND EXTRACT(YEAR FROM v.fecha) = :anio " +
            "AND (:mes IS NULL OR EXTRACT(MONTH FROM v.fecha) = :mes)")
    Double sumTotalMayoristaByAnioAndMesOpcional(@Param("anio") Integer anio, @Param("mes") Integer mes);


    // Consulta para Ventas por Día de la Semana
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaPorNombreDTO( " +
            "CASE FUNCTION('to_char', v.fecha, 'ID') " +
            "  WHEN '1' THEN 'Lunes' " +
            "  WHEN '2' THEN 'Martes' " +
            "  WHEN '3' THEN 'Miércoles' " +
            "  WHEN '4' THEN 'Jueves' " +
            "  WHEN '5' THEN 'Viernes' " +
            "  WHEN '6' THEN 'Sábado' " +
            "  WHEN '7' THEN 'Domingo' " +
            "END, " +
            "SUM(v.totalNeto)) " +
            "FROM Venta v " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR v.fecha >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp) IS NULL OR v.fecha <= :fechaFin) " +
            "AND (CAST(:tipoCliente AS string) IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY FUNCTION('to_char', v.fecha, 'ID') " +
            "ORDER BY FUNCTION('to_char', v.fecha, 'ID') ASC")
    List<VentaAgrupadaPorNombreDTO> findVentasPorDiaDeLaSemana(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente
    );



    // Consulta para Rendimiento por Vendedor
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaPorNombreDTO(u.username, SUM(v.totalNeto)) " +
            "FROM Venta v JOIN v.usuario u " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR v.fecha >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp) IS NULL OR v.fecha <= :fechaFin) " +
            "GROUP BY u.username " +
            "ORDER BY SUM(v.totalNeto) DESC")
    List<VentaAgrupadaPorNombreDTO> findVentasPorVendedor(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin
    );




    /**
     * Gráfico 3: Ventas por Hora del Día
     * Usa to_char(fecha, 'HH24') para obtener la hora (00-23) como texto.
     */
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaPorNombreDTO(" +
            "CAST(FUNCTION('to_char', v.fecha, 'HH24') AS string), " +
            "SUM(v.totalNeto)) " +
            "FROM Venta v " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR v.fecha >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp) IS NULL OR v.fecha <= :fechaFin) " +
            "AND (CAST(:tipoCliente AS string) IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY FUNCTION('to_char', v.fecha, 'HH24') " +
            "ORDER BY FUNCTION('to_char', v.fecha, 'HH24') ASC")
    List<VentaAgrupadaPorNombreDTO> findVentasPorHora(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente
    );


    /**
     * Gráfico 5: Análisis de Descuentos vs Ventas (Mensual)
     * Agrupa por mes (usando to_char 'MM') y suma totalNeto y montoDescuentoGlobalCalculado.
     * Se filtran por año y tipoCliente (opcionales).
     */
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.AnalisisDescuentoDTO(" +
            "CAST(FUNCTION('to_char', v.fecha, 'MM') AS int), " +
            "SUM(v.totalNeto), " +
            "SUM(v.montoDescuentoGlobalCalculado)) " +
            "FROM Venta v " +
            "WHERE (CAST(:anio AS integer) IS NULL OR EXTRACT(YEAR FROM v.fecha) = :anio) " +
            "AND (CAST(:tipoCliente AS string) IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY FUNCTION('to_char', v.fecha, 'MM') " +
            "ORDER BY FUNCTION('to_char', v.fecha, 'MM') ASC")
    List<AnalisisDescuentoDTO> findAnalisisDescuentos(
            @Param("anio") Integer anio,
            @Param("tipoCliente") TipoCliente tipoCliente
    );


    /**
     * Obtiene los KPIs de Ventas (Transacciones, Descuentos, Ticket Promedio)
     * para un rango de fechas y tipo de cliente opcionales.
     */
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.KpiVentasDTO(" +
            "   COUNT(v.ventaId), " +
            "   COALESCE(SUM(v.montoDescuentoGlobalCalculado), 0.0), " +
            "   COALESCE(AVG(v.totalNeto), 0.0), " +
            "   COALESCE(SUM(v.totalNeto), 0.0)) " +
            "FROM Venta v " +
            "WHERE (CAST(:fechaInicio AS timestamp) IS NULL OR v.fecha >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS timestamp) IS NULL OR v.fecha <= :fechaFin) " +
            "AND (CAST(:tipoCliente AS string) IS NULL OR v.tipoCliente = :tipoCliente)")
    KpiVentasDTO getKpisVentas(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente
    );


    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaDiariaDTO(DAY(v.fecha), SUM(v.totalNeto)) " +
            "FROM Venta v " +
            "LEFT JOIN v.detalles dv " + // Unimos con detalles
            "LEFT JOIN dv.producto p " + // y producto para poder filtrar
            "WHERE YEAR(v.fecha) = :anio " +
            "AND MONTH(v.fecha) = :mes " + // Mes es requerido para este gráfico
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY DAY(v.fecha) " +
            "ORDER BY DAY(v.fecha) ASC")
    List<VentaDiariaDTO> findVentasDiariasPorMes(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId);


    // Obtener Kpis de analisis general
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.KpiVentasAgregadasDTO(" +
            "   COALESCE(SUM(v.totalNeto), 0.0), " +
            "   COALESCE(SUM(v.totalNeto - v.costoGeneral), 0.0), " + // Utilidad Neta
            "   COUNT(v.ventaId)) " +
            "FROM Venta v " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            // Subconsulta para filtrar por producto sin duplicar ventas
            "AND ((:familiaId IS NULL AND :aromaId IS NULL) OR EXISTS (" +
            "   SELECT 1 FROM DetalleVenta dv " +
            "   JOIN dv.producto p " +
            "   WHERE dv.venta = v " +
            "   AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "   AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId)" +
            "))")
    KpiVentasAgregadasDTO getKpisAgregadosGenerales(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId
    );


    // Obtener ventas diarias según rango
    @Query("SELECT CAST(v.fecha AS date) AS fecha, COALESCE(SUM(v.totalNeto), 0.0) AS total " +
            "FROM Venta v " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "AND ((:familiaId IS NULL AND :aromaId IS NULL) OR EXISTS (" +
            "   SELECT 1 FROM DetalleVenta dv " +
            "   JOIN dv.producto p " +
            "   WHERE dv.venta = v " +
            "   AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "   AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId)" +
            ")) " +
            "GROUP BY CAST(v.fecha AS date) " +
            "ORDER BY fecha ASC")
    List<VentaDiariaProjection> findVentasDiariasPorRango(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId
    );

    // Obtener ventas mensuales según rango
    @Query("SELECT " +
            "   CAST(function('date_part', 'year', v.fecha) AS INTEGER) AS anio, " +
            "   CAST(function('date_part', 'month', v.fecha) AS INTEGER) AS mes, " +
            "   COALESCE(SUM(v.totalNeto), 0.0) AS total " +
            "FROM Venta v " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "AND ((:familiaId IS NULL AND :aromaId IS NULL) OR EXISTS (" +
            "   SELECT 1 FROM DetalleVenta dv " +
            "   JOIN dv.producto p " +
            "   WHERE dv.venta = v " +
            "   AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "   AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId)" +
            ")) " +
            "GROUP BY function('date_part', 'year', v.fecha), function('date_part', 'month', v.fecha) " +
            "ORDER BY anio ASC, mes ASC")
    List<VentaMensualProjection> findVentasMensualesPorRango(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId
    );


    // Obtener mapa de calor de ventas por hora y dia de semana
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaPorHoraDiaDTO(" +
            "   CAST(function('date_part', 'dow', v.fecha) AS INTEGER), " +
            "   CAST(function('date_part', 'hour', v.fecha) AS INTEGER), " +
            "   COALESCE(SUM(v.totalNeto), 0.0)) " +
            "FROM Venta v " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "AND ((:familiaId IS NULL AND :aromaId IS NULL) OR EXISTS (" +
            "   SELECT 1 FROM DetalleVenta dv " +
            "   JOIN dv.producto p " +
            "   WHERE dv.venta = v " +
            "   AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "   AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId)" +
            ")) " +
            "GROUP BY function('date_part', 'dow', v.fecha), function('date_part', 'hour', v.fecha)")
    List<VentaPorHoraDiaDTO> findVentasPorHoraYDiaSemana(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId
    );


    // 2. Top N Clientes por Valor Monetario (Ventas Totales) con filtros de tiempo y tipo.
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ClienteAgregadoDTO(" +
            "    CONCAT(c.nombre, ' ', c.apellido), " +
            "    SUM(v.totalNeto)) " +
            "FROM Venta v JOIN v.cliente c " +
            "WHERE c.activo = true " +
            "  AND v.cliente IS NOT NULL " +
            "  AND (:anio IS NULL OR EXTRACT(YEAR FROM v.fecha) = :anio) " +
            "  AND (:mes IS NULL OR EXTRACT(MONTH FROM v.fecha) = :mes) " +
            "  AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "GROUP BY c.clienteId, c.nombre, c.apellido " +
            "ORDER BY SUM(v.totalNeto) DESC")
    List<ClienteAgregadoDTO> findTopClientesByTotalVenta(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("tipoCliente") TipoCliente tipoCliente,
            Pageable pageable
    );

    // Query para obtener la última compra de cada cliente activo y su nombre completo
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ClienteInactivoDTO(" +
            "    c.clienteId, c.nombre, c.apellido, MAX(v.fecha)) " +
            "FROM Cliente c LEFT JOIN Venta v ON c.clienteId = v.cliente.clienteId " +
            "WHERE c.activo = true " +
            "  AND (:tipoCliente IS NULL OR c.tipo = :tipoCliente) " +
            "GROUP BY c.clienteId, c.nombre, c.apellido " +
            "ORDER BY MAX(v.fecha) ASC")
    List<ClienteInactivoDTO> findClientesLastPurchaseDate(TipoCliente tipoCliente);
}
