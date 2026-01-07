import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import PropTypes from 'prop-types';

const RouteLoaderWrapper = ({ children }) => {
  const [loading, setLoading] = useState(false);
  const location = useLocation();

  const showLoader = () => {
    setLoading(true);
  };

  const hideLoader = () => {
    setLoading(false);
    window.scrollTo(0, 0);
  };

  useEffect(() => {
    showLoader();
    const timeoutId = setTimeout(() => {
      hideLoader();
    }, 600);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [location.pathname]);

  // NUEVO EFECTO: Bloqueo del Scroll del Body
  useEffect(() => {
    if (loading) {
      // Cuando está cargando, quitamos la barra de scroll del body
      document.body.style.overflow = 'hidden';
    } else {
      // Cuando termina, restauramos el comportamiento normal
      document.body.style.overflow = 'unset'; 
    }

    // Cleanup: Importante por si el componente se desmonta inesperadamente
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [loading]);

  return (
    <div>
      {/* El div del loader */}
      {loading && (
        <div id="global-loader">
          <div className="whirly-loader"></div>
        </div>
      )}
      {/* Renderiza los componentes hijos */}
      {children} 
    </div>
  );
};

RouteLoaderWrapper.propTypes = {
  children: PropTypes.node, 
};

export default RouteLoaderWrapper;