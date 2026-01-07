import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Alert } from 'react-bootstrap';
import Select from 'react-select';
import PropTypes from 'prop-types';
import Swal from 'sweetalert2';
import { actualizarRolDeUsuario } from '../../../services/usuarioService';

const rolOptions = [
    { value: 'VENDEDOR', label: 'Vendedor' },
    { value: 'ADMIN', label: 'Administrador' },
];

const CambiarRolModal = ({ show, handleClose, usuario, onRolActualizado }) => {
    const [selectedRol, setSelectedRol] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [apiError, setApiError] = useState('');

    useEffect(() => {
        if (usuario) {
            const currentRol = rolOptions.find(opt => opt.value === usuario.rol);
            setSelectedRol(currentRol);
            setApiError('');
        }
    }, [usuario]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedRol) {
            setApiError('Debes seleccionar un rol.');
            return;
        }

        setIsSubmitting(true);
        setApiError('');

        try {
            await actualizarRolDeUsuario(usuario.usuarioId, selectedRol.value);
            Swal.fire('¡Rol Actualizado!', `El rol de ${usuario.nombreCompleto} ha sido cambiado a ${selectedRol.label}.`, 'success');
            onRolActualizado();
            handleClose();
        } catch (error) {
            setApiError(error.response?.data?.message || 'Error al actualizar el rol.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!usuario) return null;

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Cambiar Rol de Usuario</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    <p>
                        Estás modificando el rol de <strong>{usuario.nombreCompleto}</strong>.
                    </p>
                    <Form.Group>
                        <Form.Label>Nuevo Rol <span className="text-danger">*</span></Form.Label>
                        <Select
                            classNamePrefix="select"
                            options={rolOptions}
                            value={selectedRol}
                            onChange={setSelectedRol}
                            placeholder="Seleccionar rol"
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleClose} disabled={isSubmitting}>
                        Cancelar
                    </button>
                    <Button variant="primary" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? 'Guardando...' : 'Actualizar Rol'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

CambiarRolModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    usuario: PropTypes.object,
    onRolActualizado: PropTypes.func.isRequired,
};

export default CambiarRolModal;