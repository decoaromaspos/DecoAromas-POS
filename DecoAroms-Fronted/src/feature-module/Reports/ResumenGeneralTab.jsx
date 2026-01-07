import React, { useState, useEffect } from "react";
import ChartRenderer from "./ChartRenderer";
import * as reportesService from "../../services/reportesService";
import { chartOptions, pieChartOptions, horizontalBarOptions, formatCurrency } from "../../utils/charts";
import PropTypes from 'prop-types';

const formatTitleCase = (str) => {
  if (!str || typeof str !== 'string') return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

// Componente de tarjeta para un solo KPI
const KpiCard = ({ title, value, subtitle, loading, format = true, color = '' }) => (
  <div className="card h-100">
    <div className="card-body text-center d-flex flex-column justify-content-center">
      <h6 className="card-title text-muted mb-2">{title}</h6>
      <h3 className={`display-6 fw-bold ${color}`}>
        {loading ? '...' : (format ? formatCurrency(value) : value.toLocaleString('es-CL'))}
      </h3>
      {subtitle && <small className="text-muted">{subtitle}</small>}
    </div>
  </div>
);

KpiCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  subtitle: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  format: PropTypes.bool,
  color: PropTypes.string,
};

KpiCard.defaultProps = {
  subtitle: '',
  format: true,
  color: '',
};


// Función helper para calcular la diferencia de días
const getDaysDiff = (dateStr1, dateStr2) => {
  const date1 = new Date(dateStr1);
  const date2 = new Date(dateStr2);
  // Asegurarse de que las fechas son válidas
  if (isNaN(date1) || isNaN(date2)) return 0;
  const diffTime = Math.abs(date2.getTime() - date1.getTime());
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
};

