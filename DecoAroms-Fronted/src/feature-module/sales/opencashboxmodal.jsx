import React, { useState, useEffect } from 'react';
import { Modal } from 'react-bootstrap';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';
import { abrirCaja } from '../../services/cajaService';
import PropTypes from 'prop-types';

const MySwal = withReactContent(Swal);

const OpenCashboxModal = ({ show, onClose, onSuccess, usuarioId }) => {
    // Estado como string para manejar la máscara ($1.000)
    const [montoDisplay, setMontoDisplay] = useState('');
    const [loading, setLoading] = useState(false);

    // Limpiar el campo cuando el modal se abre
    useEffect(() => {
        if (show) {
            setMontoDisplay('');
        }
    }, [show]);

    // Formateador dinámico (Estándar Chileno)
    const handleInputChange = (e) => {
        let value = e.target.value.replace(/\D/g, ''); // Eliminar no numéricos
        if (value) {
            const formatted = parseInt(value).toLocaleString('es-CL');
            setMontoDisplay(`$${formatted}`);
        } else {
            setMontoDisplay('');
        }
    };

    const handleAbrirCaja = async () => {
        // Limpiamos el formato para obtener el número puro
        const valorLimpio = montoDisplay.replace(/\D/g, '');
        const monto = parseFloat(valorLimpio);

        if (!valorLimpio || isNaN(monto)) {
            MySwal.fire('Atención', 'Debe ingresar un monto de apertura válido.', 'warning');
            return;
        }

        setLoading(true);
        try {
            await abrirCaja(usuarioId, monto);

            onClose();

            // Notificar éxito y refrescar
            await onSuccess();

            MySwal.fire({
                title: '¡Caja Abierta!',
                text: `Turno iniciado con un efectivo de $${monto.toLocaleString('es-CL')}`,
                icon: 'success',
                confirmButtonColor: '#198754'
            });
        } catch (error) {
            console.error("Error al abrir la caja:", error);
            const errorMessage = error.response?.data?.error || 'No se pudo abrir la caja. Verifique si ya hay una caja abierta.';
            MySwal.fire('Error', errorMessage, 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        if (!loading) onClose();
    };

    return (
        <Modal show={show} onHide={handleClose} centered size="md">
            {/* Header con color Verde para Apertura */}
            <Modal.Header closeButton className="bg-success text-white">
                <Modal.Title style={{ color: '#fff !important' }}>Abrir Caja 💰</Modal.Title>
            </Modal.Header>

            <Modal.Body className="p-4">
                <div className="text-center mb-4">
                    <p style={{ color: '#333 !important', fontSize: '1.1rem' }}>
                        Ingrese el monto de efectivo disponible en la gaveta para iniciar las operaciones.
                    </p>
                </div>

                <div className="input-block text-center">
                    <label className="form-label fw-bold" style={{ color: '#555 !important' }}>
                        Efectivo de Apertura
                    </label>
                    <input
                        type="text"
                        className="form-control form-control-lg text-center fw-bold"
                        value={montoDisplay}
                        onChange={handleInputChange}
                        placeholder="$0"
                        disabled={loading}
                        style={{
                            fontSize: '2.2rem',
                            color: '#198754 !important',
                            border: '2px solid #198754 !important',
                            backgroundColor: '#fff !important',
                            height: '80px'
                        }}
                    />
                    <small className="text-muted d-block mt-3">
                        Este valor será el saldo inicial para el cálculo de arqueo al final del turno de ventas.
                    </small>
                </div>
            </Modal.Body>

            <Modal.Footer className="border-0 pb-4">
                <button
                    className="btn btn-cancel px-4"
                    onClick={handleClose}
                    disabled={loading}
                    style={{ minWidth: '120px' }}
                >
                    Cancelar
                </button>
                <button
                    className="btn btn-success px-4 fw-bold"
                    onClick={handleAbrirCaja}
                    disabled={loading}
                    style={{ minWidth: '160px', backgroundColor: '#198754' }}
                >
                    {loading ? 'Abriendo...' : 'Abrir Caja'}
                </button>
            </Modal.Footer>
        </Modal>
    );
}

OpenCashboxModal.propTypes = {
    show: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onSuccess: PropTypes.func.isRequired,
    usuarioId: PropTypes.number.isRequired,
};

export default OpenCashboxModal;