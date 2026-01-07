import React, { useState, useEffect, useCallback } from "react";
import PropTypes from 'prop-types';
import { getResumenCajaById } from '../../services/cajaService';
import { useAuth } from "../../context/AuthContext";
import { MEDIOS_PAGO_MAP } from "../../utils/medioPago";

// Constante para definir las etiquetas de los medios de pago
const PAYMENT_METHODS = {
    totalEfectivo: MEDIOS_PAGO_MAP.EFECTIVO.label,
    totalMercadoPago: MEDIOS_PAGO_MAP.MERCADO_PAGO.label,
    totalBCI: MEDIOS_PAGO_MAP.BCI.label,
    totalBotonDePago: MEDIOS_PAGO_MAP.BOTON_DE_PAGO.label,
    totalTransferencia: MEDIOS_PAGO_MAP.TRANSFERENCIA.label,
    totalPost: MEDIOS_PAGO_MAP.POST.label,
};

// Definición de las etiquetas y el orden para la sección de cierre
const CIERRE_METHODS_ORDERED = [
    { key: 'postCierre', label: MEDIOS_PAGO_MAP.POST.label },
    { key: 'bciCierre', label: 'BCI' },
    { key: 'mercadoPagoCierre', label: 'Mercado Pago' },
    { key: 'botonDePagoCierre', label: 'Botón de Pago' },
    { key: 'transferenciaCierre', label: 'Transferencia' },
    { key: 'efectivoCierre', label: 'Efectivo Reportado' },
];

// Función de formato de moneda (ajustada para el formato chileno/local)
const formatCurrency = (amount, zeroMessage = "N/A") => {
    if (amount === null || amount === undefined || (typeof amount === 'number' && isNaN(amount))) return zeroMessage;

    const numericAmount = Number(amount);
    if (numericAmount === 0 && zeroMessage !== "N/A") return zeroMessage;

    return new Intl.NumberFormat('es-CL', {
        style: 'currency',
        currency: 'CLP',
        minimumFractionDigits: numericAmount % 1 === 0 ? 0 : 2,
        maximumFractionDigits: 2,
    }).format(numericAmount);
};

const formatDate = (isoDate) => {
    if (!isoDate) return "N/A";
    try {
        // Aseguramos que se analiza correctamente con la zona horaria
        const date = new Date(isoDate);
        return date.toLocaleString('es-CL', {
            year: 'numeric',
            month: 'short',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        });
    } catch (e) {
        return "Fecha Inválida";
    }
};


