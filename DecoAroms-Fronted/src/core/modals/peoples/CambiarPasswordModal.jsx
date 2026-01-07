import React, { useState } from 'react';
import { Modal, Button, Form, Alert } from 'react-bootstrap';
import { Eye, EyeOff } from 'feather-icons-react';
import PropTypes from 'prop-types';
import Swal from 'sweetalert2';
import { cambiarPassword } from '../../../services/usuarioService';

const CambiarPasswordModal = ({ show, handleClose, userId }) => {
    const [formData, setFormData] = useState({
        passwordActual: '',
        passwordNueva: '',
        confirmarPassword: '',
    });
    const [errors, setErrors] = useState({});
    const [apiError, setApiError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showPass, setShowPass] = useState({ actual: false, nueva: false, confirmar: false });

    const toggleShowPass = (field) => setShowPass(prev => ({ ...prev, [field]: !prev[field] }));

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
        setApiError('');
    };

    const validateForm = () => {
        let formErrors = {};
        if (!formData.passwordActual) formErrors.passwordActual = 'La contraseña actual es obligatoria.';
        if (!formData.passwordNueva) {
            formErrors.passwordNueva = 'La nueva contraseña es obligatoria.';
        } else if (formData.passwordNueva.length < 6) {
            formErrors.passwordNueva = 'La contraseña debe tener al menos 6 caracteres.';
        }
        if (formData.passwordNueva !== formData.confirmarPassword) {
            formErrors.confirmarPassword = 'Las contraseñas no coinciden.';
        }
        setErrors(formErrors);
        return Object.keys(formErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validateForm()) return;

        setIsSubmitting(true);
        setApiError('');
        try {
            await cambiarPassword(userId, formData.passwordActual, formData.passwordNueva);
            Swal.fire('¡Éxito!', 'Tu contraseña ha sido actualizada.', 'success');
            handleModalClose();
        } catch (error) {
            setApiError(error.response?.data?.message || 'Error al cambiar la contraseña.');
        } finally {
            setIsSubmitting(false);
        }
    };
    
    
    const handleModalClose = () => {
        setFormData({ passwordActual: '', passwordNueva: '', confirmarPassword: '' });
        setErrors({});
        setApiError('');
        handleClose();
    };

    return (
        <Modal show={show} onHide={handleModalClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Cambiar Contraseña</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    
                    <Form.Group className="mb-3">
                        <Form.Label>Contraseña Actual <span className="text-danger">*</span></Form.Label>
                        <div className="pass-group">
                            <Form.Control
                                type={showPass.actual ? 'text' : 'password'} name="passwordActual"
                                value={formData.passwordActual} onChange={handleChange} isInvalid={!!errors.passwordActual}
                            />
                            <span className="toggle-password" onClick={() => toggleShowPass('actual')}>
                                {showPass.actual ? <Eye /> : <EyeOff />}
                            </span>
                            <Form.Control.Feedback type="invalid">{errors.passwordActual}</Form.Control.Feedback>
                        </div>
                    </Form.Group>
                    
                    <Form.Group className="mb-3">
                        <Form.Label>Nueva Contraseña <span className="text-danger">*</span></Form.Label>
                        <div className="pass-group">
                            <Form.Control
                                type={showPass.nueva ? 'text' : 'password'} name="passwordNueva"
                                value={formData.passwordNueva} onChange={handleChange} isInvalid={!!errors.passwordNueva}
                            />
                            <span className="toggle-password" onClick={() => toggleShowPass('nueva')}>
                                {showPass.nueva ? <Eye /> : <EyeOff />}
                            </span>
                            <Form.Control.Feedback type="invalid">{errors.passwordNueva}</Form.Control.Feedback>
                        </div>
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Confirmar Nueva Contraseña <span className="text-danger">*</span></Form.Label>
                        <div className="pass-group">
                            <Form.Control
                                type={showPass.confirmar ? 'text' : 'password'} name="confirmarPassword"
                                value={formData.confirmarPassword} onChange={handleChange} isInvalid={!!errors.confirmarPassword}
                            />
                            <span className="toggle-password" onClick={() => toggleShowPass('confirmar')}>
                                {showPass.confirmar ? <Eye /> : <EyeOff />}
                            </span>
                            <Form.Control.Feedback type="invalid">{errors.confirmarPassword}</Form.Control.Feedback>
                        </div>
                    </Form.Group>

                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleModalClose} disabled={isSubmitting}>
                        Cancelar
                    </button>
                    <Button variant="primary" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? 'Actualizando...' : 'Actualizar Contraseña'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

CambiarPasswordModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    userId: PropTypes.number.isRequired,
};

export default CambiarPasswordModal;