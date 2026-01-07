import React, { useState } from "react";
import { Modal, Button, Form } from "react-bootstrap";
import Select from "react-select";
import PropTypes from 'prop-types';
import { exportarProductosCSV } from "../../../services/productoService";
import Swal from "sweetalert2";

const ExportProductCsvModal = ({ show, handleClose, aromasOptions, familiasOptions }) => {
    const [filtros, setFiltros] = useState({
        aromaId: null,
        familiaId: null,
        activo: { value: true, label: "Activos" } // Valor por defecto
    });
    const [isExporting, setIsExporting] = useState(false);

    const estadoOptions = [
        { value: true, label: "Activos" },
        { value: false, label: "Inactivos" },
        { value: null, label: "Todos los estados" },
    ];

    const handleExport = async () => {
        setIsExporting(true);
        try {
            const params = {
                aromaId: filtros.aromaId?.value || null,
                familiaId: filtros.familiaId?.value || null,
                activo: filtros.activo?.value
            };

            await exportarProductosCSV(params);

            Swal.fire({
                title: "¡Éxito!",
                text: "El archivo CSV se ha generado correctamente.",
                icon: "success"
            });
            handleClose();
        } catch (error) {
            Swal.fire("Error", "No se pudo generar el archivo CSV.", "error");
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <Modal show={show} onHide={handleClose} centered>
            <Modal.Header closeButton>
                <Modal.Title>Exportar Productos a CSV</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p className="text-muted mb-4">Seleccione los filtros para el archivo de exportación. Si deja los campos vacíos, se exportarán todos los registros.</p>

                <Form.Group className="mb-3">
                    <Form.Label>Familia de Producto</Form.Label>
                    <Select
                        options={familiasOptions}
                        value={filtros.familiaId}
                        onChange={(val) => setFiltros({ ...filtros, familiaId: val })}
                        placeholder="Todas las familias..."
                        isClearable
                    />
                </Form.Group>

                <Form.Group className="mb-3">
                    <Form.Label>Aroma</Form.Label>
                    <Select
                        options={aromasOptions}
                        value={filtros.aromaId}
                        onChange={(val) => setFiltros({ ...filtros, aromaId: val })}
                        placeholder="Todos los aromas..."
                        isClearable
                    />
                </Form.Group>

                <Form.Group className="mb-3">
                    <Form.Label>Estado</Form.Label>
                    <Select
                        options={estadoOptions}
                        value={filtros.activo}
                        onChange={(val) => setFiltros({ ...filtros, activo: val })}
                        placeholder="Seleccionar estado..."
                    />
                </Form.Group>
            </Modal.Body>
            <Modal.Footer>
                <button className="btn btn-cancel" onClick={handleClose} disabled={isExporting}>
                    Cancelar
                </button>
                <Button variant="primary" onClick={handleExport} disabled={isExporting}>
                    {isExporting ? "Generando..." : "Descargar CSV"}
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

ExportProductCsvModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    aromasOptions: PropTypes.array.isRequired,
    familiasOptions: PropTypes.array.isRequired,
};

export default ExportProductCsvModal;