const ResumenGeneralTab = ({ filters }) => {
  const [chartData, setChartData] = useState({
    utilidad: null,
    ventasAnuales: null,
    tendenciaVentas: null,
    topFamilias: null,
    topAromas: null,
    mapaCalor: null
  });

  const [kpis, setKpis] = useState({
    totalVentasNetas: 0,
    utilidadNeta: 0,
    totalTransacciones: 0,
    ticketPromedio: 0,
    descuadreNetoTotal: 0
  });

  const [loading, setLoading] = useState(true);

  // --- NUEVA LÓGICA DE FILTROS ---
  // Obtenemos todos los filtros, incluidos los de rango de fechas
  const { anio, mes, tipoCliente, fechaInicio, fechaFin, familiaId, aromaId } = filters;

  useEffect(() => {
    const cargarReportes = async () => {
      setLoading(true);
      try {
        let effectiveFechaInicio, effectiveFechaFin;

        if (fechaInicio && fechaFin) {
          effectiveFechaInicio = fechaInicio;
          effectiveFechaFin = fechaFin;
        } else {
          effectiveFechaInicio = `${anio}-01-01`;
          effectiveFechaFin = `${anio}-12-31`;
        }

        const filtrosParams = {
          fechaInicio: effectiveFechaInicio,
          fechaFin: effectiveFechaFin,
          tipoCliente: tipoCliente || null,
          familiaId: familiaId || null,
          aromaId: aromaId || null
        };

        const diffInDays = getDaysDiff(effectiveFechaInicio, effectiveFechaFin);

        // Decide qué servicio de tendencia llamar
        const tendenciaPromise = (diffInDays > 90)
          ? reportesService.getTendenciaVentasMensual(filtrosParams)
          : reportesService.getTendenciaVentas(filtrosParams);

        const [
          kpiData,
          utilidadData,
          ventasAnualesData,
          tendenciaData, // Este 'await' resolverá la promesa correcta
          topFamiliasData,
          topAromasData,
          mapaCalorData
        ] = await Promise.all([
          reportesService.getKpisGenerales(filtrosParams),
          reportesService.getReporteUtilidadAnual(anio, tipoCliente),
          reportesService.getVentasAnualesTorta(anio, mes || null, tipoCliente || null),
          tendenciaPromise, // Usamos la promesa decidida
          reportesService.getTopFamilias(filtrosParams, 5),
          reportesService.getTopAromas(filtrosParams, 5),
          reportesService.getMapaCalorVentas(filtrosParams),
        ]);

        setKpis(kpiData);
        setChartData({
          utilidad: utilidadData,
          ventasAnuales: ventasAnualesData,
          tendenciaVentas: tendenciaData,
          topFamilias: topFamiliasData,
          topAromas: topAromasData,
          mapaCalor: mapaCalorData
        });

      } catch (error) {
        console.error("Error al cargar reportes de resumen:", error);
        setKpis({ totalVentasNetas: 0, utilidadNeta: 0, totalTransacciones: 0, ticketPromedio: 0, descuadreNetoTotal: 0 });
        setChartData({ utilidad: null, ventasAnuales: null, tendenciaVentas: null, topFamilias: null, topAromas: null, mapaCalor: null });
      } finally {
        setLoading(false);
      }
    };

    cargarReportes();
  }, [anio, mes, tipoCliente, fechaInicio, fechaFin, familiaId, aromaId]);

  const clienteTexto = filters.tipoCliente ? "Cliente " + formatTitleCase(filters.tipoCliente) : 'Todos los clientes';

  // --- Subtítulos ---
  const getAnualSubtitle = () => {
    return `Datos para el año: ${filters.anio} | ${clienteTexto} `;
  };

  // Subtítulo para KPIs y gráficos por RANGO
  const getRangoSubtitle = () => {
    let periodoTexto;
    if (filters.fechaInicio && filters.fechaFin) {
      periodoTexto = `Período: ${filters.fechaInicio} al ${filters.fechaFin}`;
    } else {
      // Muestra el año si no hay rango, ya que ese es el fallback
      periodoTexto = `Año: ${filters.anio}`;
    }
    return `${periodoTexto} | ${clienteTexto}`;
  };

  // --- Opciones de Gráficos ---

  const utilidadVentasOptions = {
    ...chartOptions(`Utilidad Anual ${filters.anio}`, chartData.utilidad?.categories, filters.anio),
    yaxis: { title: { text: 'Valor (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
    tooltip: { y: { formatter: (val) => formatCurrency(val) } }
  };

  const tendenciaVentasOptions = {
    ...chartOptions('Tendencia de Ventas', chartData.tendenciaVentas?.categories, filters.anio),
    chart: {
      height: 350,
      type: 'line',
      toolbar: { show: true },
      zoom: { enabled: true, type: 'x', autoScaleYaxis: true }
    },
    xaxis: {
      categories: chartData.tendenciaVentas?.categories,
      labels: { rotate: 0 }
    },
    stroke: { curve: 'smooth', width: 2 },
    yaxis: { title: { text: 'Total Vendido (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
    tooltip: { y: { formatter: (val) => formatCurrency(val) } }
  };

  const ventasAnualesOptions = {
    ...pieChartOptions('Ventas Tienda vs. Online', chartData.ventasAnuales?.labels, filters.anio),
    colors: ['#775DD0', '#FEB019']
  };

  const topFamiliasOptions = horizontalBarOptions(
    'Top 5 Familias más Vendidas',
    chartData.topFamilias?.categories,
    filters.anio,
    null,
    'Familia'
  );

  const topAromasOptions = horizontalBarOptions(
    'Top 5 Aromas más Vendidos',
    chartData.topAromas?.categories,
    filters.anio,
    null,
    'Aroma'
  );

  const mapaCalorOptions = {
    ...chartOptions('Mapa de Calor de Ventas (Hora vs Día)', chartData.mapaCalor?.categories, filters.anio),
    plotOptions: {
      heatmap: {
        shadeIntensity: 0.5,
        radius: 0,
        useFillColorAsStroke: true,
        colorScale: {
          ranges: [
            { from: 0, to: 1000, name: 'Bajo', color: '#00A100' },
            { from: 1001, to: 50000, name: 'Medio', color: '#128FD9' },
            { from: 50001, to: 100000, name: 'Alto', color: '#FFB200' },
            { from: 100001, to: 10000000, name: 'Muy Alto', color: '#FF0000' }
          ]
        }
      }
    },
    yaxis: { title: { text: 'Día de la Semana' } },
    xaxis: {
      ...chartOptions().xaxis, // Hereda la estructura base
      categories: chartData.mapaCalor?.categories, // Asigna las categorías correctas
      title: { text: 'Hora del Día' } // Añade el título
    },
    tooltip: {
      y: {
        formatter: (value, { series, seriesIndex, dataPointIndex, w }) => {
          const dia = w.config.series[seriesIndex].name;
          const valor = series[seriesIndex][dataPointIndex];
          return `${dia}: ${formatCurrency(valor)}`;
        }
      }
    }
  };


  return (
    <>
      {/* --- FILA DE KPIs (USA getRangoSubtitle) --- */}
      <div className="row mt-4 g-3">
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard title="Ventas en Tienda" value={kpis.totalVentasNetas} subtitle={getRangoSubtitle()} loading={loading} />
        </div>
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard title="Ventas Online" value={kpis.totalVentasOnline} subtitle={getRangoSubtitle()} loading={loading} />
        </div>
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard title="Utilidad Neta en Tienda" value={kpis.utilidadNeta} subtitle={getRangoSubtitle()} loading={loading} color={kpis.utilidadNeta < 0 ? 'text-danger' : 'text-success'} />
        </div>
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard
            title="Ticket Promedio en Tienda"
            value={kpis.ticketPromedio}
            subtitle={
              <>
                {getRangoSubtitle()} <br />
                <small className="text-muted">(Ventas Tienda / Transacciones)</small>
              </>
            }
            loading={loading}
          />
        </div>
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard title="Transacciones en Tienda" value={kpis.totalTransacciones} subtitle={getRangoSubtitle()} loading={loading} format={false} />
        </div>
        <div className="col-xl-2 col-md-4 col-sm-6">
          <KpiCard title="Descuadre Neto en Tienda" value={kpis.descuadreNetoTotal} subtitle={getRangoSubtitle()} loading={loading} color={kpis.descuadreNetoTotal !== 0 ? 'text-danger' : 'text-muted'} />
        </div>
      </div>

      {/* --- FILA 1: TENDENCIA DE VENTAS (USA getRangoSubtitle) --- */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Tendencia de Ventas en Tienda</h5>
              <small className="text-muted">{getRangoSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={tendenciaVentasOptions}
                series={chartData.tendenciaVentas?.series || []}
                type="line"
                height={350}
              />
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 2: UTILIDAD ANUAL (USA getAnualSubtitle) --- */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Utilidad Anual en Tienda (Ingresos vs Costos)</h5>
              <small className="text-muted">{getAnualSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={utilidadVentasOptions}
                series={chartData.utilidad?.series}
                type="bar"
                height={350}
              />
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 3: TOP 5s (USA getRangoSubtitle) --- */}
      <div className="row mt-4">
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Top 5 Familias más Vendidas (Tienda)</h5>
              <small className="text-muted">{getRangoSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={topFamiliasOptions}
                series={chartData.topFamilias?.series}
                type="bar"
                height={350}
              />
            </div>
          </div>
        </div>
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Top 5 Aromas más Vendidos (Tienda)</h5>
              <small className="text-muted">{getRangoSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={topAromasOptions}
                series={chartData.topAromas?.series}
                type="bar"
                height={350}
              />
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 4: DONUT Y MAPA DE CALOR --- */}
      <div className="row mt-4">
        <div className="col-lg-5 mb-4">
          <div className="card h-100">
            <div className="card-header">
              {/* Este gráfico es ANUAL */}
              <h5 className="card-title mb-0">Ventas Tienda vs. Online</h5>
              <small className="text-muted">{getAnualSubtitle()}</small>
            </div>
            <div className="card-body d-flex align-items-center justify-content-center">
              <ChartRenderer
                loading={loading}
                options={ventasAnualesOptions}
                series={chartData.ventasAnuales?.series}
                type="donut"
                height={420}
              />
            </div>
          </div>
        </div>
        <div className="col-lg-7 mb-4">
          <div className="card h-100">
            {/* Este gráfico es por RANGO */}
            <div className="card-header">
              <h5 className="card-title mb-0">Mapa de Calor de Ventas en Tienda</h5>
              <small className="text-muted">{getAnualSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={mapaCalorOptions}
                series={chartData.mapaCalor?.series || []}
                type="heatmap"
                height={420}
              />
            </div>
          </div>
        </div>
      </div>


      {/* --- FILA 5: TABLA DE UTILIDAD (Es ANUAL) --- */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card">
            <div className="card-body">
              <h5 className="card-title">Detalle de Utilidad en Tienda Mensual ({filters.anio}) | {clienteTexto} </h5>
              {loading ? <p>Cargando...</p> : (
                <div className="table-responsive">
                  <table className="table table-striped">
                    <thead><tr><th>Mes</th><th>Ingresos</th><th>Costos</th><th>Utilidad</th></tr></thead>
                    <tbody>
                      {chartData.utilidad?.categories?.map((mes, index) => {
                        const ingresos = chartData.utilidad.series.find(s => s.name === 'Ingresos')?.data[index] || 0;
                        const costos = chartData.utilidad.series.find(s => s.name === 'Costos')?.data[index] || 0;
                        const utilidad = chartData.utilidad.series.find(s => s.name === 'Utilidad')?.data[index] || 0;
                        return (
                          <tr key={index}>
                            <td><strong>{mes}</strong></td>
                            <td>{formatCurrency(ingresos)}</td>
                            <td>{formatCurrency(costos)}</td>
                            <td className={utilidad < 0 ? 'text-danger' : 'text-success'}><strong>{formatCurrency(utilidad)}</strong></td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

// Actualizamos los PropTypes para que coincidan con el objeto 'filters' completo
ResumenGeneralTab.propTypes = {
  filters: PropTypes.shape({
    anio: PropTypes.string.isRequired,
    mes: PropTypes.string.isRequired,
    tipoVenta: PropTypes.string.isRequired,
    tipoCliente: PropTypes.string.isRequired,
    familiaId: PropTypes.string.isRequired,
    aromaId: PropTypes.string.isRequired,
    fechaInicio: PropTypes.string.isRequired,
    fechaFin: PropTypes.string.isRequired,
  }).isRequired,
};

export default ResumenGeneralTab;