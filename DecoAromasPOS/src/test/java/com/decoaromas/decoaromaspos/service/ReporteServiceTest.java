package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.graficos.ChartDataDTO;
import com.decoaromas.decoaromaspos.dto.graficos.PieChartDataDTO;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.reportes.*;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import com.decoaromas.decoaromaspos.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private VentaOnlineMensualRepository ventaOnlineMensualRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private PagoVentaRepository pagoVentaRepository;
    @Mock private CajaRepository cajaRepository;
    @InjectMocks
    private ReporteService reporteService;
    private static final int ANIO = 2024;
    @Mock private ClienteRepository clienteRepository;

    // Utilidad: crear un mock o clase anónima para VentaMensualDTO
    private VentaMensualDTO mockVentaMensual(int mes, double total) {
        return new VentaMensualDTO() {
            @Override public Integer getMes() { return mes; }
            @Override public Double getTotal() { return total; }
        };
    }

    private UtilidadMensualDTO mockUtilidadMensual(int mes, double ingresos, double costos, double utilidad) {
        return new UtilidadMensualDTO() {
            @Override public Integer getMes() { return mes; }
            @Override public Double getTotalIngresos() { return ingresos; }
            @Override public Double getTotalCostos() { return costos; }
            @Override public Double getTotalUtilidad() { return utilidad; }
        };
    }

    @Test
    @DisplayName("Test para obtener el reporte de ventas totales por mes, debe combinar online y tienda")
    void getReporteVentasTotalesPorMes_deberiaCombinarTiendaYOnline() {
        // Arrange
        VentaMensualDTO ventaTienda = mockVentaMensual(1, 100.0);
        VentaMensualDTO ventaOnline = mockVentaMensual(1, 50.0);

        when(ventaRepository.findTotalVentasEnTiendaPorMes(ANIO, TipoCliente.DETALLE))
                .thenReturn(List.of(ventaTienda));
        when(ventaOnlineMensualRepository.findTotalVentasOnlinePorMes(ANIO, "DETALLE"))
                .thenReturn(List.of(ventaOnline));

        // Act
        ChartDataDTO resultado = reporteService.getReporteVentasTotalesPorMes(ANIO, null, TipoCliente.DETALLE);
        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getSeries()).hasSize(1);
        assertThat(resultado.getSeries().get(0).getData().get(0)).isEqualTo(150.0);
        verify(ventaRepository).findTotalVentasEnTiendaPorMes(ANIO, TipoCliente.DETALLE);
        verify(ventaOnlineMensualRepository).findTotalVentasOnlinePorMes(ANIO, "DETALLE");
    }

    @Test
    @DisplayName("Test para obtener reporte comparativo de ventas, debe retornar tres en serie")
    void getReporteComparativoVentas_deberiaRetornarTresSeries() {
        // Arrange
        when(ventaRepository.findTotalVentasEnTiendaPorMes(anyInt(), any()))
                .thenReturn(List.of(mockVentaMensual(1, 100.0)));
        when(ventaOnlineMensualRepository.findTotalVentasOnlinePorMes(anyInt(), any()))
                .thenReturn(List.of(mockVentaMensual(1, 50.0)));
        // Act
        ChartDataDTO result = reporteService.getReporteComparativoVentas(ANIO, TipoCliente.MAYORISTA);
        // Assert
        assertThat(result.getSeries()).hasSize(3);
        assertThat(result.getSeries().get(2).getData().get(0)).isEqualTo(150.0);
        verify(ventaRepository).findTotalVentasEnTiendaPorMes(ANIO, TipoCliente.MAYORISTA);
        verify(ventaOnlineMensualRepository).findTotalVentasOnlinePorMes(ANIO, "MAYORISTA");
    }

    @Test
    @DisplayName("Test para obtener distribucion online por tipo de cliente, debe retornar los correctos")
    void getDistribucionOnlinePorTipoCliente_conMes_deberiaRetornarDatosCorrectos() {
        // Arrange
        VentaOnlineMensual mensual = new VentaOnlineMensual();
        mensual.setTotalDetalle(200.0);
        mensual.setTotalMayorista(300.0);
        when(ventaOnlineMensualRepository.findByAnioAndMes(ANIO, 5)).thenReturn(Optional.of(mensual));
        // Act
        PieChartDataDTO result = reporteService.getDistribucionOnlinePorTipoCliente(ANIO, 5);
        // Assert
        assertThat(result.getSeries()).containsExactly(200.0, 300.0);
        assertThat(result.getLabels()).containsExactly("Detalle", "Mayorista");
        verify(ventaOnlineMensualRepository).findByAnioAndMes(ANIO, 5);
    }

    @Test
    @DisplayName("Test para obtener distribucion de ventas vs tipo de venta, debe usar sumatorias de detalle")
    void getReporteDistribucionVentasVsTipoVenta_deberiaUsarSumatoriasDetalle() {
        // Arrange
        when(ventaRepository.sumTotalDetalleByAnioAndMesOpcional(ANIO, null)).thenReturn(100.0);
        when(ventaOnlineMensualRepository.sumTotalDetalleByAnioAndMesOpcional(ANIO, null)).thenReturn(50.0);
        // Act
        PieChartDataDTO result = reporteService.getReporteDistribucionVentasVsTipoVenta(ANIO, null, TipoCliente.DETALLE);
        // Assert
        assertThat(result.getSeries()).containsExactly(100.0, 50.0);
        assertThat(result.getLabels()).containsExactly("Ventas en Tienda", "Ventas Online");
        verify(ventaRepository).sumTotalDetalleByAnioAndMesOpcional(ANIO, null);
        verify(ventaOnlineMensualRepository).sumTotalDetalleByAnioAndMesOpcional(ANIO, null);
    }


    @Test
    @DisplayName("Test para obtener kpis ventas, debe llamar el repositorio correcto")
    void getKpisVentas_deberiaLlamarRepositorioCorrecto() {
        // Arrange
        KpiVentasDTO dto = new KpiVentasDTO(10L, 50.0, 100.0, 500.0);
        when(ventaRepository.getKpisVentas(any(), any(), any())).thenReturn(dto);
        // Act
        KpiVentasDTO result = reporteService.getKpisVentas(LocalDate.now().minusDays(7), LocalDate.now(), TipoCliente.DETALLE);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalVentasNetas()).isEqualTo(500.0);
        verify(ventaRepository).getKpisVentas(any(), any(), eq(TipoCliente.DETALLE));
    }

    @Test
    @DisplayName("Test para obtener reporte top5 de ventas por aroma")
    void getReporteTopNVentasPorAroma_deberiaRetornarDatosCorrectos() {
        // Arrange
        VentaAgrupadaDTO dto1 = new VentaAgrupadaDTO();
        dto1.setNombre("Lavanda");
        dto1.setCantidad(20L);

        VentaAgrupadaDTO dto2 = new VentaAgrupadaDTO();
        dto2.setNombre("Vainilla");
        dto2.setCantidad(10L);

        when(detalleVentaRepository.findVentasPorAromaPaginado(
                any(), any(), eq(TipoCliente.DETALLE), eq(1L), any()))
                .thenReturn(List.of(dto1, dto2));

        // Act
        ChartDataDTO result = reporteService.getReporteTopNVentasPorAroma(
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                TipoCliente.DETALLE,
                1L,
                5
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Cantidad Vendida");
        assertThat(result.getSeries().get(0).getData()).containsExactly(20.0, 10.0);
        assertThat(result.getCategories()).containsExactly("Lavanda", "Vainilla");

        verify(detalleVentaRepository).findVentasPorAromaPaginado(
                any(), any(), eq(TipoCliente.DETALLE), eq(1L), any());
    }

    @Test
    @DisplayName("Test para obtener reporte top5 ventas por familia")
    void getReporteTopNVentasPorFamilia_deberiaRetornarDatosCorrectos() {
        // Arrange
        VentaAgrupadaDTO dto1 = new VentaAgrupadaDTO();
        dto1.setNombre("Ambientadores");
        dto1.setCantidad(15L);

        VentaAgrupadaDTO dto2 = new VentaAgrupadaDTO();
        dto2.setNombre("Velas");
        dto2.setCantidad(8L);

        when(detalleVentaRepository.findVentasPorFamiliaPaginado(
                any(), any(), eq(TipoCliente.MAYORISTA), eq(2L), any()))
                .thenReturn(List.of(dto1, dto2));

        // Act
        ChartDataDTO result = reporteService.getReporteTopNVentasPorFamilia(
                LocalDate.now().minusDays(15),
                LocalDate.now(),
                TipoCliente.MAYORISTA,
                2L,
                5
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getData()).containsExactly(15.0, 8.0);
        assertThat(result.getCategories()).containsExactly("Ambientadores", "Velas");

        verify(detalleVentaRepository).findVentasPorFamiliaPaginado(
                any(), any(), eq(TipoCliente.MAYORISTA), eq(2L), any());
    }

    @Test
    @DisplayName("Test para obtener reporte mapa calor de ventas")
    void getReporteMapaCalorVentas_deberiaGenerarMatrizCompletaYSeriesPorDia() {
        // Arrange
        VentaPorHoraDiaDTO venta1 = new VentaPorHoraDiaDTO(1, 10, 100.0); // Lunes 10:00
        VentaPorHoraDiaDTO venta2 = new VentaPorHoraDiaDTO(5, 15, 50.0);  // Viernes 15:00

        when(ventaRepository.findVentasPorHoraYDiaSemana(
                any(), any(), eq(TipoCliente.DETALLE), eq(1L), eq(2L)))
                .thenReturn(List.of(venta1, venta2));

        // Act
        ChartDataDTO result = reporteService.getReporteMapaCalorVentas(
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                TipoCliente.DETALLE,
                1L,
                2L
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategories()).hasSize(24); // 24 horas
        assertThat(result.getSeries()).hasSize(7); // 7 días de la semana
        assertThat(result.getSeries().get(1).getName()).isEqualTo("Lunes");
        assertThat(result.getSeries().get(1).getData().get(10)).isEqualTo(100.0);
        assertThat(result.getSeries().get(5).getData().get(15)).isEqualTo(50.0);

        verify(ventaRepository).findVentasPorHoraYDiaSemana(any(), any(), eq(TipoCliente.DETALLE), eq(1L), eq(2L));
    }

    @Test
    @DisplayName("Test para generar operaciones kpis, debe llamar repositorio con mes filtrado")
    void getOperacionesKpis_deberiaLlamarRepositorioConMesFiltrado() {
        // Arrange
        OperacionesKpiDTO dto = new OperacionesKpiDTO();
        when(cajaRepository.findOperacionesKpis(2024, null)).thenReturn(dto);

        // Act
        OperacionesKpiDTO result = reporteService.getOperacionesKpis(2024, 0);

        // Assert
        assertThat(result).isNotNull();
        verify(cajaRepository).findOperacionesKpis(2024, null);
    }

    @Test
    @DisplayName("Test para generar kpis generales, debe calcular ticket promedio y devolver los valores correctos")
    void getKpisGenerales_deberiaCalcularTicketPromedioYDevolverValoresCorrectos() {
        // Arrange
        KpiVentasAgregadasDTO kpiVentas = new KpiVentasAgregadasDTO();
        kpiVentas.setTotalVentasNetas(1000.0);
        kpiVentas.setUtilidadNeta(400.0);
        kpiVentas.setTotalTransacciones(10L);

        when(ventaRepository.getKpisAgregadosGenerales(any(), any(), any(), any(), any()))
                .thenReturn(kpiVentas);
        when(cajaRepository.getDescuadreNetoPorRango(any(), any())).thenReturn(50.0);

        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 1, 31);

        // Act
        KpiGeneralesDTO result = reporteService.getKpisGenerales(inicio, fin, TipoCliente.DETALLE, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalVentasNetas()).isEqualTo(1000.0);
        assertThat(result.getUtilidadNeta()).isEqualTo(400.0);
        assertThat(result.getTotalTransacciones()).isEqualTo(10L);
        assertThat(result.getTicketPromedio()).isEqualTo(100.0);
        assertThat(result.getDescuadreNetoTotal()).isEqualTo(50.0);

        verify(ventaRepository).getKpisAgregadosGenerales(any(), any(), eq(TipoCliente.DETALLE), eq(1L), eq(2L));
        verify(cajaRepository).getDescuadreNetoPorRango(any(), any());
    }

    @Test
    @DisplayName("Test para obtener reporte tendencia ventas diarias")
    void getReporteTendenciaVentasDiarias_deberiaConstruirDatosPorRangoDeFechas() {
        // Arrange
        LocalDate inicio = LocalDate.of(2024, 10, 1);
        LocalDate fin = LocalDate.of(2024, 10, 3);

        VentaDiariaProjection v1 = new VentaDiariaProjection() {
            @Override public LocalDate getFecha() { return LocalDate.of(2024, 10, 1); }
            @Override public Double getTotal() { return 100.0; }
        };
        VentaDiariaProjection v2 = new VentaDiariaProjection() {
            @Override public LocalDate getFecha() { return LocalDate.of(2024, 10, 3); }
            @Override public Double getTotal() { return 300.0; }
        };

        when(ventaRepository.findVentasDiariasPorRango(any(), any(), any(), any(), any()))
                .thenReturn(List.of(v1, v2));

        // Act
        ChartDataDTO result = reporteService.getReporteTendenciaVentasDiarias(inicio, fin, TipoCliente.MAYORISTA, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ventas Diarias");
        assertThat(result.getSeries().get(0).getData()).containsExactly(100.0, 0.0, 300.0);
        assertThat(result.getCategories()).hasSize(3); // 1, 2, 3 de octubre

        verify(ventaRepository).findVentasDiariasPorRango(any(), any(), eq(TipoCliente.MAYORISTA), eq(1L), eq(2L));
    }

    @Test
    @DisplayName("Test para obtener reporte tendencia ventas mensual")
    void getReporteTendenciaVentasMensual_deberiaConstruirDatosDeMesesConFormato() {
        // Arrange
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 3, 31);

        VentaMensualProjection v1 = new VentaMensualProjection() {
            @Override public Integer getAnio() { return 2024; }
            @Override public Integer getMes() { return 1; }
            @Override public Double getTotal() { return 500.0; }
        };
        VentaMensualProjection v2 = new VentaMensualProjection() {
            @Override public Integer getAnio() { return 2024; }
            @Override public Integer getMes() { return 3; }
            @Override public Double getTotal() { return 700.0; }
        };

        when(ventaRepository.findVentasMensualesPorRango(any(), any(), any(), any(), any()))
                .thenReturn(List.of(v1, v2));

        // Act
        ChartDataDTO result = reporteService.getReporteTendenciaVentasMensual(inicio, fin, TipoCliente.DETALLE, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ventas Mensuales");
        assertThat(result.getSeries().get(0).getData()).containsExactly(500.0, 0.0, 700.0);
        assertThat(result.getCategories()).hasSize(3); // Ene, Feb, Mar

        verify(ventaRepository).findVentasMensualesPorRango(any(), any(), eq(TipoCliente.DETALLE), eq(1L), eq(2L));
    }

    @Test
    @DisplayName("Test para obtener kpis analisis de productos")
    void getKpisAnalisisProducto_deberiaRetornarValoresCorrectos() {
        // Arrange
        VentaAgrupadaDTO estrella = new VentaAgrupadaDTO("Vela Lavanda", 10L);
        VentaAgrupadaDTO aroma = new VentaAgrupadaDTO("Lavanda", 20L);
        VentaAgrupadaDTO familia = new VentaAgrupadaDTO("Velas", 30L);
        VentaAgrupadaDTO menosVendido = new VentaAgrupadaDTO("Difusor", 5L);

        when(detalleVentaRepository.findProductoEstrella(eq(2024), eq(5), eq(1L), eq(2L), any()))
                .thenReturn(List.of(estrella));
        when(detalleVentaRepository.findAromaMasPopular(eq(2024), eq(5), eq(1L), any()))
                .thenReturn(List.of(aroma));
        when(detalleVentaRepository.findFamiliaMasPopular(eq(2024), eq(5), eq(2L), any()))
                .thenReturn(List.of(familia));
        when(detalleVentaRepository.findProductoMenosVendido(eq(2024), eq(5), eq(1L), eq(2L), any()))
                .thenReturn(List.of(menosVendido));

        // Act
        ProductoKpiDTO result = reporteService.getKpisAnalisisProducto(2024, 5, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductoEstrella()).isEqualTo("Vela Lavanda");
        assertThat(result.getAromaMasPopular()).isEqualTo("Lavanda");
        assertThat(result.getFamiliaMasPopular()).isEqualTo("Velas");
        assertThat(result.getProductoMenosVendido()).isEqualTo("Difusor");

        verify(detalleVentaRepository).findProductoEstrella(eq(2024), eq(5), eq(1L), eq(2L), any());
        verify(detalleVentaRepository).findAromaMasPopular(eq(2024), eq(5), eq(1L), any());
        verify(detalleVentaRepository).findFamiliaMasPopular(eq(2024), eq(5), eq(2L), any());
        verify(detalleVentaRepository).findProductoMenosVendido(eq(2024), eq(5), eq(1L), eq(2L), any());
    }

    @Test
    @DisplayName("Test para obtener reporte descuadres por usuario")
    void getReporteDescuadresPorUsuario_deberiaGenerarSeriesCorrectas() {
        // Arrange
        VentaAgrupadaPorNombreDTO dto1 = new VentaAgrupadaPorNombreDTO("Usuario1", 100.0);
        VentaAgrupadaPorNombreDTO dto2 = new VentaAgrupadaPorNombreDTO("Usuario2", 50.0);

        when(cajaRepository.findDescuadresPorUsuario(any(), any())).thenReturn(List.of(dto1, dto2));

        // Act
        ChartDataDTO result = reporteService.getReporteDescuadresPorUsuario(LocalDate.now().minusDays(7), LocalDate.now());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategories()).containsExactly("Usuario1", "Usuario2");
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Monto Descuadrado");
        assertThat(result.getSeries().get(0).getData()).containsExactly(100.0, 50.0);

        verify(cajaRepository).findDescuadresPorUsuario(any(), any());
    }

    @Test
    @DisplayName("Test para obtener reporte segun metodo pago de cierre")
    void getReporteMetodosPagoCierre_deberiaMapearCamposCorrectamente() {
        // Arrange
        MetodosPagoCierreDTO dto = new MetodosPagoCierreDTO();
        dto.setEfectivoCierre(100.0);
        dto.setMercadoPagoCierre(200.0);
        dto.setBciCierre(50.0);
        dto.setBotonDePagoCierre(75.0);
        dto.setTransferenciaCierre(125.0);
        dto.setPostCierre(75.0);

        when(cajaRepository.findTotalesMetodosPagoCierre(any(), any())).thenReturn(dto);

        // Act
        PieChartDataDTO result = reporteService.getReporteMetodosPagoCierre(LocalDate.now().minusDays(10), LocalDate.now());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).containsExactly(100.0, 200.0, 50.0, 75.0, 125.0, 75.0);
        assertThat(result.getLabels()).containsExactly("Efectivo", "MercadoPago", "BCI", "Botón de Pago", "Transferencia", "Post");
        verify(cajaRepository).findTotalesMetodosPagoCierre(any(), any());
    }

    @Test
    @DisplayName("Test para obtener reporte segun tendencia ventas vs descuadres")
    void getReporteTendenciaVentasVsDescuadres_deberiaCombinarAmbosMapas() {
        // Arrange
        VentaMensualDTO ventaEnero = new VentaMensualDTO() {
            @Override public Integer getMes() { return 1; }
            @Override public Double getTotal() { return 1000.0; }
        };

        VentaMensualDTO ventaFebrero = new VentaMensualDTO() {
            @Override public Integer getMes() { return 2; }
            @Override public Double getTotal() { return 2000.0; }
        };

        when(ventaRepository.findTotalVentasEnTiendaPorMes(eq(2024), isNull()))
                .thenReturn(List.of(ventaEnero, ventaFebrero));

        VentaMensualDTO descuadreEnero = new VentaMensualDTO() {
            @Override public Integer getMes() { return 1; }
            @Override public Double getTotal() { return 100.0; }
        };

        VentaMensualDTO descuadreMarzo = new VentaMensualDTO() {
            @Override public Integer getMes() { return 3; }
            @Override public Double getTotal() { return 300.0; }
        };

        when(cajaRepository.findTotalDescuadresPorMes(eq(2024)))
                .thenReturn(List.of(descuadreEnero, descuadreMarzo));

        // Act
        ChartDataDTO result = reporteService.getReporteTendenciaVentasVsDescuadres(2024);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(2);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ventas Totales");
        assertThat(result.getSeries().get(1).getName()).isEqualTo("Descuadres");

        // Deben existir 12 valores (uno por mes)
        assertThat(result.getCategories()).hasSize(12);

        // Enero
        assertThat(result.getSeries().get(0).getData().get(0)).isEqualTo(1000.0);
        assertThat(result.getSeries().get(1).getData().get(0)).isEqualTo(100.0);

        // Marzo
        assertThat(result.getSeries().get(0).getData().get(2)).isEqualTo(0.0);
        assertThat(result.getSeries().get(1).getData().get(2)).isEqualTo(300.0);

        verify(ventaRepository).findTotalVentasEnTiendaPorMes(eq(2024), isNull());
        verify(cajaRepository).findTotalDescuadresPorMes(eq(2024));
    }


    @Test
    @DisplayName("Test para obtener reporte tendencia ventas diarias con mes nulo, debe retornar series vacias")
    void getReporteTendenciaVentasDiarias_conMesNulo_deberiaRetornarSeriesVacia() {
        // Act
        ChartDataDTO result = reporteService.getReporteTendenciaVentasDiarias(2024, null, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ventas Diarias");
        assertThat(result.getSeries().get(0).getData()).isEmpty();
        assertThat(result.getCategories()).isEmpty();

        verify(ventaRepository, never()).findVentasDiariasPorMes(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Test para obtener reporte tendencia ventas diarias con datos, debe rellenar dias faltantes con ceros")
    void getReporteTendenciaVentasDiarias_conDatos_deberiaRellenarDiasFaltantesConCero() {
        // Arrange
        VentaDiariaDTO ventaDia1 = new VentaDiariaDTO() {
            @Override public Integer getDia() { return 1; }
            @Override public Double getTotal() { return 100.0; }
        };
        VentaDiariaDTO ventaDia3 = new VentaDiariaDTO() {
            @Override public Integer getDia() { return 3; }
            @Override public Double getTotal() { return 300.0; }
        };

        when(ventaRepository.findVentasDiariasPorMes(eq(2024), eq(2), eq(1L), eq(2L)))
                .thenReturn(List.of(ventaDia1, ventaDia3));

        // Act
        ChartDataDTO result = reporteService.getReporteTendenciaVentasDiarias(2024, 2, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ventas Diarias");
        assertThat(result.getSeries().get(0).getData()).hasSize(29); // Febrero 2024 tiene 29 días
        assertThat(result.getSeries().get(0).getData().get(0)).isEqualTo(100.0); // Día 1
        assertThat(result.getSeries().get(0).getData().get(1)).isEqualTo(0.0);   // Día 2
        assertThat(result.getSeries().get(0).getData().get(2)).isEqualTo(300.0); // Día 3
        assertThat(result.getCategories().get(0)).isEqualTo("1");
        assertThat(result.getCategories().get(1)).isEqualTo("2");
        assertThat(result.getCategories().get(2)).isEqualTo("3");

        verify(ventaRepository).findVentasDiariasPorMes(eq(2024), eq(2), eq(1L), eq(2L));
    }

    @Test
    @DisplayName("Test para obtener reporte rentabilidad volumen")
    void getReporteRentabilidadVolumen_deberiaRetornarListaDePerformance() {
        // Arrange
        ProductPerformanceDTO prod1 = new ProductPerformanceDTO("Vela Lavanda", 120L, 10.0);
        ProductPerformanceDTO prod2 = new ProductPerformanceDTO("Difusor Cítrico", 80L, 5.0);

        when(detalleVentaRepository.findProductoPerformance(eq(2024), eq(5), eq(1L), eq(2L)))
                .thenReturn(List.of(prod1, prod2));

        // Act
        List<ProductPerformanceDTO> result = reporteService.getReporteRentabilidadVolumen(2024, 5, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNombreProducto()).isEqualTo("Vela Lavanda");
        assertThat(result.get(0).getRentabilidad()).isEqualTo(10.0);
        assertThat(result.get(1).getVolumen()).isEqualTo(80L);

        verify(detalleVentaRepository).findProductoPerformance(eq(2024), eq(5), eq(1L), eq(2L));
    }


    @Test
    @DisplayName("Test para obtener reporte ventas tiendas por dia de semana")
    void getReporteVentasTiendaPorDiaSemana_deberiaGenerarDatosCorrectos() {
        // Arrange
        VentaAgrupadaPorNombreDTO dto1 = new VentaAgrupadaPorNombreDTO("Lunes", 100.0);
        VentaAgrupadaPorNombreDTO dto2 = new VentaAgrupadaPorNombreDTO("Martes", 200.0);

        when(ventaRepository.findVentasPorDiaDeLaSemana(any(), any(), eq(TipoCliente.DETALLE)))
                .thenReturn(List.of(dto1, dto2));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasTiendaPorDiaSemana(LocalDate.now().minusDays(7), LocalDate.now(), TipoCliente.DETALLE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategories()).containsExactly("Lunes", "Martes");
        assertThat(result.getSeries().get(0).getData()).containsExactly(100.0, 200.0);
        verify(ventaRepository).findVentasPorDiaDeLaSemana(any(), any(), eq(TipoCliente.DETALLE));
    }

    @Test
    @DisplayName("Test para obtener reporte tienda por vendendor")
    void getReporteVentasTiendaPorVendedor_deberiaMapearResultadosCorrectamente() {
        // Arrange
        VentaAgrupadaPorNombreDTO v1 = new VentaAgrupadaPorNombreDTO("Pedro", 300.0);
        VentaAgrupadaPorNombreDTO v2 = new VentaAgrupadaPorNombreDTO("Ana", 500.0);
        when(ventaRepository.findVentasPorVendedor(any(), any())).thenReturn(List.of(v1, v2));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasTiendaPorVendedor(LocalDate.now().minusDays(30), LocalDate.now());

        // Assert
        assertThat(result.getCategories()).containsExactly("Pedro", "Ana");
        assertThat(result.getSeries().get(0).getData()).containsExactly(300.0, 500.0);
        verify(ventaRepository).findVentasPorVendedor(any(), any());
    }

    @Test
    @DisplayName("Test para obtener reporte ventas por hora")
    void getReporteVentasPorHora_deberiaRellenarHorasFaltantesConCero() {
        // Arrange
        VentaAgrupadaPorNombreDTO dto = new VentaAgrupadaPorNombreDTO("10", 250.0);
        when(ventaRepository.findVentasPorHora(any(), any(), eq(TipoCliente.MAYORISTA)))
                .thenReturn(List.of(dto));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasPorHora(LocalDate.now().minusDays(1), LocalDate.now(), TipoCliente.MAYORISTA);

        // Assert
        assertThat(result.getCategories()).hasSize(24);
        assertThat(result.getSeries().get(0).getData().get(10)).isEqualTo(250.0);
        assertThat(result.getSeries().get(0).getData().get(0)).isEqualTo(0.0);
        verify(ventaRepository).findVentasPorHora(any(), any(), eq(TipoCliente.MAYORISTA));
    }

    @Test
    @DisplayName("Test para obtener reporte ventas por producto")
    void getReporteVentasPorProducto_deberiaGenerarGraficoCorrecto() {
        // Arrange
        ProductoVendidoDTO dto1 = new ProductoVendidoDTO("Vela", 10L);
        ProductoVendidoDTO dto2 = new ProductoVendidoDTO("Difusor", 5L);
        when(detalleVentaRepository.findVentasPorProducto(eq(2024), eq(5), eq(1L), eq(2L)))
                .thenReturn(List.of(dto1, dto2));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasPorProducto(2024, 5, 1L, 2L);

        // Assert
        assertThat(result.getCategories()).containsExactly("Vela", "Difusor");
        assertThat(result.getSeries().get(0).getData()).containsExactly(10.0, 5.0);
        verify(detalleVentaRepository).findVentasPorProducto(eq(2024), eq(5), eq(1L), eq(2L));
    }


    @Test
    @DisplayName("Test para obtener reporte ventas por productos paginados")
    void getVentasPorProductoPaginados_deberiaRetornarRespuestaPaginada() {
        // Arrange
        ProductoVendidoDTO dto = new ProductoVendidoDTO("Vela", 10L);
        Page<ProductoVendidoDTO> page = new PageImpl<>(List.of(dto));

        when(detalleVentaRepository.findVentasPorProductoPaginados(eq(2024), eq(5), eq(1L), eq(2L), any()))
                .thenReturn(page);

        // Act
        PaginacionResponse<ProductoVendidoDTO> result = reporteService.getVentasPorProductoPaginados(0, 10, "nombre", 2024, 5, 1L, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent().get(0).getNombreProducto()).isEqualTo("Vela");
        verify(detalleVentaRepository).findVentasPorProductoPaginados(eq(2024), eq(5), eq(1L), eq(2L), any());
    }

    @Test
    @DisplayName("Test para obtener distribucion tienda por tipo de cliente")
    void getDistribucionTiendaPorTipoCliente_deberiaRetornarSeriesYLabelsCorrectos() {
        // Arrange
        VentaPorTipoClienteDTO dto1 = new VentaPorTipoClienteDTO() {
            @Override public TipoCliente getTipoCliente() { return TipoCliente.DETALLE; }
            @Override public Double getTotal() { return 500.0; }
        };

        VentaPorTipoClienteDTO dto2 = new VentaPorTipoClienteDTO() {
            @Override public TipoCliente getTipoCliente() { return TipoCliente.MAYORISTA; }
            @Override public Double getTotal() { return 300.0; }
        };

        when(ventaRepository.findTotalPorTipoCliente(eq(2024), eq(5)))
                .thenReturn(List.of(dto1, dto2));

        // Act
        PieChartDataDTO result = reporteService.getDistribucionTiendaPorTipoCliente(2024, 5);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).containsExactly(500.0, 300.0);
        assertThat(result.getLabels().get(0)).isEqualTo(TipoCliente.DETALLE.getNombreParaUi());
        assertThat(result.getLabels().get(1)).isEqualTo(TipoCliente.MAYORISTA.getNombreParaUi());

        verify(ventaRepository).findTotalPorTipoCliente(eq(2024), eq(5));
    }

    @Test
    @DisplayName("Test para obtener reporte utilidad anual, debe rellenar meses y crear 3 series")
    void getReporteUtilidadAnual_deberiaRellenarMesesYCrearTresSeries() {
        // Arrange
        UtilidadMensualDTO enero = new UtilidadMensualDTO() {
            @Override public Integer getMes() { return 1; }
            @Override public Double getTotalIngresos() { return 1000.0; }
            @Override public Double getTotalCostos() { return 500.0; }
            @Override public Double getTotalUtilidad() { return 500.0; }
        };

        UtilidadMensualDTO marzo = new UtilidadMensualDTO() {
            @Override public Integer getMes() { return 3; }
            @Override public Double getTotalIngresos() { return 2000.0; }
            @Override public Double getTotalCostos() { return 800.0; }
            @Override public Double getTotalUtilidad() { return 1200.0; }
        };

        when(ventaRepository.findUtilidadMensualPorAnio(eq(2024), eq(TipoCliente.DETALLE)))
                .thenReturn(List.of(enero, marzo));

        // Act
        ChartDataDTO result = reporteService.getReporteUtilidadAnual(2024, TipoCliente.DETALLE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(3);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Ingresos");
        assertThat(result.getSeries().get(1).getName()).isEqualTo("Costos");
        assertThat(result.getSeries().get(2).getName()).isEqualTo("Utilidad");

        // Enero
        assertThat(result.getSeries().get(0).getData().get(0)).isEqualTo(1000.0);
        assertThat(result.getSeries().get(1).getData().get(0)).isEqualTo(500.0);
        assertThat(result.getSeries().get(2).getData().get(0)).isEqualTo(500.0);

        // Marzo
        assertThat(result.getSeries().get(0).getData().get(2)).isEqualTo(2000.0);
        assertThat(result.getSeries().get(1).getData().get(2)).isEqualTo(800.0);
        assertThat(result.getSeries().get(2).getData().get(2)).isEqualTo(1200.0);

        verify(ventaRepository).findUtilidadMensualPorAnio(eq(2024), eq(TipoCliente.DETALLE));
    }


    @Test
    @DisplayName("Test para obtener analisis medios de pago")
    void getAnalisisMediosDePago_deberiaConstruirPieChartCorrectamente() {
        // Arrange
        LocalDate inicio = LocalDate.of(2024, 1, 1);
        LocalDate fin = LocalDate.of(2024, 1, 31);

        MedioPagoTotalDTO efectivo = new MedioPagoTotalDTO() {
            @Override public MedioPago getMedioPago() { return MedioPago.EFECTIVO; }
            @Override public Double getTotal() { return 1000.0; }
        };

        MedioPagoTotalDTO mercadoPago = new MedioPagoTotalDTO() {
            @Override public MedioPago getMedioPago() { return MedioPago.MERCADO_PAGO; }
            @Override public Double getTotal() { return 500.0; }
        };

        when(pagoVentaRepository.findTotalPorMedioPago(
                any(ZonedDateTime.class),
                any(ZonedDateTime.class),
                eq(TipoCliente.DETALLE))
        ).thenReturn(List.of(efectivo, mercadoPago));

        // Act
        PieChartDataDTO result = reporteService.getAnalisisMediosDePago(inicio, fin, TipoCliente.DETALLE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).containsExactly(1000.0, 500.0);
        assertThat(result.getLabels()).containsExactly(
                MedioPago.EFECTIVO.getNombreParaUi(),
                MedioPago.MERCADO_PAGO.getNombreParaUi()
        );

        verify(pagoVentaRepository).findTotalPorMedioPago(any(), any(), eq(TipoCliente.DETALLE));
    }

    @Test
    @DisplayName("Test para obtener reporte ventas por aroma")
    void getReporteVentasPorAroma_deberiaRetornarChartConLabelsYDatosCorrectos() {
        // Arrange
        VentaAgrupadaDTO aroma1 = new VentaAgrupadaDTO() {
            @Override public String getNombre() { return "Lavanda"; }
            @Override public Long getCantidad() { return 10L; }
        };
        VentaAgrupadaDTO aroma2 = new VentaAgrupadaDTO() {
            @Override public String getNombre() { return "Vainilla"; }
            @Override public Long getCantidad() { return 15L; }
        };

        when(detalleVentaRepository.findVentasPorAroma(eq(2024), eq(5), eq(1L), any()))
                .thenReturn(List.of(aroma1, aroma2));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasPorAroma(2024, 5, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Cantidad Vendida");
        assertThat(result.getSeries().get(0).getData()).containsExactly(10.0, 15.0);
        assertThat(result.getCategories()).containsExactly("Lavanda", "Vainilla");
        verify(detalleVentaRepository).findVentasPorAroma(eq(2024), eq(5), eq(1L), any());
    }


    @Test
    @DisplayName("Test para obtener reporte ventas por familia")
    void getReporteVentasPorFamilia_deberiaRetornarChartConCategoriesYDatosCorrectos() {
        // Arrange
        VentaAgrupadaDTO familia1 = new VentaAgrupadaDTO() {
            @Override public String getNombre() { return "Ambientadores"; }
            @Override public Long getCantidad() { return 20L; }
        };
        VentaAgrupadaDTO familia2 = new VentaAgrupadaDTO() {
            @Override public String getNombre() { return "Velas"; }
            @Override public Long getCantidad() { return 30L; }
        };

        when(detalleVentaRepository.findVentasPorFamilia(eq(2024), eq(5), eq(2L), any()))
                .thenReturn(List.of(familia1, familia2));

        // Act
        ChartDataDTO result = reporteService.getReporteVentasPorFamilia(2024, 5, 2L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSeries()).hasSize(1);
        assertThat(result.getSeries().get(0).getName()).isEqualTo("Cantidad Vendida");
        assertThat(result.getSeries().get(0).getData()).containsExactly(20.0, 30.0);
        assertThat(result.getCategories()).containsExactly("Ambientadores", "Velas");
        verify(detalleVentaRepository).findVentasPorFamilia(eq(2024), eq(5), eq(2L), any());
    }

    @Test
    @DisplayName("Test para obtener descuadres caja paginados")
    void getDescuadresCajaPaginados_deberiaTransformarYRetornarPaginacionCorrecta() {
        // Arrange
        int page = 0;
        int size = 5;
        String sortBy = "fechaCierre";
        int anio = 2024;
        int mes = 10;

        DescuadreCajaDTO dto1 = new DescuadreCajaDTO() {
            @Override public Long getCajaId() { return 1L; }
            @Override public String getUsuario() { return "Admin"; }
            @Override public ZonedDateTime getFechaCierre() { return ZonedDateTime.parse("2024-10-15T00:00:00Z"); }
            @Override public Double getDiferencia() { return 150.0; }
        };

        DescuadreCajaDTO dto2 = new DescuadreCajaDTO() {
            @Override public Long getCajaId() { return 2L; }
            @Override public String getUsuario() { return "Cajero1"; }
            @Override public ZonedDateTime getFechaCierre() { return ZonedDateTime.parse("2024-10-20T00:00:00Z"); }
            @Override public Double getDiferencia() { return -50.0; }
        };

        List<DescuadreCajaDTO> lista = List.of(dto1, dto2);
        Page<DescuadreCajaDTO> paginaMock = new PageImpl<>(lista);

        when(cajaRepository.findDescuadresPaginado(eq(anio), eq(mes), any(Pageable.class)))
                .thenReturn(paginaMock);

        // Act
        PaginacionResponse<CajaDescuadrada> result =
                reporteService.getDescuadresCajaPaginados(page, size, sortBy, anio, mes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        CajaDescuadrada primera = result.getContent().get(0);
        assertThat(primera.getNombre()).contains("Caja #1");
        assertThat(primera.getNombre()).contains("15-10-2024");
        assertThat(primera.getUsuario()).isEqualTo("Admin");
        assertThat(primera.getDiferencia()).isEqualTo(150.0);

        CajaDescuadrada segunda = result.getContent().get(1);
        assertThat(segunda.getNombre()).contains("Caja #2");
        assertThat(segunda.getNombre()).contains("20-10-2024");
        assertThat(segunda.getUsuario()).isEqualTo("Cajero1");
        assertThat(segunda.getDiferencia()).isEqualTo(-50.0);

        verify(cajaRepository).findDescuadresPaginado(eq(anio), eq(mes), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para obtener familia por paginado")
    void testGetVentasPorFamiliaPaginado() {
        // Parámetros de entrada
        int page = 0;
        int size = 5;
        String sortBy = "nombre";
        Integer anio = 2025;
        Integer mes = 1;
        Long aromaId = 10L;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        // Datos simulados
        VentaAgrupadaDTO dto1 = new VentaAgrupadaDTO("Familia 1", 100L);
        VentaAgrupadaDTO dto2 = new VentaAgrupadaDTO("Familia 2", 50L);

        Page<VentaAgrupadaDTO> pageData =
                new PageImpl<>(List.of(dto1, dto2), pageable, 2);
        when(detalleVentaRepository.findVentasPorFamiliaPaginadosTabla(
                anio, mes, aromaId, pageable))
                .thenReturn(pageData);
        PaginacionResponse<VentaAgrupadaDTO> response =
                reporteService.getVentasPorFamiliaPaginado(page, size, sortBy, anio, mes, aromaId);
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertEquals(2, response.getContent().size());
        assertEquals("Familia 1", response.getContent().get(0).getNombre());
        assertEquals(100L, response.getContent().get(0).getCantidad());
        assertEquals("Familia 2", response.getContent().get(1).getNombre());
        assertEquals(50L, response.getContent().get(1).getCantidad());
        verify(detalleVentaRepository, times(1))
                .findVentasPorFamiliaPaginadosTabla(anio, mes, aromaId, pageable);
    }

    @Test
    @DisplayName("Test para para obtener reporte de clientes inactivos del tipo detalle")
    void getReporteClientesInactivosDetalle() {

        ClienteInactivoDTO c1 = new ClienteInactivoDTO();
        c1.setClienteId(1L);
        c1.setUltimaCompra(null); // nunca compró

        ClienteInactivoDTO c2 = new ClienteInactivoDTO();
        c2.setClienteId(2L);
        c2.setUltimaCompra(ZonedDateTime.now().minusDays(120));

        when(ventaRepository.findClientesLastPurchaseDate(TipoCliente.DETALLE))
                .thenReturn(List.of(c1, c2));

        List<ClienteInactivoDTO> resultado =
                reporteService.getReporteClientesInactivosDetalle(90, TipoCliente.DETALLE);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getDiasInactivo())
                .isGreaterThanOrEqualTo(resultado.get(1).getDiasInactivo());

        verify(ventaRepository).findClientesLastPurchaseDate(TipoCliente.DETALLE);
    }

    @Test
    @DisplayName("Test para obtener KPis de los clientes")
    void getKpisClientes() {

        when(clienteRepository.countByActivo(true)).thenReturn(100L);
        when(clienteRepository.countByActivoAndTipo(true, TipoCliente.MAYORISTA)).thenReturn(30L);
        when(clienteRepository.countByActivoAndTipo(true, TipoCliente.DETALLE)).thenReturn(70L);

        ClienteKpisDTO kpis = reporteService.getKpisClientes();

        assertNotNull(kpis);
        assertEquals(100, kpis.getTotalClientesActivos());
        assertEquals(30, kpis.getTotalClientesMayoristas());
        assertEquals(70, kpis.getTotalClientesDetalle());
    }

    @Test
    @DisplayName("Test para distribucion de clientes por actividad, calcula activos e inactivos")
    void getReporteDistribucionClientesPorActividad() {

        when(clienteRepository.findClientesActivosInactivosDesde(any(), eq(TipoCliente.DETALLE)))
                .thenReturn(List.of(1L, 2L)); // 2 inactivos

        when(clienteRepository.countByActivoAndTipoOpcional(true, TipoCliente.DETALLE))
                .thenReturn(10L); // total activos

        PieChartDataDTO pie =
                reporteService.getReporteDistribucionClientesPorRecencia(90, TipoCliente.DETALLE);

        assertNotNull(pie);
        assertThat(pie.getSeries()).containsExactly(8.0, 2.0);
        assertThat(pie.getLabels()).hasSize(2);
    }

    @Test
    @DisplayName("Test para obtener el top de clientes por gasto, debe retornar los clientes respectivos")
    void getReporteTopClientesPorGasto() {

        ClienteAgregadoDTO dto1 = new ClienteAgregadoDTO("Juan", 50000.0);
        ClienteAgregadoDTO dto2 = new ClienteAgregadoDTO("Pedro", 30000.0);

        when(ventaRepository.findTopClientesByTotalVenta(
                eq(ANIO),
                eq(5),
                eq(TipoCliente.DETALLE),
                any(Pageable.class)
        )).thenReturn(List.of(dto1, dto2));

        ChartDataDTO chart =
                reporteService.getReporteTopClientesPorGasto(2, ANIO, 5, TipoCliente.DETALLE);

        assertNotNull(chart);
        assertThat(chart.getCategories()).containsExactly("Juan", "Pedro");
        assertThat(chart.getSeries().get(0).getData()).containsExactly(50000.0, 30000.0);
    }

    @Test
    @DisplayName("Test para descuadre de cajas, debe retornar grafico de descuadres")
    void getReporteDescuadresCaja_retornagrafico() {

        DescuadreCajaDTO d1 = mock(DescuadreCajaDTO.class);
        DescuadreCajaDTO d2 = mock(DescuadreCajaDTO.class);

        when(d1.getCajaId()).thenReturn(1L);
        when(d1.getFechaCierre()).thenReturn(
                ZonedDateTime.of(2024, 5, 10, 0, 0, 0, 0, ZoneId.systemDefault())
        );
        when(d1.getDiferencia()).thenReturn(-500.0);

        when(d2.getCajaId()).thenReturn(2L);
        when(d2.getFechaCierre()).thenReturn(
                ZonedDateTime.of(2024, 5, 12, 0, 0, 0, 0, ZoneId.systemDefault())
        );
        when(d2.getDiferencia()).thenReturn(300.0);

        when(cajaRepository.findDescuadres(ANIO, 5))
                .thenReturn(List.of(d1, d2));

        ChartDataDTO chart = reporteService.getReporteDescuadresCaja(ANIO, 5);

        assertNotNull(chart);
        assertThat(chart.getCategories()).hasSize(2);
        assertThat(chart.getSeries().get(0).getData())
                .containsExactly(-500.0, 300.0);
    }
}

