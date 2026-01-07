import httpClient from '../apiClient';

const URL_LOCAL = '/api/familias';


// Petición GET de familias con paginación
// Los filtros pueden ser nombre, isDeleted (true/false)
export const getFamiliasByFiltrosPaginados = async (page = 0, size = 10, sortBy = 'nombre', isDeleted, nombre) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        isDeleted,
        nombre
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener familias paginados:', error);
    throw error;
  }
};


// Petición GET de todos las familias
export const getFamilias = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener familias de productos:', error);
    throw error;
  }
};

// Petición GET de todos las familias activoas
export const getFamiliasActivas = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/activas`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener familias activas de productos:', error);
    throw error;
  }
};

// Petición PUT para desactivar una familia (isDeleted true)
export const desactivarFamilia = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      deleted: true
    });
    return response.data;
  } catch (error) {
    console.error('Error al desactivar la familia:', error);
    throw error;
  }
};


// Petición PUT para activar una familia (isDeleted false)
export const activarFamilia = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      deleted: false
    });
    return response.data;
  } catch (error) {
    console.error('Error al desactivar la familia:', error);
    throw error;
  }
};


// Petición DELETE para eliminar un familia
export const eliminarFamilia = async (id) => {
  try {
    const response = await httpClient.delete(`${URL_LOCAL}/delete/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error al eliminar el familia:', error);
    throw error;
  }
};

// Petición POST para crear un nuevo familia
export const crearFamilia = async (nombre) => { 
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, {
      nombre
    });
    return response.data;
  } catch (error) {
    console.error('Error al crear el familia:', error);
    throw error;
  }
};

// Petición PUT para actualizar un familia existente
export const actualizarFamilia = async (id, nombre) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/update/${id}`, {
      nombre
    });
    return response.data;
  } catch (error) {
    console.error('Error al actualizar el familia:', error);
    throw error;
  }
};

// Petición GET para verificar si un nombre esta disponible segun respuesta HTTP y mensaje
// NO disponible (409 Conflict). SÍ disponible (200 OK)
export const checkNombreDisponible = async (nombre) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-nombre`, {
      params: { nombre }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de nombre:', error);
    throw error;
  }
};