import httpClient from '../apiClient';

const API_URL = '/auth';
const API_URL_USERS = '/api/usuarios';


// Peticion de login
const login = async (username, password) => {
    const response = await httpClient.post(API_URL + '/login', {
        username,
        password,
    });

    // Si la respuesta es exitosa y contiene el token, lo guardamos
    if (response.data.token) {
        // Guardamos el objeto de usuario completo y el token por separado
        localStorage.setItem('usuario', JSON.stringify(response.data.usuario));
        localStorage.setItem('token', response.data.token);
    }

    return response.data;
};

// Cierre de sesion. Elimina los datos de sesión del localStorage.
const logout = () => {
    console.log("Cerrando sesion")
    localStorage.removeItem('usuario');
    localStorage.removeItem('token');
};

// Peticion de registro de usuario vendedor
const register = async (userData) => {
    const response = await httpClient.post(API_URL_USERS, userData);
    return response.data;
};

// Obtiene el usuario actual desde el localStorage.
// Devuelve el objeto del usuario parseado o null si no existe.
const getCurrentUser = () => {
    const usuarioStr = localStorage.getItem('usuario');
    if (usuarioStr) {
        return JSON.parse(usuarioStr);
    }
    return null;
};

//Obtiene el token JWT desde el localStorage.
const getToken = () => {
    return localStorage.getItem('token');
};


const AuthService = {
    login,
    logout,
    register,
    getCurrentUser,
    getToken,
};

export default AuthService;