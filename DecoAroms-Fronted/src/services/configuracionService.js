import apiClient from '../apiClient';

const URL_LOCAL = '/api/configuracion';

/**
 * Obtiene el valor de una configuración por su clave.
 * Si la clave no se encuentra (error 404), devuelve null en lugar de lanzar un error.
 * @param {string} clave La clave de la configuración a buscar.
 * @returns {Promise<string|null>} El valor de la configuración o null si no existe.
 */
const getConfiguracion = async (clave) => {
  try {
    const response = await apiClient.get(`${URL_LOCAL}/${clave}`);
    return response.data;
  } catch (error) {
    // Si el error es un 404 (Not Found), es un caso esperado. Devolvemos null.
    if (error.response && error.response.status === 404) {
      return null;
    }
    console.error(`Error al obtener la configuración '${clave}':`, error);
    throw error;
  }
};

/**
 * Actualiza el valor de la meta mensual.
 * @param {number | string} nuevoValor El nuevo monto para la meta.
 * @returns {Promise<any>} La respuesta del servidor.
 */
const actualizarMetaMensual = async (nuevoValor) => {
  try {
    const payload = { valor: String(nuevoValor) }; // El backend espera un objeto con la clave "valor"
    const response = await apiClient.put(`${URL_LOCAL}/meta-mensual`, payload);
    return response.data;
  } catch (error) {
    console.error('Error al actualizar la meta mensual:', error);
    throw error;
  }
};

/**
 * Actualiza la dirección IP de la impresora.
 * @param {string} nuevaIp La nueva dirección IP.
 * @returns {Promise<any>} La respuesta del servidor.
 */
const actualizarIpImpresora = async (nuevaIp) => {
  try {
    const payload = { valor: nuevaIp };
    const response = await apiClient.put(`${URL_LOCAL}/ip-impresora`, payload);
    return response.data;
  } catch (error) {
    console.error('Error al actualizar la IP de la impresora:', error);
    throw error;
  }
};


const configuracionService = {
  getConfiguracion,
  actualizarMetaMensual,
  actualizarIpImpresora,
};

export default configuracionService;