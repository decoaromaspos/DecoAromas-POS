import React, { useState, useEffect } from "react";
import PropTypes from 'prop-types';
import ChartRenderer from "./ChartRenderer";
import * as reportesService from "../../services/reportesService";
import { pieChartOptions, chartOptions, horizontalBarOptions, formatCurrency } from "../../utils/charts";

const mesesNombres = {
    "1": "Enero", "2": "Febrero", "3": "Marzo", "4": "Abril", "5": "Mayo", "6": "Junio",
    "7": "Julio", "8": "Agosto", "9": "Septiembre", "10": "Octubre", "11": "Noviembre", "12": "Diciembre"
};

const formatTitleCase = (str) => {
    if (!str || typeof str !== 'string') return '';
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

const AnalisisVentasTab = ({ filters }) => {
    const [activeSecondaryTab, setActiveSecondaryTab] = useState('distribucion');
    const [loading, setLoading] = useState(true);
    const [chartData, setChartData] = useState({
        distribucionTiendaClientes: null, distribucionOnlineClientes: null,
        distribucionVentas: null, mediosDePago: null, evolucionVentas: null,
        analisisDescuentos: null, ventasVendedor: null, ventasDiaSemana: null,
        ventasHora: null
    });

    // --- Estado para los KPIs ---
    const [kpis, setKpis] = useState({
        totalTransacciones: 0,
        totalDescuentos: 0,
        ticketPromedio: 0,
        totalVentasNetas: 0,
        totalVentasOnline: 0
    });

    useEffect(() => {
        const cargarReportes = async () => {
            setLoading(true);
            try {
                // 1. EXTRAER Y NORMALIZAR FILTROS
                let { anio, mes, tipoCliente, fechaInicio, fechaFin } = filters;

                /** * Lógica de Protección: Si no hay fechas manuales, calculamos el rango
                 * basado en el año y mes del selector para no saturar la BD.
                 */
                if (!fechaInicio || !fechaFin) {
                    const yearNum = parseInt(anio);
                    if (mes) {
                        const monthNum = parseInt(mes);
                        // Rango del mes seleccionado
                        fechaInicio = `${anio}-${String(monthNum).padStart(2, '0')}-01`;
                        fechaFin = new Date(yearNum, monthNum, 0).toISOString().split('T')[0];
                    } else {
                        // Rango del año completo seleccionado
                        fechaInicio = `${anio}-01-01`;
                        fechaFin = `${anio}-12-31`;
                    }
                }

                // 2. LLAMADAS PARALELAS (Usando las fechas normalizadas)
                const [
                    clientesData, clientesOnlineData, ventasData, pagosData,
                    evolucionData, descuentosData, vendedorData,
                    diaSemanaData, horaData, kpiData
                ] = await Promise.all([
                    reportesService.getDistribucionTiendaPorTipoCliente(anio, mes),
                    reportesService.getDistribucionOnlinePorTipoCliente(anio, mes),
                    reportesService.getVentasAnualesTorta(anio, mes, tipoCliente),
                    reportesService.getAnalisisMediosDePago(fechaInicio, fechaFin, tipoCliente),
                    reportesService.getVentasAnualesComparacionBarras(anio, tipoCliente),
                    reportesService.getAnalisisDescuentos(anio, tipoCliente),
                    reportesService.getVentasTiendaPorVendedor(fechaInicio, fechaFin),
                    reportesService.getVentasTiendaPorDiaSemana(fechaInicio, fechaFin, tipoCliente),
                    reportesService.getVentasTiendaPorHora(fechaInicio, fechaFin, tipoCliente),
                    reportesService.getKpisVentas(fechaInicio, fechaFin, tipoCliente)
                ]);

                setChartData({
                    distribucionTiendaClientes: clientesData,
                    distribucionOnlineClientes: clientesOnlineData,
                    distribucionVentas: ventasData,
                    mediosDePago: pagosData,
                    evolucionVentas: evolucionData,
                    analisisDescuentos: descuentosData,
                    ventasVendedor: vendedorData,
                    ventasDiaSemana: diaSemanaData,
                    ventasHora: horaData
                });

                setKpis(kpiData);

            } catch (error) {
                console.error("Error al cargar reportes de ventas:", error);
                setChartData({
                    distribucionTiendaClientes: null, distribucionOnlineClientes: null,
                    distribucionVentas: null, mediosDePago: null, evolucionVentas: null,
                    analisisDescuentos: null, ventasVendedor: null, ventasDiaSemana: null,
                    ventasHora: null
                });
                setKpis({ totalTransacciones: 0, totalDescuentos: 0, ticketPromedio: 0, totalVentasNetas: 0, totalVentasOnline: 0 });
            } finally {
                setLoading(false);
            }
        };
        cargarReportes();
    }, [filters]);

    // --- Funciones de Subtítulos ---
    const getRangoFechasSubtitle = (conTipoCliente = true) => {
        let { fechaInicio, fechaFin, tipoCliente, anio, mes } = filters;
        const clienteTexto = (conTipoCliente && tipoCliente) ? `Cliente ${formatTitleCase(tipoCliente)}` : (conTipoCliente ? 'Todos los clientes' : null);

        let periodoTexto;
        if (fechaInicio && fechaFin) {
            periodoTexto = `Período: ${fechaInicio} al ${fechaFin}`;
        } else {
            // Si el usuario no puso fechas, el subtítulo indica el año/mes del selector
            periodoTexto = mes ? `Mes: ${mesesNombres[mes]} ${anio}` : `Año: ${anio}`;
        }
        return [periodoTexto, clienteTexto].filter(Boolean).join(' | ');
    };
    const getAnioMesSubtitle = (conTipoCliente = false) => {
        const { anio, mes, tipoCliente } = filters;
        const clienteTexto = (conTipoCliente && tipoCliente) ? `Cliente ${formatTitleCase(tipoCliente)}` : (conTipoCliente ? 'Todos los clientes' : null);
        const mesTexto = mes ? mesesNombres[mes] : 'Todos los meses';
        return [`Año ${anio}`, mesTexto, clienteTexto].filter(Boolean).join(' | ');
    };
    const getAnioTipoClienteSubtitle = () => {
        const { anio, tipoCliente } = filters;
        const clienteTexto = tipoCliente ? `Cliente ${formatTitleCase(tipoCliente)}` : 'Todos los clientes';
        return `Año ${anio} | ${clienteTexto}`;
    };
    const getEvolucionSubtitle = () => {
        const { anio, tipoCliente } = filters;
        const clienteTexto = tipoCliente ? `Cliente ${formatTitleCase(tipoCliente)}` : 'Todos los clientes';
        return `Año ${anio} | ${clienteTexto}`;
    };


    // Definición de Opciones para TODOS los gráficos
    const clientesChartOptions = { ...pieChartOptions('Ventas Tienda por Tipo de Cliente', chartData.distribucionTiendaClientes?.labels, filters.anio, filters.mes), colors: ['#008FFB', '#00E396'] };
    const clientesOnlineChartOptions = { ...pieChartOptions('Ventas Online por Tipo de Cliente', chartData.distribucionOnlineClientes?.labels, filters.anio, filters.mes), colors: ['#008FFB', '#00E396'] };
    const ventasChartOptions = { ...pieChartOptions('Ventas Tienda vs. Online', chartData.distribucionVentas?.labels, filters.anio, filters.mes), colors: ['#775DD0', '#FEB019'] };
    const pagosChartOptions = { ...pieChartOptions('Análisis de Medios de Pago', chartData.mediosDePago?.labels), colors: ['#775DD0', '#FF4560', '#00E396', '#FEB019', '#008FFB'] };

    // Opciones modificadas para el gráfico de barras agrupadas.
    const evolucionVentasOptions = {
        ...chartOptions('Comparativo de Ventas Mensuales', chartData.evolucionVentas?.categories, filters.anio),
        yaxis: { title: { text: 'Total Vendido (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
        tooltip: { y: { formatter: (val) => formatCurrency(val) } },
    };

    const descuentosOptions = {
        ...chartOptions('Análisis de Descuentos vs. Ventas Netas', chartData.analisisDescuentos?.categories, filters.anio),
        yaxis: { title: { text: 'Total Descuento (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
        tooltip: { y: { formatter: (val) => formatCurrency(val) } }
    };

    const vendedorOptions = {
        ...chartOptions('Rendimiento por Vendedor', chartData.ventasVendedor?.categories),
        plotOptions: { bar: { horizontal: true, borderRadius: 4, distributed: true } },
        yaxis: { title: { text: 'Usuario' } },
        xaxis: {
            categories: chartData.ventasVendedor?.categories,
            title: { text: 'Ventas Totales (CLP)' },
            labels: { formatter: (val) => formatCurrency(val) }
        },
        legend: { show: false },
        tooltip: {
            x: { formatter: (val) => val }, // Eje X es el valor
            y: { formatter: (val) => formatCurrency(val) } // Eje Y es el nombre/categoría
        }
    };

    const diaSemanaOptions = {
        ...horizontalBarOptions('Ventas por Día de la Semana', chartData.ventasDiaSemana?.categories),
        plotOptions: { bar: { horizontal: true, borderRadius: 4, distributed: true } },
        yaxis: { title: { text: 'Día' } },
        xaxis: {
            categories: chartData.ventasDiaSemana?.categories,
            title: { text: 'Total Vendido (CLP)' },
            labels: { formatter: (val) => formatCurrency(val) }
        },
        legend: { show: false },
        tooltip: {
            x: { formatter: (val) => val }, // Eje X es el valor
            y: { formatter: (val) => formatCurrency(val) } // Eje Y es el nombre/categoría
        }
    };

    const horaOptions = {
        ...chartOptions('Ventas por Hora del Día', chartData.ventasHora?.categories),
        chart: { height: 350, type: 'line', toolbar: { show: true } },
        stroke: { curve: 'smooth', width: 2 },
        yaxis: { title: { text: 'Total Vendido (CLP)' }, labels: { formatter: (val) => formatCurrency(val) } },
        xaxis: {
            categories: chartData.ventasHora?.categories,
            title: { text: 'Hora del día' }
        },
        tooltip: { y: { formatter: (val) => formatCurrency(val) } }
    };


    return (
        <>
            {/* --- FILA DE KPIs --- */}
            <div className="row mt-4 g-3">
                {/* Ticket Promedio */}
                <div className="col-lg col-md-6">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Ticket Promedio</h6>
                            <h3 className="display-6 fw-bold">
                                {loading ? '...' : formatCurrency(kpis.ticketPromedio)}
                            </h3>
                            <small className=" text-muted mb-2">(Total Ventas / Número de Ventas)</small>
                            <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                        </div>
                    </div>
                </div>
                <div className="col-lg col-md-6">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Total Descuentos</h6>
                            <h3 className="display-6 fw-bold text-danger">
                                {loading ? '...' : formatCurrency(kpis.totalDescuentos)}
                            </h3>
                            <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                        </div>
                    </div>
                </div>
                <div className="col-lg col-md-4">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Nº de Transacciones</h6>
                            <h3 className="display-6 fw-bold">
                                {loading ? '...' : (kpis.totalTransacciones || 0).toLocaleString('es-CL')}
                            </h3>
                            <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                        </div>
                    </div>
                </div>
                <div className="col-lg col-md-4">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Ventas en Tienda</h6>
                            <h3 className="display-6 fw-bold">
                                {loading ? '...' : formatCurrency(kpis.totalVentasNetas)}
                            </h3>
                            <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                        </div>
                    </div>
                </div>
                <div className="col-lg col-md-4">
                    <div className="card h-100">
                        <div className="card-body text-center d-flex flex-column justify-content-center">
                            <h6 className="card-title text-muted mb-2">Ventas Online</h6>
                            <h3 className="display-6 fw-bold">
                                {loading ? '...' : formatCurrency(kpis.totalVentasOnline)}
                            </h3>
                            <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                        </div>
                    </div>
                </div>
            </div>

            {/* --- FILA 1: GRÁFICO PRINCIPAL (HERO) --- */}
            <div className="row mt-4">
                <div className="col-12">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Comparativo de Ventas (Tienda vs. Online)</h5>
                            <small className="text-muted">{getEvolucionSubtitle()}</small>
                        </div>
                        <div className="card-body">
                            <ChartRenderer
                                loading={loading}
                                options={evolucionVentasOptions}
                                series={chartData.evolucionVentas?.series || []}
                                type="bar"
                                height={350}
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* --- NUEVA FILA: TABLA DE DETALLE --- */}
            <div className="row mt-4">
                <div className="col-12">
                    <div className="card">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Detalle Comparativo de Ventas (Tienda vs. Online)</h5>
                            <small className="text-muted">{getEvolucionSubtitle()}</small>
                        </div>
                        <div className="card-body">
                            {loading ? <p>Cargando...</p> : (
                                <div className="table-responsive">
                                    <table className="table table-striped table-hover">
                                        <thead>
                                            <tr>
                                                <th>Mes</th>
                                                <th>Ventas Tienda</th>
                                                <th>Ventas Online</th>
                                                <th>Ventas Generales</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {chartData.evolucionVentas?.categories?.map((mes, index) => {
                                                // Se buscan los datos por el nombre de la serie
                                                const tienda = chartData.evolucionVentas.series.find(s => s.name === 'Ventas Tienda')?.data[index] || 0;
                                                const online = chartData.evolucionVentas.series.find(s => s.name === 'Ventas Online')?.data[index] || 0;
                                                const general = chartData.evolucionVentas.series.find(s => s.name === 'Ventas Generales')?.data[index] || 0;

                                                return (
                                                    <tr key={index}>
                                                        <td><strong>{mes}</strong></td>
                                                        <td>{formatCurrency(tienda)}</td>
                                                        <td>{formatCurrency(online)}</td>
                                                        <td><strong>{formatCurrency(general)}</strong></td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>


            {/* --- FILA 2: GRÁFICOS SECUNDARIOS DE ALTO NIVEL --- */}
            <div className="row mt-4">
                <div className="col-md-6">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Análisis de Descuentos (Tienda)</h5>
                            <small className="text-muted">{getAnioTipoClienteSubtitle()}</small>
                        </div>
                        <div className="card-body">
                            <ChartRenderer
                                loading={loading}
                                options={descuentosOptions}
                                series={chartData.analisisDescuentos?.series || []}
                                type="bar"
                                height={350}
                            />
                        </div>
                    </div>
                </div>
                <div className="col-md-6">
                    <div className="card h-100">
                        <div className="card-header">
                            <h5 className="card-title mb-0">Rendimiento por Vendedor (Tienda)</h5>
                            <small className="text-muted">{getRangoFechasSubtitle(false)}</small>
                        </div>
                        <div className="card-body">
                            <ChartRenderer
                                loading={loading}
                                options={vendedorOptions}
                                series={chartData.ventasVendedor?.series || []}
                                type="bar"
                                height={350}
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* --- FILA 3: SUB-NAVEGACIÓN (Sin cambios) --- */}
            <div className="row mt-4">
                <div className="col-12 text-center">
                    <p className="text-muted mb-2">
                        Selecciona una vista para explorar más a fondo:
                    </p>
                    <div className="btn-group" role="group">
                        <button
                            type="button"
                            className={`btn ${activeSecondaryTab === 'distribucion' ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => setActiveSecondaryTab('distribucion')}
                        >
                            📊 Análisis de Distribución
                        </button>
                        <button
                            type="button"
                            className={`btn ${activeSecondaryTab === 'operaciones' ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => setActiveSecondaryTab('operaciones')}
                        >
                            ⚙️ Análisis de Operaciones
                        </button>
                    </div>
                </div>
            </div>

            {/* --- Pestaña "Distribución" (Sin cambios) --- */}
            {activeSecondaryTab === 'distribucion' && (
                <div className="row mt-4">
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Ventas en Tienda por Tipo de Cliente</h5>
                                <small className="text-muted">{getAnioMesSubtitle()}</small>
                            </div>
                            <div className="card-body d-flex align-items-center justify-content-center">
                                <ChartRenderer loading={loading} options={clientesChartOptions} series={chartData.distribucionTiendaClientes?.series} type="donut" height={420} />
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Ventas Online por Tipo de Cliente</h5>
                                <small className="text-muted">{getAnioMesSubtitle()}</small>
                            </div>
                            <div className="card-body d-flex align-items-center justify-content-center">
                                <ChartRenderer loading={loading} options={clientesOnlineChartOptions} series={chartData.distribucionOnlineClientes?.series} type="donut" height={420} />
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Ventas Tienda vs. Online</h5>
                                <small className="text-muted">{getAnioMesSubtitle(true)}</small>
                            </div>
                            <div className="card-body d-flex align-items-center justify-content-center">
                                <ChartRenderer loading={loading} options={ventasChartOptions} series={chartData.distribucionVentas?.series} type="donut" height={420} />
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Análisis de Medios de Pago (Tienda)</h5>
                                <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                            </div>
                            <div className="card-body d-flex align-items-center justify-content-center">
                                <ChartRenderer loading={loading} options={pagosChartOptions} series={chartData.mediosDePago?.series} type="donut" height={420} />
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* --- Pestaña "Operaciones" --- */}
            {activeSecondaryTab === 'operaciones' && (
                <div className="row mt-4">
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Ventas en Tienda por Día de la Semana</h5>
                                <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                            </div>
                            <div className="card-body">
                                <ChartRenderer loading={loading} options={diaSemanaOptions} series={chartData.ventasDiaSemana?.series || []} type="bar" height={350} />
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6 mb-4">
                        <div className="card h-100">
                            <div className="card-header">
                                <h5 className="card-title mb-0">Ventas en Tienda por Hora del Día</h5>
                                <small className="text-muted">{getRangoFechasSubtitle(true)}</small>
                            </div>
                            <div className="card-body">
                                <ChartRenderer loading={loading} options={horaOptions} series={chartData.ventasHora?.series || []} type="line" height={350} />
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

AnalisisVentasTab.propTypes = {
    filters: PropTypes.shape({
        anio: PropTypes.string.isRequired,
        mes: PropTypes.string.isRequired,
        tipoVenta: PropTypes.string,
        tipoCliente: PropTypes.string,
        fechaInicio: PropTypes.string,
        fechaFin: PropTypes.string,
    }).isRequired,
};

export default AnalisisVentasTab;