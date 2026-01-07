package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.graficos.ChartDataDTO;
import com.decoaromas.decoaromaspos.dto.graficos.PieChartDataDTO;
import com.decoaromas.decoaromaspos.dto.graficos.SeriesItemDTO;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import com.decoaromas.decoaromaspos.repository.*;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final VentaOnlineMensualRepository ventaOnlineMensualRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final CajaRepository cajaRepository;
    private final ClienteRepository clienteRepository;

    private static final String CANTIDAD_VENDIDA_STRING = "Cantidad Vendida";
    private static final String VENTAS_DIARIAS_STRING = "Ventas Diarias";
    private static final String VENTAS_TOTALES_STRING = "Ventas Totales";

    // Gráfico de barras ventas mensuales según año, tipo de venta (TIENDA, ONLINE) y tipo de cliente (MAYORISTA, DETALLE)
    public ChartDataDTO getReporteVentasTotalesPorMes(Integer anio, String tipoVenta, TipoCliente tipoCliente) {
        // Usamos un mapa para consolidar los datos de ambas fuentes
        Map<Integer, Double> ventasPorMes = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            ventasPorMes.put(i, 0.0); // Inicializamos todos los meses en 0
        }

        // Caso ventas en tienda física
        if (tipoVenta == null || "TIENDA".equalsIgnoreCase(tipoVenta)) {
            List<VentaMensualDTO> ventasTienda = ventaRepository.findTotalVentasEnTiendaPorMes(anio, tipoCliente);
            ventasTienda.forEach(v -> ventasPorMes.merge(v.getMes(), v.getTotal(), Double::sum));
        }

        // Caso ventas online
        if (tipoVenta == null || "ONLINE".equalsIgnoreCase(tipoVenta)) {
            // Convierte el enum a String, sino a null. Método jpql admite String, no enum.
            String tipoClienteStr = (tipoCliente != null) ? tipoCliente.name() : null;

            List<VentaMensualDTO> ventasOnline = ventaOnlineMensualRepository.findTotalVentasOnlinePorMes(anio, tipoClienteStr);
            ventasOnline.forEach(v -> ventasPorMes.merge(v.getMes(), v.getTotal(), Double::sum));
        }

        // Formatear DTO
        List<String> mesesLabels = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");
        List<Double> data = new ArrayList<>(ventasPorMes.values());

        SeriesItemDTO seriesItem = new SeriesItemDTO(VENTAS_TOTALES_STRING, data);

        return new ChartDataDTO(List.of(seriesItem), mesesLabels);
    }


    /**
     * Genera los datos para un gráfico de barras comparativo de ventas (Tienda vs. Online vs. Total)
     * para un año específico, con filtro opcional por tipo de cliente.
     */
    public ChartDataDTO getReporteComparativoVentas(int anio, TipoCliente tipoCliente) {
        // Obtener datos de Ventas en Tienda
        List<VentaMensualDTO> ventasTienda = ventaRepository.findTotalVentasEnTiendaPorMes(anio, tipoCliente);
        Map<Integer, Double> mapaTienda = ventasTienda.stream()
                .collect(Collectors.toMap(VentaMensualDTO::getMes, VentaMensualDTO::getTotal));

        // Obtener datos de Ventas Online
        String tipoClienteStr = (tipoCliente != null) ? tipoCliente.name() : null; // Enum a String para query ventas online

        List<VentaMensualDTO> ventasOnline = ventaOnlineMensualRepository.findTotalVentasOnlinePorMes(anio, tipoClienteStr);
        Map<Integer, Double> mapaOnline = ventasOnline.stream()
                .collect(Collectors.toMap(VentaMensualDTO::getMes, VentaMensualDTO::getTotal));

        // Series para el Gráfico
        List<Double> seriesTiendaData = new ArrayList<>();
        List<Double> seriesOnlineData = new ArrayList<>();
        List<Double> seriesGeneralesData = new ArrayList<>(); // Tienda + Online

        // Generar datos de meses a las series
        for (int mes = 1; mes <= 12; mes++) {
            double totalTienda = mapaTienda.getOrDefault(mes, 0.0);
            double totalOnline = mapaOnline.getOrDefault(mes, 0.0);
            double totalGeneral = totalTienda + totalOnline;

            seriesTiendaData.add(totalTienda);
            seriesOnlineData.add(totalOnline);
            seriesGeneralesData.add(totalGeneral);
        }

        SeriesItemDTO seriesTienda = new SeriesItemDTO("Ventas Tienda", seriesTiendaData);
        SeriesItemDTO seriesOnline = new SeriesItemDTO("Ventas Online", seriesOnlineData);
        SeriesItemDTO seriesGenerales = new SeriesItemDTO("Ventas Generales", seriesGeneralesData);
        List<String> mesesLabels = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");

        return new ChartDataDTO(List.of(seriesTienda, seriesOnline, seriesGenerales), mesesLabels);
    }


    // Gráfico de torta ventas anuales Tienda vs Online según tipo de cliente
    public PieChartDataDTO getReporteDistribucionVentasVsTipoVenta(Integer anio, Integer mes, TipoCliente tipoCliente) {
        double totalTienda;
        double totalOnline;
        // Caso todos los tipos
        if (tipoCliente == null) {
            totalTienda = Optional.ofNullable(ventaRepository.sumTotalGeneralByAnioAndMesOpcional(anio, mes)).orElse(0.0);
            totalOnline = Optional.ofNullable(ventaOnlineMensualRepository.sumTotalGeneralByAnioAndMesOpcional(anio, mes)).orElse(0.0);
        } else {
            // Filtros según tipo de cliente
            totalTienda = switch (tipoCliente) {
                case DETALLE -> Optional.ofNullable(ventaRepository.sumTotalDetalleByAnioAndMesOpcional(anio, mes)).orElse(0.0);
                case MAYORISTA -> Optional.ofNullable(ventaRepository.sumTotalMayoristaByAnioAndMesOpcional(anio, mes)).orElse(0.0);
            };

            totalOnline = switch (tipoCliente) {
                case DETALLE -> Optional.ofNullable(ventaOnlineMensualRepository.sumTotalDetalleByAnioAndMesOpcional(anio, mes)).orElse(0.0);
                case MAYORISTA -> Optional.ofNullable(ventaOnlineMensualRepository.sumTotalMayoristaByAnioAndMesOpcional(anio, mes)).orElse(0.0);
            };
        }

        List<Double> series = List.of(totalTienda, totalOnline);
        List<String> labels = List.of("Ventas en Tienda", "Ventas Online");

        return new PieChartDataDTO(series, labels);
    }


    // Gráfico de barras de ventas por día de la semana
    public ChartDataDTO getReporteVentasTiendaPorDiaSemana(LocalDate fechaInicio, LocalDate fechaFin, TipoCliente tipoCliente) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaAgrupadaPorNombreDTO> resultados = ventaRepository.findVentasPorDiaDeLaSemana(inicioZoned, finZoned, tipoCliente);

        // Mapear a ChartDataDTO
        List<String> labels = resultados.stream().map(VentaAgrupadaPorNombreDTO::getNombre).toList();
        List<Double> data = resultados.stream().map(VentaAgrupadaPorNombreDTO::getTotal).toList();
        SeriesItemDTO series = new SeriesItemDTO("Total Vendido", data);

        return new ChartDataDTO(List.of(series), labels);
    }

    // Gráfico de barras de rendimiento por vendedor
    public ChartDataDTO getReporteVentasTiendaPorVendedor(LocalDate fechaInicio, LocalDate fechaFin) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaAgrupadaPorNombreDTO> resultados = ventaRepository.findVentasPorVendedor(inicioZoned, finZoned);

        // Mapear a ChartDataDTO
        List<String> labels = resultados.stream().map(VentaAgrupadaPorNombreDTO::getNombre).toList();
        List<Double> data = resultados.stream().map(VentaAgrupadaPorNombreDTO::getTotal).toList();
        SeriesItemDTO series = new SeriesItemDTO(VENTAS_TOTALES_STRING, data);

        return new ChartDataDTO(List.of(series), labels);
    }


    // Prepara los datos para el gráfico de ventas por hora.
    public ChartDataDTO getReporteVentasPorHora(LocalDate fechaInicio, LocalDate fechaFin, TipoCliente tipoCliente) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaAgrupadaPorNombreDTO> resultados = ventaRepository.findVentasPorHora(inicioZoned, finZoned, tipoCliente);

        // Mapear resultados para búsqueda rápida (Llave "00", "01", ..., "23")
        Map<String, Double> ventasPorHoraMap = resultados.stream()
                .collect(Collectors.toMap(VentaAgrupadaPorNombreDTO::getNombre, VentaAgrupadaPorNombreDTO::getTotal));

        // Rellenar las 24 horas del día
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            String horaFormateada = String.format("%02d", i); // Formato "00", "01", etc.
            labels.add(horaFormateada + ":00"); // Etiqueta "00:00", "01:00", etc.
            data.add(ventasPorHoraMap.getOrDefault(horaFormateada, 0.0));
        }

        SeriesItemDTO series = new SeriesItemDTO("Total Vendido", data);
        return new ChartDataDTO(List.of(series), labels);
    }

    // Prepara los datos para el gráfico de análisis de descuentos.
    public ChartDataDTO getReporteAnalisisDescuentos(Integer anio, TipoCliente tipoCliente) {
        List<AnalisisDescuentoDTO> resultados = ventaRepository.findAnalisisDescuentos(anio, tipoCliente);

        // Mapear resultados para búsqueda rápida (Llave 1, 2, ..., 12)
        Map<Integer, AnalisisDescuentoDTO> mapMensual = resultados.stream()
                .collect(Collectors.toMap(AnalisisDescuentoDTO::getMes, Function.identity()));

        // Rellenar los 12 meses
        List<String> mesesLabels = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");
        List<Double> dataVentas = new ArrayList<>();
        List<Double> dataDescuentos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            AnalisisDescuentoDTO dto = mapMensual.getOrDefault(i, new AnalisisDescuentoDTO(i, 0.0, 0.0));
            dataVentas.add(dto.getTotalVendido());
            dataDescuentos.add(dto.getTotalDescuento());
        }

        SeriesItemDTO seriesVentas = new SeriesItemDTO("Ventas Netas", dataVentas);
        SeriesItemDTO seriesDescuentos = new SeriesItemDTO("Descuentos", dataDescuentos);

        return new ChartDataDTO(List.of(seriesVentas, seriesDescuentos), mesesLabels);
    }

    public ChartDataDTO getReporteVentasPorProducto(Integer anio, Integer mes, Long familiaId, Long aromaId) {
        List<ProductoVendidoDTO> ventas = detalleVentaRepository.findVentasPorProducto(anio, mes, familiaId, aromaId);

        List<String> nombresProducto = ventas.stream()
                .map(ProductoVendidoDTO::getNombreProducto)
                .toList();

        List<Double> cantidades = ventas.stream()
                .map(dto -> (double) dto.getCantidadVendida())
                .toList();

        SeriesItemDTO series = new SeriesItemDTO(CANTIDAD_VENDIDA_STRING, cantidades);

        return new ChartDataDTO(List.of(series), nombresProducto);
    }


    public PaginacionResponse<ProductoVendidoDTO> getVentasPorProductoPaginados(
            int page, int size, String sortBy,
            Integer anio, Integer mes, Long familiaId, Long aromaId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<ProductoVendidoDTO> ventas =
                detalleVentaRepository.findVentasPorProductoPaginados(anio, mes, familiaId, aromaId, pageable);

        return PaginacionMapper.mapToResponse(ventas);
    }

    public PaginacionResponse<VentaAgrupadaDTO> getVentasPorAromaPaginado(
            int page, int size, String sortBy,
            Integer anio, Integer mes, Long familiaId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<VentaAgrupadaDTO> ventas = detalleVentaRepository.findVentasPorAromaPaginadosTabla(anio, mes, familiaId, pageable);
        return PaginacionMapper.mapToResponse(ventas);
    }

    public PaginacionResponse<VentaAgrupadaDTO> getVentasPorFamiliaPaginado(
            int page, int size, String sortBy,
            Integer anio, Integer mes, Long aromaId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<VentaAgrupadaDTO> ventas = detalleVentaRepository.findVentasPorFamiliaPaginadosTabla(anio, mes, aromaId, pageable);
        return PaginacionMapper.mapToResponse(ventas);
    }


    public PaginacionResponse<CajaDescuadrada> getDescuadresCajaPaginados(
            int page, int size, String sortBy,
            Integer anio, Integer mes) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<DescuadreCajaDTO> paginaResultados = cajaRepository.findDescuadresPaginado(anio, mes, pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Transformamos el contenido de la página
        Page<CajaDescuadrada> paginaFormateada = paginaResultados.map(dto -> {
            // Nombre formateado
            String nombreFormateado = "Caja #" + dto.getCajaId() +
                    " (" + dto.getFechaCierre().format(formatter) + ")";

            return new CajaDescuadrada(nombreFormateado, dto.getUsuario(), dto.getDiferencia());
        });

        return PaginacionMapper.mapToResponse(paginaFormateada);
    }



    // Gráfico de barras de utilidad (ganancias menos costos)
    public ChartDataDTO getReporteUtilidadAnual(int anio, TipoCliente tipoCliente) {
        List<UtilidadMensualDTO> resultados = ventaRepository.findUtilidadMensualPorAnio(anio, tipoCliente);
        Map<Integer, UtilidadMensualDTO> datosPorMes = resultados.stream()
                .collect(Collectors.toMap(UtilidadMensualDTO::getMes, Function.identity()));

        List<Double> ingresos = new ArrayList<>();
        List<Double> costos = new ArrayList<>();
        List<Double> utilidad = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            UtilidadMensualDTO dato = datosPorMes.getOrDefault(i, null);
            if (dato != null) {
                ingresos.add(dato.getTotalIngresos());
                costos.add(dato.getTotalCostos());
                utilidad.add(dato.getTotalUtilidad());
            } else {
                ingresos.add(0.0);
                costos.add(0.0);
                utilidad.add(0.0);
            }
        }

        SeriesItemDTO seriesIngresos = new SeriesItemDTO("Ingresos", ingresos);
        SeriesItemDTO seriesCostos = new SeriesItemDTO("Costos", costos);
        SeriesItemDTO seriesUtilidad = new SeriesItemDTO("Utilidad", utilidad);

        List<String> mesesLabels = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");

        return new ChartDataDTO(List.of(seriesIngresos, seriesCostos, seriesUtilidad), mesesLabels);
    }



    // Gráfico de torta de ganancias según método de pago entre dos fechas
    public PieChartDataDTO getAnalisisMediosDePago(LocalDate fechaInicio, LocalDate fechaFin, TipoCliente tipoCliente) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<MedioPagoTotalDTO> resultados = pagoVentaRepository.findTotalPorMedioPago(inicioZoned, finZoned, tipoCliente);

        List<Double> series = resultados.stream()
                .map(MedioPagoTotalDTO::getTotal)
                .toList();

        List<String> labels = resultados.stream()
                .map(r -> r.getMedioPago().getNombreParaUi())
                .toList();

        return new PieChartDataDTO(series, labels);
    }



    // Gráfico de barras de descuadre de cajas
    public ChartDataDTO getReporteDescuadresCaja(Integer anio, Integer mes) {
        List<DescuadreCajaDTO> descuadres = cajaRepository.findDescuadres(anio, mes);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<String> labels = descuadres.stream()
                .map(d -> "Caja #" + d.getCajaId() + " (" + d.getFechaCierre().format(formatter) + ")")
                .toList();

        List<Double> data = descuadres.stream()
                .map(DescuadreCajaDTO::getDiferencia)
                .toList();

        SeriesItemDTO series = new SeriesItemDTO("Diferencia", data);

        return new ChartDataDTO(List.of(series), labels);
    }


    // Gráfico de torta distribución tienda de ganancias según tipo de cliente (Mayorista, Detalle)
    public PieChartDataDTO getDistribucionTiendaPorTipoCliente(Integer anio, Integer mes) {
        List<VentaPorTipoClienteDTO> resultados = ventaRepository.findTotalPorTipoCliente(anio, mes);

        List<Double> series = resultados.stream()
                .map(VentaPorTipoClienteDTO::getTotal)
                .toList();

        List<String> labels = resultados.stream()
                .map(r -> r.getTipoCliente().getNombreParaUi())
                .toList();

        return new PieChartDataDTO(series, labels);
    }

    // Gráfico de torta de distribución online según tipo de cliente (Mayorista, Detalle)
    public PieChartDataDTO getDistribucionOnlinePorTipoCliente(Integer anio, Integer mes) {
        double totalDetalleGeneral;
        double totalMayoristaGeneral;

        if (mes != null) {
            // Lógica para un mes
            // Crea un objeto opcional por defecto con valores en cero, para usar en caso de no existir el registro buscado
            VentaOnlineMensual ventaPorDefecto = new VentaOnlineMensual();
            ventaPorDefecto.setTotalDetalle(0.0);
            ventaPorDefecto.setTotalMayorista(0.0);

            VentaOnlineMensual resultado = ventaOnlineMensualRepository.findByAnioAndMes(anio, mes).orElse(ventaPorDefecto);

            totalDetalleGeneral = resultado.getTotalDetalle();
            totalMayoristaGeneral = resultado.getTotalMayorista();
        } else {
            // Lógica para el año completo
            List<VentaOnlineMensual> resultadosAnuales = ventaOnlineMensualRepository.findByAnio(anio);

            totalDetalleGeneral = resultadosAnuales.stream()
                    .mapToDouble(VentaOnlineMensual::getTotalDetalle)
                    .sum();

            totalMayoristaGeneral = resultadosAnuales.stream()
                    .mapToDouble(VentaOnlineMensual::getTotalMayorista)
                    .sum();
        }

        List<Double> series = Arrays.asList(totalDetalleGeneral, totalMayoristaGeneral);
        List<String> labels = Arrays.asList("Detalle", "Mayorista");

        return new PieChartDataDTO(series, labels);
    }

    // Gráfico de barras ganancias de ventas según Aroma
    public ChartDataDTO getReporteVentasPorAroma(Integer anio, Integer mes, Long familiaId) {
        Pageable top10 = Pageable.ofSize(10);
        List<VentaAgrupadaDTO> resultados = detalleVentaRepository.findVentasPorAroma(anio, mes, familiaId, top10);
        return procesarVentasAgrupadas(resultados, CANTIDAD_VENDIDA_STRING);
    }


    // Gráfico de barras ganancias de ventas según Familia
    public ChartDataDTO getReporteVentasPorFamilia(Integer anio, Integer mes, Long aromaId) {
        Pageable top10 = Pageable.ofSize(10);
        List<VentaAgrupadaDTO> resultados = detalleVentaRepository.findVentasPorFamilia(anio, mes, aromaId, top10);
        return procesarVentasAgrupadas(resultados, CANTIDAD_VENDIDA_STRING);
    }

    // Obtiene KPIS de ventas en un único método
    public KpiVentasDTO getKpisVentas(LocalDate fechaInicio, LocalDate fechaFin, TipoCliente tipoCliente) {
        // 1. Validar / Asignar valores por defecto si vienen nulos
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().withDayOfMonth(1); // Primero de este mes
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now(); // Hoy
        }

        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        // Obtener KPIs de la tienda física
        KpiVentasDTO kpis = ventaRepository.getKpisVentas(inicioZoned, finZoned, tipoCliente);

        // Obtener datos online
        String tipoClienteStr = (tipoCliente == null) ? "TODOS" : tipoCliente.name();
        Double online = ventaOnlineMensualRepository.sumVentasOnlineByRango(
                fechaInicio.getYear(),
                fechaInicio.getMonthValue(),
                fechaFin.getYear(),
                fechaFin.getMonthValue(),
                tipoClienteStr);
        kpis.setTotalVentasOnline(online != null ? online : 0.0);

        return kpis;
    }


    /**
     * Gráfico de Línea de Tendencia Diaria (con relleno de días 0).
     * Nota: Requiere que 'mes' NO sea nulo.
     */
    public ChartDataDTO getReporteTendenciaVentasDiarias(Integer anio, Integer mes, Long familiaId, Long aromaId) {
        if (mes == null) {
            // Este gráfico no tiene sentido sin un mes
            return new ChartDataDTO(List.of(new SeriesItemDTO(VENTAS_DIARIAS_STRING, List.of())), List.of());
        }

        List<VentaDiariaDTO> ventas = ventaRepository.findVentasDiariasPorMes(anio, mes, familiaId, aromaId);
        Map<Integer, Double> ventasPorDiaMap = ventas.stream()
                .collect(Collectors.toMap(VentaDiariaDTO::getDia, VentaDiariaDTO::getTotal));

        int diasDelMes = YearMonth.of(anio, mes).lengthOfMonth();

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Rellenar los días con 0 ventas (gap-filling)
        for (int dia = 1; dia <= diasDelMes; dia++) {
            labels.add(String.valueOf(dia)); // "1", "2", "3"...
            data.add(ventasPorDiaMap.getOrDefault(dia, 0.0));
        }

        SeriesItemDTO series = new SeriesItemDTO(VENTAS_DIARIAS_STRING, data);
        return new ChartDataDTO(List.of(series), labels);
    }

    /**
     * Gráfico de Dispersión (Rentabilidad vs. Volumen).
     * Devuelve una lista plana de objetos, el frontend se encarga de graficar.
     */
    public List<ProductPerformanceDTO> getReporteRentabilidadVolumen(Integer anio, Integer mes, Long familiaId, Long aromaId) {
        return detalleVentaRepository.findProductoPerformance(anio, mes, familiaId, aromaId);
    }


    // KPIs (Tarjetas) para la pestaña de Productos.
    public ProductoKpiDTO getKpisAnalisisProducto(Integer anio, Integer mes, Long familiaId, Long aromaId) {
        Pageable topOne = Pageable.ofSize(1);

        // Producto Estrella
        String productoEstrella = detalleVentaRepository
                .findProductoEstrella(anio, mes, familiaId, aromaId, topOne)
                .stream()
                .findFirst()
                .map(VentaAgrupadaDTO::getNombre)
                .orElse("N/A");

        // Aroma Más Popular
        String aromaMasPopular = detalleVentaRepository
                .findAromaMasPopular(anio, mes, familiaId, topOne) // Aroma no se filtra por aromaId
                .stream()
                .findFirst()
                .map(VentaAgrupadaDTO::getNombre)
                .orElse("N/A");

        // Familia Más Popular
        String familiaMasPopular = detalleVentaRepository
                .findFamiliaMasPopular(anio, mes, aromaId, topOne) // Familia no se filtra por familiaId
                .stream()
                .findFirst()
                .map(VentaAgrupadaDTO::getNombre)
                .orElse("N/A");

        // Producto menos vendido
        String productoMenosVendido = detalleVentaRepository
                .findProductoMenosVendido(anio, mes, familiaId, aromaId, topOne)
                .stream()
                .findFirst()
                .map(VentaAgrupadaDTO::getNombre)
                .orElse("N/A");

        return ProductoKpiDTO.builder()
                .productoEstrella(productoEstrella)
                .aromaMasPopular(aromaMasPopular)
                .familiaMasPopular(familiaMasPopular)
                .productoMenosVendido(productoMenosVendido)
                .build();
    }




    /**
     * GRÁFICO 1: Desempeño de Descuadres por Usuario (Gráfico de Barras)
     * Muestra el monto total (absoluto) descuadrado por cada usuario.
     */
    public ChartDataDTO getReporteDescuadresPorUsuario(LocalDate fechaInicio, LocalDate fechaFin) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaAgrupadaPorNombreDTO> resultados = cajaRepository.findDescuadresPorUsuario(inicioZoned, finZoned);

        List<String> labels = resultados.stream()
                .map(VentaAgrupadaPorNombreDTO::getNombre)
                .toList();

        List<Double> data = resultados.stream()
                .map(VentaAgrupadaPorNombreDTO::getTotal)
                .toList();

        SeriesItemDTO series = new SeriesItemDTO("Monto Descuadrado", data);
        return new ChartDataDTO(List.of(series), labels);
    }

    /**
     * GRÁFICO 2: Distribución de Métodos de Pago en Cierre (Gráfico de Torta)
     * Muestra la proporción de cada método de pago según lo declarado en los cierres de caja.
     */
    public PieChartDataDTO getReporteMetodosPagoCierre(LocalDate fechaInicio, LocalDate fechaFin) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        MetodosPagoCierreDTO dto = cajaRepository.findTotalesMetodosPagoCierre(inicioZoned, finZoned);

        List<Double> series = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (dto != null) {
            series.add(Optional.ofNullable(dto.getEfectivoCierre()).orElse(0.0));
            labels.add("Efectivo");

            series.add(Optional.ofNullable(dto.getMercadoPagoCierre()).orElse(0.0));
            labels.add("MercadoPago");

            series.add(Optional.ofNullable(dto.getBciCierre()).orElse(0.0));
            labels.add("BCI");

            series.add(Optional.ofNullable(dto.getBotonDePagoCierre()).orElse(0.0));
            labels.add("Botón de Pago");

            series.add(Optional.ofNullable(dto.getTransferenciaCierre()).orElse(0.0));
            labels.add("Transferencia");

            series.add(Optional.ofNullable(dto.getPostCierre()).orElse(0.0));
            labels.add("Post");
        }

        return new PieChartDataDTO(series, labels);
    }

    /**
     * GRÁFICO 3: Tendencia de Ventas vs. Descuadres (Gráfico de Líneas)
     * Compara mensualmente el total de ventas en tienda vs. el monto neto de descuadres.
     */
    public ChartDataDTO getReporteTendenciaVentasVsDescuadres(Integer anio) {
        List<VentaMensualDTO> ventas = ventaRepository.findTotalVentasEnTiendaPorMes(anio, null);
        Map<Integer, Double> mapaVentas = ventas.stream()
                .collect(Collectors.toMap(VentaMensualDTO::getMes, VentaMensualDTO::getTotal));

        List<VentaMensualDTO> descuadres = cajaRepository.findTotalDescuadresPorMes(anio);
        Map<Integer, Double> mapaDescuadres = descuadres.stream()
                .collect(Collectors.toMap(VentaMensualDTO::getMes, VentaMensualDTO::getTotal));

        // Construir las series para los 12 meses
        List<Double> seriesVentasData = new ArrayList<>();
        List<Double> seriesDescuadresData = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            seriesVentasData.add(mapaVentas.getOrDefault(mes, 0.0));
            seriesDescuadresData.add(mapaDescuadres.getOrDefault(mes, 0.0));
        }

        SeriesItemDTO seriesVentas = new SeriesItemDTO(VENTAS_TOTALES_STRING, seriesVentasData);
        SeriesItemDTO seriesDescuadres = new SeriesItemDTO("Descuadres", seriesDescuadresData);
        List<String> mesesLabels = List.of("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic");

        return new ChartDataDTO(List.of(seriesVentas, seriesDescuadres), mesesLabels);
    }

    // Obtiene los KPIs agregados para la vista de Operaciones.
    public OperacionesKpiDTO getOperacionesKpis(Integer anio, Integer mes) {
        // Si el mes es 0 o nulo (viene de "Todos los meses"), lo pasamos como null
        Integer mesFiltrado = (mes != null && mes > 0) ? mes : null;
        return cajaRepository.findOperacionesKpis(anio, mesFiltrado);
    }


    // Obtener kpis generales
    public KpiGeneralesDTO getKpisGenerales(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long familiaId,
            Long aromaId) {

        // Convertir fechas
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        // 1. KPIs Tienda Física (VentaRepository actual)
        KpiVentasAgregadasDTO kpisVentas = ventaRepository.getKpisAgregadosGenerales(
                inicioZoned, finZoned, tipoCliente, familiaId, aromaId);

        // 2. KPIs Ventas Online
        String tipoClienteStr = (tipoCliente == null) ? "TODOS" : tipoCliente.name();
        Double totalVentasOnline = ventaOnlineMensualRepository.sumVentasOnlineByRango(
                fechaInicio.getYear(),
                fechaInicio.getMonthValue(),
                fechaFin.getYear(),
                fechaFin.getMonthValue(),
                tipoClienteStr
        );

        totalVentasOnline = (totalVentasOnline != null) ? totalVentasOnline : 0.0;

        // 3. Otros cálculos
        Double descuadreNeto = cajaRepository.getDescuadreNetoPorRango(inicioZoned, finZoned);

        // Calcular Ticket Promedio
        double ticketPromedio = 0.0;
        if (kpisVentas.getTotalTransacciones() > 0) {
            ticketPromedio = kpisVentas.getTotalVentasNetas() / kpisVentas.getTotalTransacciones();
        }

        return KpiGeneralesDTO.builder()
                .totalVentasNetas(kpisVentas.getTotalVentasNetas())
                .totalVentasOnline(totalVentasOnline)
                .utilidadNeta(kpisVentas.getUtilidadNeta())
                .totalTransacciones(kpisVentas.getTotalTransacciones())
                .ticketPromedio(ticketPromedio)
                .descuadreNetoTotal(descuadreNeto)
                .build();
    }



    // Obtener tendencias de ventas según periodo y filtros
    public ChartDataDTO getReporteTendenciaVentasDiarias(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long familiaId,
            Long aromaId) {

        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaDiariaProjection> ventas = ventaRepository.findVentasDiariasPorRango(
                inicioZoned, finZoned, tipoCliente, familiaId, aromaId);

        Map<LocalDate, Double> ventasPorDiaMap = ventas.stream()
                .collect(Collectors.toMap(VentaDiariaProjection::getFecha, VentaDiariaProjection::getTotal));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Formateador para el eje X (ej.: "29 Oct")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");

        for (LocalDate date = fechaInicio; !date.isAfter(fechaFin); date = date.plusDays(1)) {
            labels.add(date.format(formatter));
            data.add(ventasPorDiaMap.getOrDefault(date, 0.0));
        }

        SeriesItemDTO series = new SeriesItemDTO(VENTAS_DIARIAS_STRING, data);
        return new ChartDataDTO(List.of(series), labels);
    }

    // Obtener tendencias de ventas según periodo y filtros
    public ChartDataDTO getReporteTendenciaVentasMensual(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long familiaId,
            Long aromaId) {

        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaMensualProjection> ventas = ventaRepository.findVentasMensualesPorRango(
                inicioZoned, finZoned, tipoCliente, familiaId, aromaId);

        // Mapa para búsqueda rápida
        // Usamos YearMonth como clave (ej: 2024-10)
        Map<YearMonth, Double> ventasPorMesMap = ventas.stream()
                .collect(Collectors.toMap(
                        v -> YearMonth.of(v.getAnio(), v.getMes()),
                        VentaMensualProjection::getTotal
                ));

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Formateador para el eje X (ej.: "Oct 2024"). "Ene", "Feb", "Mar"...
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CL"));

        YearMonth inicioMes = YearMonth.from(fechaInicio);
        YearMonth finMes = YearMonth.from(fechaFin);

        for (YearMonth mes = inicioMes; !mes.isAfter(finMes); mes = mes.plusMonths(1)) {
            labels.add(mes.format(formatter));
            data.add(ventasPorMesMap.getOrDefault(mes, 0.0));
        }

        SeriesItemDTO series = new SeriesItemDTO("Ventas Mensuales", data);
        return new ChartDataDTO(List.of(series), labels);
    }

    public ChartDataDTO getReporteTopNVentasPorAroma(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long familiaId,
            Integer top) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        Pageable top5 = Pageable.ofSize(top);
        List<VentaAgrupadaDTO> resultados = detalleVentaRepository.findVentasPorAromaPaginado(
                inicioZoned, finZoned, tipoCliente, familiaId, top5);

        return procesarVentasAgrupadas(resultados, CANTIDAD_VENDIDA_STRING);
    }

    public ChartDataDTO getReporteTopNVentasPorFamilia(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long aromaId,
            Integer top) {
        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        Pageable top5 = Pageable.ofSize(top);
        List<VentaAgrupadaDTO> resultados = detalleVentaRepository.findVentasPorFamiliaPaginado(
                inicioZoned, finZoned, tipoCliente, aromaId, top5);

        return procesarVentasAgrupadas(resultados, CANTIDAD_VENDIDA_STRING);
    }

    // Obtener reporte de mapa de calor de ventas según periodo y filtros
    public ChartDataDTO getReporteMapaCalorVentas(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoCliente tipoCliente,
            Long familiaId,
            Long aromaId) {

        ZonedDateTime inicioZoned = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime finZoned = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        List<VentaPorHoraDiaDTO> ventasAgrupadas = ventaRepository.findVentasPorHoraYDiaSemana(
                inicioZoned, finZoned, tipoCliente, familiaId, aromaId);

        // Preparar los "labels" del eje X (00:00 a 23:00)
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            labels.add(String.format("%02d:00", i));
        }

        // Preparar la estructura de datos. Nombres de las series (eje Y)
        String[] nombresDias = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

        Map<String, List<Double>> seriesDataMap = new LinkedHashMap<>();
        for (String dia : nombresDias) {
            List<Double> horas = new ArrayList<>(Collections.nCopies(24, 0.0));
            seriesDataMap.put(dia, horas);
        }

        // Llenar la matriz con los datos de la consulta
        for (VentaPorHoraDiaDTO venta : ventasAgrupadas) {
            Integer diaSemanaDb = venta.getDiaSemana(); // 0-6
            Integer horaDb = venta.getHora();           // 0-23
            Double total = venta.getTotal();

            // Mapear el índice de la BD al nombre del día
            if (diaSemanaDb >= 0 && diaSemanaDb <= 6) {
                String nombreDia = nombresDias[diaSemanaDb];
                if (horaDb >= 0 && horaDb < 24) {
                    seriesDataMap.get(nombreDia).set(horaDb, total);
                }
            }
        }

        List<SeriesItemDTO> series = seriesDataMap.entrySet().stream()
                .map(entry -> new SeriesItemDTO(entry.getKey(), entry.getValue()))
                .toList();

        return new ChartDataDTO(series, labels);
    }


    // Helper para convertir DTOs de agregación a ChartDataDTO (Gráfico de Barras)
    private ChartDataDTO procesarClientesAgrupados(List<ClienteAgregadoDTO> resultados, String seriesName) {
        List<String> labels = resultados.stream().map(ClienteAgregadoDTO::getNombre).toList();
        List<Double> data = resultados.stream().map(ClienteAgregadoDTO::getValor).toList();
        SeriesItemDTO series = new SeriesItemDTO(seriesName, data);

        return new ChartDataDTO(List.of(series), labels);
    }

    // Helper para convertir DTOs de agregación a PieChartDataDTO (Gráfico de Torta)
    private PieChartDataDTO procesarClientesAgrupadosParaTorta(List<ClienteAgregadoDTO> resultados) {
        List<String> labels = resultados.stream().map(ClienteAgregadoDTO::getNombre).toList();
        List<Double> series = resultados.stream().map(ClienteAgregadoDTO::getValor).toList();

        return new PieChartDataDTO(series, labels);
    }


    /**
     * 1. Gráfico de Torta: Distribución de Clientes Activos por Tipo (MAYORISTA/DETALLE).
     */
    public PieChartDataDTO getReporteDistribucionClientesPorTipo() {
        List<ClienteAgregadoDTO> resultados = clienteRepository.countClientesByTipo();

        // Mapeamos el Enum a un String más amigable para la etiqueta
        resultados.forEach(dto -> {
            try {
                TipoCliente tipo = TipoCliente.valueOf(dto.getNombre());
                dto.setNombre(tipo.toString()); // Usamos el nombre del enum, o puedes usar una lógica de traducción.
            } catch (IllegalArgumentException e) {
                // Manejar caso donde el nombre no sea un TipoCliente válido
                dto.setNombre("Desconocido");
            }
        });

        return procesarClientesAgrupadosParaTorta(resultados);
    }


    /**
     * 2. Gráfico de Barras: Top N Clientes por Ingreso Total Generado (CLV simplificado).
     * @param topN - Top numero de clientes
     * @param anio - Filtro opcional por año de las ventas.
     * @param mes - Filtro opcional por mes de las ventas.
     * @param tipoCliente - Filtro opcional por tipo de cliente.
     */
    public ChartDataDTO getReporteTopClientesPorGasto(
            Integer topN,
            Integer anio,
            Integer mes,
            TipoCliente tipoCliente
    ) {
        if (topN == null || topN <= 0) {
            topN = 10;
        }

        // Creamos Pageable solo para el límite (limit), no necesitamos el Sort si ya está en el @Query
        Pageable topPageable = PageRequest.of(0, topN);

        List<ClienteAgregadoDTO> resultados = ventaRepository.findTopClientesByTotalVenta(
                anio,
                mes,
                tipoCliente,
                topPageable
        );

        return procesarClientesAgrupados(resultados, "Total Gastado ($)");
    }

    /**
     * 3. Gráfico de Torta: Distribución de Clientes por Recencia (Clientes Activos vs. Inactivos).
     * @param diasInactividad - Límite de días para considerar inactivo.
     * @param tipoCliente - Filtro opcional para segmentar solo un tipo (Mayorista/Detalle).
     * NOTA: Los filtros de tiempo (anio/mes) se manejan internamente con diasInactividad.
     */
    public PieChartDataDTO getReporteDistribucionClientesPorRecencia(
            Integer diasInactividad,
            TipoCliente tipoCliente
    ) {
        if (diasInactividad == null || diasInactividad <= 0) {
            diasInactividad = 90;
        }

        ZonedDateTime fechaLimite = DateUtils.obtenerFechaHoraActual().minusDays(diasInactividad);

        // 1. Obtener los IDs de clientes (de un TIPO específico, si se filtra) que NO han comprado desde la fecha límite
        // (Necesitaremos un método en VentaRepository para filtrar la subquery por tipoCliente)

        // Asumiremos que VentaRepository ya fue modificado para manejar esto en el WHERE clause
        List<Long> idsClientesInactivos = clienteRepository.findClientesActivosInactivosDesde(fechaLimite, tipoCliente); // <-- Necesita nuevo método en ClienteRepository
        long countClientesInactivos = idsClientesInactivos.size();

        // 2. Obtener el total de clientes activos (de ese TIPO específico)
        long countTotalClientesActivos = clienteRepository.countByActivoAndTipoOpcional(true, tipoCliente); // <-- Necesita nuevo método en ClienteRepository

        long countClientesActivos = countTotalClientesActivos - countClientesInactivos;

        // ... (Creación de PieChartDataDTO) ...
        List<String> labels = List.of(
                "Clientes Activos (" + diasInactividad + " días)",
                "Clientes Inactivos (En Riesgo)"
        );
        List<Double> series = List.of((double) countClientesActivos, (double) countClientesInactivos);

        return new PieChartDataDTO(series, labels);
    }


    /**
     * Genera los KPIs básicos de la base de clientes activos.
     */
    public ClienteKpisDTO getKpisClientes() {
        long totalActivos = clienteRepository.countByActivo(true);
        long totalMayoristas = clienteRepository.countByActivoAndTipo(true, TipoCliente.MAYORISTA);
        long totalDetalle = clienteRepository.countByActivoAndTipo(true, TipoCliente.DETALLE);

        return ClienteKpisDTO.builder()
                .totalClientesActivos(totalActivos)
                .totalClientesMayoristas(totalMayoristas)
                .totalClientesDetalle(totalDetalle)
                .build();
    }


    /**
     * Lista clientes inactivos (última compra supera el límite de diasInactividad).
     * @param diasInactividad - Límite de días para considerar inactivo.
     * @param tipoCliente - Filtro opcional por tipo de cliente.
     * @return Lista de ClienteInactivoDTO ordenados por la mayor inactividad.
     */
    public List<ClienteInactivoDTO> getReporteClientesInactivosDetalle(Integer diasInactividad, TipoCliente tipoCliente) {
        // 1. Definir el valor final para la lambda
        final long finalDiasInactividad = (diasInactividad == null || diasInactividad <= 0) ? 90 : diasInactividad;
        ZonedDateTime ahora = DateUtils.obtenerFechaHoraActual();

        // 2. Calcular los días inactivos y filtrar
        return ventaRepository.findClientesLastPurchaseDate(tipoCliente).stream()
                .map(dto -> {
                    // Si nunca ha comprado, asumimos que su inactividad es máxima
                    // (ej. 9999 días) para que siempre supere el umbral.
                    long dias = (dto.getUltimaCompra() == null)
                            ? 9999L
                            : ChronoUnit.DAYS.between(dto.getUltimaCompra(), ahora);
                    dto.setDiasInactivo(dias);
                    return dto;
                })
                // 3. Usamos la variable final en la expresión lambda
                .filter(dto -> dto.getDiasInactivo() >= finalDiasInactividad)
                // 4. Ordenar por el cliente con MAYOR inactividad
                .sorted(Comparator.comparing(ClienteInactivoDTO::getDiasInactivo).reversed())
                .toList();
    }



    // Lógica interna
    // Método genérico para procesar ventas agrupadas. Genera gráficos de barras de ventas según un parámetro
    private ChartDataDTO procesarVentasAgrupadas(List<VentaAgrupadaDTO> resultados, String seriesName) {
        List<String> labels = resultados.stream()
                .map(VentaAgrupadaDTO::getNombre)
                .toList();

        List<Double> data = resultados.stream()
                .map(r -> r.getCantidad().doubleValue())
                .toList();

        SeriesItemDTO series = new SeriesItemDTO(seriesName, data);
        return new ChartDataDTO(List.of(series), labels);
    }
}