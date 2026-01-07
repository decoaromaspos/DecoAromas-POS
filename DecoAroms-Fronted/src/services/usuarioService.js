import httpClient from '../apiClient';

const URL_LOCAL = '/api/usuarios';


// Petición GET de todos los usuarios
export const getUsuarios = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener usuarios:', error);
    throw error;
  }
};

// Petición GET de todos los usuarios. Incluyendo super admin
// Utilizado para seleccionar super admin en busqueda de movimeientos de inventario
export const getUsuariosConSuperAdmin = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/super`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener usuarios:', error);
    throw error;
  }
};



// Peticion GET para obtener usuario segun id
export const getUsuarioById = async (id) => {
  try{
    const response = await httpClient.get(`${URL_LOCAL}/${id}`);
    return response.data;
  } catch (error) {
      console.error('Error al obtener el usuario:', error);
      throw error;
  }
};

// Peticion GET para obtener usuarios paginados con filtros
export const getUsuariosFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'usuarioId', nombreCompletoParcial, 
  correoParcial, usernameParcial, rol, activo
) => {
  const filtrosBody = {
    nombreCompletoParcial,
    correoParcial,
    usernameParcial,
    rol,
    activo
  };
  
  try {
    const response = await httpClient.post(
      `${URL_LOCAL}/filtros/paginas`,
      filtrosBody,
      {
        params: {
          page,
          size,
          sortBy,
        }
      }
    );
    return response.data;
  } catch (error) {
    console.error('Error al obtener usuarios paginados:', error);
    throw error;
  }
};

// Peticion para desactivar un usuario
export const desactivarUsuario = async (id) => {
  try{
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`,{
      id,
      activo: false
    });
    return response.data;
  } catch (error) {
    console.error('Error al desativar el usuario:', error);
    throw error;
  }
};

// Peticion para activar un cliente
export const activarUsuario = async (id) => {
  try{
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`,{
      id,
      activo: true
    });
    return response.data;
  } catch (error) {
    console.error('Error al activar el usuario:', error);
    throw error;
  }
};

// Peticion para actualizar un usuario
export const actualizarUsuario2 = async (id, usuarioData) => {
  try{
    const response = await httpClient.put(`${URL_LOCAL}/update/${id}`, usuarioData);
    return response.data;
  } catch (error) {
    console.error('Error al actualizar el usuario:', error);
    throw error;
  }
};
export const actualizarUsuario = async (usuarioId, usuarioData) => {
    try {
        // La URL debe coincidir con la de tu API
        const response = await httpClient.put(`http://localhost:8080/api/usuarios/update/${usuarioId}`, usuarioData);
        
        console.log("Respuesta de Axios en el servicio:", response);

        // ✅ ESTA LÍNEA ES FUNDAMENTAL
        // Si falta, la función devuelve 'undefined' y el .catch del componente se activa.
        return response.data; 
        
    } catch (error) {
        // Si hay un error de red, lo relanzamos para que el componente lo capture.
        console.error("Error en el servicio actualizarUsuario:", error);
        throw error;
    }
};
// Peticion para actualizar rol de un usuario
export const actualizarRolDeUsuario = async (usuarioId, rol) => {
  try{
    const response = await httpClient.put(`${URL_LOCAL}/update/rol`, {
      usuarioId,
      rol
    });
    return response.data;
  } catch (error) {
    console.error('Error al actualizar el usuario:', error);
    throw error;
  }
};

// Petición GET para verificar correo disponible
// NO disponible 409 Conflict. SÍ disponible 200 OK.
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

// Petición GET para verificar username disponible
// NO disponible 409 Conflict. SÍ disponible 200 OK.
export const checkUsernameDisponible = async (username) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-username`, {
      params: { username }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de username:', error);
    throw error;
  }
};

// Peticion POST para actualizar la contraseña de un usuario
export const cambiarPassword = async (usuarioId, passwordActual, passwordNueva) => {
  try{
    const response = await httpClient.post(`${URL_LOCAL}/${usuarioId}/password`,{
      passwordActual,
      passwordNueva
    });
    return response.data;
  } catch (error) {
    console.error('Error al cambiar la contraseña del usuario:', error);
    throw error;
  }
};