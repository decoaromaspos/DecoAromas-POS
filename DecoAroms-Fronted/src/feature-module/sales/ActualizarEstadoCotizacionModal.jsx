import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Alert, Spinner } from 'react-bootstrap';
import Select from 'react-select';
import PropTypes from 'prop-types';
import Swal from 'sweetalert2';
import { actualizarEstadoCotizacion } from '../../services/cotizacionService';

// Opciones de estado para la cotización
const estadoOptions = [
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'RECHAZADA', label: 'Rechazada' },
];

// Función de utilidad para formatear moneda (para mostrar el total)
const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '$0';
    return new Intl.NumberFormat('es-CL', {
        style: 'currency',
        currency: 'CLP',
        minimumFractionDigits: 0,
    }).format(amount);
};

// Función para obtener la clase de la "badge" según el estado
const getEstadoBadgeClass = (estadoValue) => {
    switch (estadoValue) {
        case 'RECHAZADA':
            return 'badge bg-danger';
        case 'CONVERTIDA':
            return 'badge bg-success';
        case 'PENDIENTE':
        default:
            return 'badge bg-warning text-dark';
    }
};


const ActualizarEstadoCotizacionModal = ({ show, handleClose, cotizacion, onEstadoActualizado }) => {
    // Estado del formulario
    const [selectedEstado, setSelectedEstado] = useState(null);
    
    // Estado original (para comparar)
    const [originalEstado, setOriginalEstado] = useState(null);

    // Estado de la UI
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [apiError, setApiError] = useState(''); // Error al enviar
    
    // Cargar datos de la cotización cuando el modal se abre
    useEffect(() => {
        if (cotizacion) {
            // 1. Setear el Estado Actual
            const currentEstado = estadoOptions.find(opt => opt.value === cotizacion.estado);
            setSelectedEstado(currentEstado);
            setOriginalEstado(cotizacion.estado);
            
            // 2. Resetear estados de UI y errores
            setApiError('');
            setIsSubmitting(false);
        }
    }, [cotizacion, show]); // Se ejecuta cada vez que 'cotizacion' o 'show' cambian

    // Manejador del submit
    const handleSubmit = async (e) => {
        e.preventDefault();
        setApiError('');

        if (!selectedEstado) {
            setApiError('Debe seleccionar un nuevo estado.');
            return;
        }

        const nuevoEstado = selectedEstado.value;

        // Evitar API call si el estado no cambió
        if (nuevoEstado === originalEstado) {
            setApiError('El estado seleccionado es el mismo que el actual.');
            return;
        }

        // Regla de negocio: No se puede cambiar el estado de una cotización ya CONVERTIDA
        if (originalEstado === 'CONVERTIDA') {
            setApiError('Una cotización convertida en venta no puede cambiar su estado.');
            return;
        }
        
        setIsSubmitting(true);

        try {
            // Llamada a la API
            await actualizarEstadoCotizacion(cotizacion.cotizacionId, nuevoEstado);
            
            Swal.fire(
                '¡Estado Actualizado!', 
                `La cotización #${cotizacion.cotizacionId} ha sido actualizada a ${nuevoEstado}.`, 
                'success'
            );
            
            onEstadoActualizado(); // Llama al callback (para recargar la lista)
            handleClose(); // Cierra el modal

        } catch (error) {
            setApiError(error.response?.data?.message || 'Error al actualizar el estado.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!cotizacion) return null;

    // Determinar si el modal debe estar deshabilitado
    const isAlreadyConverted = originalEstado === 'CONVERTIDA';
    const noChange = selectedEstado && (selectedEstado.value === originalEstado);

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Actualizar Estado (Cotización #{cotizacion.cotizacionId})</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    
                    {isAlreadyConverted && (
                        <Alert variant="info">
                            Esta cotización ya fue convertida en venta y no puede ser modificada.
                        </Alert>
                    )}

                    <div className="mb-3">
                        <p className="mb-1">
                            Total Neto: <strong>{formatCurrency(cotizacion.totalNeto)}</strong>
                        </p>
                        <p className="mb-1">
                            Cliente: <strong>{cotizacion.clienteNombre || 'N/A'}</strong>
                        </p>
                        <p>
                            Estado Actual: <span className={getEstadoBadgeClass(originalEstado)}>
                                {originalEstado}
                            </span>
                        </p>
                    </div>

                    {/* Fila: Nuevo Estado */}
                    <Form.Group className="mb-3">
                        <Form.Label>Nuevo Estado <span className="text-danger">*</span></Form.Label>
                        <Select
                            classNamePrefix="select"
                            options={estadoOptions}
                            value={selectedEstado}
                            onChange={setSelectedEstado}
                            placeholder="Seleccionar estado..."
                            // Deshabilitar si ya está convertida
                            isDisabled={isSubmitting || isAlreadyConverted}
                        />
                    </Form.Group>

                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleClose} disabled={isSubmitting}>
                        Cancelar
                    </button>
                    <Button 
                        variant="primary" 
                        type="submit" 
                        disabled={
                            isSubmitting || 
                            isAlreadyConverted || // Deshabilitar si ya está convertida
                            noChange // Deshabilitar si no hay cambios
                        }
                    >
                        {isSubmitting ? (
                            <>
                                <Spinner
                                    as="span"
                                    animation="border"
                                    size="sm"
                                    role="status"
                                    aria-hidden="true"
                                />
                                <span className="ms-2">Guardando...</span>
                            </>
                        ) : 'Guardar Estado'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

// Prop Types para el modal
ActualizarEstadoCotizacionModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    cotizacion: PropTypes.object, // La cotización que se está editando
    onEstadoActualizado: PropTypes.func.isRequired, // Callback para refrescar la lista
};

export default ActualizarEstadoCotizacionModal;