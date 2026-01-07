import httpClient from '../apiClient';

const URL_LOCAL = '/api/productos';

// Bajo Stock,    desde 1 a umbral
// Petición GET de productos activos bajo stock segun filtros paginados
export const getProductosBajoStockByFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'stock', aromaId, familiaId, nombre, umbralMaximo) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/bajo-stock/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        aromaId,
        familiaId,
        nombre,
        umbralMaximo
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener productos bajo stock con filtros paginados:', error);
    throw error;
  }
};


// Fuera Stock,    menor o igual a 0
// Petición GET de productos activos fuera de stock con filtros paginados
export const getProductosFueraStockByFiltrosPaginados = async (
  page = 0, size = 10, sortBy = 'stock', aromaId, familiaId, nombre) => {
  try {
    const response = await httpClient.get(`${URL_LOCAL}/fuera-stock/filtros/paginas`, {
      params: {
        page,
        size,
        sortBy,
        aromaId,
        familiaId,
        nombre
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error al obtener productos fuera stock con filtros paginados:', error);
    throw error;
  }
};