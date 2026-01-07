import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Spinner } from 'react-bootstrap';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';
import { cerrarCaja, getResumenCajaById } from '../../services/cajaService';
import PropTypes from 'prop-types';

const MySwal = withReactContent(Swal);

const CloseCashboxModal = ({ show, onClose, onSuccess, cajaInfo }) => {
    const [efectivoContado, setEfectivoContado] = useState(''); // String para la máscara
    const [resumen, setResumen] = useState(null);
    const [loading, setLoading] = useState(false);
    const [loadingResumen, setLoadingResumen] = useState(false);

    // Formateador de moneda local
    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('es-CL', {
            style: 'currency',
            currency: 'CLP',
            minimumFractionDigits: 0
        }).format(amount || 0);
    };

    // Cargar resumen de ventas al abrir el modal
    const cargarResumen = useCallback(async () => {
        if (!cajaInfo?.cajaId) return;
        setLoadingResumen(true);
        try {
            const data = await getResumenCajaById(cajaInfo.cajaId);
            setResumen(data);
        } catch (error) {
            console.error("Error al obtener resumen:", error);
        } finally {
            setLoadingResumen(false);
        }
    }, [cajaInfo]);

    useEffect(() => {
        if (show) {
            setEfectivoContado('');
            cargarResumen();
        }
    }, [show, cargarResumen]);

    // Manejador de input con máscara chilena
    const handleInputChange = (e) => {
        let value = e.target.value.replace(/\D/g, ''); // Solo números
        if (value) {
            const formatted = parseInt(value).toLocaleString('es-CL');
            setEfectivoContado(`$${formatted}`);
        } else {
            setEfectivoContado('');
        }
    };

    const handleCerrarCaja = async () => {
        const valorLimpio = efectivoContado.replace(/\D/g, '');
        const monto = parseFloat(valorLimpio);

        if (isNaN(monto)) {
            MySwal.fire('Atención', 'Debe ingresar el monto de efectivo contado.', 'warning');
            return;
        }

        setLoading(true);
        try {
            const result = await cerrarCaja(monto); // El backend usa la caja abierta actual

            onClose();
            await onSuccess(); // Refresca la tabla

            // Mostrar resultado de cuadratura
            const diferencia = result.diferenciaReal;
            let msg = "Caja cerrada correctamente.";
            let icon = "success";

            if (diferencia > 0) {
                msg = `Cierre exitoso con SOBRANTE de ${formatCurrency(diferencia)}.`;
                icon = "warning";
            } else if (diferencia < 0) {
                msg = `Cierre exitoso con FALTANTE de ${formatCurrency(Math.abs(diferencia))}.`;
                icon = "error";
            }

            MySwal.fire('¡Caja Cerrada!', msg, icon);
        } catch (error) {
            const errorMessage = error.response?.data?.error || 'Error al cerrar la caja.';
            MySwal.fire('Error', errorMessage, 'error');
        } finally {
            setLoading(false);
        }
    };

    const apertura = cajaInfo?.efectivoApertura || 0;
    const netoVentas = resumen?.totalEfectivo || 0;
    const esperado = apertura + netoVentas;

    return (
        <Modal show={show} onHide={onClose} centered size="md">
            <Modal.Header closeButton className="bg-danger text-white">
                <Modal.Title>Cierre de Caja 🔒</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {loadingResumen ? (
                    <div className="text-center p-4">
                        <Spinner animation="border" variant="danger" />
                        <p className="mt-2">Calculando totales de venta...</p>
                    </div>
                ) : (
                    <>
                        <div className="text-start border rounded p-3 mb-4" style={{ backgroundColor: '#f8f9fa', color: '#333' }}>
                            <div className="d-flex justify-content-between mb-2">
                                <span style={{ color: '#555' }}>Apertura:</span>
                                <span className="fw-bold" style={{ color: '#000' }}>{formatCurrency(apertura)}</span>
                            </div>
                            <div className="d-flex justify-content-between mb-2">
                                <span style={{ color: '#555' }}>Ventas Efectivo (Neto):</span>
                                <span className="fw-bold" style={{ color: '#000' }}>{formatCurrency(netoVentas)}</span>
                            </div>
                            <hr style={{ borderTop: '1px solid #ccc', opacity: 1 }} />
                            <div className="d-flex justify-content-between fs-5">
                                <strong style={{ color: '#333' }}>Efectivo Esperado:</strong>
                                <strong className="text-primary">{formatCurrency(esperado)}</strong>
                            </div>
                        </div>

                        <div className="text-center mt-3">
                            <label className="form-label fw-bold text-dark">Efectivo Real en Caja (Físico):</label>
                            <input
                                type="text"
                                className="form-control form-control-lg text-center fw-bold"
                                value={efectivoContado}
                                onChange={handleInputChange}
                                placeholder="$0"
                                style={{
                                    fontSize: '1.8rem',
                                    color: '#198754',
                                    border: '2px solid #198754',
                                    backgroundColor: '#fff'
                                }}
                                disabled={loading}
                            />
                            <small className="text-muted d-block mt-2">
                                Ingrese el monto total de billetes y monedas contados.
                            </small>
                        </div>
                    </>
                )}
            </Modal.Body>
            <Modal.Footer>
                <button className="btn btn-cancel px-4" onClick={onClose} disabled={loading}>
                    Cancelar
                </button>
                <button className="btn btn-danger px-4" onClick={handleCerrarCaja} disabled={loading || loadingResumen}>
                    {loading ? 'Procesando...' : 'Confirmar Cierre'}
                </button>
            </Modal.Footer>
        </Modal>
    );
}

CloseCashboxModal.propTypes = {
    show: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onSuccess: PropTypes.func.isRequired,
    cajaInfo: PropTypes.shape({
        cajaId: PropTypes.number.isRequired,
        efectivoApertura: PropTypes.number.isRequired,
        nombreUsuario: PropTypes.string.isRequired,
    }).isRequired,
}

export default CloseCashboxModal;