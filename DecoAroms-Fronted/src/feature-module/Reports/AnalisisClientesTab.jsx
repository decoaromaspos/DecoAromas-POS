import React, { useState, useEffect } from "react";
import PropTypes from 'prop-types';
import ChartRenderer from "./ChartRenderer"; // Componente usado para renderizar los gráficos
import * as reportesService from "../../services/reportesService";
import { pieChartCountOptions, horizontalBarOptions, formatCurrency } from "../../utils/charts"; // Asume la existencia de estas utilidades
import { FaUserFriends, FaStore, FaWarehouse, FaClock } from 'react-icons/fa';

const mesesNombres = {
    "1": "Enero", "2": "Febrero", "3": "Marzo", "4": "Abril", "5": "Mayo", "6": "Junio",
    "7": "Julio", "8": "Agosto", "9": "Septiembre", "10": "Octubre", "11": "Noviembre", "12": "Diciembre"
};

const formatTitleCase = (str) => {
    if (!str || typeof str !== 'string') return '';
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

const AnalisisClientesTab = ({ filters }) => {
    const [loading, setLoading] = useState(true);
    const [kpis, setKpis] = useState({
        totalClientesActivos: 0,
        totalClientesMayoristas: 0,
        totalClientesDetalle: 0,
    });
    const [chartData, setChartData] = useState({
        distribucionTipo: null,
        topGasto: null,
        recencia: null,
    });

    // --- Constantes de Parámetros ---
    const TOP_N_DEFAULT = 10;
    const DIAS_INACTIVIDAD_FILTER = filters.diasInactividad ? parseInt(filters.diasInactividad) : 90;

    useEffect(() => {
        const cargarReportesClientes = async () => {
            setLoading(true);
            try {
                const { anio, mes, tipoCliente } = filters;

                const [
                    kpisData,
                    distribucionTipoData,
                    topGastoData,
                    recenciaData,
                    inactivosDetalleData, // Nueva llamada
                ] = await Promise.all([
                    reportesService.getClientesKpis(), // Nueva llamada
                    reportesService.getClientesDistribucionTipo(),
                    reportesService.getClientesTopGasto(TOP_N_DEFAULT, anio, mes, tipoCliente),
                    reportesService.getClientesRecencia(DIAS_INACTIVIDAD_FILTER, tipoCliente),
                    reportesService.getClientesInactivosDetalle(DIAS_INACTIVIDAD_FILTER, tipoCliente), // Nueva llamada
                ]);

                setKpis(kpisData); // Setear KPIs
                setChartData({
                    distribucionTipo: distribucionTipoData,
                    topGasto: topGastoData,
                    recencia: recenciaData,
                    inactivosDetalle: inactivosDetalleData,
                });

            } catch (error) {
                console.error("Error al cargar reportes de clientes:", error);
                setKpis({ totalClientesActivos: 0, totalClientesMayoristas: 0, totalClientesDetalle: 0 });
                setChartData({
                    distribucionTipo: null, topGasto: null, recencia: null, inactivosDetalle: []
                });
            } finally {
                setLoading(false);
            }
        };
        cargarReportesClientes();
    }, [filters, DIAS_INACTIVIDAD_FILTER]);

    // --- Funciones de Subtítulos ---

    // Subtítulo para reportes que filtran por año, mes y tipo de cliente (Top Gasto)
    const getTopGastoSubtitle = () => {
        const { anio, mes, tipoCliente } = filters;
        const clienteTexto = tipoCliente ? `Cliente ${formatTitleCase(tipoCliente)}` : 'Todos los clientes';
        const mesTexto = mes ? mesesNombres[mes] : 'Todo el año';
        return [`Ventas de ${anio}`, mesTexto, clienteTexto].filter(Boolean).join(' | ');
    };

    // Subtítulo para reportes que filtran por días de inactividad y tipo de cliente (Recencia)
    const getRecenciaSubtitle = () => {
        const { tipoCliente } = filters;
        const clienteTexto = tipoCliente ? `Solo clientes ${formatTitleCase(tipoCliente)}` : 'Todos los clientes';
        return `Inactividad: >${DIAS_INACTIVIDAD_FILTER} días | ${clienteTexto}`;
    };

    // --- Definición de Opciones para los Gráficos ---

    // 1. Opciones para Distribución por Tipo (Gráfico de Torta/Donut)
    const tipoChartOptions = {
        ...pieChartCountOptions('Distribución de Clientes Activos', chartData.distribucionTipo?.labels),
        colors: ['#008FFB', '#FEB019'] // Colores para Mayorista/Detalle
    };

    // 2. Opciones para Top Clientes por Gasto (Gráfico de Barras Horizontal)
    const topGastoOptions = {
        // Usamos horizontalBarOptions, pero ajustamos el tooltip y el eje X para la moneda.
        ...horizontalBarOptions('Top Clientes por Total Gastado', chartData.topGasto?.categories),
        plotOptions: { bar: { horizontal: true, borderRadius: 4, distributed: true } },
        yaxis: { title: { text: 'Cliente' } },
        xaxis: {
            categories: chartData.topGasto?.categories,
            title: { text: `Total Gastado (${filters.anio || 'Todo el tiempo'})` },
            labels: { formatter: (val) => formatCurrency(val) }
        },
        legend: { show: false },
        tooltip: {
            // El eje X es el valor (gasto), el eje Y es el nombre
            x: { formatter: (val) => val },
            y: { formatter: (val) => formatCurrency(val) }
        }
    };

    // 3. Opciones para Recencia (Gráfico de Torta/Donut)
    const recenciaOptions = {
        ...pieChartCountOptions('Clientes Activos vs. En Riesgo', chartData.recencia?.labels),
        colors: ['#00E396', '#FF4560'] // Verde para Activo, Rojo para En Riesgo
    };


    return (
        <div className="tab-pane active" id="analisis-clientes-tab">

            {/* --- FILA DE KPIs (NUEVA) --- */}
            <div className="row mt-4">
                <div className="col-md-3 mb-3">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Total Clientes Activos</h6>
                            <h3 className="display-6 fw-bold">
                                {loading ? '...' : (kpis.totalClientesActivos || 0).toLocaleString('es-CL')}
                            </h3>
                            <small className="text-muted"><FaUserFriends className="me-1" /> Base de datos principal</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Clientes Mayoristas</h6>
                            <h3 className="display-6 fw-bold text-info">
                                {loading ? '...' : (kpis.totalClientesMayoristas || 0).toLocaleString('es-CL')}
                            </h3>
                            <small className="text-muted"><FaWarehouse className="me-1" /> Segmento clave</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Clientes Detalle</h6>
                            <h3 className="display-6 fw-bold text-secondary">
                                {loading ? '...' : (kpis.totalClientesDetalle || 0).toLocaleString('es-CL')}
                            </h3>
                            <small className="text-muted"><FaStore className="me-1" /> Consumidor final</small>
                        </div>
                    </div>
                </div>
                <div className="col-md-3 mb-3">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Umbral de Inactividad</h6>
                            <h3 className="display-6 fw-bold text-danger">
                                {DIAS_INACTIVIDAD_FILTER} Días
                            </h3>
                            <small className="text-muted"><FaClock className="me-1" /> Filtro de Recencia</small>
                        </div>
                    </div>
                </div>
            </div>

            {/* --- FILA 1: DISTRIBUCIÓN Y RECENCIA --- */}
            <div className="row mt-4">

                {/* 1. Distribución de Clientes por Tipo (Base Estática) */}
                <div className="col-lg-6 col-sm-12 mb-4">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Distribución de la Base de Clientes por Tipo</h5>
                            <small className="text-muted">Total de clientes activos.</small>
                        </div>
                        <div className="card-body d-flex align-items-center justify-content-center">
                            <ChartRenderer
                                loading={loading}
                                options={tipoChartOptions}
                                series={chartData.distribucionTipo?.series}
                                type="donut"
                                height={420}
                            />
                        </div>
                    </div>
                </div>

                {/* 2. Recencia de Clientes (Clientes en Riesgo) */}
                <div className="col-lg-6 col-sm-12 mb-4">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Distribución de Recencia (Clientes Activos vs. En Riesgo)</h5>
                            <small className="text-muted">{getRecenciaSubtitle()}</small>
                        </div>
                        <div className="card-body d-flex align-items-center justify-content-center">
                            <ChartRenderer
                                loading={loading}
                                options={recenciaOptions}
                                series={chartData.recencia?.series}
                                type="donut"
                                height={420}
                            />
                        </div>
                    </div>
                </div>

            </div>

            {/* --- FILA 2: TOP CLIENTES (VALOR MONETARIO) --- */}
            <div className="row mt-4">
                <div className="col-12">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Clientes Más Valiosos (Top {TOP_N_DEFAULT} por Gasto)</h5>
                            <small className="text-muted">{getTopGastoSubtitle()}</small>
                        </div>
                        <div className="card-body">
                            <ChartRenderer
                                loading={loading}
                                options={topGastoOptions}
                                series={chartData.topGasto?.series || []}
                                type="bar"
                                height={450}
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* FILA 3: Tabla de Top Clientes para detalle */}
            <div className="row mt-4">
                <div className="col-12">
                    <div className="card">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Detalle Top {TOP_N_DEFAULT} Clientes por Gasto</h5>
                            <small className="text-muted">{getTopGastoSubtitle()}</small>
                        </div>
                        <div className="card-body">
                            {loading ? <p>Cargando...</p> : (
                                <div className="table-responsive">
                                    <table className="table table-striped table-hover">
                                        <thead>
                                            <tr>
                                                <th>#</th>
                                                <th>Cliente</th>
                                                <th className="text-end">Total Gastado</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {chartData.topGasto?.categories?.map((clienteNombre, index) => {
                                                const totalGastado = chartData.topGasto.series.find(s => s.name === 'Total Gastado (€)')?.data[index] || 0;
                                                return (
                                                    <tr key={index}>
                                                        <td>{index + 1}</td>
                                                        <td>{clienteNombre}</td>
                                                        <td className="text-end"><strong>{formatCurrency(totalGastado)}</strong></td>
                                                    </tr>
                                                );
                                            })}
                                            {(chartData.topGasto?.categories?.length === 0) && (
                                                <tr>
                                                    <td colSpan="3" className="text-center">No hay clientes con compras para los filtros seleccionados.</td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* --- FILA 4: CLIENTES INACTIVOS DETALLE (NUEVA TABLA DE ACCIÓN) --- */}
            <div className="row mt-4">
                <div className="col-12">
                    <div className="card">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Clientes Inactivos (Recomendados para Campaña de Descuento)</h5>
                            <small className="text-muted">Clientes con más de {DIAS_INACTIVIDAD_FILTER} días sin compra. {filters.tipoCliente ? `Solo clientes ${formatTitleCase(filters.tipoCliente)}` : 'Todos los clientes'}.</small>
                        </div>
                        <div className="card-body">
                            {loading ? <p>Cargando lista de inactivos...</p> : (
                                <div className="table-responsive">
                                    <table className="table table-striped table-hover">
                                        <thead>
                                            <tr>
                                                <th>#</th>
                                                <th>Cliente</th>
                                                <th className="text-center">Última Compra</th>
                                                <th className="text-center text-danger">Días Inactivo</th>
                                                <th>Acción Sugerida</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {chartData.inactivosDetalle?.map((cliente, index) => (
                                                <tr key={cliente.clienteId}>
                                                    <td>{index + 1}</td>
                                                    <td>{cliente.nombreCompleto}</td>
                                                    <td className="text-center">
                                                        {cliente.ultimaCompra && new Date(cliente.ultimaCompra).getFullYear() > 2020
                                                            ? new Date(cliente.ultimaCompra).toLocaleDateString('es-CL')
                                                            : 'Nunca'}
                                                    </td>
                                                    <td className="text-center text-danger fw-bold">
                                                        {cliente.diasInactivo && new Date(cliente.ultimaCompra).getFullYear() > 2020
                                                            ? cliente.diasInactivo
                                                            : 'N/A'}
                                                    </td>
                                                    <td><span className="badge bg-warning text-dark">Ofrecer Descuento</span></td>
                                                </tr>
                                            ))}
                                            {(chartData.inactivosDetalle?.length === 0) && (
                                                <tr>
                                                    <td colSpan="5" className="text-center">No hay clientes inactivos según el umbral de {DIAS_INACTIVIDAD_FILTER} días.</td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

        </div>
    );
};

AnalisisClientesTab.propTypes = {
    filters: PropTypes.shape({
        anio: PropTypes.string.isRequired,
        mes: PropTypes.string.isRequired,
        // No necesitamos tipoVenta para clientes
        tipoCliente: PropTypes.string,
        fechaInicio: PropTypes.string, // No usado en esta pestaña, pero parte de filters globales
        fechaFin: PropTypes.string, // No usado en esta pestaña, pero parte de filters globales
        familiaId: PropTypes.string,
        aromaId: PropTypes.string,
        diasInactividad: PropTypes.string.isRequired,
    }).isRequired,
};

export default AnalisisClientesTab;