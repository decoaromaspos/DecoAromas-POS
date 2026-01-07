import httpClient from '../apiClient';

const URL_LOCAL = '/api/barcodes';

export const getAllBarcodes = async (widthCm = 3, heightCm = 1.2) => {
  try {
    // pedir como blob para recibir bytes del PDF
    const response = await httpClient.get(`${URL_LOCAL}/exportAll`, {
      params: { widthCm, heightCm },
      responseType: 'blob',
    });
    return response.data; // será un Blob
  } catch (error) {
    console.error("Error al obtener códigos de barras:", error);
    throw error;
  }
};

export const getBarcodeListByIds = async (
  ids,
  widthCm = 3,
  heightCm = 1.2
) => {
  try {
    const response = await httpClient.post(`${URL_LOCAL}/exportList`, ids, {
      params: { widthCm, heightCm },
      responseType: 'blob',
    });
    return response.data;
  } catch (error) {
    console.error("Error al obtener códigos de barras:", error);
    throw error;
  }
};

