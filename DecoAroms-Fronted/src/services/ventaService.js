import httpClient from '../apiClient';

const URL_LOCAL = '/api/ventas';
const URL_PRINT = '/api/print';

// Petición POST para crear una venta
export const crearVenta = async (ventaData) => {
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, ventaData);
    return response.data;
  } catch (error) {
    console.error('Error al crear la venta:', error);
    throw error;
  }
};

// Petición GET para obtener todas las ventas paginadas
export const getVentasByFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'fecha', fechaInicio, fechaFin, tipoCliente, tipoDocumento, medioPago,
  minTotalNeto, maxTotalNeto, usuarioId, clienteId, numeroDocumentoParcial, pendienteAsignacion, productoId
) => {

  // Objeto con filtros para el body
  const filtrosBody = {
    tipoCliente,
    tipoDocumento,
    medioPago,
    minTotalNeto,
    maxTotalNeto,
    usuarioId,
    clienteId,
    numeroDocumentoParcial,
    pendienteAsignacion,
    productoId
  }

  try {
    const response = await httpClient.post(`${URL_LOCAL}/filtros/paginas`, filtrosBody, {
      params: {
        page,
        size,
        sortBy,
        fechaInicio,
        fechaFin
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener ventas paginadas:', error);
    throw error;
  }
};

// Peticion PATCH para actualizar documento de venta
export const actualizarDocumentoVenta = async (id, dataDoc) => {
  try {
    const response = await httpClient.patch(`${URL_LOCAL}/${id}/documento`, dataDoc)
    return response.data;
  } catch (error) {
    console.error('Error al actualizar documento de venta: ', error);
    throw error;
  }
};


// Peticion DELETE para eliminar venta
export const deleteVenta = async (id, usuarioId) => {
  try {
    const response = await httpClient.delete(`${URL_LOCAL}/delete/${id}`, {
      params: { usuarioId }
    }
    )
    return response.data;
  } catch (error) {
    console.error('Error al eliminar venta: ', error);
    throw error;
  }
};

// Petición POST para imprimir comprobante de venta
export const imprimirComprobanteVenta = async (ventaId, printerConfig = null) => {
  try {
    const response = await httpClient.post(
      `${URL_PRINT}/imprimir/${ventaId}`,
      printerConfig // puede ser null o un objeto { ip, port }
    );
    return response.data;
  } catch (error) {
    console.error('Error al imprimir comprobante de venta:', error);
    throw error;
  }
};

// Peticion GET para obtener las ganancias del mes actual
// La respuesta es del tipo: { "gananciaMesActual": 12345.67 }
export const getGananciasDelMesActual = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/ganancias/mes-actual`);
    return response.data.gananciaMesActual;
  } catch (error) {
    console.error('Error al obtener las ganancias del mes actual:', error);
    throw error;
  }
};

// Peticion GET para obtener las ganancias del día actual
// La respuesta es del tipo: { "gananciaDiaActual": 12345.67 }
export const getGananciasDelDiaActual = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/ganancias/mes-dia`);
    return response.data.gananciaDiaActual;
  } catch (error) {
    console.error('Error al obtener las ganancias del día actual:', error);
    throw error;
  }
};

// Petición GET para verificar si un número de docuemnto esta disponible segun respuesta HTTP y mensaje
// NO disponible, retornamos 409 Conflict. SÍ disponible, retornamos 200 OK.
export const checkNumDocDisponible = async (numDoc) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-num-doc`, {
      params: { numDoc }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de número de documento:', error);
    throw error;
  }
};

export const exportarVentasCSV = async (filtros = {}) => {
  try {
    const params = new URLSearchParams();

    // Mapeo de filtros a Query Params
    if (filtros.fechaInicio) params.append('fechaInicio', filtros.fechaInicio);
    if (filtros.fechaFin) params.append('fechaFin', filtros.fechaFin);
    if (filtros.tipoCliente) params.append('tipoCliente', filtros.tipoCliente);
    if (filtros.medioPago) params.append('medioPago', filtros.medioPago);
    if (filtros.tipoDocumento) params.append('tipoDocumento', filtros.tipoDocumento);
    if (filtros.usuarioId) params.append('usuarioId', filtros.usuarioId);
    if (filtros.clienteId) params.append('clienteId', filtros.clienteId);

    const response = await httpClient.get(`${URL_LOCAL}/exportar-csv?${params.toString()}`, {
      responseType: 'blob',
    });

    // Manejo de caracteres especiales con BOM
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Ventas_${new Date().toLocaleDateString()}.csv`);

    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

  } catch (error) {
    console.error('Error al exportar ventas:', error);
    throw error;
  }
};