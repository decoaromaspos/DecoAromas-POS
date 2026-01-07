import axios from 'axios';
import AuthService from './services/authService';
import { toast } from 'react-toastify';

//const API_URL = import.meta.env.BACK_URL;

const apiClient = axios.create({
    baseURL: 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Configuración del interceptor
apiClient.interceptors.request.use(
    (config) => {
        // Obtenemos el token usando la función de nuestro servicio
        const token = AuthService.getToken();
        
        // Si el token existe, lo añadimos a las cabeceras
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        
        return config; // Devolvemos la configuración modificada
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Interceptor de Respuestas
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        const originalRequest = error.config;

        if (error.response) {
            // Caso 1: Error 401 (No autenticado) -> La sesión es inválida.
            if (error.response.status === 401 && originalRequest.url !== '/auth/login') {
                console.error("Error 401: Token inválido o expirado. Cerrando sesión.");
                
                toast.warn("Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
                AuthService.logout();
                window.location.href = '/signin';
            }

            // Caso 2: Error 403 (Prohibido) -> El usuario no tiene permisos.
            if (error.response.status === 403) {
                console.warn("Error 403: Acceso prohibido.");
                
                // <-- 2. Muestra una notificación de error en lugar de cerrar sesión
                const errorMessage = error.response.data?.error || "No tienes permiso para realizar esta acción.";
                toast.error(errorMessage);
            }
        }
        return Promise.reject(error);
    }
);

export default apiClient;