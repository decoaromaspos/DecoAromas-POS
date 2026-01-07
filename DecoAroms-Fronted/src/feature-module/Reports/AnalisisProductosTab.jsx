import React, { useState, useEffect } from "react";
import ChartRenderer from "./ChartRenderer";
import * as reportesService from "../../services/reportesService";
import * as aromasService from "../../services/aromaService";
import * as familiasService from "../../services/familiaService";
import { chartOptions, horizontalBarOptions, formatCurrency, getDynamicTitles } from "../../utils/charts";
import PropTypes from 'prop-types';
import Table from '../../core/pagination/datatable';
import Select from 'react-select';
import { Sliders } from 'feather-icons-react/build/IconComponents';

const mesesNombres = {
  "1": "Enero", "2": "Febrero", "3": "Marzo", "4": "Abril", "5": "Mayo", "6": "Junio",
  "7": "Julio", "8": "Agosto", "9": "Septiembre", "10": "Octubre", "11": "Noviembre", "12": "Diciembre"
};

// Opciones para el gráfico de Línea (CON formatCurrency)
const getLineChartOptions = (title, categories, anio, mes) => ({ 
  ...chartOptions(title, categories, anio, mes), // Pasamos anio y mes
  chart: { height: 350, type: 'line', toolbar: { show: true } },
  stroke: { curve: 'smooth', width: 2 },
  
  xaxis: {
    ...chartOptions(title, categories, anio, mes).xaxis,
    title: { text: 'Número de día del mes' } 
  },

  yaxis: {
    title: { text: 'Total Vendido' },
    labels: { formatter: (val) => formatCurrency(val) }
  },
  tooltip: {
    y: { formatter: (val) => formatCurrency(val) }
  }
});

// Opciones para el gráfico de Dispersión
const getScatterChartOptions = (title, anio, mes, maxScale = 20) => ({
  chart: {
    height: 350,
    type: 'scatter',
    zoom: { enabled: true, type: 'xy' },
    toolbar: { show: true }
  },
  title: getDynamicTitles(title, anio, mes),
  xaxis: {
    type: 'numeric',
    min: 0,       
    max: maxScale, 
    tickAmount: maxScale <= 30 ? maxScale : 10,

    tickPlacement: 'on', // Asegura que el tick esté en el valor
    decimalsInFloat: 0,
    title: { text: 'Volumen (Unidades Vendidas)' },
    labels: {
      formatter: (val) => parseFloat(val).toFixed(0),
      style: {
        fontSize: '11px'
      }
    },
    // Opcional: añade un poco de padding para que los puntos extremos no se corten
    crosshairs: {
      show: true
    },
    tooltip: {
      enabled: false 
    }
  },
  yaxis: {
    title: { text: 'Rentabilidad (Ganancia)' },
    labels: { formatter: (val) => formatCurrency(val) }
  },
  tooltip: {
     enabled: true,
     custom: function ({ seriesIndex, dataPointIndex, w }) {
        const data = w.globals.initialSeries[seriesIndex].data[dataPointIndex];
        if (!data) { return ''; }
        const nombreProducto = data.name;
        const volumen = data.x;
        const rentabilidad = data.y;

        return `
        <div class="apexcharts-tooltip-content p-2 shadow-sm border rounded" style="font-family: Arial, sans-serif; font-size: 12px; color: #333; background-color: #fff;">
            <div class="fw-bold mb-1" style="border-bottom: 1px solid #ddd; padding-bottom: 4px;">
                ${nombreProducto}
            </div>
            <div class="mt-1">
                <span style="color: #666;">Rentabilidad:</span>
                <span class="fw-bold" style="margin-left: 8px;">${formatCurrency(rentabilidad)}</span>
            </div>
            <div>
                <span style="color: #666;">Volumen:</span>
                <span class="fw-bold" style="margin-left: 8px;">${volumen} unid.</span>
            </div>
        </div>
      `;
     }
  }
});

