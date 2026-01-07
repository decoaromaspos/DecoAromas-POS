import React, { useState } from "react";
import { Modal, Button, Form, Alert } from "react-bootstrap";
import Select from "react-select";
import PropTypes from 'prop-types';
import { exportarVentasCSV } from "../../services/ventaService";
import Swal from "sweetalert2";
import { MEDIOS_PAGO } from "../../utils/medioPago";

const ExportSaleCsvModal = ({ show, handleClose, usuarios }) => {
    const [filtros, setFiltros] = useState({
        fechaInicio: '',
        fechaFin: '',
        tipoCliente: null,
        medioPago: null,
        usuario: null
    });
    const [isExporting, setIsExporting] = useState(false);
    const [errorLocal, setErrorLocal] = useState(null);


    const setRangoRapido = (dias) => {
        const fin = new Date();
        const inicio = new Date();
        if (dias > 0) {
            inicio.setDate(fin.getDate() - dias);
            setFiltros({
                ...filtros,
                fechaInicio: inicio.toISOString().split('T')[0],
                fechaFin: fin.toISOString().split('T')[0]
            });
        } else {
            setFiltros({ ...filtros, fechaInicio: '', fechaFin: '' });
        }
        setErrorLocal(null);
    };

    const handleExport = async () => {
        // --- VALIDACIÓN PARA EVITAR BLOQUEO ---
        if (filtros.fechaInicio && filtros.fechaFin) {
            const inicio = new Date(filtros.fechaInicio);
            const fin = new Date(filtros.fechaFin);
            const diffTime = Math.abs(fin - inicio);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

            if (diffDays > 730) { // 2 años
                setErrorLocal("El rango de fechas es demasiado extenso (máx. 2 años) para evitar sobrecarga del sistema.");
                return;
            }
        }

        setIsExporting(true);
        setErrorLocal(null);

        try {
            const params = {
                fechaInicio: filtros.fechaInicio ? `${filtros.fechaInicio}T00:00:00Z` : null,
                fechaFin: filtros.fechaFin ? `${filtros.fechaFin}T23:59:59Z` : null,
                tipoCliente: filtros.tipoCliente?.value,
                usuarioId: filtros.usuario?.value,
                medioPago: filtros.medioPago?.value
            };

            await exportarVentasCSV(params);

            Swal.fire({
                title: "Descarga iniciada",
                text: "El archivo se está generando. Por favor, espere.",
                icon: "success"
            });
            handleClose();
        } catch (error) {
            // Si el error es 401 o 403, el interceptor de Axios redirigirá al login
            Swal.fire("Error", "Ocurrió un problema al generar el archivo. Verifique su conexión.", "error");
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <Modal show={show} onHide={handleClose} centered size="lg">
            <Modal.Header closeButton>
                <Modal.Title>Exportar Reporte de Ventas a CSV</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {errorLocal && <Alert variant="warning">{errorLocal}</Alert>}

                <div className="mb-4">
                    <label className="form-label">Selección Rápida:</label>
                    <div className="d-flex gap-2 flex-wrap">
                        <Button variant="outline-primary" size="sm" onClick={() => setRangoRapido(30)}>30 días</Button>
                        <Button variant="outline-primary" size="sm" onClick={() => setRangoRapido(90)}>90 días</Button>
                        <Button variant="outline-secondary" size="sm" onClick={() => setRangoRapido(0)}>Todo el historial</Button>
                    </div>
                </div>

                <div className="row">
                    <Form.Group className="col-md-6 mb-3">
                        <Form.Label>Fecha Inicio</Form.Label>
                        <Form.Control
                            type="date"
                            value={filtros.fechaInicio}
                            onChange={(e) => setFiltros({ ...filtros, fechaInicio: e.target.value })}
                        />
                    </Form.Group>
                    <Form.Group className="col-md-6 mb-3">
                        <Form.Label>Fecha Fin</Form.Label>
                        <Form.Control
                            type="date"
                            value={filtros.fechaFin}
                            onChange={(e) => setFiltros({ ...filtros, fechaFin: e.target.value })}
                        />
                    </Form.Group>
                </div>

                <div className="row">
                    <Form.Group className="col-md-4 mb-3">
                        <Form.Label>Vendedor</Form.Label>
                        <Select options={usuarios} value={filtros.usuario} onChange={(val) => setFiltros({ ...filtros, usuario: val })} isClearable placeholder="Todos..." />
                    </Form.Group>
                    <Form.Group className="col-md-4 mb-3">
                        <Form.Label>Tipo Cliente</Form.Label>
                        <Select
                            options={[{ value: 'DETALLE', label: 'Detalle' }, { value: 'MAYORISTA', label: 'Mayorista' }]}
                            value={filtros.tipoCliente}
                            onChange={(val) => setFiltros({ ...filtros, tipoCliente: val })}
                            isClearable
                            placeholder="Todos..."
                        />
                    </Form.Group>
                    <Form.Group className="col-md-4 mb-3">
                        <Form.Label>Medio de Pago</Form.Label>
                        <Select
                            options={MEDIOS_PAGO}
                            value={filtros.medioPago}
                            onChange={(val) => setFiltros({ ...filtros, medioPago: val })}
                            isClearable
                            placeholder="Todos..."
                        />
                    </Form.Group>
                </div>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="btn btn-cancel" onClick={handleClose} disabled={isExporting}>Cancelar</Button>
                <Button variant="primary" onClick={handleExport} disabled={isExporting}>
                    {isExporting ? (
                        <>
                            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                            Procesando...
                        </>
                    ) : "Descargar CSV"}
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

// PropTypes para evitar warnings y asegurar que los datos llegan bien
ExportSaleCsvModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    usuarios: PropTypes.array.isRequired
};

export default ExportSaleCsvModal;