import React, { useState, useEffect } from "react";
import * as aromasService from "../../services/aromaService";
import * as familiasService from "../../services/familiaService";
import PropTypes from 'prop-types';


/**
 * Genera un array de opciones para un select de años.
 * @param {number} startYear El primer año del rango (ej: 2023)
 * @returns {{value: string, label: string}[]}
 */
const generateYearOptions = (startYear) => {
    const currentYear = new Date().getFullYear();
    const years = [];
    for (let year = currentYear; year >= startYear; year--) {
        years.push({ value: year.toString(), label: year.toString() });
    }
    return years;
};


const anioOptions = generateYearOptions(2023); // Genera desde 2023 hasta el año actual
const mesOptions = [
    { value: "1", label: "Enero" },
    { value: "2", label: "Febrero" },
    { value: "3", label: "Marzo" },
    { value: "4", label: "Abril" },
    { value: "5", label: "Mayo" },
    { value: "6", label: "Junio" },
    { value: "7", label: "Julio" },
    { value: "8", label: "Agosto" },
    { value: "9", label: "Septiembre" },
    { value: "10", label: "Octubre" },
    { value: "11", label: "Noviembre" },
    { value: "12", label: "Diciembre" }];
//const tipoVentaOptions = [{ value: "TIENDA", label: "Tienda" }, { value: "ONLINE", label: "Online" }];
const tipoClienteOptions = [{ value: "MAYORISTA", label: "Mayorista" }, { value: "DETALLE", label: "Detalle" }];


// Opciones para Recencia
const recenciaOptions = [
    { value: "30", label: "30 días" },
    { value: "60", label: "60 días" },
    { value: "90", label: "90 días" },
    { value: "120", label: "120 días" },
];

