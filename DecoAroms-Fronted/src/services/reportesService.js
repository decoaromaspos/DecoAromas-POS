import httpClient from '../apiClient';

const URL_BASE = '/api/reportes';


/**
 * Obtiene el total de ventas por mes para un año.
 * @param {number} anio - El año a consultar.
 * @param {string} [tipoVenta] - 'TIENDA' u 'ONLINE'.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getVentasAnualesBarras = async (anio, tipoVenta, tipoCliente) => {
  try {
    const params = { anio, tipoVenta, tipoCliente };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/ventas-anuales/barras`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventas anuales:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de ventas (tienda vs online).
 * @param {number} anio - El año a consultar.
 * @param {number} mes - El mes a consultar.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getVentasAnualesTorta = async (anio, mes, tipoCliente) => {
  try {
    const params = { anio, mes, tipoCliente };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/ventas-anuales/torta/tipo-venta`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener distribución de ventas segun tipo de venta anuales:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de ventas (detalle vs mayorista).
 * @param {number} anio - El año a consultar.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getVentasAnualesTipoClienteTorta = async (anio) => {
  try {
    const params = { anio };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/ventas-anuales/torta/tipo-cliente`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener distribución de ventas segun tipo de cliente anuales:', error);
    throw error;
  }
};

/**
 * Obtiene el ranking de productos vendidos.
 * @param {number} [anio] - Año opcional.
 * @param {number} [mes] - Mes opcional.
 * @param {number} [aromaId] - aroma id opcional.
 * @param {number} [familiaId] - familia id opcional.
 * @returns {Promise<object>} Datos para el gráfico y la tabla.
 */
