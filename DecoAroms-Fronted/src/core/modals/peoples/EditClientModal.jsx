import React, { useState, useEffect } from "react";
import { Modal, Button, Form, Alert, OverlayTrigger, Tooltip } from "react-bootstrap";
import { Info } from 'feather-icons-react';
import Select from "react-select";
import PropTypes from 'prop-types';
import Swal from "sweetalert2";

import { 
    actualizarCliente, 
    checkRutDisponible,
    checkCorreoDisponible,
    checkTelefonoDisponible 
} from "../../../services/clienteService"; 

// --- CONSTANTES Y VALIDACIONES REUTILIZADAS ---
const PHONE_PREFIX = "+569";
const MAX_PHONE_LENGTH = 12;

const rutValidator = (rut) => {
    const regex = /^\d{7,8}-[0-9K]$/;
    return regex.test(rut);
};

const emailValidator = (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
};

const tipoOptions = [
    { value: "DETALLE", label: "Detalle" },
    { value: "MAYORISTA", label: "Mayorista" },
];

const getPhoneValue = (phone) => {
    if (!phone) return PHONE_PREFIX;
    return phone.startsWith(PHONE_PREFIX) ? phone : PHONE_PREFIX + phone.replace(/\D/g, '').substring(0, 8);
};
// ------------------------------------------------

