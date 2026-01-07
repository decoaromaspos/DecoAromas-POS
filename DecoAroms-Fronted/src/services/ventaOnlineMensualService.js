import httpClient from '../apiClient';

const URL_BASE = '/api/ventasonline/mensuales';

/**
 * Obtiene las ventas de un año específico.
 * @param {number} anio 
 */
export const getVentasPorAnio = async (anio) => {
    try {
        const response = await httpClient.get(`${URL_BASE}/anio/${anio}`);
        return response.data;
    } catch (error) {
        console.error(`Error al obtener ventas del año ${anio}:`, error);
        throw error;
    }
};

/**
 * Crea un nuevo registro de venta mensual.
 * @param {object} data { anio, mes, totalDetalle, totalMayorista }
 */
export const crearVentaMensual = async (data) => {
    try {
        const response = await httpClient.post(`${URL_BASE}`, data);
        return response.data;
    } catch (error) {
        console.error('Error al crear venta mensual:', error);
        throw error;
    }
};

/**
 * Actualiza una venta mensual existente.
 * @param {object} data { anio, mes, totalDetalle, totalMayorista }
 */
export const actualizarVentaMensual = async (data) => {
    try {
        const response = await httpClient.put(`${URL_BASE}/update`, data);
        return response.data;
    } catch (error) {
        console.error('Error al actualizar venta mensual:', error);
        throw error;
    }
};

/**
 * Elimina un registro por su ID.
 * @param {number} id 
 */
export const eliminarVentaMensual = async (id) => {
    try {
        await httpClient.delete(`${URL_BASE}/delete/${id}`);
    } catch (error) {
        console.error(`Error al eliminar venta mensual ${id}:`, error);
        throw error;
    }
};