// --- Componente de Tarjeta KPI ---
const KpiCard = ({ title, value, loading, color = 'text-dark', subtitle }) => (
  <div className="col-md-3 mb-3">
    <div className="card h-100">
      <div className="card-body text-center d-flex flex-column justify-content-center">
        <h6 className="card-title text-muted mb-2">{title}</h6>
        {loading ? (
          <h3 className="display-6 fw-bold">...</h3>
        ) : (
          <h3 className={`display-6 fw-bold ${color}`}>
            {value}
          </h3>
        )}
        {subtitle && <small className="text-muted mt-2">{subtitle}</small>}
      </div>
    </div>
  </div>
);

KpiCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  loading: PropTypes.bool.isRequired,
  icon: PropTypes.string,
  color: PropTypes.string,
  subtitle: PropTypes.string, // Prop para el subtítulo
};



const AnalisisProductosTab = ({ filters }) => {
  const [activeSecondaryTab, setActiveSecondaryTab] = useState('categorias');
  const [opciones, setOpciones] = useState({ aromas: [], familias: [] });

  const [kpis, setKpis] = useState(null);
  const [chartData, setChartData] = useState({
    ventasPorProducto: null,
    ventasPorFamilia: null,
    ventasPorAroma: null,
    tendenciaDiaria: null,
    performance: null
  });
  const [loading, setLoading] = useState(true);
  const [loadingOpciones, setLoadingOpciones] = useState(true);
  const { anio, mes, familiaId, aromaId } = filters;

  // Estado para el Tamaño Global (reemplaza los size individuales en la inicialización si quieres)
  const [globalPageSize, setGlobalPageSize] = useState(5);

  // Estado Paginación PRODUCTOS
  const [paginatedProducts, setPaginatedProducts] = useState({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 10 });
  const [paginationState, setPaginationState] = useState({ page: 0, size: 10 });
  const [loadingTable, setLoadingTable] = useState(true);

  // Estados para Paginación FAMILIAS
  const [paginatedFamilias, setPaginatedFamilias] = useState({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 5 });
  const [loadingTableFamilias, setLoadingTableFamilias] = useState(false);
  const [pagStateFamilias, setPagStateFamilias] = useState({ page: 0, size: 5 });

  // Estados para Paginación AROMAS
  const [paginatedAromas, setPaginatedAromas] = useState({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 5 });
  const [loadingTableAromas, setLoadingTableAromas] = useState(false);
  const [pagStateAromas, setPagStateAromas] = useState({ page: 0, size: 5 });

  // Opciones para el selector de tamaño de página
  const pageSizeOptions = [
    { value: 5, label: "5 por pág." },
    { value: 10, label: "10 por pág." },
    { value: 20, label: "20 por pág." },
  ];

  // Handler Global de Tamaño
  const handleGlobalSizeChange = (selected) => {
    const newSize = selected.value;
    setGlobalPageSize(newSize);
    
    // Actualizamos ambas tablas y reseteamos a página 0
    setPagStateFamilias(prev => ({ ...prev, size: newSize, page: 0 }));
    setPagStateAromas(prev => ({ ...prev, size: newSize, page: 0 }));
  };


  // Carga de opciones de filtros
  useEffect(() => {
    const cargarOpciones = async () => {
      setLoadingOpciones(true);
      try {
        const [aromasRes, familiasRes] = await Promise.all([
          aromasService.getAromasActivos(),
          familiasService.getFamiliasActivas(),
        ]);
        setOpciones({ aromas: aromasRes, familias: familiasRes });
      } catch (error) {
        console.error("Error al cargar opciones de filtros:", error);
      } finally {
        setLoadingOpciones(false);
      }
    };
    cargarOpciones();
  }, []);


  // Carga principal de todos los reportes
  useEffect(() => {
    const cargarReportes = async () => {
      setLoading(true);
      try {
        const promesas = [
          reportesService.getKpisProductos(anio, mes, familiaId, aromaId),
          reportesService.getReportePerformanceProducto(anio, mes, familiaId, aromaId),
          reportesService.getVentasPorProducto(anio, mes, familiaId, aromaId),
          reportesService.getReporteTop10VentasPorFamilia(anio, mes, aromaId),
          reportesService.getReporteTop10VentasPorAroma(anio, mes, familiaId),
        ];

        if (mes) {
          promesas.push(reportesService.getReporteTendenciaDiaria(anio, mes, familiaId, aromaId));
        }

        const [
          kpisData,
          performanceData,
          productoData,
          familiaData,
          aromaData,
          tendenciaData
        ] = await Promise.all(promesas);

        setKpis(kpisData);
        setChartData({
          ventasPorProducto: productoData,
          ventasPorFamilia: familiaData,
          ventasPorAroma: aromaData,
          performance: performanceData,
          tendenciaDiaria: tendenciaData || null
        });

      } catch (error) {
        console.error("Error al cargar reportes de productos:", error);
        setKpis(null);
        setChartData({
          ventasPorProducto: null, ventasPorFamilia: null, ventasPorAroma: null,
          tendenciaDiaria: null, performance: null
        });
      } finally {
        setLoading(false);
      }
    };

    cargarReportes();
  }, [anio, mes, familiaId, aromaId]);

  // ---  useEffect para la TABLA PAGINADA ---
  useEffect(() => {
    const cargarProductosPaginados = async () => {
      setLoadingTable(true);
      try {
        const data = await reportesService.getVentasPorProductosPaginados(
          paginationState.page,
          paginationState.size,
          null,
          anio,
          mes,
          aromaId,
          familiaId
        );
        setPaginatedProducts(data);
      } catch (error) {
            console.error("Error cargando productos paginados:", error);
            setPaginatedProducts({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: paginationState.size }); 
        } finally {
            setLoadingTable(false);
        }
    };

    if (activeSecondaryTab === 'productos') {
        cargarProductosPaginados();
    }
  }, [
      paginationState, 
      activeSecondaryTab,
      anio, 
      mes, 
      familiaId, 
      aromaId 
  ]);


  // useEffect para cargar FAMILIAS paginadas 
  useEffect(() => {
    const cargarFamiliasPaginadas = async () => {
      setLoadingTableFamilias(true);
      try {
        // Nota: Pasamos aromaId para filtrar, pero no familiaId
        const data = await reportesService.getVentasPorFamiliaPaginados(
          pagStateFamilias.page,
          pagStateFamilias.size,
          null, // sortBy
          anio,
          mes,
          aromaId // Filtramos familias por el aroma seleccionado si existe
        );
        setPaginatedFamilias(data);
      } catch (error) {
        console.error("Error cargando familias paginadas:", error);
      } finally {
        setLoadingTableFamilias(false);
      }
    };

    if (activeSecondaryTab === 'categorias') {
      cargarFamiliasPaginadas();
    }
  }, [pagStateFamilias, activeSecondaryTab, anio, mes, aromaId]);

  // useEffect para cargar AROMAS paginados
  useEffect(() => {
    const cargarAromasPaginados = async () => {
      setLoadingTableAromas(true);
      try {
        const data = await reportesService.getVentasPorAromaPaginados(
          pagStateAromas.page,
          pagStateAromas.size,
          null, // sortBy
          anio,
          mes,
          familiaId // Filtramos aromas por la familia seleccionada si existe
        );
        setPaginatedAromas(data);
      } catch (error) {
        console.error("Error cargando aromas paginados:", error);
      } finally {
        setLoadingTableAromas(false);
      }
    };

    if (activeSecondaryTab === 'categorias') {
      cargarAromasPaginados();
    }
  }, [pagStateAromas, activeSecondaryTab, anio, mes, familiaId]);


  // Cuando los filtros globales cambian, volvemos a la página 0
  useEffect(() => {
    setPaginationState(prev => ({ ...prev, page: 0 }));
    setPagStateFamilias(prev => ({ ...prev, page: 0 })); 
    setPagStateAromas(prev => ({ ...prev, page: 0 }));
  }, [filters]);



  const getPeriodoSubtitle = (includeTodos = true) => {
    const mesTexto = filters.mes ? mesesNombres[filters.mes] : (includeTodos ? 'Todos los meses' : '');
    return `Año: ${filters.anio}${mesTexto ? ` | Mes: ${mesTexto}` : ''}`;
  };
  const getProductosSubtitle = () => {
    if (loadingOpciones) {
      return `Año: ${filters.anio} | Cargando opciones...`;
    }
    let parts = [getPeriodoSubtitle(false)];
    if (filters.familiaId) {
      const familiaNombre = opciones.familias.find(f => f.familiaId == filters.familiaId)?.nombre || '';
      if (familiaNombre) parts.push(`Familia: ${familiaNombre}`);
    }
    if (filters.aromaId) {
      const aromaNombre = opciones.aromas.find(a => a.aromaId == filters.aromaId)?.nombre || '';
      if (aromaNombre) parts.push(`Aroma: ${aromaNombre}`);
    }
    if (parts.length === 1 && !filters.mes) parts[1] = 'Todos los meses';
    return parts.join(' | ');
  };
  const getTopAromaFamiliaSubtitle = (esAroma) => {
    if (loadingOpciones) {
      return `Año: ${filters.anio} | Cargando opciones...`;
    }
    let parts = [getPeriodoSubtitle(false)];
    if (esAroma) {
      if (filters.familiaId) {
        const familiaNombre = opciones.familias.find(f => f.familiaId == filters.familiaId)?.nombre || '';
        if (familiaNombre) parts.push(`Familia: ${familiaNombre}`);
      }
    } else {
      if (filters.aromaId) {
        const aromaNombre = opciones.aromas.find(a => a.aromaId == filters.aromaId)?.nombre || '';
        if (aromaNombre) parts.push(`Aroma: ${aromaNombre}`);
      }
    }
    if (parts.length === 1 && !filters.mes) parts[1] = 'Todos los meses';
    return parts.join(' | ');
  }



  // --- Handlers para Paginación de Productos ---
  const handlePageChange = (page) => setPaginationState(prev => ({ ...prev, page: page - 1 }));
  const handlePageSizeChange = (selected) => setPaginationState(prev => ({ ...prev, size: selected.value, page: 0 }));

  // Handlers de Cambio de Página (Solo cambian la página, el size viene del global)
  const handlePageChangeFamilias = (page) => setPagStateFamilias(prev => ({ ...prev, page: page - 1 }));
  const handlePageChangeAromas = (page) => setPagStateAromas(prev => ({ ...prev, page: page - 1 }));

  // --- Columnas para la Tabla Paginada ---
  const productosColumns = [
    {
      title: "Top",
      key: "index",
      render: (text, record, index) => {
        // Calcular el número correcto basado en la página actual
        return (paginationState.page * paginationState.size) + index + 1;
      },
      width: '10%',
    },
    {
      title: "Producto",
      dataIndex: "nombreProducto",
      key: "nombreProducto",
      sorter: (a, b) => a.nombreProducto.localeCompare(b.nombreProducto),
    },
    {
      title: "Cantidad Vendida",
      dataIndex: "cantidadVendida",
      key: "cantidadVendida",
      sorter: (a, b) => a.cantidadVendida - b.cantidadVendida,
      render: (text) => <strong>{text}</strong>,
      width: '25%',
      align: 'center'
    }
  ];

// --- Columnas para Tabla Familias ---
  const familiasColumns = [
    {
      title: "Top",
      key: "index",
      render: (text, record, index) => (pagStateFamilias.page * pagStateFamilias.size) + index + 1,
      width: '15%',
    },
    {
      title: "Nombre",
      dataIndex: "nombre", // Ajustado a lo que recibe del backend
      key: "nombre",
    },
    {
      title: "Cantidad Vendida",
      dataIndex: "cantidad", // Ajustado a lo que recibe del backend
      key: "cantidad",
      render: (text) => <strong>{text}</strong>,
      align: 'center'
    }
  ];

  // --- Columnas para Tabla Aromas ---
  const aromasColumns = [
    {
      title: "Top",
      key: "index",
      render: (text, record, index) => (pagStateAromas.page * pagStateAromas.size) + index + 1,
      width: '15%',
    },
    {
      title: "Nombre",
      dataIndex: "nombre", // Ajustado
      key: "nombre",
    },
    {
      title: "Cantidad Vendida",
      dataIndex: "cantidad", // Ajustado
      key: "cantidad",
      render: (text) => <strong>{text}</strong>,
      align: 'center'
    }
  ];


  // --- OPCIONES DE GRÁFICOS  ---
  const familiaChartCustomOptions = {
    ...horizontalBarOptions('Top 10 Ventas por Familia',
      chartData.ventasPorFamilia?.categories || [],
      anio, mes
    ),
    colors: ['#008FFB'],
    xaxis: {
      categories:chartData.ventasPorFamilia?.categories,
      title: { text: 'Cantidad Vendida' }
    },
    yaxis: {title: { text: 'Familia' },},
  };
  const aromaChartCustomOptions = {
    ...horizontalBarOptions('Top 10 Ventas por Aroma',
      chartData.ventasPorAroma?.categories || [],
      anio, mes
    ),
    colors: ['#00E396'],
    xaxis: {
      categories:chartData.ventasPorAroma?.categories,
      title: { text: 'Cantidad Vendida' }
    },
    yaxis: {title: { text: 'Aroma' },},
  };

  const topProductosChartCustomOptions = {
    ...horizontalBarOptions('Top 10 Productos',
      chartData.ventasPorProducto?.categories?.slice(0, 10) || [],
      anio, mes
    ),
    colors: ['#FEB019'],
    yaxis: {title: { text: 'Producto' },},
    xaxis: {
      categories: chartData.ventasPorProducto?.categories?.slice(0, 10) || [],
      title: { text: 'Cantidad Vendida' }
    },
  };

  const topProductosSeries = [{
    name: chartData.ventasPorProducto?.series[0]?.name || 'Cantidad Vendida',
    data: chartData.ventasPorProducto?.series[0]?.data?.slice(0, 10) || []
  }];

  const tendenciaOptions = getLineChartOptions(
    'Tendencia de Ventas Diarias',
    chartData.tendenciaDiaria?.categories || [],
    anio, // Pasamos el año
    mes   // Pasamos el mes
  );

  // Calculamos el volumen máximo de los datos (o 0 si no hay)
  const maxVolumenData = chartData.performance 
    ? Math.max(...chartData.performance.map(p => p.volumen)) 
    : 0;

  // Definimos un "techo" para el gráfico
  const maxEjeX = maxVolumenData > 0 ? maxVolumenData + 1 : 10; 
  // Pasamos este valor a la función de opciones
  const performanceOptions = {
    ...getScatterChartOptions('Rentabilidad vs. Volumen', anio, mes, maxEjeX),
    colors: ['#00E396'],
  };

  const performanceSeries = [{
    name: 'Productos',
    data: (chartData.performance || []).map(p => ({
      x: p.volumen,
      y: p.rentabilidad,
      name: p.nombreProducto
    }))
  }];


  // Helper para renderizar una Tarjeta Unificada (Gráfico + Tabla)
  const renderCombinedCard = (
    chartTitle, chartSubtitle, chartOptions, chartSeries, 
    tableTitle, tableLoading, tableData, tableColumns, handlePageChange, entityName
  ) => (
    <div className="card h-100">
      
      {/* SECCIÓN SUPERIOR: GRÁFICO */}
      <div className="card-header border-bottom-0">
        <h5 className="card-title mb-0">{chartTitle}</h5>
        <small className="text-muted">{chartSubtitle}</small>
      </div>
      <div className="card-body pb-0"> 
        <ChartRenderer loading={loading} options={chartOptions} series={chartSeries} type="bar" height={350} />
      </div>

      <hr className="my-0" style={{ opacity: 0.1 }} />

      {/* SECCIÓN INFERIOR: TABLA (Cabecera simplificada) */}
      <div className="card-header bg-transparent border-bottom-0 pt-3 pb-2">
         <h6 className="card-title mb-0 fs-6">{tableTitle}</h6>
      </div>

      <div className="card-body pt-0">
        {tableLoading ? (
             <div className="text-center p-3"><div className="spinner-border spinner-border-sm" role="status"/></div>
        ) : (
            <>
                <div className="table-responsive">
                    <Table
                        columns={tableColumns}
                        dataSource={tableData.content || []}
                        pagination={{
                            current: tableData.pageNumber + 1,
                            pageSize: tableData.pageSize, // Usa el tamaño que viene del estado
                            total: tableData.totalElements,
                            onChange: handlePageChange,
                            size: "small",
                            simple: true 
                        }}
                        rowKey="nombre" 
                    />
                </div>
                <div className="pagination-info mt-2 text-center text-muted" style={{ fontSize: '12px' }}>
                    <span>
                        Mostrando {tableData.content?.length || 0} de {tableData.totalElements || 0} {entityName} (Página {tableData.pageNumber + 1} de {tableData.totalPages || 0})
                    </span>
                </div>
            </>
        )}
      </div>
    </div>
  );


  return (
    <>
      {/* --- FILA 1: KPIs --- */}
      <div className="row mt-4">
        <KpiCard title="Producto Estrella" value={kpis?.productoEstrella || 'N/A'} loading={loading} color="text-success" subtitle={getProductosSubtitle()} />
        <KpiCard title="Producto Menos Vendido" value={kpis?.productoMenosVendido || 'N/A'} loading={loading} color="text-danger" subtitle={getProductosSubtitle()} />
        <KpiCard title="Aroma Más Popular" value={kpis?.aromaMasPopular || 'N/A'} loading={loading} subtitle={getProductosSubtitle()} />
        <KpiCard title="Familia Más Popular" value={kpis?.familiaMasPopular || 'N/A'} loading={loading} subtitle={getProductosSubtitle()} />
      </div>

      {/* --- FILA 2: GRÁFICOS "HERO" --- */}
      <div className="row mt-4">
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Rentabilidad vs. Volumen</h5>
              <small className="text-muted">{getProductosSubtitle()}</small>
            </div>
            <div className="card-body">
              <ChartRenderer loading={loading} options={performanceOptions} series={performanceSeries} type="scatter" height={350} />
            </div>
          </div>
        </div>
        <div className="col-md-6 mb-4">
          <div className="card h-100">
            <div className="card-header">
              <h5 className="card-title mb-0">Tendencia de Ventas Diarias</h5>
              <small className="text-muted">{getProductosSubtitle()}</small>
            </div>
            <div className="card-body">
              {!filters.mes ? (
                // Añadimos un min-height para que el card no colapse
                <div className="text-center d-flex flex-column justify-content-center" style={{ minHeight: '350px' }}>
                  <i className="bi bi-info-circle fs-3 text-primary"></i>
                  <p className="mt-2 text-muted">Seleccione un mes en los filtros para ver la tendencia diaria.</p>
                </div>
              ) : (
                <ChartRenderer loading={loading} options={tendenciaOptions} series={chartData.tendenciaDiaria?.series || []} type="line" height={350} />
              )}
            </div>
          </div>
        </div>
      </div>

      {/* --- FILA 3: SUB-NAVEGACIÓN --- */}
      <div className="row mt-4 align-items-center">
        
        {/* Columna Izquierda (Espacio vacío para balancear) */}
        <div className="col-md-4"></div>

        {/*  Columna Central (Botones Centrados) */}
        <div className="col-md-4 text-center">
            <div className="btn-group" role="group">
                <button
                    type="button"
                    className={`btn ${activeSecondaryTab === 'categorias' ? 'btn-primary' : 'btn-outline-primary'}`}
                    onClick={() => setActiveSecondaryTab('categorias')}
                >
                    📊 Análisis Categórico
                </button>
                <button
                    type="button"
                    className={`btn ${activeSecondaryTab === 'productos' ? 'btn-primary' : 'btn-outline-primary'}`}
                    onClick={() => setActiveSecondaryTab('productos')}
                >
                    📦 Análisis de Productos
                </button>
            </div>
        </div>

        {/* Columna Derecha (Selector alineado a la derecha) */}
        <div className="col-md-4">
            {activeSecondaryTab === 'categorias' && (
                <div className="d-flex align-items-center justify-content-end">
                    <span className="me-2 text-muted small">Mostrar:</span>
                    <div className="form-sort me-3">
                        <Sliders className="info-img" />
                        <Select
                            className="select"
                            options={pageSizeOptions}
                            value={pageSizeOptions.find(opt => opt.value === globalPageSize)}
                            onChange={handleGlobalSizeChange}
                            menuPlacement="auto"
                        />
                    </div>
                </div>
            )}
        </div>
      </div>

      {activeSecondaryTab === 'categorias' && (
        <div className="row mt-4">
          
          {/* --- COLUMNA IZQUIERDA: FAMILIAS --- */}
          <div className="col-md-6 mb-4">
            {renderCombinedCard(
              "Top 10 Ventas por Familia",
              getTopAromaFamiliaSubtitle(false),
              familiaChartCustomOptions,
              chartData.ventasPorFamilia?.series,
              "Detalle de Familias",
              loadingTableFamilias,
              paginatedFamilias,
              familiasColumns,
              handlePageChangeFamilias,
              "familias"
            )}
          </div>

          {/* --- COLUMNA DERECHA: AROMAS --- */}
          <div className="col-md-6 mb-4">
            {renderCombinedCard(
              "Top 10 Ventas por Aroma",
              getTopAromaFamiliaSubtitle(true),
              aromaChartCustomOptions,
              chartData.ventasPorAroma?.series,
              "Detalle de Aromas",
              loadingTableAromas,
              paginatedAromas,
              aromasColumns,
              handlePageChangeAromas,
              "aromas"
            )}
          </div>
        </div>
      )}

      {activeSecondaryTab === 'productos' && (
        <div className="row mt-4">
          <div className="col-12 mb-4">
            <div className="card">
              <div className="card-header">
                <h5 className="card-title mb-0">Top 10 Productos Más Vendidos</h5>
                <small className="text-muted">{getProductosSubtitle()}</small>
              </div>
              <div className="card-body">
                <ChartRenderer loading={loading} options={topProductosChartCustomOptions} series={topProductosSeries} type="bar" height={350} />
              </div>
            </div>
          </div>
          <div className="col-12 mb-4">
            <div className="card">
              <div className="card-header">
                <h5 className="card-title mb-0">Listado Completo de Productos Vendidos</h5>
                <small className="text-muted">{getProductosSubtitle()}</small>
              </div>
              <div className="card-body">

                <div className="table-top">
                  <div className="search-set">
                  </div>
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

                {/* La Tabla Paginada */}
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
                        columns={productosColumns}
                        dataSource={paginatedProducts.content || []}
                        pagination={{
                          current: paginatedProducts.pageNumber + 1, // 1-based
                          pageSize: paginatedProducts.pageSize,
                          total: paginatedProducts.totalElements,
                          onChange: handlePageChange,
                        }}
                        rowKey="nombreProducto" // Usar un campo único
                      />

                      <div className="pagination-info mt-3 text-center">
                        <span>
                          Mostrando {paginatedProducts.content?.length || 0} de {paginatedProducts.totalElements || 0} productos
                          (Página {paginatedProducts.pageNumber + 1} de {paginatedProducts.totalPages || 0})
                        </span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};


AnalisisProductosTab.propTypes = {
  filters: PropTypes.shape({
    anio: PropTypes.string.isRequired,
    mes: PropTypes.string.isRequired,
    familiaId: PropTypes.string.isRequired,
    aromaId: PropTypes.string.isRequired,
  }).isRequired,
};

export default AnalisisProductosTab;