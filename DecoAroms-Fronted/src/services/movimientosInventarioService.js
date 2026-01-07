import httpClient from '../apiClient';

const URL_LOCAL = '/api/movimientos/inventario';


// Petición GET de movimientos con paginación
export const getMovimientosPaginados = async (page = 0, size = 10, sortBy = 'fecha') => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/paginas`, {
      params: {
        page,
        size,
        sortBy
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener movimientos paginados:', error);
    throw error;
  }
};

// Petición GET de movimientos segun dos fechas con paginación
export const getMovimientosByFechasPaginados = async (page = 0, size = 10, sortBy = 'fecha', fechaInicio, fechaFin) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/fecha/paginas`, {
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
    console.error('Error al obtener movimientos segun rango de fechas paginados:', error);
    throw error;
  }
};

// Petición GET de movimientos segun usuario id con paginación
export const getMovimientosByUsuarioIdPaginados = async (page = 0, size = 10, sortBy = 'fecha', usuarioId) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/usuario/${usuarioId}/paginas`, {
      params: {
        page,
        size,
        sortBy
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener movimientos segun usuario paginados:', error);
    throw error;
  }
};

// Petición GET de movimientos segun usuario id con paginación
// motivo puede ser VENTA, AJUSTE_VENTA, PRODUCCION, NUEVO_STOCK, CORRECCION
export const getMovimientosByMotivoPaginados = async (page = 0, size = 10, sortBy = 'fecha', motivo) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/motivo/${motivo}/paginas`, {
      params: {
        page,
        size,
        sortBy
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener movimientos segun motivo paginados:', error);
    throw error;
  }
};

// Petición GET de movimientos segun tipo con paginación
// tipo puede ser ENTRADA o SALIDA
export const getMovimientosByTipoPaginados = async (page = 0, size = 10, sortBy = 'fecha', tipo) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/tipo/${tipo}/paginas`, {
      params: {
        page,
        size,
        sortBy
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener movimientos segun tipo paginados:', error);
    throw error;
  }
};



// Petición GET de movimientos segun filtros paginación
// tipo puede ser ENTRADA o SALIDA
export const getMovimientosByFiltrosPaginados = async (page = 0, size = 10, sortBy = 'fecha', fechaInicio, fechaFin, tipo, motivo, usuarioId, productoId) => {
  try {

    const filtrosBody = {
      tipo,
      motivo,
      usuarioId,
      productoId
    }

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
    console.error('Error al obtener movimientos segun filtros paginados:', error);
    throw error;
  }
};