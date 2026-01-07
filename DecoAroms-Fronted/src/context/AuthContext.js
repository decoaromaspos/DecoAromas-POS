import React, { createContext, useState, useContext, useEffect } from 'react';
import AuthService from '../services/authService';
import { useNavigate } from 'react-router-dom';
import PropTypes from 'prop-types';

const AuthContext = createContext();

export const useAuth = () => {
    return useContext(AuthContext);
};

export const AuthProvider = ({ children }) => {
    const [usuario, setUsuario] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        // Al cargar la app, revisamos si ya hay un usuario en localStorage
        const currentUser = AuthService.getCurrentUser();
        if (currentUser) {
            setUsuario(currentUser);
        }
        setLoading(false);
    }, []);

    const login = async (username, password) => {
        try {
            const data = await AuthService.login(username, password);
            setUsuario(data.usuario);
            navigate('/dashboard'); // O a la ruta principal de tu app
            return true;
        } catch (error) {
            console.error("Error en el login:", error);
            return false;
        }
    };

    const logout = () => {
        console.log("Función logout() llamada. Limpiando sesión...");
        AuthService.logout();
        setUsuario(null);
        navigate('/login');
    };

    const actualizarDatosUsuario = (nuevosDatos) => {
        const usuarioActualizado = { ...usuario, ...nuevosDatos };
        setUsuario(usuarioActualizado);
        localStorage.setItem('usuario', JSON.stringify(usuarioActualizado));
    };

    const value = {
        usuario,
        isAuthenticated: !!usuario, // Si hay un objeto usuario, está autenticado
        login,
        logout,
        actualizarDatosUsuario,
        loading,
    };

    return (
        <AuthContext.Provider value={value}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

AuthProvider.propTypes = {
    children: PropTypes.node.isRequired,
};