export const getVentasPorProducto = async (anio, mes, aromaId, familiaId) => {
  try {
    const params = { anio, mes, aromaId, familiaId };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/ventas-por-producto`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventas por producto:', error);
    throw error;
  }
};

/**
 * Obtiene los datos de utilidad (ingresos, costos, utilidad) para un año.
 * @param {number} anio - El año a consultar.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras/líneas.
 */
export const getReporteUtilidadAnual = async (anio, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/utilidad-anual`, { params: { anio, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener reporte de utilidad:', error);
    throw error;
  }
};

/**
 * Obtiene la comparacion de ventas por mes para un año según TIENDA vs ONLINE vs TOTAL.
 * @param {number} anio - El año a consultar.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getVentasAnualesComparacionBarras = async (anio, tipoCliente) => {
  try {
    const params = { anio, tipoCliente };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/ventas-anuales-comparacion/barras`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener comparacion de ventas anuales:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de ventas en tienda por tipo de cliente.
 * @param {number} [anio] - Año opcional para filtrar.
 * @param {number} [mes] - Mes opcional para filtrar.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getDistribucionTiendaPorTipoCliente = async (anio, mes) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/distribucion-tienda-tipo-cliente`, { params: { anio, mes } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener distribución por tipo de cliente:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de ventas online por tipo de cliente.
 * @param {number} [anio] - Año opcional para filtrar.
 * @param {number} [mes] - Mes opcional para filtrar.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getDistribucionOnlinePorTipoCliente = async (anio, mes) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/distribucion-online-tipo-cliente`, { params: { anio, mes } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener distribución por tipo de cliente:', error);
    throw error;
  }
};

/**
 * Obtiene el análisis de ventas por medio de pago en un rango de fechas.
 * @param {string} [fechaInicio] - Fecha de inicio en formato YYYY-MM-DD.
 * @param {string} [fechaFin] - Fecha de fin en formato YYYY-MM-DD.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getAnalisisMediosDePago = async (fechaInicio, fechaFin, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/analisis-medios-pago`, { params: { fechaInicio, fechaFin, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener análisis de medios de pago:', error);
    throw error;
  }
};

/**
 * Obtiene las ganancias de ventas por día de semana en un rango de fechas.
 * @param {string} [fechaInicio] - Fecha de inicio en formato YYYY-MM-DD.
 * @param {string} [fechaFin] - Fecha de fin en formato YYYY-MM-DD.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getVentasTiendaPorDiaSemana = async (fechaInicio, fechaFin, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-dia-semana`, { params: { fechaInicio, fechaFin, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener análisis de ventas por día de semana:', error);
    throw error;
  }
};

/**
 * Obtiene las ganancias de ventas por hora del día en un rango de fechas.
 * @param {string} [fechaInicio] - Fecha de inicio en formato YYYY-MM-DD.
 * @param {string} [fechaFin] - Fecha de fin en formato YYYY-MM-DD.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getVentasTiendaPorHora = async (fechaInicio, fechaFin, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-hora`, { params: { fechaInicio, fechaFin, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener análisis de ventas por hora:', error);
    throw error;
  }
};

/**
 * Obtiene las ganancias de ventas por vendedor (username) en un rango de fechas.
 * @param {string} [fechaInicio] - Fecha de inicio en formato YYYY-MM-DD.
 * @param {string} [fechaFin] - Fecha de fin en formato YYYY-MM-DD.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getVentasTiendaPorVendedor = async (fechaInicio, fechaFin, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-vendedor`, { params: { fechaInicio, fechaFin, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener análisis de ventas por vendedor:', error);
    throw error;
  }
};

/**
 * Obtiene un analisis de descuentos aplicados en un año
 * @param {number} [anio] - Año opcional para filtrar. Nulo considera año actual.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE'.
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getAnalisisDescuentos = async (anio, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/analisis-descuentos`, { params: { anio, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener el análisis de descuentos:', error);
    throw error;
  }
};

/**
 * Obtiene el ranking de ventas por familia de producto.
 * @param {number} [anio] - Año opcional para filtrar.
 * @param {number} [mes] - Mes opcional para filtrar.
 * @param {number} [aromaId] - Id de aroma opcional para filtrar.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getReporteTop10VentasPorFamilia = async (anio, mes, aromaId) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-familia/top10`, { params: { anio, mes, aromaId } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventas por familia:', error);
    throw error;
  }
};

/**
 * Obtiene el ranking de ventas por aroma.
 * @param {number} [anio] - Año opcional para filtrar.
 * @param {number} [mes] - Mes opcional para filtrar.
 * @param {number} [familiaId] - Id de familia opcional para filtrar.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getReporteTop10VentasPorAroma = async (anio, mes, familiaId) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-aroma/top10`, { params: { anio, mes, familiaId } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventas por aroma:', error);
    throw error;
  }
};

/**
 * Obtiene los descuadres de caja.
 * @param {number} [anio] - Año opcional para filtrar.
 * @param {number} [mes] - Mes opcional para filtrar.
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getReporteDescuadresCaja = async (anio, mes) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/descuadres-caja`, { params: { anio, mes } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener descuadres de caja:', error);
    throw error;
  }
};


export const getKpisVentas = async (fechaInicio, fechaFin, tipoCliente) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/kpis-ventas`, { params: { fechaInicio, fechaFin, tipoCliente } });
    return response.data;
  } catch (error) {
    console.error('Error al obtener kpis de ventas:', error);
    throw error;
  }
};






/**
 * Helper para crear params limpios para las peticiones de productos
 */
const getProductParams = (anio, mes, familiaId, aromaId) => {
  const params = { anio, mes, familiaId, aromaId };
  // Elimina nulos, undefined o strings vacíos
  Object.keys(params).forEach(key =>
    (params[key] == null || params[key] === '') && delete params[key]
  );
  return params;
};

/**
 * Obtiene los KPIs para la pestaña de análisis de productos.
 */
export const getKpisProductos = async (anio, mes, familiaId, aromaId) => {
  try {
    const params = getProductParams(anio, mes, familiaId, aromaId);
    const response = await httpClient.get(`${URL_BASE}/kpis-productos`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener KPIs de productos:', error);
    throw error;
  }
};

/**
 * Obtiene el gráfico de performance (Rentabilidad vs Volumen).
 */
export const getReportePerformanceProducto = async (anio, mes, familiaId, aromaId) => {
  try {
    const params = getProductParams(anio, mes, familiaId, aromaId);
    // Este endpoint devuelve un array plano, no un ChartDataDTO
    const response = await httpClient.get(`${URL_BASE}/performance-producto`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener performance de productos:', error);
    throw error;
  }
};

/**
 * Obtiene el gráfico de tendencia de ventas diarias (para un mes específico).
 */
export const getReporteTendenciaDiaria = async (anio, mes, familiaId, aromaId) => {
  try {
    // Mes es obligatorio para esta, pero el helper lo maneja
    const params = getProductParams(anio, mes, familiaId, aromaId);
    const response = await httpClient.get(`${URL_BASE}/tendencia-diaria`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener tendencia diaria:', error);
    throw error;
  }
};



/**
 * Obtiene el ranking de aromas vendidos con paginacion.
 * @param {number} [page=0] - Número de página (0-indexed).
 * @param {number} [size=10] - Tamaño de página.
 * @param {string} [sortBy=null] - Campo para ordenar (ej. 'p.nombre').
 * @param {number} [anio] - Año opcional.
 * @param {number} [mes] - Mes opcional.
 * @param {number} [familiaId] - familia id opcional.
 * @returns {Promise<object>} Datos para el gráfico y la tabla.
 */
export const getVentasPorAromaPaginados = async (
  page = 0, size = 10, sortBy = null, anio, mes, familiaId
) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-aroma/paginas`, {
      params: {
        page,
        size,
        sortBy,
        anio,
        mes,
        familiaId
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventar por aroma paginados:', error);
    throw error;
  }
};

/**
 * Obtiene el ranking de familias vendidos con paginacion.
 * @param {number} [page=0] - Número de página (0-indexed).
 * @param {number} [size=10] - Tamaño de página.
 * @param {string} [sortBy=null] - Campo para ordenar (ej. 'p.nombre').
 * @param {number} [anio] - Año opcional.
 * @param {number} [mes] - Mes opcional.
 * @param {number} [aromaId] - aroma id opcional.
 * @returns {Promise<object>} Datos para el gráfico y la tabla.
 */
export const getVentasPorFamiliaPaginados = async (
  page = 0, size = 10, sortBy = null, anio, mes, aromaId
) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-familia/paginas`, {
      params: {
        page,
        size,
        sortBy,
        anio,
        mes,
        aromaId
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventar por familia paginados:', error);
    throw error;
  }
};

/**
 * Obtiene el ranking de productos vendidos con paginacion.
 * @param {number} [page=0] - Número de página (0-indexed).
 * @param {number} [size=10] - Tamaño de página.
 * @param {string} [sortBy=null] - Campo para ordenar (ej. 'p.nombre').
 * @param {number} [anio] - Año opcional.
 * @param {number} [mes] - Mes opcional.
 * @param {number} [aromaId] - aroma id opcional.
 * @param {number} [familiaId] - familia id opcional.
 * @returns {Promise<object>} Datos para el gráfico y la tabla.
 */
export const getVentasPorProductosPaginados = async (
  page = 0, size = 10, sortBy = null, anio, mes, aromaId, familiaId
) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/ventas-por-producto/paginas`, {
      params: {
        page,
        size,
        sortBy,
        anio,
        mes,
        aromaId,
        familiaId
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventar por productos paginados:', error);
    throw error;
  }
};

/**
 * Obtiene cajas descuadradas con paginacion.
 * @param {number} [page=0] - Número de página (0-indexed).
 * @param {number} [size=10] - Tamaño de página.
 * @param {string} [sortBy=null] - Campo para ordenar.
 * @param {number} [anio] - Año opcional.
 * @param {number} [mes] - Mes opcional.
 * @returns {Promise<object>} Datos para el gráfico y la tabla.
 */
export const getCajasDescuadradasPaginacion = async (
  page = 0, size = 10, sortBy = null, anio, mes
) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/descuadres-caja/paginas`, {
      params: {
        page,
        size,
        sortBy,
        anio,
        mes
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener cajas descuadradas paginadas:', error);
    throw error;
  }
};


/**
 * Obtiene el reporte de descuadres agrupados por usuario.
 * @param {string} fechaInicio (YYYY-MM-DD)
 * @param {string} fechaFin (YYYY-MM-DD)
 * @returns {Promise<object>}
 */
export const getReporteDescuadresPorUsuario = async (fechaInicio, fechaFin) => {
  try {
    const params = { fechaInicio, fechaFin };
    // Limpia params nulos o vacíos
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/descuadres-por-usuario`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener descuadres por usuario:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de métodos de pago en los cierres de caja.
 * @param {string} fechaInicio (YYYY-MM-DD)
 * @param {string} fechaFin (YYYY-MM-DD)
 * @returns {Promise<object>}
 */
export const getReporteMetodosPagoCierre = async (fechaInicio, fechaFin) => {
  try {
    const params = { fechaInicio, fechaFin };
    // Limpia params nulos o vacíos
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);
    const response = await httpClient.get(`${URL_BASE}/distribucion-metodos-pago-cierre`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener métodos de pago en cierre:', error);
    throw error;
  }
};

/**
 * Obtiene la tendencia de ventas vs. descuadres por año.
 * @param {number} anio
 * @returns {Promise<object>}
 */
export const getReporteTendenciaVentasVsDescuadres = async (anio) => {
  try {
    const params = { anio };
    const response = await httpClient.get(`${URL_BASE}/tendencia-ventas-vs-descuadres`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener tendencia ventas vs descuadres:', error);
    throw error;
  }
};

/**
 * Obtiene los KPIs para la pestaña de Operaciones.
 * @param {number} anio
 * @param {number} mes (puede ser null, 0 o "")
 * @returns {Promise<object>}
 */
export const getOperacionesKpis = async (anio, mes) => {
  try {
    const params = { anio, mes };
    if (!mes || mes === "0" || mes === "") {
      delete params.mes;
    }
    const response = await httpClient.get(`${URL_BASE}/kpis-operaciones`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener KPIs de operaciones:', error);
    throw error;
  }
};




/**
 * Obtiene los KPIs para la Vista General.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, familiaId, aromaId }
 * @returns {Promise<object>} KpiGeneralesDTO
 */
export const getKpisGenerales = async (filtros) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/kpis`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener KPIs generales:', error);
    throw error;
  }
};

/**
 * Obtiene los datos para el gráfico de Tendencia de Ventas Diarias.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, familiaId, aromaId }
 * @returns {Promise<object>} ChartDataDTO
 */
export const getTendenciaVentas = async (filtros) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/tendencia-ventas`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener tendencia de ventas:', error);
    throw error;
  }
};

/**
 * Obtiene los datos para el gráfico de Tendencia de Ventas MENSUAL.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, familiaId, aromaId }
 * @returns {Promise<object>} ChartDataDTO
 */
export const getTendenciaVentasMensual = async (filtros) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/tendencia-ventas-mensual`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener tendencia de ventas mensual:', error);
    throw error;
  }
};

/**
 * Obtiene los datos para el gráfico Top N Familias.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, aromaId }
 * @param {number} topN , limite de elementos a mostrar
 * @returns {Promise<object>} ChartDataDTO
 */
export const getTopFamilias = async (filtros, topN) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/top-familias/${topN}`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener top 5 familias:', error);
    throw error;
  }
};

/**
 * Obtiene los datos para el gráfico Top 5 Aromas.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, familiaId }
 * @param {number} topN , limite de elementos a mostrar
 * @returns {Promise<object>} ChartDataDTO
 */
export const getTopAromas = async (filtros, topN) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/top-aromas/${topN}`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener top 5 aromas:', error);
    throw error;
  }
};

/**
 * Obtiene los datos para el Mapa de Calor de Ventas.
 * @param {object} filtros - { fechaInicio, fechaFin, tipoCliente, familiaId, aromaId }
 * @returns {Promise<object>} ChartDataDTO
 */
export const getMapaCalorVentas = async (filtros) => {
  try {
    const response = await httpClient.get(`${URL_BASE}/general/mapa-calor-ventas`, { params: filtros });
    return response.data;
  } catch (error) {
    console.error('Error al obtener mapa de calor de ventas:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de clientes por tipo (Mayorista/Detalle).
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getClientesDistribucionTipo = async () => {
  try {
    const response = await httpClient.get(`${URL_BASE}/clientes/distribucion/tipo`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener distribución de clientes por tipo:', error);
    throw error;
  }
};

/**
 * Obtiene el Top N clientes por total de gasto, con filtros opcionales.
 * @param {number} topN - La cantidad de clientes a mostrar.
 * @param {string} [anio] - El año a filtrar (opcional).
 * @param {string} [mes] - El mes a filtrar (opcional).
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE' (opcional).
 * @returns {Promise<object>} Datos para el gráfico de barras.
 */
export const getClientesTopGasto = async (topN, anio, mes, tipoCliente) => {
  try {
    const params = {
      topN,
      anio: anio ? parseInt(anio) : undefined,
      mes: mes ? parseInt(mes) : undefined,
      tipoCliente
    };
    // Limpiar parámetros nulos o vacíos
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

    const response = await httpClient.get(`${URL_BASE}/clientes/top-gasto`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener Top Clientes por Gasto:', error);
    throw error;
  }
};

/**
 * Obtiene la distribución de clientes por recencia (activos/inactivos).
 * @param {number} dias - Cantidad de días para considerar inactivo.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE' (opcional).
 * @returns {Promise<object>} Datos para el gráfico de torta.
 */
export const getClientesRecencia = async (dias, tipoCliente) => {
  try {
    const params = {
      dias: dias ? parseInt(dias) : undefined,
      tipoCliente
    };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

    const response = await httpClient.get(`${URL_BASE}/clientes/recencia`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener Recencia de Clientes:', error);
    throw error;
  }
};

/**
 * Obtiene los KPIs de la base de clientes.
 * @returns {Promise<object>} Datos de los KPIs.
 */
export const getClientesKpis = async () => {
  try {
    const response = await httpClient.get(`${URL_BASE}/clientes/kpis`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener KPIs de Clientes:', error);
    throw error;
  }
};

/**
 * Obtiene la lista detallada de clientes que no han comprado en 'dias' días.
 * @param {number} dias - Cantidad de días para considerar inactivo.
 * @param {string} [tipoCliente] - 'MAYORISTA' o 'DETALLE' (opcional).
 * @returns {Promise<Array>} Lista de ClienteInactivoDTO.
 */
export const getClientesInactivosDetalle = async (dias, tipoCliente) => {
  try {
    const params = {
      dias: dias ? parseInt(dias) : undefined,
      tipoCliente
    };
    Object.keys(params).forEach(key => (params[key] == null || params[key] === '') && delete params[key]);

    const response = await httpClient.get(`${URL_BASE}/clientes/recencia/detalle`, { params });
    return response.data;
  } catch (error) {
    console.error('Error al obtener Detalle de Clientes Inactivos:', error);
    throw error;
  }
};