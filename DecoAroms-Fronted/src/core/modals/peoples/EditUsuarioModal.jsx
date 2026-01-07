import React, { useState, useEffect } from "react";
import { Modal, Button, Form, Alert } from "react-bootstrap";
import PropTypes from 'prop-types';
import Swal from "sweetalert2";

import {
    actualizarUsuario,
    checkCorreoDisponible,
    checkUsernameDisponible
} from "../../../services/usuarioService";

const emailValidator = (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
};

const EditUsuarioModal = ({ show, handleClose, usuario, onUsuarioUpdated }) => {
    const [originalUsuario, setOriginalUsuario] = useState(null);
    const [formData, setFormData] = useState({
        nombre: "",
        apellido: "",
        correo: "",
        username: "",
    });

    const [validationErrors, setValidationErrors] = useState({});
    const [apiError, setApiError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Estado para la validación en tiempo real
    const [availabilityStatus, setAvailabilityStatus] = useState({
        correo: { checked: false, available: false, message: '' },
        username: { checked: false, available: false, message: '' },
    });

    useEffect(() => {
        if (usuario) {
            const initialData = {
                nombre: usuario.nombre || "",
                apellido: usuario.apellido || "",
                correo: usuario.correo || "",
                username: usuario.username || "",
            };
            setFormData(initialData);
            setOriginalUsuario(initialData);
            // Limpiar todos los errores y estados al abrir un nuevo usuario
            setValidationErrors({});
            setApiError(null);
            setAvailabilityStatus({
                correo: { checked: false, message: '' },
                username: { checked: false, message: '' },
            });
        }
    }, [usuario]);

    // --- VALIDACIÓN DE CORREO EN TIEMPO REAL ---
    useEffect(() => {
        const currentCorreo = formData.correo.trim();
        // Comparar en minúsculas para ignorar el caso (case-insensitive)
        if (!originalUsuario || currentCorreo.toLowerCase() === originalUsuario.correo.toLowerCase()) {
            setAvailabilityStatus(prev => ({ ...prev, correo: { checked: false, message: '' } }));
            return;
        }
        if (!currentCorreo || !emailValidator(currentCorreo)) {
            return;
        }
        
        const handler = setTimeout(async () => {
            try {
                const response = await checkCorreoDisponible(currentCorreo);
                setAvailabilityStatus(prev => ({ ...prev, correo: { checked: true, ...response } }));
            } catch (error) {
                setAvailabilityStatus(prev => ({
                    ...prev,
                    correo: { checked: true, available: false, message: error.response?.data?.message || "Correo ya en uso." }
                }));
            }
        }, 500);

        return () => clearTimeout(handler);
    }, [formData.correo, originalUsuario]);

    // --- VALIDACIÓN DE USERNAME EN TIEMPO REAL ---
    useEffect(() => {
        const currentUsername = formData.username.trim();
        //  Comparar en minúsculas para ignorar el caso (case-insensitive)
        if (!originalUsuario || currentUsername.toLowerCase() === originalUsuario.username.toLowerCase()) {
            setAvailabilityStatus(prev => ({ ...prev, username: { checked: false, message: '' } }));
            return;
        }
        if (!currentUsername) return;
        
        const handler = setTimeout(async () => {
            try {
                const response = await checkUsernameDisponible(currentUsername);
                setAvailabilityStatus(prev => ({ ...prev, username: { checked: true, ...response } }));
            } catch (error) {
                setAvailabilityStatus(prev => ({
                    ...prev,
                    username: { checked: true, available: false, message: error.response?.data?.message || "Username ya en uso." }
                }));
            }
        }, 500);

        return () => clearTimeout(handler);
    }, [formData.username, originalUsuario]);

const handleChange = (e) => {
        const { name, value } = e.target;
        let processedValue = value;

        // Si el campo que cambia es 'username', eliminamos todos los espacios
        if (name === 'username') {
            processedValue = value.replace(/\s/g, '');
        }

        setFormData((prev) => ({ ...prev, [name]: processedValue }));
        
        if (validationErrors[name]) {
            setValidationErrors((prev) => ({ ...prev, [name]: null }));
        }
        setApiError(null);
    };

const validateForm = () => {
        let errors = {};
        if (!formData.nombre.trim()) errors.nombre = "El nombre es obligatorio.";
        if (!formData.apellido.trim()) errors.apellido = "El apellido es obligatorio.";
        
        // La validación de espacios aquí ahora sirve como una doble seguridad,
        // aunque el handleChange ya previene que se escriban.
        if (!formData.username.trim()) {
            errors.username = "El username es obligatorio.";
        } else if (/\s/.test(formData.username)) {
            errors.username = "El username no puede contener espacios.";
        }
        
        if (!formData.correo.trim()) {
            errors.correo = "El correo es obligatorio.";
        } else if (!emailValidator(formData.correo)) {
            errors.correo = "Formato de correo inválido.";
        }
        setValidationErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateForm()) return;

        if ((availabilityStatus.correo.checked && !availabilityStatus.correo.available) ||
            (availabilityStatus.username.checked && !availabilityStatus.username.available)) {
            return; // No enviar si hay errores de disponibilidad
        }

        setIsSubmitting(true);
        setApiError(null);
        
        const dataToSend = {
            nombre: formData.nombre.trim(),
            apellido: formData.apellido.trim(),
            correo: formData.correo.trim(),
            username: formData.username.trim(),
        };

        try {
            await actualizarUsuario(usuario.usuarioId, dataToSend);
            Swal.fire("¡Actualizado!", `El usuario ${dataToSend.nombre} ha sido actualizado.`, "success");
            onUsuarioUpdated();
            handleClose();
        } catch (error) {
            setApiError(error.response?.data?.message || "Hubo un error al actualizar.");
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!usuario) return null;

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Editar Usuario: {originalUsuario?.nombre} {originalUsuario?.apellido}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    
                    <div className="row">
                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>Nombre <span className="text-danger">*</span></Form.Label>
                            <Form.Control type="text" name="nombre" value={formData.nombre} onChange={handleChange} isInvalid={!!validationErrors.nombre} />
                            <Form.Control.Feedback type="invalid">{validationErrors.nombre}</Form.Control.Feedback>
                        </Form.Group>
                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>Apellido <span className="text-danger">*</span></Form.Label>
                            <Form.Control type="text" name="apellido" value={formData.apellido} onChange={handleChange} isInvalid={!!validationErrors.apellido} />
                            <Form.Control.Feedback type="invalid">{validationErrors.apellido}</Form.Control.Feedback>
                        </Form.Group>
                    </div>

                    <Form.Group className="mb-3">
                        <Form.Label>Username <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            isInvalid={!!validationErrors.username || (availabilityStatus.username.checked && !availabilityStatus.username.available)}
                        />
                        <Form.Control.Feedback type="invalid">{validationErrors.username}</Form.Control.Feedback>
                        {availabilityStatus.username.checked && (
                            <Form.Text className={availabilityStatus.username.available ? 'text-success' : 'text-danger'}>
                                {availabilityStatus.username.message}
                            </Form.Text>
                        )}
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Correo <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="email"
                            name="correo"
                            value={formData.correo}
                            onChange={handleChange}
                            isInvalid={!!validationErrors.correo || (availabilityStatus.correo.checked && !availabilityStatus.correo.available)}
                        />
                        <Form.Control.Feedback type="invalid">{validationErrors.correo}</Form.Control.Feedback>
                        {availabilityStatus.correo.checked && (
                            <Form.Text className={availabilityStatus.correo.available ? 'text-success' : 'text-danger'}>
                                {availabilityStatus.correo.message}
                            </Form.Text>
                        )}
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleClose} disabled={isSubmitting}>
                        Cancelar
                    </button>
                    <Button variant="primary" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Guardando..." : "Actualizar Usuario"}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

EditUsuarioModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    usuario: PropTypes.object,
    onUsuarioUpdated: PropTypes.func.isRequired,
};

export default EditUsuarioModal;