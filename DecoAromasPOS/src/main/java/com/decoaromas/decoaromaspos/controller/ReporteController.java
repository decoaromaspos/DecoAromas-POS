package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.graficos.ChartDataDTO;
import com.decoaromas.decoaromaspos.dto.graficos.PieChartDataDTO;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.service.ReporteService;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_AUTHENTICATED;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes y Analytics", description = "API centralizada para generación de gráficos, KPIs y tablas de análisis financiero y operativo.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/reportes\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class ReporteController {

    private final ReporteService reporteService;


    // VENTAS GENERALES (ANUALES / COMPARATIVAS)

    @Operation(summary = "Gráfico Barras: Ventas Anuales", description = "Muestra la evolución mensual de las ventas para un año específico.")
    @ApiResponse(responseCode = "200", description = "Datos del gráfico generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-anuales/barras")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasAnualesBarras(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: TIENDA o ONLINE") @RequestParam(required = false) String tipoVenta, // TIENDA, ONLINE
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteVentasTotalesPorMes(anio, tipoVenta, tipoCliente));
    }


    @Operation(summary = "Gráfico Barras: Comparativo Tienda vs Online", description = "Obtiene datos para gráfico de barras comparativo de ventas tienda vs online.")
    @ApiResponse(responseCode = "200", description = "Datos comparativos generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-anuales-comparacion/barras")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasAnualesComparacionBarras(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteComparativoVentas(anio, tipoCliente));
    }

    @Operation(summary = "Gráfico Torta: Distribución por Canal", description = "Porcentaje de ventas Tienda vs Online.")
    @ApiResponse(responseCode = "200", description = "Datos distribución por canal generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/ventas-anuales/torta/tipo-venta")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getVentasAnualesTipoVentaTorta(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteDistribucionVentasVsTipoVenta(anio, mes, tipoCliente));
    }

    // ANÁLISIS DE PRODUCTOS (GRÁFICOS Y TABLAS)

    @Operation(summary = "Gráfico Barras: Ventas por Producto", description = "Top ventas desagregadas por producto.")
    @ApiResponse(responseCode = "200", description = "Datos ventas por producto generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-producto")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasPorProducto(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getReporteVentasPorProducto(anio, mes, familiaId, aromaId));
    }

    @Operation(summary = "Tabla Paginada: Ventas por Producto", description = "Listado detallado para tablas de datos con ordenamiento.")
    @ApiResponse(responseCode = "200", description = "Paginación de Ventas por producto obtenida exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/ventas-por-producto/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<ProductoVendidoDTO>> listarVentasPorProductoPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "p.nombre") String sortBy,
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getVentasPorProductoPaginados(page, size, sortBy, anio, mes, familiaId, aromaId));
    }

    @Operation(summary = "Tabla Paginada: Ventas por Aroma")
    @ApiResponse(responseCode = "200", description = "Paginación de Ventas por aroma obtenida exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/ventas-por-aroma/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<VentaAgrupadaDTO>> listarVentasPorAromaPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "a.nombre") String sortBy,
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId) {
        return ResponseEntity.ok(reporteService.getVentasPorAromaPaginado(page, size, sortBy, anio, mes, familiaId));
    }

    @Operation(summary = "Tabla Paginada: Ventas por Familia")
    @ApiResponse(responseCode = "200", description = "Paginación de Ventas por familia obtenida exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/ventas-por-familia/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<VentaAgrupadaDTO>> listarVentasPorFamiliaPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "f.nombre") String sortBy,
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getVentasPorFamiliaPaginado(page, size, sortBy, anio, mes, aromaId));
    }


    // OPERACIONES Y TIEMPO

    @Operation(summary = "Gráfico Barras: Ventas por Día de Semana", description = "Identifica los días pico de venta.")
    @ApiResponse(responseCode = "200", description = "Datos ventas por día de la semana generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-dia-semana")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasTiendaPorDiaSemana(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteVentasTiendaPorDiaSemana(fechaInicio, fechaFin, tipoCliente));
    }

    @Operation(summary = "Gráfico Barras: Rendimiento por Vendedor")
    @ApiResponse(responseCode = "200", description = "Datos rendimiento por vendedor generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-vendedor")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasTiendaPorVendedor(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin ) {
        return ResponseEntity.ok(reporteService.getReporteVentasTiendaPorVendedor(fechaInicio, fechaFin));
    }

    @Operation(summary = "Gráfico Barras: Ventas por Hora", description = "Mapa de horas pico para gestión de personal.")
    @ApiResponse(responseCode = "200", description = "Datos ventas por hora generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-hora")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasTiendaPorHora(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false)
            TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteVentasPorHora(fechaInicio, fechaFin, tipoCliente));
    }


    // FINANCIERO Y PAGOS

    @Operation(summary = "Gráfico Combinado: Análisis de Descuentos", description = "Compara Ventas Netas vs Monto total descontado.")
    @ApiResponse(responseCode = "200", description = "Datos análisis de descuentos generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/analisis-descuentos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getAnalisisDescuentos(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        // Si el año no se provee, usamos el año actual por defecto
        Integer anioAFiltrar = (anio == null) ? LocalDate.now().getYear() : anio;
        return ResponseEntity.ok(reporteService.getReporteAnalisisDescuentos(anioAFiltrar, tipoCliente));
    }


    @Operation(summary = "Gráfico Multilinea: Utilidad Anual", description = "Muestra Ingresos, Costos y Utilidad Neta mensual.")
    @ApiResponse(responseCode = "200", description = "Datos utilidad anual generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/utilidad-anual")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getUtilidadAnual(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam int anio,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getReporteUtilidadAnual(anio, tipoCliente));
    }


    @Operation(summary = "Gráfico Torta: Medios de Pago", description = "Distribución de uso de medios de pago (Efectivo, Tarjeta, etc.).")
    @ApiResponse(responseCode = "200", description = "Datos medios de pago generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/analisis-medios-pago")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getAnalisisMediosDePago(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false)
            TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getAnalisisMediosDePago(fechaInicio, fechaFin, tipoCliente  ));
    }


    // DISTRIBUCIÓN DE CLIENTES (MAYORISTA / DETALLE)

    @Operation(summary = "Gráfico Torta: Distribución Tienda (Tipo Cliente)")
    @ApiResponse(responseCode = "200", description = "Datos distribución en tienda según tipo de cliente generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/distribucion-tienda-tipo-cliente")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getDistribucionTiendaPorTipoCliente(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(reporteService.getDistribucionTiendaPorTipoCliente(anio, mes));
    }

    @Operation(summary = "Gráfico Torta: Distribución Online (Tipo Cliente)")
    @ApiResponse(responseCode = "200", description = "Datos distribución en online según tipo de cliente generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/distribucion-online-tipo-cliente")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getDistribucionOnlinePorTipoCliente(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(reporteService.getDistribucionOnlinePorTipoCliente(anio, mes));
    }


    // GESTIÓN DE CAJA Y DESCUADRES

    @Operation(summary = "Gráfico Barras: Descuadres de Caja", description = "Muestra las diferencias encontradas en los cierres de caja.")
    @ApiResponse(responseCode = "200", description = "Datos descuadres de caja generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/descuadres-caja")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getDescuadresCaja(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(reporteService.getReporteDescuadresCaja(anio, mes));
    }

    @Operation(summary = "Tabla Paginada: Descuadres de Caja")
    @ApiResponse(responseCode = "200", description = "Paginación de descuadres de caja obtenida exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/descuadres-caja/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<CajaDescuadrada>> listarDescuadresCajaPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaCierre") String sortBy,
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(reporteService.getDescuadresCajaPaginados(page, size, sortBy, anio, mes));
    }

    @Operation(summary = "Gráfico Barras: Descuadres por Usuario")
    @ApiResponse(responseCode = "200", description = "Datos descuadres por usuario generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/descuadres-por-usuario")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getDescuadresPorUsuario(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.getReporteDescuadresPorUsuario(fechaInicio, fechaFin));
    }


    @Operation(summary = "Gráfico Torta: Métodos de Pago (Declarado en Cierre)", description = "Obtiene datos para gráfico de torta de distribución de métodos de pago al cierre de caja.")
    @ApiResponse(responseCode = "200", description = "Datos métodos de pago generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/distribucion-metodos-pago-cierre")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getMetodosPagoCierre(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.getReporteMetodosPagoCierre(fechaInicio, fechaFin));
    }


    @Operation(summary = "Gráfico Comparativo: Ventas vs Descuadres", description = "Obtiene datos para gráfico comparativo de tendencia de ventas vs descuadres.")
    @ApiResponse(responseCode = "200", description = "Datos ventas vs descuadres generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/tendencia-ventas-vs-descuadres")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTendenciaVentasVsDescuadres(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio) {
        return ResponseEntity.ok(reporteService.getReporteTendenciaVentasVsDescuadres(anio));
    }



    // TOP RANKINGS

    @Operation(summary = "Gráfico Barras: Top 10 Aromas", description = "Obtiene datos de ventas agrupadas por aroma.")
    @ApiResponse(responseCode = "200", description = "Datos top 10 aromas generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-aroma/top10")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasPorAroma(
            @Parameter(description = "Filtro opcional: Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId) {
        return ResponseEntity.ok(reporteService.getReporteVentasPorAroma(anio, mes,familiaId));
    }

    @Operation(summary = "Gráfico Barras: Top 10 Familias", description = "Obtiene datos de ventas agrupadas por familia de productos.")
    @ApiResponse(responseCode = "200", description = "Datos top 10 familias generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/ventas-por-familia/top10")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getVentasPorFamilia(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getReporteVentasPorFamilia(anio, mes, aromaId));
    }


    // KPIS Y DASHBOARDS ESPECÍFICOS

    @Operation(summary = "KPIs Ventas", description = "Tarjetas de información: Total Venta, Ticket Promedio, etc.")
    @ApiResponse(responseCode = "200", description = "Datos KPIs Ventas generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = KpiVentasDTO.class)))
    @GetMapping("/kpis-ventas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<KpiVentasDTO> getKpisVentas(
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false)
            TipoCliente tipoCliente) {
        return ResponseEntity.ok(reporteService.getKpisVentas(fechaInicio, fechaFin, tipoCliente));
    }

    @Operation(summary = "Gráfico Línea: Tendencia Diaria", description = "Evolución de ventas día a día para un mes específico.")
    @ApiResponse(responseCode = "200", description = "Datos tendencia diaria generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/tendencia-diaria")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getReporteTendenciaDiaria(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Mes a consultar", example = "12") @RequestParam Integer mes, // Mes es requerido para este gráfico
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getReporteTendenciaVentasDiarias(anio, mes, familiaId, aromaId));
    }

    @Operation(summary = "Gráfico Dispersión: Performance Producto", description = "Rentabilidad vs Volumen.")
    @ApiResponse(responseCode = "200", description = "Datos performance producto generados exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductPerformanceDTO.class))))
    @GetMapping("/performance-producto")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductPerformanceDTO>> getReportePerformanceProducto(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getReporteRentabilidadVolumen(anio, mes, familiaId, aromaId));
    }

    @Operation(summary = "KPIs Productos", description = "Producto estrella, menos vendido, etc.")
    @ApiResponse(responseCode = "200", description = "Datos KPIs productos generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoKpiDTO.class)))
    @GetMapping("/kpis-productos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ProductoKpiDTO> getKpisProductos(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: ID de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro opcional: ID de aroma") @RequestParam(required = false) Long aromaId) {
        return ResponseEntity.ok(reporteService.getKpisAnalisisProducto(anio, mes, familiaId, aromaId));
    }

    @Operation(summary = "KPIs Operaciones", description = "Obtiene los indicadores clave de rendimiento (KPIs) de operaciones.")
    @ApiResponse(responseCode = "200", description = "Datos KPIs operaciones generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OperacionesKpiDTO.class)))
    @GetMapping("/kpis-operaciones")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<OperacionesKpiDTO> getOperacionesKpis(
            @Parameter(description = "Año a consultar", example = "2024") @RequestParam Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(reporteService.getOperacionesKpis(anio, mes));
    }



    // DASHBOARD GENERAL (FILTRO COMPLETO)

    @Operation(summary = "Dashboard: KPIs Generales", description = "Obtiene los KPIs principales para la vista general.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos KPIs Generales generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = KpiGeneralesDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/kpis")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<KpiGeneralesDTO> getKpisGenerales(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro) {

        KpiGeneralesDTO kpis = reporteService.getKpisGenerales(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getFamiliaId(),
                filtro.getAromaId()
        );
        return ResponseEntity.ok(kpis);
    }

    @Operation(summary = "Dashboard: Tendencia Ventas (Línea)", description = "Visualización general de tendencia según filtros.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos tendencia Ventas generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/tendencia-ventas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTendenciaVentas(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro) {

        ChartDataDTO chartData = reporteService.getReporteTendenciaVentasDiarias(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getFamiliaId(),
                filtro.getAromaId()
        );
        return ResponseEntity.ok(chartData);
    }

    @Operation(summary = "Dashboard: Tendencia Mensual (Línea)", description = "Obtiene datos para el gráfico de tendencia de ventas mensuales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos tendencia Ventas mensual generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/tendencia-ventas-mensual")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTendenciaVentasMensual(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro) {

        ChartDataDTO chartData = reporteService.getReporteTendenciaVentasMensual(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getFamiliaId(),
                filtro.getAromaId()
        );
        return ResponseEntity.ok(chartData);
    }

    @Operation(summary = "Dashboard: Top N Familias", description = "Obtiene datos para el gráfico de top N familias más vendidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos Top N Familias generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/top-familias/{topN}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTop5Familias(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro, @PathVariable int topN ) {

        ChartDataDTO chartData = reporteService.getReporteTopNVentasPorFamilia(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getAromaId(), // Nótese que usa el filtro de aroma
                topN
        );
        return ResponseEntity.ok(chartData);
    }

    @Operation(summary = "Dashboard: Top N Aromas", description = "Obtiene datos para el gráfico de top N aromas más vendidos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos Top N Aroma generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/top-aromas/{topN}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTop5Aromas(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro, @PathVariable int topN ) {

        ChartDataDTO chartData = reporteService.getReporteTopNVentasPorAroma(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getFamiliaId(), // Nótese que usa el filtro de familia
                topN
        );
        return ResponseEntity.ok(chartData);
    }

    @Operation(summary = "Dashboard: Mapa de Calor", description = "Intensidad de ventas por Día de la Semana vs Hora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos Mapa de calor generados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @GetMapping("/general/mapa-calor-ventas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getMapaCalorVentas(@ParameterObject @Valid ReporteGeneralFiltroDTO filtro) {

        ChartDataDTO chartData = reporteService.getReporteMapaCalorVentas(
                filtro.getFechaInicio(),
                filtro.getFechaFin(),
                filtro.getTipoCliente(),
                filtro.getFamiliaId(),
                filtro.getAromaId()
        );
        return ResponseEntity.ok(chartData);
    }

    // REPORTES CRM / CLIENTES

    // 1. Gráfico de Torta: Distribución de Clientes por Tipo. No requiere filtros de tiempo, es sobre la base estática.
    @Operation(summary = "Gráfico Torta: Clientes por Tipo")
    @ApiResponse(responseCode = "200", description = "Datos clientes por tipo generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/clientes/distribucion/tipo")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getDistribucionClientesPorTipo() {
        PieChartDataDTO data = reporteService.getReporteDistribucionClientesPorTipo();
        return ResponseEntity.ok(data);
    }

    // 2. Gráfico de Barras: Top N Clientes por Ingreso Total Generado (CLV simplificado). Filtros: topN, anio, mes, tipoCliente
    @Operation(summary = "Gráfico Barras: Top Clientes por Gasto (LTV)")
    @ApiResponse(responseCode = "200", description = "Datos top clientes por gasto generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChartDataDTO.class)))
    @GetMapping("/clientes/top-gasto")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ChartDataDTO> getTopClientesPorGasto(
            @Parameter(description = "Número Top N de Clientes", example = "10") @RequestParam(defaultValue = "10") Integer topN,
            @Parameter(description = "Filtro opcional: Año a consultar", example = "2024") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro opcional: Mes a consultar", example = "12") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente ) {
        ChartDataDTO data = reporteService.getReporteTopClientesPorGasto(topN, anio, mes, tipoCliente);
        return ResponseEntity.ok(data);
    }

    // 3. Gráfico de Torta: Distribución de Clientes por Recencia. Filtros: dias (para la recencia), tipoCliente
    @Operation(summary = "Gráfico Torta: Clientes Activos vs Inactivos (Recencia)")
    @ApiResponse(responseCode = "200", description = "Datos clientes activos vs inactivos generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PieChartDataDTO.class)))
    @GetMapping("/clientes/recencia")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PieChartDataDTO> getDistribucionClientesPorRecencia(
            @Parameter(description = "Días de recencia", example = "90") @RequestParam(defaultValue = "90") Integer dias,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente ) {
        PieChartDataDTO data = reporteService.getReporteDistribucionClientesPorRecencia(dias, tipoCliente);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Listado Detalle: Clientes en Riesgo (Inactivos)")
    @ApiResponse(responseCode = "200", description = "Datos clientes en riesgo generados exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteInactivoDTO.class))))
    @GetMapping("/clientes/recencia/detalle")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteInactivoDTO>> getClientesInactivosDetalle(
            @Parameter(description = "Días sin comprar (inactivos)", example = "90")@RequestParam(defaultValue = "90") Integer dias,
            @Parameter(description = "Filtro opcional: MAYORISTA o DETALLE") @RequestParam(required = false) TipoCliente tipoCliente ) {
        List<ClienteInactivoDTO> data = reporteService.getReporteClientesInactivosDetalle(dias, tipoCliente);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "KPIs Clientes", description = "Totales de cartera de clientes.")
    @ApiResponse(responseCode = "200", description = "Datos KPIs clientes generados exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteKpisDTO.class)))
    @GetMapping("/clientes/kpis")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ClienteKpisDTO> getKpisClientes() {
        ClienteKpisDTO data = reporteService.getKpisClientes();
        return ResponseEntity.ok(data);
    }
}