const FiltrosGlobales = ({
    filters,
    onFilterChange,
    activeTab,
    onApplyFilters, // Nueva Prop
    onClearFilters  // Nueva Prop
}) => {
    const [opciones, setOpciones] = useState({ aromas: [], familias: [] });
    const [loadingOpciones, setLoadingOpciones] = useState(true);

    useEffect(() => {
        // Carga las opciones de aroma/familia una sola vez
        const cargarOpciones = async () => {
            setLoadingOpciones(true);
            try {
                const [aromasRes, familiasRes] = await Promise.all([
                    aromasService.getAromasActivos(),
                    familiasService.getFamiliasActivas(),
                ]);
                setOpciones({ aromas: aromasRes, familias: familiasRes });
            } catch (error) {
                console.error("Error al cargar opciones de filtros:", error);
            } finally {
                setLoadingOpciones(false);
            }
        };
        cargarOpciones();
    }, []);

    // Lógica para saber qué filtros mostrar según la pestaña activa
    const showAnio = ['resumen', 'ventas', 'productos', 'operaciones', 'clientes'].includes(activeTab);
    const showMes = ['ventas', 'productos', 'operaciones', 'clientes'].includes(activeTab);
    const showRangoFechas = activeTab === 'ventas' || activeTab === 'operaciones' || activeTab === 'resumen';
    const showTipoVentaCliente = activeTab === 'resumen' || activeTab === 'ventas' || activeTab === 'clientes';
    const showAromaFamilia = activeTab === 'productos';
    const showRecencia = activeTab === 'clientes';

    return (
        <div className="card">
            <div className="card-body">
                <h5 className="card-title">Filtros Globales</h5>
                <div className="row g-3 align-items-end">
                    {/* Año (Casi siempre visible) */}
                    {showAnio && <div className="col-md-2"><label>Año</label><select name="anio" className="form-select" value={filters.anio} onChange={onFilterChange}>{anioOptions.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></div>}

                    {/* Mes (Visible en la mayoría, excepto resumen) */}
                    {showMes && <div className="col-md-2"><label>Mes</label><select name="mes" className="form-select" value={filters.mes} onChange={onFilterChange}><option value="">Todos</option>{mesOptions.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></div>}

                    {/* Tipo Venta y Cliente (Solo en Resumen y Ventas) */}
                    {showTipoVentaCliente && (
                        <>
                            {/*<div className="col-md-2"><label>Tipo Venta</label><select name="tipoVenta" className="form-select" value={filters.tipoVenta} onChange={onFilterChange}><option value="">Todos</option>{tipoVentaOptions.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></div>*/}
                            <div className="col-md-2"><label>Tipo Cliente</label><select name="tipoCliente" className="form-select" value={filters.tipoCliente} onChange={onFilterChange}><option value="">Todos</option>{tipoClienteOptions.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></div>
                        </>
                    )}

                    {/* Familia y Aroma (Solo en Productos) */}
                    {showAromaFamilia && (
                        <>
                            <div className="col-md-2"><label>Familia</label><select name="familiaId" className="form-select" value={filters.familiaId} onChange={onFilterChange} disabled={loadingOpciones}><option value="">Todas</option>{opciones.familias.map(f => <option key={f.familiaId} value={f.familiaId}>{f.nombre}</option>)}</select></div>
                            <div className="col-md-2"><label>Aroma</label><select name="aromaId" className="form-select" value={filters.aromaId} onChange={onFilterChange} disabled={loadingOpciones}><option value="">Todos</option>{opciones.aromas.map(a => <option key={a.aromaId} value={a.aromaId}>{a.nombre}</option>)}</select></div>
                        </>
                    )}

                    {/* Rango de Fechas (Solo en Ventas para un gráfico específico) */}
                    {showRangoFechas && (
                        <>
                            <div className="col-md-2"><label>Fecha Inicio</label><input type="date" name="fechaInicio" className="form-control" value={filters.fechaInicio} onChange={onFilterChange} /></div>
                            <div className="col-md-2"><label>Fecha Fin</label><input type="date" name="fechaFin" className="form-control" value={filters.fechaFin} onChange={onFilterChange} /></div>
                        </>
                    )}

                    {/* Días de Inactividad (Solo en Clientes) */}
                    {showRecencia && (
                        <div className="col-md-2">
                            <label>Recencia (Días)</label>
                            <select
                                name="diasInactividad"
                                className="form-select"
                                value={filters.diasInactividad}
                                onChange={onFilterChange}
                            >
                                {recenciaOptions.map(opt => (
                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                ))}
                            </select>
                        </div>
                    )}

                    <div className="col-md-auto ms-auto">
                        <button
                            className="btn btn-primary me-2"
                            onClick={onApplyFilters}
                        >
                            <i data-feather="search" className="feather-search me-1" />
                            Aplicar Filtros
                        </button>
                        <button
                            className="btn btn-secondary"
                            onClick={onClearFilters}
                        >
                            <i data-feather="x" className="feather-x me-1" />
                            Limpiar
                        </button>
                    </div>
                </div>

                <small className="form-text text-muted mt-2">Selecciona tus filtros y presiona &quot;Aplicar Filtros&quot; para actualizar los reportes.</small>
            </div>
        </div>
    );
};

FiltrosGlobales.propTypes = {
    filters: PropTypes.shape({
        anio: PropTypes.string.isRequired,
        mes: PropTypes.string.isRequired,
        tipoVenta: PropTypes.string.isRequired,
        tipoCliente: PropTypes.string.isRequired,
        diasInactividad: PropTypes.string.isRequired,
        familiaId: PropTypes.string.isRequired,
        aromaId: PropTypes.string.isRequired,
        fechaInicio: PropTypes.string.isRequired,
        fechaFin: PropTypes.string.isRequired,
    }).isRequired,
    onFilterChange: PropTypes.func.isRequired,
    activeTab: PropTypes.string.isRequired,
    onApplyFilters: PropTypes.func.isRequired,
    onClearFilters: PropTypes.func.isRequired,
};

export default FiltrosGlobales;