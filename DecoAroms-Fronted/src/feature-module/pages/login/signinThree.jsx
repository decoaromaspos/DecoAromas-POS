import React, { useState } from "react";
import ImageWithBasePath from "../../../core/img/imagewithbasebath";
import { Link, Navigate } from "react-router-dom";
import { all_routes } from "../../../Router/all_routes";
import { useAuth } from "../../../context/AuthContext";

const SigninThree = () => {
    const route = all_routes;

    // 2. Hooks y estado del componente
    const { login, isAuthenticated } = useAuth(); // Obtenemos la función login y el estado de autenticación
    const [username, setUsername] = useState(""); // Estado para el campo de email/usuario
    const [password, setPassword] = useState(""); // Estado para la contraseña
    const [error, setError] = useState("");       // Estado para mensajes de error
    const [isSubmitting, setIsSubmitting] = useState(false); // Estado para deshabilitar el botón al enviar
    const [showPassword, setShowPassword] = useState(false); // Estado para mostrar/ocultar contraseña

    // 3. Si el usuario ya está autenticado, no debería ver esta página
    if (isAuthenticated) {
        return <Navigate to={route.dashboard} />;
    }

    // 4. Función para manejar el envío del formulario
    const handleSubmit = async (e) => {
        e.preventDefault(); // Evita que la página se recargue
        setError("");       // Limpia errores anteriores
        setIsSubmitting(true); // Deshabilita el botón

        try {
            // Llamamos a la función login del contexto
            const success = await login(username, password);

            if (!success) {
                setError("Email o contraseña incorrectos. Inténtalo de nuevo.");
            }
            // Si el login es exitoso, el AuthContext se encargará de la redirección
        } catch (err) {
            setError("Ocurrió un error inesperado. Por favor, contacta a soporte.");
            console.error(err);
        } finally {
            setIsSubmitting(false); // Vuelve a habilitar el botón
        }
    };

    const togglePasswordVisibility = () => {
        setShowPassword(!showPassword);
    };

    return (
        <div className="main-wrapper">
            <div className="account-content">
                <div className="login-wrapper login-new">
                    <div className="container">
                        <div className="login-content user-login">
                            <div className="login-logo">
                                <ImageWithBasePath src="assets/img/logo-sin-fondo.png" alt="img" />
                            </div>
                            <form onSubmit={handleSubmit}>
                                <div className="login-userset">
                                    <div className="login-userheading">
                                        <h3>Iniciar sesión</h3>
                                        <h4>Accede al sistema Decoaromas POS con tu email y contraseña.</h4>
                                    </div>

                                    <div className="form-login">
                                        <label className="form-label">Email o username</label>
                                        <div className="form-addons">
                                            <input
                                                type="text"
                                                className="form-control"
                                                placeholder="Ingresa tu email o username"
                                                value={username}
                                                onChange={(e) => setUsername(e.target.value)}
                                                required
                                            />
                                            <ImageWithBasePath src="assets/img/icons/mail.svg" alt="img" />
                                        </div>
                                    </div>

                                    <div className="form-login">
                                        <label>Contraseña</label>
                                        <div className="pass-group">
                                            <input
                                                type={showPassword ? "text" : "password"}
                                                className="pass-input"
                                                placeholder="Ingresa tu contraseña"
                                                value={password}
                                                onChange={(e) => setPassword(e.target.value)}
                                                required
                                            />
                                            <span
                                                className={`fas toggle-password ${showPassword ? "fa-eye" : "fa-eye-slash"}`}
                                                onClick={togglePasswordVisibility}
                                                style={{ cursor: 'pointer' }}
                                            />
                                        </div>
                                    </div>

                                    
                                    {error && (
                                        <div className="alert alert-danger" role="alert" style={{ marginTop: '15px' }}>
                                            {error}
                                        </div>
                                    )}

                                    

                                    <div className="form-login">
                                        
                                        <button
                                            className="btn btn-login"
                                            type="submit"
                                            disabled={isSubmitting} // Deshabilitar mientras se procesa
                                        >
                                            {isSubmitting ? "Ingresando..." : "Iniciar sesión"}
                                        </button>
                                    </div>

                                    <div className="signinform">
                                        <h4>
                                            ¿Eres nuevo?
                                            <Link to={route.registerThree} className="hover-a">
                                                {" "}
                                                Crear una cuenta de Vendedor
                                            </Link>
                                        </h4>
                                    </div>
                                </div>
                            </form>
                        </div>
                        <div className="my-4 d-flex justify-content-center align-items-center copyright-text">
                            <p>Copyright © 2025 - Decoaromas.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SigninThree;