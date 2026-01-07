import React from 'react';
import { Modal, Table, OverlayTrigger, Tooltip } from 'react-bootstrap';
import PropTypes from 'prop-types';
import { Info, Clipboard, DollarSign, ShoppingCart } from 'feather-icons-react/build/IconComponents'; // Quitamos CreditCard

// Función de utilidad para formatear moneda chilena
const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '$0';
    return new Intl.NumberFormat('es-CL', {
        style: 'currency',
        currency: 'CLP',
        minimumFractionDigits: 0,
    }).format(amount);
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

const CotizacionDetailModal = ({ show, handleClose, cotizacion }) => {
    if (!cotizacion) return null;

    // La ganancia se calcula igual que en ventas
    const ganancia = (cotizacion.costoGeneral !== null && cotizacion.costoGeneral !== undefined)
        ? cotizacion.totalNeto - cotizacion.costoGeneral
        : null;

    // El descuento global se calcula igual
    const descuentoGlobalLabel = cotizacion.tipoDescuentoGlobal === 'PORCENTAJE' && cotizacion.valorDescuentoGlobal > 0
        ? `Descuento Global (${cotizacion.valorDescuentoGlobal}%):`
        : 'Descuento Global:';



    return (
        <Modal show={show} onHide={handleClose} size="xl" centered>
            <Modal.Header closeButton>
                <Modal.Title>Detalle de Cotización #{cotizacion.cotizacionId}</Modal.Title>
            </Modal.Header>
            <Modal.Body>

                {/* --- SECCIÓN DE INFORMACIÓN GENERAL (Adaptada) --- */}
                <div className="mb-4">
                    <h5 className="d-flex align-items-center">
                        <Clipboard size={20} className="me-2" /> Información General
                    </h5>
                    <div className="row mt-3">
                        <div className="col-md-6">
                            {/* Campo 'fechaEmision' en lugar de 'fecha' */}
                            <p className="mb-2"><strong>Fecha Emisión:</strong> {formatearFecha(cotizacion.fechaEmision)}</p>
                            <p className="mb-2"><strong>Tipo Cliente:</strong> {cotizacion.tipoCliente === 'DETALLE' ? 'Detalle' : 'Mayorista'}</p>
                            
                        </div>
                        <div className="col-md-6">
                            <p className="mb-2"><strong>Vendedor:</strong> {cotizacion.usuarioNombre}</p>
                            <p className="mb-2"><strong>Cliente:</strong> {cotizacion.clienteNombre || 'N/A'}</p>
                        </div>
                    </div>
                </div>

                {/* --- SECCIÓN PARA DETALLE DE PAGOS (Eliminada) --- */}
                {/* La cotización no tiene info de pagos ni vuelto */}


                {/* --- SECCIÓN DE TOTALES Y GANANCIA (Adaptada) --- */}
                <div className="mb-4">
                    <h5 className="d-flex align-items-center">
                        <DollarSign size={20} className="me-2" /> Totales y Ganancia
                    </h5>
                    <div className="row mt-3">
                        <div className="col-lg-6">
                            <p className='mb-1'><strong>Total Bruto:</strong> <span className="text-primary fw-bold">{formatCurrency(cotizacion.totalBruto)}</span></p>
                        </div>
                        <div className="col-lg-6">
                            <p className='mb-1'>
                                <strong>Costo General:</strong>
                                <span className="text-secondary fw-bold"> {formatCurrency(cotizacion.costoGeneral)}</span>
                                <OverlayTrigger placement="top" overlay={<Tooltip>Calculado en base a los costos de los productos cotizados.</Tooltip>}>
                                    <span className="ms-2" style={{ cursor: 'pointer' }}><Info size={16} /></span>
                                </OverlayTrigger>
                            </p>
                        </div>
                        <div className="col-lg-6">
                            <p className='mb-1'>
                                <strong>{descuentoGlobalLabel}</strong>
                                <span className="text-danger fw-bold"> -{formatCurrency(cotizacion.montoDescuentoGlobalCalculado)}</span>
                            </p>
                        </div>
                        <div className="col-lg-6"></div>
                        <div className="col-12"><hr className="my-2" /></div>
                        <div className="col-lg-6">
                            <p><strong>Total Neto (Cotización):</strong> <span className="text-success fw-bold fs-5">{formatCurrency(cotizacion.totalNeto)}</span></p>
                        </div>
                        <div className="col-lg-6">
                            <p><strong>Ganancia Estimada:</strong>
                                <span className="text-info fw-bold fs-5">
                                    {ganancia !== null ? formatCurrency(ganancia) : 'N/A'}
                                </span>
                            </p>
                        </div>
                    </div>
                </div>

                {/* --- SECCIÓN DE PRODUCTOS COTIZADOS (Adaptada) --- */}
                <div className="mb-0">
                    <h5 className="d-flex align-items-center">
                        <ShoppingCart size={20} className="me-2" /> Productos Cotizados ({cotizacion.detalles ? cotizacion.detalles.length : 0})
                    </h5>
                    <Table responsive striped bordered hover className="mt-2">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Cod. Barras</th>
                                <th>Producto</th>
                                <th className='text-end'>Cant.</th>
                                <th className='text-end'>P. Unitario</th>
                                <th className='text-end'>Desc. Unit.</th>
                                <th className='text-end'>Subtotal</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cotizacion.detalles && cotizacion.detalles.map((detalle, index) => {
                                let descuentoUnitarioDisplay = formatCurrency(0);
                                if (detalle.valorDescuentoUnitario > 0) {
                                    if (detalle.tipoDescuentoUnitario === 'PORCENTAJE') {
                                        descuentoUnitarioDisplay = `${detalle.valorDescuentoUnitario}%`;
                                    } else {
                                        descuentoUnitarioDisplay = formatCurrency(detalle.valorDescuentoUnitario);
                                    }
                                }
                                
                                return (
                                    // Usamos 'detalleCotizacionId' como key
                                    <tr key={detalle.detalleCotizacionId}>
                                        <td>{index + 1}</td>
                                        <td>{detalle.codigoBarras}</td>
                                        <td>{detalle.productoNombre}</td>
                                        <td className='text-end'>{detalle.cantidad}</td>
                                        <td className='text-end'>{formatCurrency(detalle.precioUnitario)}</td>
                                        <td className='text-end text-danger'>{descuentoUnitarioDisplay}</td>
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

CotizacionDetailModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    cotizacion: PropTypes.shape({
        cotizacionId: PropTypes.number,
        fechaEmision: PropTypes.string, // Campo actualizado
        tipoCliente: PropTypes.string,
        totalBruto: PropTypes.number,
        valorDescuentoGlobal: PropTypes.number,
        tipoDescuentoGlobal: PropTypes.string,
        montoDescuentoGlobalCalculado: PropTypes.number,
        totalNeto: PropTypes.number,
        costoGeneral: PropTypes.number,
        usuarioNombre: PropTypes.string,
        clienteNombre: PropTypes.string,
        detalles: PropTypes.arrayOf(PropTypes.shape({
            detalleCotizacionId: PropTypes.number, // Campo actualizado
            codigoBarras: PropTypes.string,
            productoNombre: PropTypes.string,
            cantidad: PropTypes.number,
            precioUnitario: PropTypes.number,
            valorDescuentoUnitario: PropTypes.number,
            tipoDescuentoUnitario: PropTypes.string,
            subtotal: PropTypes.number,
        })),
    }),
};

CotizacionDetailModal.defaultProps = {
    cotizacion: null,
};

export default CotizacionDetailModal;