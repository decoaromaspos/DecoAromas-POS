import httpClient from '../apiClient';

const URL_LOCAL = '/api/productos';


// Petición GET de productos segun filtros paginación
// Los filtros pueden ser nombre, sku, codigo barras, activo (true/false), aromaId, familiaId
export const getProductosByFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'nombre', aromaId, familiaId, activo, nombre, sku, codigoBarras
) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        aromaId,
        familiaId,
        activo,
        nombre,
        sku,
        codigoBarras
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener productos segun filtros paginados:', error);
    throw error;
  }
};

// Petición GET para obtener un producto por su código de barras
export const getProductoByCodigoBarras = async (codigoBarras) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/codigo-barras/${codigoBarras}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener producto por código de barras:', error);
    throw error;
  }
};

// Petición GET para buscar productos por nombre parcial según autocompletado
export const getProductosByNombreParcial = async (nombreParcial) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/buscar/nombre/${nombreParcial}/autocomplete`);
    return response.data;
  } catch (error) {
    console.error("Error al buscar productos por nombre parcial:", error);
    throw error;
  }
};


// Petición GET para verificar si un sku esta disponible segun respuesta HTTP y mensaje
// NO disponible, retornamos 409 Conflict
// SÍ disponible, retornamos 200 OK.
export const checkSkuDisponible = async (sku) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/check-sku`, {
      params: { sku }
    });
    return response.data;
  } catch (error) {
    console.error('Error al verificar disponibilidad de SKU:', error);
    throw error;
  }
};

// Petición GET para verificar si un sku esta disponible segun respuesta HTTP y mensaje
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

// Petición POST para crear un nuevo producto
export const crearProducto = async (productoData) => {
  try {
    const response = await httpClient.post(`${URL_LOCAL}`, productoData);
    return response.data;
  } catch (error) {
    if (error.response) {
      throw error.response
    } else {
      throw new Error("Error de red o desconocido al crear producto.");
    }
  }
};

// Petición PUT para actualizar un producto existente
export const actualizarProducto = async (id, productoData) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/update/${id}`, productoData);
    return response.data;
  } catch (error) {
    if (error.response) {
      throw error.response
    } else {
      throw new Error("Error de red o desconocido al actualizar producto.");
    }
  }
};

// Petición PUT para activar producto (activo true)
export const activarProducto = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      activo: true
    });
    return response.data;
  } catch (error) {
    console.error('Error al activar el producto:', error);
    throw error;
  }
};

// Petición PUT para desactivar producto (activo false)
export const desactivarProducto = async (id) => {
  try {
    const response = await httpClient.put(`${URL_LOCAL}/cambiar/estado`, {
      id,
      activo: false
    });
    return response.data;
  } catch (error) {
    console.error('Error al desactivar el producto:', error);
    throw error;
  }
};

// Petición GET para obtener un producto por su ID
export const getProductoById = async (id) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/${id}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener el producto por ID:', error);
    throw error;
  }
};

// Petcion GET para obtener stock general de productos
// La respuesta es del tipo: { "stockGeneral": 634.00 }
export const getStockGeneral = async () => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/stock/general`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener el stock general de productos:', error);
    throw error;
  }
};




// Peticiones de editar stock

// Petición PATCH para actualizar el stock de un producto.
// Ingreso de valor específico existente 
export const actualizarStockProducto = async (id, nuevaCantidad, usuarioId) => {
  try {
    const response = await httpClient.patch(`${URL_LOCAL}/stock/${id}`, {
      nuevaCantidad,
      usuarioId
    });
    return response.data;
  } catch (error) {
    if (error.response) {
      throw error.response
    } else {
      throw new Error("Error de red o desconocido al actualizar stock del producto.");
    }
  }
};

// Petición PATCH para incrementar el stock de un producto
export const incrementarStockProducto = async (id, cantidad, usuarioId) => {
  try {
    const response = await httpClient.patch(`${URL_LOCAL}/ingresar-stock/${id}`, {
      cantidad,
      usuarioId
    });
    return response.data;
  } catch (error) {
    if (error.response) {
      throw error.response
    } else {
      throw new Error("Error de red o desconocido al incrementar stock del producto.");
    }
  }
};

// Petición PATCH para decrementar el stock de un producto
export const decrementarStockProducto = async (id, cantidad, usuarioId) => {
  try {
    const response = await httpClient.patch(`${URL_LOCAL}/retirar-stock/${id}`, {
      cantidad,
      usuarioId
    });
    return response.data;
  } catch (error) {
    if (error.response) {
      throw error.response
    } else {
      throw new Error("Error de red o desconocido al decrementar stock del producto.");
    }
  }
};

export const exportarProductosCSV = async (filtros = {}) => {
  try {
    // Construimos los query params dinámicamente
    const params = new URLSearchParams();
    if (filtros.aromaId) params.append('aromaId', filtros.aromaId);
    if (filtros.familiaId) params.append('familiaId', filtros.familiaId);
    if (filtros.activo !== undefined && filtros.activo !== null) {
      params.append('activo', filtros.activo);
    }

    const response = await httpClient.get(`${URL_LOCAL}/exportar-csv?${params.toString()}`, {
      responseType: 'blob',
    });

    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `inventario_${new Date().toISOString().split('T')[0]}.csv`);

    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

  } catch (error) {
    console.error('Error al exportar productos:', error);
    throw error;
  }
};