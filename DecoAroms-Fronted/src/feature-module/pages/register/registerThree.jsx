import React, { useState } from "react";
import ImageWithBasePath from "../../../core/img/imagewithbasebath";
import { Link, useNavigate } from "react-router-dom";
import { all_routes } from "../../../Router/all_routes";
import AuthService from "../../../services/authService";
import { useFormValidator } from "../../../hooks/useFormValidator";

const RegisterThree = () => {
    const route = all_routes;
    const navigate = useNavigate();

    // 1. Renombramos el handleChange del hook para poder envolverlo.
    const { 
        formData, 
        errors, 
        availability, 
        handleChange: handleHookChange, 
        validateForm 
    } = useFormValidator({
        nombre: "", apellido: "", correo: "", username: "", password: "", confirmPassword: ""
    });

    const [apiError, setApiError] = useState("");
    const [success, setSuccess] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showPass, setShowPass] = useState({ password: false, confirmPassword: false });

    // --- MODIFICACIÓN CORREGIDA ---
    const handleChange = (e) => {
        const { name, value } = e.target;
        
        if (name === 'username') {
            const sanitizedValue = value.replace(/\s/g, '');
            // Creamos un objeto simple que simula la estructura del evento
            // que el hook espera, en lugar de clonar el evento completo.
            const fakeEvent = {
                target: {
                    name: name,
                    value: sanitizedValue
                }
            };
            handleHookChange(fakeEvent);
        } else {
            // Para los demás inputs, pasamos el evento original.
            handleHookChange(e);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setApiError("");
        setSuccess("");
        if (!validateForm()) return;
        setIsSubmitting(true);
        try {
            const { nombre, apellido, correo, username, password } = formData;
            await AuthService.register({ nombre, apellido, correo, username, password, rol: "VENDEDOR" });
            setSuccess("¡Registro exitoso! Serás redirigido al login...");
            setTimeout(() => navigate(route.signinthree), 2000);
        } catch (err) {
            setApiError(err.response?.data?.message || "Ocurrió un error en el registro.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const toggleShowPass = (field) => {
        setShowPass(prev => ({ ...prev, [field]: !prev[field] }));
    };

    const getAvailabilityClass = (status) => {
        if (status === 'checking') return 'text-muted';
        if (status === 'available') return 'text-success';
        if (status === 'unavailable') return 'text-danger';
        return '';
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
                                        <h3>Crear Cuenta</h3>
                                        <h4>Crea una nueva cuenta de Vendedor</h4>
                                    </div>

                                    {apiError && <div className="alert alert-danger mt-3">{apiError}</div>}
                                    {success && <div className="alert alert-success mt-3">{success}</div>}
                                    
                                    {/* El resto del JSX no cambia, sigue usando onChange={handleChange} */}

                                    <div className="form-login">
                                        <label>Nombre</label>
                                        <div className="form-addons">
                                            <input type="text" name="nombre" value={formData.nombre} onChange={handleChange} className="form-control" placeholder="Ej: Juan" />
                                            <ImageWithBasePath src="assets/img/icons/users1.svg" alt="user icon" />
                                        </div>
                                        {errors.nombre && <small className="text-danger d-block mt-1">{errors.nombre}</small>}
                                    </div>
                                    <div className="form-login">
                                        <label>Apellido</label>
                                        <div className="form-addons">
                                            <input type="text" name="apellido" value={formData.apellido} onChange={handleChange} className="form-control" placeholder="Ej: Pérez" />
                                            <ImageWithBasePath src="assets/img/icons/users1.svg" alt="user icon" />
                                        </div>
                                        {errors.apellido && <small className="text-danger d-block mt-1">{errors.apellido}</small>}
                                    </div>
                                    <div className="form-login">
                                        <label>Username</label>
                                        <div className="form-addons">
                                            <input type="text" name="username" value={formData.username} onChange={handleChange} className="form-control" placeholder="Elige un nombre de usuario" />
                                            <ImageWithBasePath src="assets/img/icons/users1.svg" alt="user icon" />
                                        </div>
                                        {errors.username && <small className="text-danger d-block mt-1">{errors.username}</small>}
                                        {availability.username.status !== 'idle' && (<small className={`d-block mt-1 ${getAvailabilityClass(availability.username.status)}`}>{availability.username.message}</small>)}
                                    </div>
                                    <div className="form-login">
                                        <label>Correo Electrónico</label>
                                        <div className="form-addons">
                                            <input type="email" name="correo" value={formData.correo} onChange={handleChange} className="form-control" placeholder="tu.correo@ejemplo.com" />
                                            <ImageWithBasePath src="assets/img/icons/mail.svg" alt="mail icon" />
                                        </div>
                                        {errors.correo && <small className="text-danger d-block mt-1">{errors.correo}</small>}
                                        {availability.correo.status !== 'idle' && (<small className={`d-block mt-1 ${getAvailabilityClass(availability.correo.status)}`}>{availability.correo.message}</small>)}
                                    </div>
                                    <div className="form-login">
                                        <label>Contraseña</label>
                                        <div className="pass-group">
                                            <input type={showPass.password ? "text" : "password"} name="password" value={formData.password} onChange={handleChange} className="form-control pass-input" placeholder="Mínimo 6 caracteres" />
                                            <span className={`fas toggle-password ${showPass.password ? "fa-eye" : "fa-eye-slash"}`} onClick={() => toggleShowPass('password')} />
                                        </div>
                                        {errors.password && <small className="text-danger d-block mt-1">{errors.password}</small>}
                                    </div>
                                    <div className="form-login">
                                        <label>Confirmar Contraseña</label>
                                        <div className="pass-group">
                                            <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} className="form-control pass-input" placeholder="Repite la contraseña" />
                                            <span className={`fas toggle-password ${showPass.confirmPassword ? "fa-eye" : "fa-eye-slash"}`} onClick={() => toggleShowPass('confirmPassword')} />
                                        </div>
                                        {errors.confirmPassword && <small className="text-danger d-block mt-1">{errors.confirmPassword}</small>}
                                    </div>

                                    <div className="form-login">
                                        <button className="btn btn-login" type="submit" disabled={isSubmitting || availability.correo.status === 'checking' || availability.username.status === 'checking'}>
                                            {isSubmitting ? "Registrando..." : "Crear Cuenta"}
                                        </button>
                                    </div>
                                    <div className="signinform">
                                        <h4>
                                            ¿Ya tienes una cuenta?{" "}
                                            <Link to={route.signinthree} className="hover-a">Iniciar Sesión</Link>
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

export default RegisterThree;