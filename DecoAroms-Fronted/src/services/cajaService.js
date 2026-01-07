import httpClient from '../apiClient';

const URL_LOCAL = '/api/cajas';

// Petición GET de caja abierta
export const getCajaAbierta = async () => {
    try {
        const response = await httpClient.get(`${URL_LOCAL}/abierta`);
        return response.data;
    } catch (error) {
        console.error('Error al obtener la caja abierta:', error);
        throw error;
    }
};


// Petición GET de resumen ganancias actuales de caja abierta
export const getResumenCajaAbierta = async () => {
    try {
        const response = await httpClient.get(`${URL_LOCAL}/abierta/resumen`);
        return response.data;
    } catch (error) {
        console.error('Error al obtener la caja abierta:', error);
        throw error;
    }
};

// Petición GET de resumen ganancias actuales de caja abierta
export const getResumenCajaById = async (cajaId) => {
    try {
        const response = await httpClient.get(`${URL_LOCAL}/${cajaId}/resumen`);
        return response.data;
    } catch (error) {
        console.error('Error al obtener la caja abierta:', error);
        throw error;
    }
};



// Petición POST para abrir caja
export const abrirCaja = async (usuarioId, efectivoApertura) => {
    try {
        const response = await httpClient.post(`${URL_LOCAL}/abrir`, {
            usuarioId,
            efectivoApertura
        });
        return response.data;
    } catch (error) {
        console.error('Error al abrir la caja:', error);
        throw error;
    } 
};

// Petición POST para cerrar caja
export const cerrarCaja = async (efectivoCierre) => {
    try {
        const response = await httpClient.post(`${URL_LOCAL}/cerrar-abierta/efectivo/${efectivoCierre}`);
        return response.data;
    } catch (error) {
        console.error('Error al cerrar la caja:', error);
        throw error;
    }
};


// Petición GET de cajas segun filtros paginación
// estado puede ser ABIERTA o CERRADA
// cuadrada es un booleano
export const getCajasByFiltrosPaginados = async (page = 0, size = 10, sortBy = 'fecha', fechaInicio, fechaFin, estado, cuadrada, usuarioId) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        fechaInicio,
        fechaFin,
        estado,
        cuadrada,
        usuarioId
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener cajas segun filtros paginados:', error);
    throw error;
  }
};