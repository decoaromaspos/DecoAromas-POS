import httpClient from '../apiClient';

const URL_LOCAL = '/api/cotizaciones';

// Petición POST para crear una cotización
export const crearCotizacion = async (cotizacionData) => {
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, cotizacionData);
    return response.data;
  } catch (error) {
    console.error('Error al crear la cotizacion:', error);
    throw error;
  }
};

// Petición GET de todos las cotizaciones
export const getCotizaciones = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener cotizaciones:', error);
    throw error;
  }
};


// Petición GET para obtener todas las cotizaciones paginadas
export const getCotizacionesByFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'fechaEmision', fechaInicio, fechaFin, tipoCliente, 
  minTotalNeto, maxTotalNeto, usuarioId, clienteId
) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        fechaInicio,
        fechaFin,
        tipoCliente,
        minTotalNeto,
        maxTotalNeto,
        usuarioId,
        clienteId
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener cotizaciones paginadas:', error);
    throw error;
  }
};

// Peticion DELETE para eliminar cotizacion
export const deleteCotizacion = async (id, usuarioId) => {
  try{
    const response = await httpClient.delete(`${URL_LOCAL}/delete/${id}`, {
        params: {usuarioId}
      }
    )
    return response.data;
  } catch (error) {
    console.error('Error al eliminar cotizacion: ', error);
    throw error;
  }
};

// Peticion PATCH para actualizar estado de cotizacion
export const actualizarEstadoCotizacion = async (cotizacionId, estado) => {
    try{
        const response = await httpClient.patch(`${URL_LOCAL}/cambiar-estado`, {cotizacionId, estado});
        return response.data;
    } catch (error) {
        console.error('Error al actualizar estado de cotizacion: ', error);
        throw error;
    }
};