import React, { useState, useEffect } from "react";
import PropTypes from 'prop-types';
import ChartRenderer from "./ChartRenderer";
import * as reportesService from "../../services/reportesService";
import { pieChartOptions, chartOptions, horizontalBarOptions, formatCurrency } from "../../utils/charts";
import Table from '../../core/pagination/datatable';
import Select from 'react-select';
import { Sliders } from 'feather-icons-react/build/IconComponents';

const mesesNombres = {
  "1": "Enero", "2": "Febrero", "3": "Marzo", "4": "Abril", "5": "Mayo", "6": "Junio",
  "7": "Julio", "8": "Agosto", "9": "Septiembre", "10": "Octubre", "11": "Noviembre", "12": "Diciembre"
};

const formatTitleCase = (str) => {
  if (!str || typeof str !== 'string') return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

const OperacionesTab = ({ filters }) => {
  const [loading, setLoading] = useState(true);

  const [chartData, setChartData] = useState({
    descuadresCaja: null,
    descuadresPorUsuario: null,
    metodosPagoCierre: null,
    tendenciaVentasVsDescuadres: null,
    ventasPorHora: null
  });

  const [kpis, setKpis] = useState({
    totalDescuadradoNeto: 0,
    totalDescuadradoAbsoluto: 0,
    numCajasDescuadre: 0,
  });

  // --- Estado para Paginación  ---
  const [paginatedDescuadres, setPaginatedDescuadres] = useState({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 10 });
  const [paginationState, setPaginationState] = useState({ page: 0, size: 10 });
  const [loadingTable, setLoadingTable] = useState(true);

  // Opciones para el selector de tamaño de página
  const pageSizeOptions = [
    { value: 5, label: "5 por pág." },
    { value: 10, label: "10 por pág." },
    { value: 20, label: "20 por pág." },
  ];

  useEffect(() => {
    const cargarReportes = async () => {
      setLoading(true);
      try {
        const { anio, mes, fechaInicio, fechaFin } = filters;

        const [
          kpisData,
          descuadresCajaData,
          tendenciaData, // Datos crudos del backend
          descuadresUsuarioData,
          metodosPagoData
        ] = await Promise.all([
          reportesService.getOperacionesKpis(anio, mes),
          reportesService.getReporteDescuadresCaja(anio, mes),
          reportesService.getReporteTendenciaVentasVsDescuadres(anio),
          reportesService.getReporteDescuadresPorUsuario(fechaInicio, fechaFin),
          reportesService.getReporteMetodosPagoCierre(fechaInicio, fechaFin)
        ]);


        const seriesMixtas = [];
        if (tendenciaData?.series && tendenciaData.series.length > 0) {
          // Serie 1 (Ventas) -> tipo 'bar'
          seriesMixtas.push({
            name: tendenciaData.series[0]?.name || 'Ventas Totales',
            data: tendenciaData.series[0]?.data || [],
            type: 'bar'
          });
          // Serie 2 (Descuadres) -> tipo 'line'
          seriesMixtas.push({
            name: tendenciaData.series[1]?.name || 'Descuadres',
            data: tendenciaData.series[1]?.data || [],
            type: 'line'
          });
        }

        setKpis(kpisData);
        setChartData({
          descuadresCaja: descuadresCajaData,
          // Usamos un nuevo objeto para el gráfico mixto
          tendenciaVentasVsDescuadres: {
            ...tendenciaData, // Mantenemos las categorías
            series: seriesMixtas // Usamos las series modificadas
          },
          descuadresPorUsuario: descuadresUsuarioData,
          metodosPagoCierre: metodosPagoData
        });

      } catch (error) {
        console.error("Error al cargar reportes de operaciones:", error);
        setChartData({
          descuadresCaja: null, descuadresPorUsuario: null,
          metodosPagoCierre: null, tendenciaVentasVsDescuadres: null,
          ventasPorHora: null
        });
        setKpis({ totalDescuadradoNeto: 0, totalDescuadradoAbsoluto: 0, numCajasDescuadre: 0 });
      } finally {
        setLoading(false);
      }
    };

    cargarReportes();
  }, [filters]);


  // --- useEffect para la TABLA PAGINADA de Descuadres ---
  useEffect(() => {
    const cargarDescuadresPaginados = async () => {
      setLoadingTable(true);
      try {
        const data = await reportesService.getCajasDescuadradasPaginacion(
          paginationState.page,
          paginationState.size,
          null, // sortBy
          filters.anio,
          filters.mes
        );
        setPaginatedDescuadres(data);
      } catch (error) {
        console.error("Error cargando descuadres paginados:", error);
        setPaginatedDescuadres({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: paginationState.size });
      } finally {
        setLoadingTable(false);
      }
    };

    cargarDescuadresPaginados();
  }, [
    paginationState,
    filters.anio,
    filters.mes
  ]);

  // Cuando los filtros globales (año/mes) cambian, volvemos a la página 0
  useEffect(() => {
    setPaginationState(prev => ({ ...prev, page: 0 }));
  }, [filters.anio, filters.mes]);


  // --- Funciones de Subtítulos ---
  const getRangoFechasSubtitle = (conTipoCliente = false) => {
    const { fechaInicio, fechaFin, tipoCliente } = filters;
    const clienteTexto = (conTipoCliente && tipoCliente) ? `Cliente ${formatTitleCase(tipoCliente)}` : (conTipoCliente ? 'Todos los clientes' : null);
    let periodoTexto;
    if (fechaInicio && fechaFin) periodoTexto = `Período: ${fechaInicio} al ${fechaFin}`;
    else if (fechaInicio) periodoTexto = `Desde: ${fechaInicio}`;
    else if (fechaFin) periodoTexto = `Hasta: ${fechaFin}`;
    else periodoTexto = 'Todo el período';
    return [periodoTexto, clienteTexto].filter(Boolean).join(' | ');
  };
  const getAnioMesSubtitle = () => {
    const { anio, mes } = filters;
    const mesTexto = (mes && mes !== "0") ? mesesNombres[mes] : 'Todos los meses';
    return `Año: ${anio} | Mes: ${mesTexto}`;
  };
  const getAnioSubtitle = () => {
    return `Año: ${filters.anio}`;
  };


  // --- Handlers para Paginación ---
  const handlePageChange = (page) => {
    // El componente <Table> devuelve 1-based, lo convertimos a 0-based
    setPaginationState(prev => ({ ...prev, page: page - 1 }));
  };

  const handlePageSizeChange = (selected) => {
    setPaginationState(prev => ({
      ...prev,
      size: selected.value,
      page: 0 // Resetea a la página 0
    }));
  };

  // --- Columnas para la Tabla Paginada---
  const descuadresColumns = [
    {
      title: "Caja / Fecha Cierre",
      dataIndex: "nombre",
      key: "nombre",
      sorter: (a, b) => a.nombre.localeCompare(b.nombre),
    },
    {
      title: "Usuario",
      dataIndex: "usuario",
      key: "usuario",
      sorter: (a, b) => a.usuario.localeCompare(b.usuario),
      width: '25%',
    },
    {
      title: "Monto del Descuadre",
      dataIndex: "diferencia",
      key: "diferencia",
      sorter: (a, b) => a.diferencia - b.diferencia,
      render: (monto) => {
        const colorClass = monto < 0 ? 'text-danger' : 'text-success';
        return (
          <strong className={colorClass}>
            {formatCurrency(monto)}
          </strong>
        );
      },
      align: 'end', // Alinea la columna (header y celdas) a la derecha
      width: '25%',
    }
  ];




  // --- Opciones de Gráficos  ---
  const descuadresChartOptions = {
    ...chartOptions('Descuadres de Caja', chartData.descuadresCaja?.categories),
    yaxis: { title: { text: 'Monto (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
    tooltip: { y: { formatter: (val) => formatCurrency(val) } },
    plotOptions: { bar: { colors: { ranges: [{ from: -Infinity, to: -0.01, color: '#FF4560' }, { from: 0, to: Infinity, color: '#00E396' }] }, columnWidth: '80%' } },
  };
  const descuadresPorUsuarioOptions = {
    ...horizontalBarOptions('Top Usuarios por Monto Descuadrado (Absoluto)', chartData.descuadresPorUsuario?.categories),
    xaxis: {
      categories: chartData.descuadresPorUsuario?.categories,
      title: { text: 'Monto Total Descuadrado (CLP)' },
      labels: { formatter: (val) => formatCurrency(val) }
    },
    yaxis: { title: { text: 'Usuario' } },
    tooltip: { y: { formatter: (val) => formatCurrency(val) } },
    legend: { show: false },
    plotOptions: {
      bar: {
        horizontal: true,
        borderRadius: 4,
        distributed: true,
      },
    },
  };
  const metodosPagoCierreOptions = {
    ...pieChartOptions('Composición Métodos de Pago', chartData.metodosPagoCierre?.labels),
  };

  const tendenciaVentasVsDescuadresOptions = {
    ...chartOptions('Tendencia: Ventas Totales vs. Descuadres Netos', chartData.tendenciaVentasVsDescuadres?.categories),
    yaxis: [
      {
        seriesName: 'Ventas Totales',
        title: { text: 'Ventas Totales (CLP)' },
        labels: { formatter: (val) => formatCurrency(val) }
      },
      {
        seriesName: 'Descuadres',
        opposite: true,
        title: { text: 'Descuadres Netos (CLP)' },
        labels: { formatter: (val) => formatCurrency(val) }
      }
    ],
    tooltip: {
      y: { formatter: (val) => formatCurrency(val) },
      shared: true,
      intersect: false,
    },
    stroke: { width: [0, 2], curve: 'smooth' },
    dataLabels: {
      enabled: true,
      enabledOnSeries: [1],
      formatter: function (val) {
        return formatCurrency(val);
      }
    },
  };

  return (
    <>
      {/* --- FILA DE KPIs --- */}
      <div className="row mt-4">
        <div className="col-md-4 mb-3">
          <div className="card h-100">
            <div className="card-body text-center d-flex flex-column justify-content-center">
              <h6 className="card-title text-muted mb-2">Descuadre Neto</h6>
              <h3 className={`display-6 fw-bold ${kpis.totalDescuadradoNeto < 0 ? 'text-danger' : 'text-success'}`}>
                {loading ? '...' : formatCurrency(kpis.totalDescuadradoNeto)}
              </h3>
              <small className="text-muted">(Suma de faltantes y sobrantes)</small>
              <small className="text-muted mt-2">{getAnioMesSubtitle()}</small>
            </div>
          </div>
        </div>
        <div className="col-md-4 mb-3">
          <div className="card h-100">
            <div className="card-body text-center d-flex flex-column justify-content-center">
              <h6 className="card-title text-muted mb-2">Descuadre Total (Absoluto)</h6>
              <h3 className="display-6 fw-bold text-danger">
                {loading ? '...' : formatCurrency(kpis.totalDescuadradoAbsoluto)}
              </h3>
              <small className="text-muted">(Impacto real en caja)</small>
              <small className="text-muted mt-2">{getAnioMesSubtitle()}</small>
            </div>
          </div>
        </div>
        <div className="col-md-4 mb-3">
          <div className="card h-100">
            <div className="card-body text-center d-flex flex-column justify-content-center">
              <h6 className="card-title text-muted mb-2">Cajas con Descuadre</h6>
              <h3 className="display-6 fw-bold">
                {loading ? '...' : kpis.numCajasDescuadre.toLocaleString('es-CL')}
              </h3>
              <small className="text-muted">(Nº de cierres con diferencias)</small>
              <small className="text-muted mt-2">{getAnioMesSubtitle()}</small>
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 1: GRÁFICO PRINCIPAL (HERO) --- */}
      <div className="row">
        <div className="col-12">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Evolución de Ventas vs. Descuadres</h5>
              <small className="text-muted">{getAnioSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={tendenciaVentasVsDescuadresOptions}
                series={chartData.tendenciaVentasVsDescuadres?.series || []}
                type="line"
                height={350}
              />
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 2: GRÁFICOS DE DESCUADRES --- */}
      <div className="row mt-4">
        <div className="col-md-12 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Gráfico de Descuadres de Caja</h5>
              <small className="text-muted">{getAnioMesSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={descuadresChartOptions}
                series={chartData.descuadresCaja?.series}
                type="bar"
                height={350}
                message="¡Felicidades! No se encontraron descuadres en este período."
              />
            </div>
          </div>
        </div>

      </div>

      {/* --- FILA 3: GRÁFICOS DE OPERACIÓN --- */}
      <div className="row mt-4">
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Composición Métodos de Pago (Cierre de Caja)</h5>
              <small className="text-muted">{getRangoFechasSubtitle()}</small>
            </div>
            <div className="card-body d-flex align-items-center justify-content-center">
              <ChartRenderer
                loading={loading}
                options={metodosPagoCierreOptions}
                series={chartData.metodosPagoCierre?.series}
                type="donut"
                height={400}
                message="No hay datos de cierres para este período."
              />
            </div>
          </div>
        </div>
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Rendimiento de Descuadre por Usuario</h5>
              <small className="text-muted">{getRangoFechasSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer
                loading={loading}
                options={descuadresPorUsuarioOptions}
                series={chartData.descuadresPorUsuario?.series}
                type="bar"
                height={350}
                message="No se encontraron descuadres de usuarios en este período."
              />
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 4: TABLA DE DETALLE --- */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card">
            <div className="card-header">
              <h5 className="card-title mb-0">Detalle de Descuadres de Caja</h5>
              <small className="text-muted">{getAnioMesSubtitle()}</small>
            </div>
            <div className="card-body">

              {/* --- Controles de Paginación  --- */}
              <div className="table-top">
                <div className="search-set"></div>
                <div className="d-flex align-items-center">
                  <div className="form-sort me-3">
                    <Sliders className="info-img" />
                    <Select
                      className="select"
                      options={pageSizeOptions}
                      value={pageSizeOptions.find(opt => opt.value === paginationState.size)}
                      onChange={handlePageSizeChange}
                      placeholder="Tamaño"
                    />
                  </div>
                </div>
              </div>

              {/* --- Tabla Paginada  --- */}
              <div className="table-responsive">
                {loadingTable ? (
                  <div className="text-center p-4">
                    <div className="spinner-border" role="status">
                      <span className="visually-hidden">Cargando...</span>
                    </div>
                  </div>
                ) : (
                  <>
                    <Table
                      columns={descuadresColumns}
                      dataSource={paginatedDescuadres.content || []}
                      pagination={{
                        current: paginatedDescuadres.pageNumber + 1, // 1-based
                        pageSize: paginatedDescuadres.pageSize,
                        total: paginatedDescuadres.totalElements,
                        onChange: handlePageChange,
                      }}
                      rowKey="nombre" // Usar un campo único (nombre de caja)
                    />

                    <div className="pagination-info mt-3 text-center">
                      <span>
                        Mostrando {paginatedDescuadres.content?.length || 0} de {paginatedDescuadres.totalElements || 0} registros
                        (Página {paginatedDescuadres.pageNumber + 1} de {paginatedDescuadres.totalPages || 0})
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

OperacionesTab.propTypes = {
  filters: PropTypes.shape({
    anio: PropTypes.string.isRequired,
    mes: PropTypes.string.isRequired,
    tipoVenta: PropTypes.string,
    tipoCliente: PropTypes.string,
    fechaInicio: PropTypes.string,
    fechaFin: PropTypes.string,
  }).isRequired,
};

export default OperacionesTab;