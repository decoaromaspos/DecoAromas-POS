import httpClient from '../apiClient';

const URL_LOCAL = '/api/clientes';

// Petición GET de clientes activos
export const getClientesActivos = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/activos`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener los clientes activos:', error);
    throw error;
  }
};

// Peticion GET para obtener cliente segun id
export const getClienteById = async (id) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener el cliente:', error);
    throw error;
  }
};

// Peticion GET para obtener clientes paginados
export const getClientesFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'clienteId', nombreCompletoParcial,
  rutParcial, correoParcial, telefonoParcial, tipo, activo, ciudadParcial
) => {
  // Objeto con los filtros que irá en el body (ClienteFilterDTO)
  const filtrosBody = {
    nombreCompletoParcial: nombreCompletoParcial || null,
    rutParcial: rutParcial || null,
    correoParcial: correoParcial || null,
    telefonoParcial: telefonoParcial || null,
    tipo: tipo || null,
    activo: activo !== undefined ? activo : null,
    ciudadParcial: ciudadParcial || null
  };

  try {
    const response = await httpClient.post(
      `${URL_LOCAL}/filtros/paginas`,
      filtrosBody,
      { params: { page, size, sortBy } }
    );
    return response.data;
  } catch (error) {
    console.error('Error al obtener clientes paginados:', error);
    throw error;
  }
};

// Peticion para desativar un cliente
export const desactivarCliente = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      activo: false
    });
    return response.data;
  } catch (error) {
    console.error('Error al desativar el cliente:', error);
    throw error;
  }
};

// Peticion para activar un cliente
export const activarCliente = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      activo: true
    });
    return response.data;
  } catch (error) {
    console.error('Error al activar el cliente:', error);
    throw error;
  }
};

// Peticion para crear un cliente
export const crearCliente = async (clienteData) => {
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, clienteData);
    return response.data;
  } catch (error) {
    console.error('Error al crear cliente:', error);
    throw error;
  }
};

// Peticion para actualizar cliente
export const actualizarCliente = async (id, clienteData) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/update/${id}`, clienteData);
    return response.data;
  } catch (error) {
    console.error('Error al actualizar cliente:', error);
    throw error;
  }
};


// Petición GET para verificar si un correo esta disponible segun respuesta HTTP y mensaje
// NO disponible, retornamos 409 Conflict. SÍ disponible, retornamos 200 OK.
export const checkCorreoDisponible = async (correo) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-correo`, {
      params: { correo }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de correo:', error);
    throw error;
  }
};

// Petición GET para verificar si un rut esta disponible segun respuesta HTTP y mensaje
// NO disponible, retornamos 409 Conflict. SÍ disponible, retornamos 200 OK.
export const checkRutDisponible = async (rut) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-rut`, {
      params: { rut }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de rut:', error);
    throw error;
  }
};

// Petición GET para verificar si un telefono esta disponible segun respuesta HTTP y mensaje
// NO disponible, retornamos 409 Conflict. SÍ disponible, retornamos 200 OK.
export const checkTelefonoDisponible = async (telefono) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-telefono`, {
      params: { telefono }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de telefono:', error);
    throw error;
  }
};

// Petcion GET para obtener la cantidad de clientes activos
// La respuesta es del tipo: { "cantidadClientesActivos": 634.00 }
export const getcantidadClientesActivos = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/activos/cantidad`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener la cantidad de clientes activos:', error);
    throw error;
  }
};

// Petición GET para exportar clientes a CSV
export const exportarClientesCSV = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/exportar-csv`, {
      responseType: 'blob',
    });

    // Creamos el blob especificando explícitamente el set de caracteres
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `clientes_${new Date().toLocaleDateString()}.csv`);

    document.body.appendChild(link);
    link.click();

    link.parentNode.removeChild(link);
    window.URL.revokeObjectURL(url);

  } catch (error) {
    console.error('Error al exportar CSV:', error);
    throw error;
  }
};