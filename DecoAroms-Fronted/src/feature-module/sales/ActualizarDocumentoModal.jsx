import React, { useState, useEffect, useRef } from 'react';
import { Modal, Button, Form, Alert, InputGroup, Spinner } from 'react-bootstrap';
import Select from 'react-select';
import PropTypes from 'prop-types';
import Swal from 'sweetalert2';
import { actualizarDocumentoVenta, checkNumDocDisponible } from '../../services/ventaService'; // Asumiendo que ambos están en ventaService

const tipoDocumentoOptions = [
    { value: 'BOLETA', label: 'Boleta' },
    { value: 'FACTURA', label: 'Factura' },
];

const ActualizarDocumentoModal = ({ show, handleClose, venta, onDocumentoActualizado }) => {
    // Estado del formulario
    const [selectedTipo, setSelectedTipo] = useState(null);
    const [numero, setNumero] = useState(''); // Solo la parte numérica
    
    // Estado original (para comparar)
    const [originalNumeroCompleto, setOriginalNumeroCompleto] = useState(null);

    // Estado de la UI
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [apiError, setApiError] = useState(''); // Error al enviar
    const [formatError, setFormatError] = useState(''); // NUEVO: Error de formato
    
    // Estado de validación en tiempo real (basado en tu ejemplo)
    const [isValidating, setIsValidating] = useState(false);
    const [availabilityStatus, setAvailabilityStatus] = useState({
        checked: false, // Indica si la validación ya se ejecutó
        available: false, // true si está disponible, false si no
        message: ''       // Mensaje de la API
    });
    
    // Ref para el debounce de la validación
    const validationTimeoutRef = useRef(null);

    // Cargar datos de la venta cuando el modal se abre
    useEffect(() => {
        if (venta) {
            // 1. Setear el Tipo de Documento
            const currentTipo = tipoDocumentoOptions.find(opt => opt.value === venta.tipoDocumento);
            setSelectedTipo(currentTipo);

            // 2. Setear solo el número (quitar B o F) y guardar el original
            if (venta.numeroDocumento) {
                setNumero(venta.numeroDocumento.substring(1)); // Quita el primer caracter (B o F)
                setOriginalNumeroCompleto(venta.numeroDocumento); // Guarda el original
            } else {
                setNumero('');
                setOriginalNumeroCompleto(null); // El original era null
            }
            
            // 3. Resetear estados de UI y errores
            setApiError('');
            setFormatError(''); // Resetear error de formato
            setIsSubmitting(false);
            setIsValidating(false);
            setAvailabilityStatus({ checked: false, available: false, message: '' });
        }
    }, [venta]); // Depende de 'venta'

    // Efecto que escucha cambios en el número o tipo (para validar API)
    useEffect(() => {
        // Limpiar timeout anterior
        if (validationTimeoutRef.current) {
            clearTimeout(validationTimeoutRef.current);
        }

        const numDocCompleto = selectedTipo ? selectedTipo.value.charAt(0) + numero : null;

        // --- VALIDACIONES PREVIAS (GUARD CLAUSES) ---

        // 1. Si no hay tipo o número, o si hay error de formato, no validar API
        if (!selectedTipo || numero.trim() === '' || formatError) {
            setAvailabilityStatus({ checked: false, message: '' });
            setIsValidating(false);
            return;
        }

        // 2. Si el número es IDÉNTICO al original, no validar (es válido)
        if (numDocCompleto === originalNumeroCompleto) {
            setAvailabilityStatus({ checked: false, message: '' });
            setIsValidating(false);
            return;
        }

        // 3. (Se eliminó el chequeo de formato, ya que se hace en handleNumeroChange)

        // --- FIN DE VALIDACIONES PREVIAS ---

        // Si pasó las validaciones, iniciar debounce para la API
        setIsValidating(true); // Mostrar spinner
        setAvailabilityStatus({ checked: false, message: '' }); // Limpiar mensajes

        validationTimeoutRef.current = setTimeout(async () => {
            try {
                // Éxito (HTTP 200)
                const response = await checkNumDocDisponible(numDocCompleto);
                // response = { message: "...", available: true }
                setAvailabilityStatus({
                    checked: true,
                    available: response.available,
                    message: response.message
                });
            } catch (error) {
                // Error (HTTP 409 u otro)
                setAvailabilityStatus({
                    checked: true,
                    available: false,
                    message: error.response?.data?.message || "Error al validar."
                });
            } finally {
                setIsValidating(false); // Ocultar spinner
            }
        }, 500); // 500ms debounce

        // Limpiar el timeout al desmontar o si el input cambia
        return () => {
            if (validationTimeoutRef.current) {
                clearTimeout(validationTimeoutRef.current);
            }
        };
    }, [numero, selectedTipo, originalNumeroCompleto, formatError]); // Depende de estos 4


    // NUEVO HANDLER: para controlar la entrada de solo números
    const handleNumeroChange = (e) => {
        const value = e.target.value;

        // Regex: permite string vacío o solo números
        const regex = /^\d*$/; 

        if (regex.test(value)) {
            // Si es válido (número o vacío), actualiza el estado y limpia el error
            setNumero(value);
            setFormatError(''); 
        } else {
            // Si es inválido (contiene letras/símbolos),
            // NO actualiza el estado 'numero' (el input no cambia)
            // y MUESTRA el error de formato.
            setFormatError('Solo debe ingresar números.');
        }

        // Limpiar el error general de la API al escribir
        if (apiError) setApiError('');
        
        // Limpiar el estado de disponibilidad de la API (se recalculará en el useEffect)
        setAvailabilityStatus({ checked: false, message: '' });
    };


    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!selectedTipo) {
            setApiError('Debe seleccionar un tipo de documento.');
            return;
        }
        
        if (!numero || numero.trim() === '') {
            setApiError('Debe ingresar un número de documento.');
            return;
        }

        // Chequeo de error de formato
        if (formatError) {
            setApiError(formatError);
            return;
        }

        // Si la validación se ejecutó y el resultado fue "no disponible"
        if (availabilityStatus.checked && !availabilityStatus.available) {
             setApiError(availabilityStatus.message || "El número de documento no está disponible.");
             return;
        }
        
        setIsSubmitting(true);
        setApiError('');

        const numDocCompleto = selectedTipo.value.charAt(0) + numero;

        const dataDoc = {
            tipoDocumento: selectedTipo.value,
            numeroDocumento: numDocCompleto
        };

        try {
            await actualizarDocumentoVenta(venta.ventaId, dataDoc);
            
            Swal.fire(
                '¡Documento Actualizado!', 
                `Se asignó el documento ${numDocCompleto} a la Venta #${venta.ventaId}.`, 
                'success'
            );
            
            onDocumentoActualizado(); // Llama al callback (para recargar la lista)
            handleClose(); // Cierra el modal

        } catch (error) {
            setApiError(error.response?.data?.message || 'Error al actualizar el documento.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!venta) return null;

    const prefijo = selectedTipo ? selectedTipo.value.charAt(0) : '';

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Asignar Documento (Venta #{venta.ventaId})</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {apiError && <Alert variant="danger">{apiError}</Alert>}
                    
                    <p>
                        Total Neto: <strong>{formatCurrency(venta.totalNeto)}</strong>
                    </p>

                    {/* Fila 1: Tipo de Documento */}
                    <Form.Group className="mb-3">
                        <Form.Label>Tipo Documento <span className="text-danger">*</span></Form.Label>
                        <Select
                            classNamePrefix="select"
                            options={tipoDocumentoOptions}
                            value={selectedTipo}
                            onChange={setSelectedTipo}
                            placeholder="Seleccionar tipo..."
                        />
                    </Form.Group>

                    {/* Fila 2: Número de Documento con Prefijo */}
                    <Form.Group>
                        <Form.Label>Número Documento <span className="text-danger">*</span></Form.Label>
                        <InputGroup hasValidation>
                            {/* Prefijo (B o F) */}
                            {prefijo && (
                                <InputGroup.Text>{prefijo}</InputGroup.Text>
                            )}
                            
                            {/* Input solo para números */}
                            <Form.Control
                                type="text"
                                placeholder={selectedTipo ? "Ingrese solo números (ej: 12345)" : "Seleccione un tipo primero"}
                                value={numero}
                                // onChange={(e) => setNumero(e.target.value)} // Reemplazado
                                onChange={handleNumeroChange} // Usar el nuevo handler
                                disabled={!selectedTipo || isSubmitting}
                                // Se marca inválido si hay error de formato O si no está disponible
                                isInvalid={!!formatError || (availabilityStatus.checked && !availabilityStatus.available)}
                            />

                            {/* Spinner de Validación */}
                            {isValidating && (
                                <InputGroup.Text>
                                    <Spinner animation="border" size="sm" />
                                </InputGroup.Text>
                            )}
                        </InputGroup>
                        
                        {/* Mensaje de Éxito (VERDE) - Se muestra si no hay error de formato */}
                        {availabilityStatus.checked && availabilityStatus.available && !formatError && (
                            <Form.Text className='text-success'>
                                {availabilityStatus.message}
                            </Form.Text>
                        )}
                        
                        {/* Mensaje de Error de Formato (ROJO) - Tiene prioridad */}
                        {formatError && (
                            <Form.Text className='text-danger'>
                                {formatError}
                            </Form.Text>
                        )}
                        
                        {/* Mensaje de Error de Disponibilidad (ROJO) - Se muestra si no hay error de formato */}
                        {availabilityStatus.checked && !availabilityStatus.available && !formatError && (
                            <Form.Text className='text-danger'>
                                {availabilityStatus.message}
                            </Form.Text>
                        )}
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
                            isValidating || // Deshabilitar si está validando
                            !!formatError || // Deshabilitar si hay error de formato
                            (availabilityStatus.checked && !availabilityStatus.available) // Deshabilitar si no está disponible
                        }
                    >
                        {isSubmitting ? 'Guardando...' : 'Guardar Documento'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

// Prop Types para el modal
ActualizarDocumentoModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    venta: PropTypes.object, // La venta que se está editando
    onDocumentoActualizado: PropTypes.func.isRequired, // Callback para refrescar la lista
};

// Función de utilidad (si no la tienes global)
const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '$0';
    return new Intl.NumberFormat('es-CL', {
        style: 'currency',
        currency: 'CLP',
        minimumFractionDigits: 0,
    }).format(amount);
};

export default ActualizarDocumentoModal;