const EditClientModal = ({ show, handleClose, client, onClientUpdated }) => {
    const [originalClient, setOriginalClient] = useState(null);
    const [formData, setFormData] = useState({
        rut: "",
        nombre: "",
        apellido: "",
        correo: "",
        telefono: PHONE_PREFIX,
        ciudad: "",
        tipo: null,
    });

    const [validationErrors, setValidationErrors] = useState({});
    const [apiError, setApiError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [availabilityStatus, setAvailabilityStatus] = useState({
        rut: { checked: false, available: false, message: '' },
        correo: { checked: false, available: false, message: '' },
        telefono: { checked: false, available: false, message: '' },
    });

    useEffect(() => {
        if (client) {
            const initialData = {
                rut: client.rut || "",
                nombre: client.nombre || "",
                apellido: client.apellido || "",
                correo: client.correo || "",
                telefono: getPhoneValue(client.telefono),
                ciudad: client.ciudad || "",
                tipo: client.tipo || null,
            };
            setFormData(initialData);
            setOriginalClient(initialData);
            
            setValidationErrors({});
            setApiError(null);
            setAvailabilityStatus({
                rut: { checked: false, message: '' },
                correo: { checked: false, message: '' },
                telefono: { checked: false, message: '' },
            });
        }
    }, [client]);

    // Hook para el RUT
    useEffect(() => {
        const currentRut = formData.rut;
        if (!originalClient || currentRut === originalClient.rut || !rutValidator(currentRut)) {
            setAvailabilityStatus(prev => ({ ...prev, rut: { checked: false, message: '' } }));
            return;
        }
        const handler = setTimeout(async () => {
            try {
                const response = await checkRutDisponible(currentRut);
                setAvailabilityStatus(prev => ({ ...prev, rut: { checked: true, ...response } }));
            } catch (error) {
                setAvailabilityStatus(prev => ({
                    ...prev,
                    rut: { checked: true, available: false, message: error.response?.data?.message || "RUT ya en uso." }
                }));
            }
        }, 500);
        return () => clearTimeout(handler);
    }, [formData.rut, originalClient]);
    
    // Hook para el Correo (insensible a mayúsculas/minúsculas)
    useEffect(() => {
        const currentCorreo = formData.correo; 
        const originalCorreo = originalClient?.correo || "";

        if (!originalClient || currentCorreo.toLowerCase() === originalCorreo.toLowerCase() || !currentCorreo || !emailValidator(currentCorreo)) {
            setAvailabilityStatus(prev => ({ ...prev, correo: { checked: false, message: '' } }));
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
    }, [formData.correo, originalClient]);

    // Hook para el Teléfono
    useEffect(() => {
        const currentTelefono = formData.telefono;
        if (!originalClient || currentTelefono === originalClient.telefono || currentTelefono.length < MAX_PHONE_LENGTH) {
            setAvailabilityStatus(prev => ({ ...prev, telefono: { checked: false, message: '' } }));
            return;
        }
        const handler = setTimeout(async () => {
            try {
                const response = await checkTelefonoDisponible(currentTelefono);
                setAvailabilityStatus(prev => ({ ...prev, telefono: { checked: true, ...response } }));
            } catch (error) {
                setAvailabilityStatus(prev => ({
                    ...prev,
                    telefono: { checked: true, available: false, message: error.response?.data?.message || "Teléfono ya en uso." }
                }));
            }
        }, 500);
        return () => clearTimeout(handler);
    }, [formData.telefono, originalClient]);

    const clearFieldErrors = (fieldName) => {
        if (validationErrors[fieldName]) {
            setValidationErrors((prev) => ({ ...prev, [fieldName]: null }));
        }
        setApiError(null);
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        clearFieldErrors(name);
    };

    const handleEmailChange = (e) => {
        const { name, value } = e.target;
        const sanitizedValue = value.replace(/\s/g, '');

        setFormData((prev) => ({ ...prev, [name]: sanitizedValue }));
        clearFieldErrors(name);
    };

    const handleRutChange = (e) => {
        let value = e.target.value;
        let cleanValue = value.replace(/[^0-9kK]/g, "").toUpperCase();
        
        let formattedRut = cleanValue;
        if (cleanValue.length > 1) {
            const body = cleanValue.slice(0, -1);
            const dv = cleanValue.slice(-1);
            formattedRut = `${body}-${dv}`;
        }
        if (formattedRut.length > 10) {
            formattedRut = formattedRut.substring(0, 10);
        }

        setFormData((prev) => ({ ...prev, rut: formattedRut }));
        clearFieldErrors('rut');
    };

    const handlePhoneChange = (e) => {
        let value = e.target.value;
        if (!value.startsWith(PHONE_PREFIX)) value = PHONE_PREFIX;
        const numberPart = value.substring(PHONE_PREFIX.length).replace(/\D/g, '');
        value = PHONE_PREFIX + numberPart;
        if (value.length > MAX_PHONE_LENGTH) value = value.substring(0, MAX_PHONE_LENGTH);
        
        setFormData((prev) => ({ ...prev, telefono: value }));
        clearFieldErrors('telefono');
    };

    const handleSelectChange = (selectedOption) => {
        setFormData((prev) => ({ ...prev, tipo: selectedOption ? selectedOption.value : null }));
        clearFieldErrors('tipo');
    };

    const validateForm = () => {
        let errors = {};
        if (!formData.rut || !rutValidator(formData.rut)) errors.rut = "RUT inválido. Debe ser como 12345678-K.";
        if (!formData.nombre.trim()) errors.nombre = "El nombre es obligatorio.";
        if (!formData.tipo) errors.tipo = "Debe seleccionar un tipo de cliente.";
        if (formData.correo && !emailValidator(formData.correo)) errors.correo = "Formato de correo inválido.";
        if (formData.telefono.length > PHONE_PREFIX.length && formData.telefono.length < MAX_PHONE_LENGTH) {
            errors.telefono = `El número debe tener 8 dígitos después de ${PHONE_PREFIX}.`;
        }
        setValidationErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateForm()) return;

        if ((availabilityStatus.rut.checked && !availabilityStatus.rut.available) ||
            (availabilityStatus.correo.checked && !availabilityStatus.correo.available) ||
            (availabilityStatus.telefono.checked && !availabilityStatus.telefono.available)) {
            setApiError("Por favor, corrija los campos marcados antes de continuar.");
            return;
        }

        setIsSubmitting(true);
        setApiError(null);
        
        // El .trim() aquí es una última capa de seguridad antes de enviar al backend
        const dataToSend = {
            rut: formData.rut,
            nombre: formData.nombre.trim(),
            apellido: formData.apellido.trim(),
            tipo: formData.tipo,
            correo: formData.correo.trim() === "" ? null : formData.correo.trim(),
            telefono: formData.telefono.length === PHONE_PREFIX.length ? null : formData.telefono,
            ciudad: formData.ciudad.trim() === "" ? null : formData.ciudad.trim(),
        };

        try {
            const updatedClient = await actualizarCliente(client.clienteId, dataToSend);
            Swal.fire({
                title: "¡Actualizado!",
                text: `El cliente ${updatedClient.nombre} ${updatedClient.apellido} ha sido actualizado.`,
                icon: "success",
                timer: 2000,
                showConfirmButton: false,
            });
            onClientUpdated(updatedClient);
            handleCloseModal();
        } catch (error) {
            const errorData = error.response?.data || error;
            if (errorData.details) {
                setValidationErrors(errorData.details);
            } else {
                setApiError(errorData.error || errorData.message || "Error desconocido.");
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleCloseModal = () => {
        setValidationErrors({});
        setApiError(null);
        handleClose();
    };
    
    if (!client) return null;

    return (
        <Modal show={show} onHide={handleCloseModal} centered>
            <Modal.Header closeButton>
                <Modal.Title>Editar Cliente: {client.nombre} {client.apellido}</Modal.Title>
            </Modal.Header>
            <Form noValidate onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    
                    <Form.Group className="mb-3">
                        <Form.Label>
                            RUT <span className="text-danger">*</span>
                            <OverlayTrigger
                                placement="top"
                                overlay={<Tooltip>El formato debe ser sin puntos y con guión. Ej: 12345678-K</Tooltip>}
                            >
                                <span className="ms-2" style={{ cursor: 'pointer' }}><Info size={16} /></span>
                            </OverlayTrigger>
                        </Form.Label>
                        <Form.Control
                            type="text" name="rut" value={formData.rut}
                            onChange={handleRutChange}
                            isInvalid={!!validationErrors.rut || (availabilityStatus.rut.checked && !availabilityStatus.rut.available)}
                            placeholder="12345678-K" maxLength={10} autoComplete="off"
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.rut || (availabilityStatus.rut.checked && availabilityStatus.rut.message)}
                        </Form.Control.Feedback>
                        {availabilityStatus.rut.checked && availabilityStatus.rut.available && (
                            <Form.Text className='text-success'>{availabilityStatus.rut.message}</Form.Text>
                        )}
                    </Form.Group>

                    <div className="row">
                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>Nombre <span className="text-danger">*</span></Form.Label>
                            <Form.Control
                                type="text" name="nombre" value={formData.nombre}
                                onChange={handleChange} isInvalid={!!validationErrors.nombre}
                            />
                            <Form.Control.Feedback type="invalid">{validationErrors.nombre}</Form.Control.Feedback>
                        </Form.Group>
                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>Apellido (Opcional)</Form.Label>
                            <Form.Control
                                type="text" name="apellido" value={formData.apellido}
                                onChange={handleChange} isInvalid={!!validationErrors.apellido}
                            />
                            <Form.Control.Feedback type="invalid">{validationErrors.apellido}</Form.Control.Feedback>
                        </Form.Group>
                    </div>

                    <Form.Group className="mb-3">
                        <Form.Label>Correo (Opcional)</Form.Label>
                        <Form.Control
                            type="email"
                            name="correo"
                            value={formData.correo}
                            onChange={handleEmailChange} 
                            isInvalid={!!validationErrors.correo || (availabilityStatus.correo.checked && !availabilityStatus.correo.available)}
                            placeholder="Ej: cliente@ejemplo.cl"
                        />
                        <Form.Control.Feedback type="invalid">
                            {validationErrors.correo || (availabilityStatus.correo.checked && availabilityStatus.correo.message)}
                        </Form.Control.Feedback>
                        {availabilityStatus.correo.checked && availabilityStatus.correo.available && (
                            <Form.Text className='text-success'>{availabilityStatus.correo.message}</Form.Text>
                        )}
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Ciudad (Opcional)</Form.Label>
                        <Form.Control
                            type="text"
                            name="ciudad"
                            value={formData.ciudad}
                            onChange={handleChange}
                            isInvalid={!!validationErrors.ciudad}
                            placeholder="Ej: Santiago"
                        />
                        <Form.Control.Feedback type="invalid">{validationErrors.ciudad}</Form.Control.Feedback>
                    </Form.Group>

                    <div className="row">
                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>
                                Teléfono (Opcional)
                                <OverlayTrigger
                                    placement="top"
                                    overlay={<Tooltip>Debe contener 8 dígitos después del prefijo +569.</Tooltip>}
                                >
                                    <span className="ms-2" style={{ cursor: 'pointer' }}><Info size={16} /></span>
                                </OverlayTrigger>
                            </Form.Label>
                            <Form.Control
                                type="tel" name="telefono" value={formData.telefono}
                                onChange={handlePhoneChange} 
                                isInvalid={!!validationErrors.telefono || (availabilityStatus.telefono.checked && !availabilityStatus.telefono.available)}
                                placeholder="+569xxxxxxxx" maxLength={MAX_PHONE_LENGTH} 
                            />
                            <Form.Control.Feedback type="invalid">
                                {validationErrors.telefono || (availabilityStatus.telefono.checked && availabilityStatus.telefono.message)}
                            </Form.Control.Feedback>
                            {availabilityStatus.telefono.checked && availabilityStatus.telefono.available && (
                                <Form.Text className='text-success'>{availabilityStatus.telefono.message}</Form.Text>
                            )}
                        </Form.Group>

                        <Form.Group className="mb-3 col-md-6">
                            <Form.Label>Tipo de Cliente <span className="text-danger">*</span></Form.Label>
                            <Select
                                options={tipoOptions}
                                value={tipoOptions.find(opt => opt.value === formData.tipo)}
                                onChange={handleSelectChange}
                                placeholder="Seleccionar tipo..."
                                className={validationErrors.tipo ? 'is-invalid' : ''}
                                classNamePrefix="select"
                            />
                            {validationErrors.tipo && <div className="invalid-feedback d-block">{validationErrors.tipo}</div>}
                        </Form.Group>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleCloseModal} disabled={isSubmitting}>
                        Cancelar
                    </button>
                    <Button variant="primary" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Guardando..." : "Actualizar Cliente"}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

EditClientModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    client: PropTypes.object, 
    onClientUpdated: PropTypes.func.isRequired,
};

export default EditClientModal;