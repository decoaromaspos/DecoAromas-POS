package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.DetalleVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.aroma.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " + // Filtro añadido
            "AND p.aroma.nombre IS NOT NULL " + // Evitar nulos
            "GROUP BY p.aroma.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findVentasPorAroma(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            Pageable pageable);


    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.familia.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " + // Filtro añadido
            "AND p.familia.nombre IS NOT NULL " + // Evitar nulos
            "GROUP BY p.familia.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findVentasPorFamilia(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("aromaId") Long aromaId,
            Pageable pageable);

    // Obtener productos mas vendidos
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ProductoVendidoDTO(p.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY p.productoId, p.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<ProductoVendidoDTO> findVentasPorProducto(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId);


    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ProductoVendidoDTO(p.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY p.productoId, p.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    Page<ProductoVendidoDTO> findVentasPorProductoPaginados(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId,
            Pageable pageable);

    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(a.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "JOIN p.aroma a " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "GROUP BY a.aromaId, a.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    Page<VentaAgrupadaDTO> findVentasPorAromaPaginadosTabla(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            Pageable pageable);

    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(f.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "JOIN p.familia f " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY f.familiaId, f.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    Page<VentaAgrupadaDTO> findVentasPorFamiliaPaginadosTabla(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("aromaId") Long aromaId,
            Pageable pageable);




    // --- NUEVOS MÉTODOS PARA GRÁFICOS ---

    // 1. Para Gráfico de Dispersión (Rentabilidad vs Volumen)
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ProductPerformanceDTO(p.nombre, SUM(dv.cantidad), SUM(dv.subtotal - (p.costo * dv.cantidad))) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "AND p.costo IS NOT NULL " + // Solo incluir productos con costo definido
            "GROUP BY p.productoId, p.nombre")
    List<ProductPerformanceDTO> findProductoPerformance(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId);

    // 2. Para KPIs - Producto Estrella
    // Usamos Pageable para pedir solo el primer resultado
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY p.productoId, p.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findProductoEstrella(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId,
            Pageable pageable); // Le pasaremos un Pageable.ofSize(1)

    // 3. Para KPIs - Aroma Más Popular
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.aroma.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND p.aroma IS NOT NULL " +
            "GROUP BY p.aroma.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findAromaMasPopular(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            Pageable pageable); // Pageable.ofSize(1)

    // 4. Para KPIs - Familia Más Popular
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.familia.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "AND p.familia IS NOT NULL " +
            "GROUP BY p.familia.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findFamiliaMasPopular(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("aromaId") Long aromaId,
            Pageable pageable); // Pageable.ofSize(1)


    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v JOIN dv.producto p " +
            "WHERE YEAR(v.fecha) = :anio " +
            "AND (:mes IS NULL OR MONTH(v.fecha) = :mes) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "GROUP BY p.productoId, p.nombre " +
            "ORDER BY SUM(dv.cantidad) ASC")
    List<VentaAgrupadaDTO> findProductoMenosVendido(
            @Param("anio") Integer anio,
            @Param("mes") Integer mes,
            @Param("familiaId") Long familiaId,
            @Param("aromaId") Long aromaId,
            Pageable pageable); // Le pasaremos un Pageable.ofSize(1)



    // Obtener ventas aromas paginados para obtener top 5
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.aroma.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "AND (:familiaId IS NULL OR p.familia.familiaId = :familiaId) " +
            "AND p.aroma.nombre IS NOT NULL " +
            "GROUP BY p.aroma.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findVentasPorAromaPaginado(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente, // Filtro de venta
            @Param("familiaId") Long familiaId,     // Filtro de producto
            Pageable pageable);                     // Para el "Top 5"

    // Obtener ventas familias paginadas para obtener top 5
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.VentaAgrupadaDTO(p.familia.nombre, SUM(dv.cantidad)) " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "JOIN dv.producto p " +
            "WHERE v.fecha >= :fechaInicio " +
            "AND v.fecha <= :fechaFin " +
            "AND (:tipoCliente IS NULL OR v.tipoCliente = :tipoCliente) " +
            "AND (:aromaId IS NULL OR p.aroma.aromaId = :aromaId) " +
            "AND p.familia.nombre IS NOT NULL " +
            "GROUP BY p.familia.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC")
    List<VentaAgrupadaDTO> findVentasPorFamiliaPaginado(
            @Param("fechaInicio") ZonedDateTime fechaInicio,
            @Param("fechaFin") ZonedDateTime fechaFin,
            @Param("tipoCliente") TipoCliente tipoCliente,
            @Param("aromaId") Long aromaId,
            Pageable pageable);
}