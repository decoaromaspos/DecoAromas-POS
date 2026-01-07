import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; 
import PropTypes from 'prop-types';
import { all_routes } from "./all_routes";

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();
    const location = useLocation();
    const route = all_routes;

    // Mientras se verifica si hay sesión (carga inicial), no mostramos nada
    // para evitar un parpadeo de la página de login.
    if (loading) {
        return null; // O un componente de Spinner/Cargando si lo prefieres
    }

    // Si no está autenticado, lo redirigimos a la página de login.
    // Guardamos la ubicación a la que intentaba ir (`state={{ from: location }}`)
    // por si quieres redirigirlo de vuelta después del login.
    if (!isAuthenticated) {
        return <Navigate to={route.signinthree} state={{ from: location }} replace />;
    }

    // Si está autenticado, renderizamos el componente hijo (la página solicitada).
    return children;
};

export default ProtectedRoute;

ProtectedRoute.propTypes = {
    children: PropTypes.node.isRequired,
};