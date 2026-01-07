import httpClient from '../apiClient';

const URL_LOCAL = '/api/aromas';


// Petición GET de todos los aromas segun filtros paginación
// Los filtros pueden ser nombre, isDeleted (true/false)
export const getAromasByFiltrosPaginados = async (page = 0, size = 10, sortBy = 'nombre', isDeleted, nombre) => {
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
    console.error('Error al obtener aromas paginados:', error);
    throw error;
  }
};



// Petición GET de todos los aromas
export const getAromas = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener aromas:', error);
    throw error;
  }
};

// Petición GET de todos los aromas activos
export const getAromasActivos = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/activos`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener aromas activos:', error);
    throw error;
  }
};


// Petición PUT para desactivar un aroma (isDeleted true)
export const desactivarAroma = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      deleted: true
    });
    return response.data;
  } catch (error) {
    console.error('Error al desactivar el aroma:', error);
    throw error;
  }
};


// Petición PUT para activar un aroma (isDeleted false)
export const activarAroma = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      deleted: false
    });
    return response.data;
  } catch (error) {
    console.error('Error al desactivar el aroma:', error);
    throw error;
  }
};

// Petición DELETE para eliminar un aroma
export const eliminarAroma = async (id) => {
  try {
    const response = await httpClient.delete(`${URL_LOCAL}/delete/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error al eliminar el aroma:', error);
    throw error;
  }
};

// Petición POST para crear un nuevo aroma
export const crearAroma = async (nombre) => { 
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, {
      nombre
    });
    return response.data;
  } catch (error) {
    console.error('Error al crear el aroma:', error);
    throw error;
  }
};

// Petición PUT para actualizar un aroma existente
export const actualizarAroma = async (id, nombre) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/update/${id}`, {
      nombre
    });
    return response.data;
  } catch (error) {
    console.error('Error al actualizar el aroma:', error);
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