import React from 'react';
import { Modal, Table, OverlayTrigger, Tooltip } from 'react-bootstrap';
import PropTypes from 'prop-types';
import { Info, Clipboard, DollarSign, ShoppingCart, CreditCard } from 'feather-icons-react/build/IconComponents';
import { MEDIOS_PAGO_MAP } from '../../utils/medioPago';

// Función de utilidad para formatear moneda chilena
const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '$0';

    const options = {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    };

    // Formatear el valor absoluto con separadores de miles
    const absoluteAmount = Math.abs(amount);
    const formatted = new Intl.NumberFormat('es-CL', options).format(absoluteAmount);

    // Agregar manualmente el signo y el símbolo de moneda
    if (amount < 0) {
        return `-$${formatted}`; // Resultado: "-$1
    } else {
        return `$${formatted}`; // Resultado: "$1"
    }
};

// Función para formatear la fecha
const formatearFecha = (fechaISO) => {
    if (!fechaISO) return '-';
    const fecha = new Date(fechaISO);
    return fecha.toLocaleDateString('es-CL') + ' ' + fecha.toLocaleTimeString('es-CL', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

// Función para formatear el nombre del medio de pago
const formatPagoLabel = (pagoEnum) => {
    if (!pagoEnum) return 'N/A';
    // Buscamos en el mapa. Si existe, devolvemos el label, si no, devolvemos el enum original
    return MEDIOS_PAGO_MAP[pagoEnum] ? MEDIOS_PAGO_MAP[pagoEnum].label : pagoEnum;
};

const SaleDetailModal = ({ show, handleClose, venta }) => {
    if (!venta) return null;

    const ganancia = (venta.costoGeneral !== null && venta.costoGeneral !== undefined)
        ? venta.totalNeto - venta.costoGeneral
        : null;

    // Se mantiene esta constante, es útil para el desglose del efectivo
    const hayVuelto = venta.vuelto > 0;

    // --- NUEVA CONSTANTE PARA MOSTRAR VUELTO (INCLUYENDO 0) ---
    const vueltoEsValido = venta.vuelto !== null && venta.vuelto !== undefined;


    return (
        <Modal show={show} onHide={handleClose} size="xl" centered>
            <Modal.Header closeButton>
                <Modal.Title>Detalle de Venta #{venta.ventaId}</Modal.Title>
            </Modal.Header>
            <Modal.Body>

                {/* --- SECCIÓN DE INFORMACIÓN GENERAL (Sin cambios) --- */}
                <div className="mb-4">
                    <h5 className="d-flex align-items-center">
                        <Clipboard size={20} className="me-2" /> Información General
                    </h5>
                    <div className="row mt-3">
                        <div className="col-md-6">
                            <p className="mb-2"><strong>Fecha:</strong> {formatearFecha(venta.fecha)}</p>
                            <p className="mb-2"><strong>Tipo Cliente:</strong> {venta.tipoCliente === 'DETALLE' ? 'Detalle' : 'Mayorista'}</p>

                            <p className="mb-2">
                                <strong>Tipo Documento:</strong>
                                <span className="ms-2">{venta.tipoDocumento.charAt(0).toUpperCase() + venta.tipoDocumento.slice(1).toLowerCase()}</span>
                            </p>
                            <p className="mb-2">
                                <strong>Número Documento:</strong>
                                {venta.numeroDocumento ? (
                                    <span className="fw-bold ms-2">{venta.numeroDocumento}</span>
                                ) : (
                                    <span className="badge bg-warning text-dark ms-2">(pendiente de asignación)</span>
                                )}
                            </p>

                        </div>
                        <div className="col-md-6">
                            <p className="mb-2"><strong>Vendedor:</strong> {venta.usuarioNombre}</p>
                            <p className="mb-2"><strong>Cliente:</strong> {venta.clienteNombre || 'N/A'}</p>
                        </div>
                    </div>
                </div>

                {/* --- SECCIÓN PARA DETALLE DE PAGOS (Sin cambios) --- */}
                <div className="mb-4">
                    <h5 className="d-flex align-items-center">
                        <CreditCard size={20} className="me-2" /> Detalle de Pagos
                    </h5>
                    <div className="row mt-3">
                        <div className="col-md-6">
                            {venta.pagos && venta.pagos.map((pago, index) => (
                                <p key={index} className="mb-2">
                                    <strong>{formatPagoLabel(pago.medioPago)}:</strong>
                                    <span className="fw-bold ms-2">{formatCurrency(pago.monto)}</span>

                                    {pago.medioPago === 'EFECTIVO' && hayVuelto && (
                                        <span className="text-muted ms-2" style={{ fontSize: '0.85rem' }}>
                                            (Cubre: {formatCurrency(pago.monto - venta.vuelto)})
                                        </span>
                                    )}
                                </p>
                            ))}
                        </div>
                        <div className="col-md-6">
                            {vueltoEsValido && (
                                <p className="mb-2">
                                    <strong>Vuelto Entregado:</strong>
                                    <span className={`fw-bold fs-5 ms-2 ${venta.vuelto > 0 ? 'text-success' : 'text-dark'}`}>
                                        {formatCurrency(venta.vuelto)}
                                    </span>
                                </p>
                            )}
                        </div>
                    </div>
                </div>

                {/* --- SECCIÓN DE TOTALES Y GANANCIA (REESTRUCTURADA) --- */}
                <div className="mb-4">
                    <h5 className="d-flex align-items-center">
                        <DollarSign size={20} className="me-2" /> Totales y Ganancia
                    </h5>
                    <div className="row mt-3">

                        {/* Columna de Totales de Venta */}
                        <div className="col-lg-6">
                            <p className='mb-1 d-flex justify-content-between'>
                                <strong>Total Bruto:</strong>
                                <span className="text-primary fw-bold">{formatCurrency(venta.totalBruto)}</span>
                            </p>

                            {/* Descuentos Unitarios (NUEVO) */}
                            {venta.totalDescuentosUnitarios > 0 && (
                                <p className='mb-1 d-flex justify-content-between'>
                                    <strong>Descuentos Unitarios:</strong>
                                    <span className="text-danger fw-bold">{formatCurrency(venta.totalDescuentosUnitarios * -1)}</span>
                                </p>
                            )}

                            {/* Descuento Global */}
                            {venta.montoDescuentoGlobalCalculado > 0 && (
                                <p className='mb-1 d-flex justify-content-between'>
                                    <strong>
                                        {venta.tipoDescuentoGlobal === 'PORCENTAJE' && venta.valorDescuentoGlobal > 0
                                            ? `Descuento Global (${venta.valorDescuentoGlobal}%)`
                                            : 'Descuento Global:'}
                                    </strong>
                                    <span className="text-danger fw-bold">{formatCurrency(venta.montoDescuentoGlobalCalculado * -1)}</span>
                                </p>
                            )}

                            <hr className="my-2" />

                            <p className='d-flex justify-content-between'>
                                <strong>Total (Venta):</strong>
                                <span className="text-success fw-bold fs-5">{formatCurrency(venta.totalNeto)}</span>
                            </p>
                        </div>

                        {/* Columna de Ganancia */}
                        <div className="col-lg-6">
                            <p className='mb-1 d-flex justify-content-between'>
                                <strong>
                                    Costo General:
                                    <OverlayTrigger placement="top" overlay={<Tooltip>Calculado en base a los costos de los productos vendidos.</Tooltip>}>
                                        <span className="ms-2" style={{ cursor: 'pointer' }}><Info size={16} /></span>
                                    </OverlayTrigger>
                                </strong>
                                <span className="text-secondary fw-bold"> {formatCurrency(venta.costoGeneral)}</span>
                            </p>

                            <hr className="my-2" />

                            <p className='d-flex justify-content-between'>
                                <strong>Ganancia Estimada:</strong>
                                <span className="text-info fw-bold fs-5">
                                    {ganancia !== null ? formatCurrency(ganancia) : 'N/A'}
                                </span>
                            </p>
                        </div>
                    </div>
                </div>


                {/* --- SECCIÓN DE PRODUCTOS VENDIDOS (TABLA ACTUALIZADA) --- */}
                <div className="mb-0">
                    <h5 className="d-flex align-items-center">
                        <ShoppingCart size={20} className="me-2" /> Productos Vendidos ({venta.detalles ? venta.detalles.length : 0})
                    </h5>
                    <Table responsive striped bordered hover className="mt-2" style={{ verticalAlign: 'middle' }}>
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Cod. Barras</th>
                                <th>Producto</th>
                                <th className='text-end'>Cant.</th>
                                <th className='text-end'>P. Unitario</th>
                                <th className='text-end'>Subtotal Bruto</th>
                                <th className='text-end'>Descuento</th>
                                <th className='text-end'>Monto Final</th>
                            </tr>
                        </thead>
                        <tbody>
                            {venta.detalles && venta.detalles.map((detalle, index) => {

                                // Tooltip para mostrar el descuento original (ej: "10% / unidad")
                                let descuentoTooltip = "Sin descuento";
                                if (detalle.valorDescuentoUnitario > 0) {
                                    if (detalle.tipoDescuentoUnitario === 'PORCENTAJE') {
                                        descuentoTooltip = `${detalle.valorDescuentoUnitario}% por unidad`;
                                    } else {
                                        descuentoTooltip = `${formatCurrency(detalle.valorDescuentoUnitario)} por unidad`;
                                    }
                                }

                                return (
                                    <tr key={detalle.detalleId}>
                                        <td>{index + 1}</td>
                                        <td>{detalle.codigoBarras}</td>
                                        <td>{detalle.productoNombre}</td>
                                        <td className='text-end'>{detalle.cantidad}</td>
                                        <td className='text-end'>{formatCurrency(detalle.precioUnitario)}</td>

                                        {/* --- CELDAS ACTUALIZADAS --- */}

                                        {/* Muestra (Cant * P.Unit) */}
                                        <td className='text-end'>{formatCurrency(detalle.subtotalBruto)}</td>

                                        {/* Muestra el descuento total calculado para la línea */}
                                        <td className='text-end text-danger'>
                                            <OverlayTrigger placement="top" overlay={<Tooltip id={`tooltip-dcto-${detalle.detalleId}`}>{descuentoTooltip}</Tooltip>}>
                                                <span>
                                                    {detalle.montoDescuentoUnitarioCalculado > 0
                                                        ? formatCurrency(detalle.montoDescuentoUnitarioCalculado * -1)
                                                        : formatCurrency(0)
                                                    }
                                                </span>
                                            </OverlayTrigger>
                                        </td>

                                        {/* Muestra el monto final (SubtotalBruto - Descuento) */}
                                        <td className='text-end fw-bold'>{formatCurrency(detalle.subtotal)}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </Table>
                </div>

            </Modal.Body>
            <Modal.Footer>
                <button type="button" className="btn btn-cancel" onClick={handleClose}>
                    Cerrar
                </button>
            </Modal.Footer>
        </Modal>
    );
};

SaleDetailModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    venta: PropTypes.shape({
        ventaId: PropTypes.number,
        fecha: PropTypes.string,
        tipoCliente: PropTypes.string,
        totalBruto: PropTypes.number,
        valorDescuentoGlobal: PropTypes.number,
        tipoDescuentoGlobal: PropTypes.string,
        montoDescuentoGlobalCalculado: PropTypes.number,
        totalDescuentosUnitarios: PropTypes.number,
        totalDescuentoTotal: PropTypes.number,
        totalNeto: PropTypes.number,
        costoGeneral: PropTypes.number,
        tipoDocumento: PropTypes.string,
        numeroDocumento: PropTypes.string,
        usuarioNombre: PropTypes.string,
        clienteNombre: PropTypes.string,
        vuelto: PropTypes.number,
        detalles: PropTypes.arrayOf(PropTypes.shape({
            detalleId: PropTypes.number,
            codigoBarras: PropTypes.string,
            productoNombre: PropTypes.string,
            cantidad: PropTypes.number,
            precioUnitario: PropTypes.number,
            valorDescuentoUnitario: PropTypes.number,
            tipoDescuentoUnitario: PropTypes.string,
            subtotalBruto: PropTypes.number,
            montoDescuentoUnitarioCalculado: PropTypes.number,
            subtotal: PropTypes.number,
        })),
        pagos: PropTypes.arrayOf(PropTypes.shape({
            medioPago: PropTypes.string,
            monto: PropTypes.number
        })),
    }),
};

SaleDetailModal.defaultProps = {
    venta: null,
};

export default SaleDetailModal;