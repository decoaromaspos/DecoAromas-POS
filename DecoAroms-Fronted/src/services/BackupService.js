import httpClient from '../apiClient';

const URL_BACKUPS = '/api/backups';

/**
 * Llama al backend para listar todos los archivos de backup disponibles
 * en el directorio montado del servidor (C:\Backups\decoaromas).
 * @returns {Promise<Array<string>>} Lista de nombres de archivos (ej: ["decoaromas_2025-12-08_18-30.dump"])
 */
export const listBackups = async () => {
    try {
        const response = await httpClient.get(`${URL_BACKUPS}/list`);
        return response.data;
    } catch (error) {
        console.error('Error al listar los backups:', error);
        throw error;
    }
};

/**
 * Llama al backend para iniciar el proceso de pg_dump
 * y crear un nuevo archivo de backup en el servidor.
 * @returns {Promise<string>} Mensaje de éxito.
 */
export const createBackup = async () => {
    try {
        const response = await httpClient.post(`${URL_BACKUPS}/create`);
        return response.data; // Mensaje de éxito
    } catch (error) {
        console.error('Error al crear el backup:', error);
        throw error;
    }
};

/**
 * Llama al backend para iniciar el proceso de pg_dump
 * y crear un nuevo archivo de backup solo con el inventario de productos.
 * Donde todos los productos tienen stock 0, y solo existe el usuario super admin.
 * @returns {Promise<string>} Mensaje de éxito.
 */
export const createBackupInventario = async () => {
    try {
        const response = await httpClient.post(`${URL_BACKUPS}/create-smart-inventario`);
        return response.data; // Mensaje de éxito
    } catch (error) {
        console.error('Error al crear el backup:', error);
        throw error;
    }
};

/**
 * Llama al backend para restaurar la base de datos usando un archivo de backup
 * previamente seleccionado por el usuario.
 * @param {string} filename Nombre del archivo .dump a restaurar.
 * @returns {Promise<string>} Mensaje de éxito.
 */
export const restoreBackup = async (filename) => {
    try {
        const response = await httpClient.post(`${URL_BACKUPS}/restore`, {
            filename: filename // Envía el DTO al backend
        });
        return response.data; // Mensaje de éxito
    } catch (error) {
        console.error('Error al restaurar la base de datos:', error);
        throw error;
    }
};


const backupService = {
    listBackups,
    createBackup,
    restoreBackup,
    createBackupInventario
};

export default backupService;