const CashboxViewModal = ({ show, handleClose, caja, onOpenCloseModal }) => {
    // Estado para el resumen de ventas en tiempo real (solo si la caja está ABIERTA)
    const [resumenVentas, setResumenVentas] = useState(null);
    const [loadingResumen, setLoadingResumen] = useState(false);
    const { usuario } = useAuth();
    const isSuperAdmin = usuario && usuario.rol === 'SUPER_ADMIN';

    // Cargar el resumen de ventas solo si la caja está abierta
    const loadResumen = useCallback(async (cajaId) => {
        if (!cajaId || caja?.estado !== "ABIERTA") {
            setResumenVentas(null);
            return;
        }

        setLoadingResumen(true);
        try {
            const resumen = await getResumenCajaById(cajaId);
            setResumenVentas(resumen);
        } catch (error) {
            console.error("Error al obtener resumen de caja:", error);
            setResumenVentas(null);
        } finally {
            setLoadingResumen(false);
        }
    }, [caja]);


    // Efecto para cargar el resumen cada vez que el modal se muestra o cambia la caja (cajaId)
    useEffect(() => {
        if (show && caja && caja.cajaId) {
            loadResumen(caja.cajaId);
            // Podrías añadir un setInterval aquí si quieres que se actualice cada X segundos
        }
    }, [show, caja, loadResumen]);


    // Si no hay información de caja, no renderiza el contenido del modal
    if (!caja) return null;

    const modalId = `view-cashbox-${caja.cajaId}`;
    const isCerrada = caja.estado === "CERRADA";
    const isCuadrada = isCerrada && caja.diferenciaReal === 0;

    // --- LÓGICA DE VISUALIZACIÓN DE CUADRATURA ---
    const renderCuadratura = () => {
        if (!isCerrada) {
            return <p className="text-muted">Pendiente de Cierre</p>;
        }

        if (isCuadrada) {
            return <p className="fs-5 fw-bold text-success">Sí, Cuadrada ✅</p>;
        } else {
            return (
                <p className="fs-5 fw-bold text-danger">
                    No ❌ (Diferencia: {formatCurrency(caja.diferenciaReal)})
                </p>
            );
        }
    };

    // --- MAPEO DE MEDIOS DE PAGO (para caja cerrada) ---
    const renderCierreMediosPago = () => {

        if (!isCerrada) return <p className="text-muted">Los totales se mostrarán al cerrar la caja.</p>;

        // Usamos el array ordenado para generar el JSX
        return CIERRE_METHODS_ORDERED.map(method => {
            const value = caja[method.key] !== undefined && caja[method.key] !== null
                ? caja[method.key]
                : 0; // Usar 0.0 si el valor es nulo/indefinido

            return (
                <div className="col-lg-6 col-md-6 mb-3" key={method.key}>
                    <label className="form-label mb-0 fw-bold">{method.label} (Cierre)</label>
                    <p className="fs-6 text-dark">{formatCurrency(value)}</p>
                </div>
            );
        });
    };

    // --- MAPEO DE RESUMEN EN TIEMPO REAL (para caja abierta) ---
    const renderResumenVentas = () => {
        if (!resumenVentas) return null;

        return (
            <div className="row mt-3 p-3 border rounded" style={{ backgroundColor: '#f9f9f9' }}>
                <div className="col-12">
                    <h6 className="mb-3 text-warning">Resumen de Ventas (Tiempo Real) 🔄</h6>
                </div>
                {Object.entries(PAYMENT_METHODS).map(([key, label]) => (
                    <div className="col-lg-6 col-md-6 mb-3" key={key}>
                        <label className="form-label mb-0 fw-bold">{label}</label>
                        <p className="fs-5 text-dark">{formatCurrency(resumenVentas[key] || 0)}</p>
                    </div>
                ))}
            </div>
        );
    };

    return (
        <>
            <div
                className={`modal fade ${show ? 'show d-block' : ''}`}
                id={modalId}
                tabIndex="-1"
                aria-labelledby={`${modalId}Label`}
                aria-hidden={!show}
                style={{
                    display: show ? 'block' : 'none',
                    zIndex: 1055
                }}
            >
                <div className="modal-dialog modal-dialog-centered modal-lg custom-modal-two">
                    <div className="modal-content">
                        <div className="page-wrapper-new p-0">
                            <div className="content">
                                {/* === HEADER === */}
                                <div className="modal-header border-0 custom-modal-header">
                                    <div className="page-title">
                                        <h4>Detalle de Caja 🏦</h4>
                                        <small className="text-muted">
                                            Caja ID: {caja.cajaId} | Por: {caja.nombreUsuario}
                                        </small>
                                    </div>
                                    {/* Botón de cerrar con estilos (Manteniendo tu estilo) */}
                                    <button
                                        type="button"
                                        className="btn-close"
                                        onClick={handleClose}
                                        aria-label="Close"
                                        style={{
                                            borderRadius: '50%',
                                            transition: 'all 0.2s ease-in-out',
                                            background: 'transparent url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e") center/1em auto no-repeat',
                                            opacity: '0.7'
                                        }}
                                        onMouseEnter={(e) => {
                                            e.target.style.backgroundColor = '#FF4D4D';
                                            e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%23ffffff%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")';
                                            e.target.style.opacity = '1';
                                        }}
                                        onMouseLeave={(e) => {
                                            e.target.style.backgroundColor = 'transparent';
                                            e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")';
                                            e.target.style.opacity = '0.7';
                                        }}
                                    ></button>
                                </div>

                                {/* === BODY === */}
                                <div className="modal-body custom-modal-body">
                                    <div className="row">
                                        {/* Bloque 1: Tiempos y Estado */}
                                        <div className="col-lg-6 col-md-6 col-sm-12">
                                            <h6 className="mb-3 text-primary">Información General</h6>
                                            <div className="row">
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Usuario</label>
                                                    <p>{caja.nombreUsuario} ({caja.username})</p>
                                                </div>
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Estado</label>
                                                    <p>
                                                        <span className={`badge ${isCerrada ? "bg-secondary" : "bg-success"}`}>
                                                            {caja.estado}
                                                        </span>
                                                    </p>
                                                </div>
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Apertura</label>
                                                    <p>{formatDate(caja.fechaApertura)}</p>
                                                </div>
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Cierre</label>
                                                    <p className={isCerrada ? "" : "text-muted"}>
                                                        {isCerrada ? formatDate(caja.fechaCierre) : "Pendiente"}
                                                    </p>
                                                </div>
                                            </div>
                                        </div>

                                        {/* Bloque 2: Efectivo y Cuadratura */}
                                        <div className="col-lg-6 col-md-6 col-sm-12">
                                            <h6 className="mb-3 text-primary">Detalles Financieros</h6>
                                            <div className="row">
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Efectivo de Apertura</label>
                                                    <p className="fs-5 text-dark">{formatCurrency(caja.efectivoApertura)}</p>
                                                </div>
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Efectivo de Cierre</label>
                                                    <p className={`fs-5 ${isCerrada ? "text-dark" : "text-muted"}`}>
                                                        {isCerrada ? formatCurrency(caja.efectivoCierre) : "N/A (Caja Abierta)"}
                                                    </p>
                                                </div>
                                                <div className="col-12 mb-3">
                                                    <label className="form-label mb-0 fw-bold">Cuadratura</label>
                                                    {renderCuadratura()}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Bloque 3: Medios de Pago (Cambia si la caja está abierta o cerrada) */}
                                        <div className="col-lg-12 col-md-12 col-sm-12 mt-3">
                                            <h6 className="mb-3 text-primary">Totales de Pago</h6>
                                            <div className="row">
                                                {isCerrada ? (
                                                    renderCierreMediosPago()
                                                ) : loadingResumen ? (
                                                    <div className="text-center p-4">Cargando resumen de ventas en tiempo real...</div>
                                                ) : (
                                                    renderResumenVentas()
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* === FOOTER - CON BOTÓN CERRAR/CANCELAR === */}
                                <div className="modal-footer custom-modal-footer">
                                    <button
                                        type="button"
                                        className="btn btn-cancel" // Estilo de "Cancelar"
                                        onClick={handleClose}
                                    >
                                        Cerrar
                                    </button>
                                    {/* Botón opcional para acción rápida, por ejemplo, Cerrar Caja si está abierta */}
                                    {!isCerrada && !isSuperAdmin && (
                                        <button
                                            type="button"
                                            className="btn btn-danger"
                                            onClick={() => {
                                                handleClose(); // Primero cerramos el modal de vista
                                                onOpenCloseModal(caja); // Disparamos la acción en el padre
                                            }}
                                        >
                                            Cerrar Caja
                                        </button>
                                    )}
                                </div>

                            </div>
                        </div>
                    </div>
                </div>
            </div>
            {show && <div className="modal-backdrop fade show"></div>}
        </>
    );
};

CashboxViewModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    onOpenCloseModal: PropTypes.func.isRequired,
    caja: PropTypes.shape({
        cajaId: PropTypes.number.isRequired,
        fechaApertura: PropTypes.string.isRequired,
        efectivoApertura: PropTypes.number.isRequired,
        fechaCierre: PropTypes.string,
        efectivoCierre: PropTypes.number,
        mercadoPagoCierre: PropTypes.number,
        bciCierre: PropTypes.number,
        botonDePagoCierre: PropTypes.number,
        transferenciaCierre: PropTypes.number,
        postCierre: PropTypes.number,
        estado: PropTypes.oneOf(['ABIERTA', 'CERRADA']).isRequired,
        diferenciaReal: PropTypes.number,
        usuarioId: PropTypes.number.isRequired,
        nombreUsuario: PropTypes.string.isRequired,
        username: PropTypes.string.isRequired,
    })
};

export default CashboxViewModal;