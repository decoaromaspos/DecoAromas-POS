import React, { useState, useEffect } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import { ChevronUp, RotateCcw, Sliders, Filter, Eye, Trash2, FileText, Edit3, Send } from 'feather-icons-react';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import Table from '../../core/pagination/datatable';
import CotizacionDetailModal from './CotizacionDetailModal';
import ActualizarEstadoCotizacionModal from './ActualizarEstadoCotizacionModal';
import {
    getCotizacionesByFiltrosPaginados,
    deleteCotizacion
} from '../../services/cotizacionService';
import { getClientesActivos } from '../../services/clienteService';
import { getUsuarios } from '../../services/usuarioService';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import { PDFDownloadLink } from '@react-pdf/renderer'; 
import CotizacionComprobantePDF from './CotizacionComprobantePDF';
import { useAuth } from "../../context/AuthContext";
import { all_routes } from '../../Router/all_routes';


const MySwal = withReactContent(Swal);


// Función de utilidad para formatear moneda (formato chileno)
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
    if (!fechaISO) return 'N/A';
    const fecha = new Date(fechaISO);
    return fecha.toLocaleDateString('es-CL') + ' ' + fecha.toLocaleTimeString('es-CL', {
        hour: '2-digit',
        minute: '2-digit'
    });
};

// Función para formatear la fecha para nombres de archivo (YYYY-MM-DD_HHmm)
const formatearFechaParaNombreArchivo = (fechaISO) => {
    if (!fechaISO) return 'fecha_desconocida';
    try {
        const fecha = new Date(fechaISO);
        
        // Obtenemos las partes de la fecha/hora
        const year = fecha.getFullYear();
        const month = String(fecha.getMonth() + 1).padStart(2, '0');
        const day = String(fecha.getDate()).padStart(2, '0');
        const hours = String(fecha.getHours()).padStart(2, '0');
        const minutes = String(fecha.getMinutes()).padStart(2, '0');

        // Formato: AAAA-MM-DD_HHmm
        return `${year}-${month}-${day}_${hours}-${minutes}`;
    } catch (e) {
        console.error("Error formateando fecha para archivo:", e);
        return 'fecha_invalida';
    }
};

const CotizacionesList = () => {
    const dispatch = useDispatch();
    const data = useSelector((state) => state.toggle_header);
    const { usuario } = useAuth();
    const CURRENT_USER_ID = usuario?.usuarioId;
    const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';
    const esSuperAdmin = usuario?.rol === 'SUPER_ADMIN';
    const route = all_routes;

    const [isFilterVisible, setIsFilterVisible] = useState(false);
    const [cotizaciones, setCotizaciones] = useState([]);
    const [usuarios, setUsuarios] = useState([]);
    const [clientes, setClientes] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);

    // Estados para Modales
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedCotizacionForDetail, setSelectedCotizacionForDetail] = useState(null);
    const [showEstadoModal, setShowEstadoModal] = useState(false);
    const [selectedCotizacionForEstado, setSelectedCotizacionForEstado] = useState(null);

    // Estados para filtros (simplificados para cotización)
    const [filtros, setFiltros] = useState({
        fechaInicio: '',
        fechaFin: '',
        tipoCliente: null,
        minTotalNeto: '',
        maxTotalNeto: '',
        usuario: null,
        cliente: null,
    });

    // Opciones estáticas para ComboBoxes
    const tipoClienteOptions = [
        { value: 'DETALLE', label: 'Detalle' },
        { value: 'MAYORISTA', label: 'Mayorista' },
    ];


    const toggleFilterVisibility = () => {
        setIsFilterVisible((prevVisibility) => !prevVisibility);
    };

    // Cargar datos iniciales al montar
    useEffect(() => {
        cargarUsuarios();
        cargarClientes();
        cargarCotizaciones();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Función para cargar usuarios (solo para el ComboBox)
    const cargarUsuarios = async () => {
        try {
            const usuariosData = await getUsuarios();
            const opcionesUsuarios = usuariosData.map(usuario => ({
                value: usuario.usuarioId,
                label: `${usuario.username} - ${usuario.nombreCompleto}`
            }));
            setUsuarios(opcionesUsuarios);
        } catch (error) {
            console.error('Error al cargar usuarios:', error);
        }
    };

    // Función para cargar clientes (solo para el ComboBox)
    const cargarClientes = async () => {
        try {
            const clientesData = await getClientesActivos();
            const opcionesClientes = clientesData.map(c => {
                const fullName = `${c.nombre} ${c.apellido || ''}`.trim();

                return {
                    value: c.clienteId,
                    label: `${fullName} (${c.rut})`
                };
            });
            setClientes(opcionesClientes);
        } catch (error) {
            console.error('Error al cargar clientes:', error);
        }
    };

    // Función auxiliar para resolver nombres (asumiendo que el backend ya los provee)
    const resolveNombres = async (cotizacionesData) => {
        if (!cotizacionesData || cotizacionesData.length === 0) return [];

        // Asumimos que el backend envía 'usuarioNombre' y 'clienteNombre'
        return cotizacionesData.map(c => ({
            ...c,
            nombreCliente: c.clienteNombre || 'N/A'
        }));
    };

    // Función principal para cargar cotizaciones con filtros
    const cargarCotizaciones = async (page = currentPage, size = pageSize, filtrosParaUsar = filtros) => {
        try {
            setLoading(true);

            // Preparamos los parámetros para el servicio
            const params = {
                fechaInicio: filtrosParaUsar.fechaInicio || null,
                fechaFin: filtrosParaUsar.fechaFin || null,
                tipoCliente: filtrosParaUsar.tipoCliente ? filtrosParaUsar.tipoCliente.value : null,
                minTotalNeto: filtrosParaUsar.minTotalNeto ? parseFloat(filtrosParaUsar.minTotalNeto) : null,
                maxTotalNeto: filtrosParaUsar.maxTotalNeto ? parseFloat(filtrosParaUsar.maxTotalNeto) : null,
                usuarioId: filtrosParaUsar.usuario ? filtrosParaUsar.usuario.value : null,
                clienteId: filtrosParaUsar.cliente ? filtrosParaUsar.cliente.value : null,
            };

            // Llamada al servicio de cotizaciones
            const response = await getCotizacionesByFiltrosPaginados(
                page,
                size,
                'fechaEmision', // Ordenar por fechaEmision (según el service)
                params.fechaInicio,
                params.fechaFin,
                params.tipoCliente,
                params.minTotalNeto,
                params.maxTotalNeto,
                params.usuarioId,
                params.clienteId
            );

            // Asumimos que el backend resuelve nombres
            const cotizacionesConNombres = await resolveNombres(response.content);

            setCotizaciones(cotizacionesConNombres);
            setCurrentPage(response.pageNumber);
            setPageSize(response.pageSize);
            setTotalElements(response.totalElements);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Error al cargar cotizaciones:', error);
        } finally {
            setLoading(false);
        }
    };

    const aplicarFiltros = () => {
        setCurrentPage(0);
        cargarCotizaciones(0, pageSize);
    };

    const limpiarFiltros = () => {
        const filtrosLimpios = {
            fechaInicio: '',
            fechaFin: '',
            tipoCliente: null,
            minTotalNeto: '',
            maxTotalNeto: '',
            usuario: null,
            cliente: null,
        };

        setFiltros(filtrosLimpios);
        setCurrentPage(0);
        cargarCotizaciones(0, pageSize, filtrosLimpios);
    };

    const handlePageChange = (page) => {
        const newPage = page - 1;
        setCurrentPage(newPage);
        cargarCotizaciones(newPage, pageSize);
    };

    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(0);
        cargarCotizaciones(0, size);
    };

    const handleFiltroChange = (campo, valor) => {
        setFiltros(prev => ({
            ...prev,
            [campo]: valor
        }));
    };

    // --- MANEJADORES DE ACCIONES (PLACEHOLDERS Y REALES) ---

    // Placeholder para Ver Detalle
    const handleShowDetailModal = (cotizacion) => {
        setSelectedCotizacionForDetail(cotizacion);
        setShowDetailModal(true);
    };

    const handleCloseDetailModal = () => {
        setShowDetailModal(false);
        setSelectedCotizacionForDetail(null);
    };

    // Placeholder para Editar Estado
    const handleShowEstadoModal = (cotizacion) => {
        setSelectedCotizacionForEstado(cotizacion);
        setShowEstadoModal(true);
    };

    const handleCloseEstadoModal = () => {
        setShowEstadoModal(false);
        setSelectedCotizacionForEstado(null);
    };

    // Callback para cuando el estado se actualiza exitosamente
    const handleEstadoActualizado = () => {
        handleCloseEstadoModal(); // Cierra el modal
        cargarCotizaciones(currentPage, pageSize); // Recarga la lista
    };

    // Función real para Eliminar Cotización
    const handleEliminarCotizacion = async (cotizacionId) => {
        const result = await MySwal.fire({
            title: '¿Eliminar esta Cotización?',
            text: `La Cotización #${cotizacionId} será eliminada permanentemente. ¡Esta acción es irreversible!`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            confirmButtonText: 'Sí, Eliminar',
            cancelButtonText: 'Cancelar',
        });

        if (result.isConfirmed) {
            try {
                // Usamos CURRENT_USER_ID del contexto de autenticación
                await deleteCotizacion(cotizacionId, CURRENT_USER_ID);

                await MySwal.fire(
                    '¡Eliminada!',
                    `La Cotización #${cotizacionId} ha sido eliminada.`,
                    'success'
                );

                cargarCotizaciones(); // Recargar la lista
            } catch (error) {
                console.error('Error al eliminar cotización:', error);
                await MySwal.fire(
                    'Error',
                    'Hubo un problema al eliminar la cotización. Inténtalo de nuevo.',
                    'error'
                );
            }
        }
    };

    // Opciones y Tooltips
    const pageSizeOptions = [
        { value: 5, label: "5 por página" },
        { value: 10, label: "10 por página" },
        { value: 20, label: "20 por página" },
        { value: 50, label: "50 por página" },
    ];

    const renderRefreshTooltip = (props) => (<Tooltip id="refresh-tooltip" {...props}>Actualizar</Tooltip>);
    const renderCollapseTooltip = (props) => (<Tooltip id="collapse-tooltip" {...props}>Colapsar</Tooltip>);
    const renderViewTooltip = (props) => (<Tooltip id="view-tooltip" {...props}>Ver Detalle</Tooltip>);
    const renderEditStatusTooltip = (props) => (<Tooltip id="edit-status-tooltip" {...props}>Editar Estado</Tooltip>);
    const renderPdfTooltip = (props) => (<Tooltip id="pdf-tooltip" {...props}>Generar PDF</Tooltip>);
    const renderDeleteTooltip = (props) => (<Tooltip id="delete-tooltip" {...props}>Eliminar Cotización</Tooltip>);
    const renderConvertTooltip = (props) => (<Tooltip id="convert-tooltip" {...props}>Convertir a Venta</Tooltip>);


    // Columnas para la tabla de cotizaciones
    const columns = [
        {
            title: "ID",
            dataIndex: "cotizacionId",
            sorter: (a, b) => a.cotizacionId - b.cotizacionId,
            key: "cotizacionId",
            width: '5%',
        },
        {
            title: "Fecha Emisión",
            dataIndex: "fechaEmision",
            sorter: (a, b) => new Date(a.fechaEmision) - new Date(b.fechaEmision),
            key: "fechaEmision",
            width: '15%',
            render: (fecha) => formatearFecha(fecha),
        },
        {
            title: "Estado",
            dataIndex: "estado",
            sorter: (a, b) => (a.estado || '').localeCompare(b.estado || ''),
            key: "estado",
            width: '10%',
            render: (estado) => {
                let badgeClass = 'bg-secondary';
                if (estado === 'CONVERTIDA') badgeClass = 'bg-success';
                if (estado === 'RECHAZADA') badgeClass = 'bg-danger';
                if (estado === 'PENDIENTE') badgeClass = 'bg-warning text-dark';
                return (
                    <span className={`badge ${badgeClass}`}>
                        {estado || 'N/A'}
                    </span>
                );
            }
        },
        {
            title: "Tipo",
            dataIndex: "tipoCliente",
            sorter: (a, b) => a.tipoCliente.localeCompare(b.tipoCliente),
            key: "tipoCliente",
            width: '10%',
            render: (tipo) => (
                <span className={`badge ${tipo === 'MAYORISTA' ? 'bg-info' : 'bg-secondary'}`}>
                    {tipo}
                </span>
            ),
        },
        {
            title: "Usuario",
            dataIndex: "usuarioNombre",
            sorter: (a, b) => (a.usuarioNombre || '').localeCompare(b.usuarioNombre || ''),
            key: "nombreUsuario",
            width: '20%',
        },
        {
            title: "Cliente",
            dataIndex: "clienteNombre",
            sorter: (a, b) => (a.clienteNombre || 'N/A').localeCompare(b.clienteNombre || 'N/A'),
            key: "nombreCliente",
            width: '20%',
            render: (nombreCliente) => nombreCliente || 'N/A',
        },
        {
            title: "Total Neto",
            dataIndex: "totalNeto",
            sorter: (a, b) => a.totalNeto - b.totalNeto,
            key: "totalNeto",
            width: '10%',
            render: (total) => <span className="text-success fw-bold">{formatCurrency(total)}</span>,
        },
        {
            title: "Acciones",
            dataIndex: "acciones",
            key: "acciones",
            width: '10%',
            render: (text, record) => (
                <div className="text-center d-flex justify-content-left">
                    {/* Ver Detalle */}
                    <OverlayTrigger placement="top" overlay={renderViewTooltip}>
                        <Link
                            className="btn btn-sm btn-primary me-2"
                            onClick={() => handleShowDetailModal(record)}
                        >
                            <Eye className='feather-16' />
                        </Link>
                    </OverlayTrigger>

                    {/* Editar Estado */}
                    <OverlayTrigger placement="top" overlay={renderEditStatusTooltip}>
                        <Link
                            className="btn btn-sm btn-success me-2"
                            onClick={() => handleShowEstadoModal(record)}
                        >
                            <Edit3 className='feather-16' />
                        </Link>
                    </OverlayTrigger>

                    {/* Generar PDF */}
                    <PDFDownloadLink
                        document={<CotizacionComprobantePDF cotizacion={record} />}
                        fileName={`Cotizacion_${formatearFechaParaNombreArchivo(record.fechaEmision)}_${(record.clienteNombre || 'NA').replace(/[^a-zA-Z0-9 ]/g, '').replace(/ /g, '_')}.pdf`}
                    >
                        {({ loading }) => (
                            <OverlayTrigger placement="top" overlay={renderPdfTooltip}>
                                <button
                                    className="btn btn-sm btn-warning me-2"
                                    disabled={loading}
                                    style={{ border: 'none' }} 
                                >
                                    {loading ? '...' : <FileText className='feather-16' />}
                                </button>
                            </OverlayTrigger>
                        )}
                    </PDFDownloadLink>

                    {/* Convertir a Venta (Deshabilitado si ya está convertida) */}
                    {record.estado === 'PENDIENTE' &&  !esSuperAdmin && (
                        <OverlayTrigger placement="top" overlay={renderConvertTooltip}>
                            <Link
                                className={`btn btn-sm btn-info me-2 ${record.estado === 'CONVERTIDA' ? 'disabled' : ''}`}
                                to={record.estado !== 'CONVERTIDA' ? route.pos : '#'} // Navega al POS
                                state={{ cotizacion: record }} // Pasa la cotización al POS
                            >
                                <Send className='feather-16' />
                            </Link>
                        </OverlayTrigger>
                    )}

                    {/* Eliminar Cotización (solo admin y si no está convertida) */}
                    {esAdmin && record.estado !== 'CONVERTIDA' && (
                        <OverlayTrigger placement="top" overlay={renderDeleteTooltip}>
                            <Link
                                className="btn btn-sm btn-danger"
                                onClick={() => handleEliminarCotizacion(record.cotizacionId)}
                            >
                                <Trash2 className='feather-16' />
                            </Link>
                        </OverlayTrigger>
                    )}
                </div>
            ),
            align: 'left',
        },
    ];


    return (
        <div>
            <div className="page-wrapper">
                <div className="content">
                    <div className="page-header">
                        <div className="page-title me-auto">
                            <h4>Lista de Cotizaciones</h4>
                            <h6>Gestionar y revisar el historial de cotizaciones</h6>
                        </div>
                        <ul className="table-top-head">
                            <li>
                                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                                    <Link
                                        data-bs-toggle="tooltip"
                                        data-bs-placement="top"
                                        onClick={() => cargarCotizaciones()}
                                        style={{ cursor: 'pointer' }}
                                    >
                                        <RotateCcw />
                                    </Link>
                                </OverlayTrigger>
                            </li>
                            <li>
                                <OverlayTrigger placement="top" overlay={renderCollapseTooltip}>
                                    <Link
                                        data-bs-toggle="tooltip"
                                        data-bs-placement="top"
                                        id="collapse-header"
                                        className={data ? "active" : ""}
                                        onClick={() => { dispatch(setToogleHeader(!data)) }}
                                    >
                                        <ChevronUp />
                                    </Link>
                                </OverlayTrigger>
                            </li>
                        </ul>
                    </div>

                    <div className="card table-list-card">
                        <div className="card-body">
                            <div className="table-top">
                                <div className="search-set">
                                </div>
                                <div className="d-flex align-items-center">
                                    <div className="form-sort me-3">
                                        <Sliders className="info-img" />
                                        <Select
                                            className="select"
                                            options={pageSizeOptions}
                                            value={pageSizeOptions.find(opt => opt.value === pageSize)}
                                            onChange={(selected) => handlePageSizeChange(selected.value)}
                                            placeholder="Tamaño de página"
                                        />
                                    </div>
                                    <div className="search-path">
                                        <Link
                                            className={`btn btn-filter ${isFilterVisible ? "setclose" : ""}`}
                                            id="filter_search"
                                            onClick={toggleFilterVisibility}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            <Filter className="filter-icon" />
                                            <span>
                                                <ImageWithBasePath src="assets/img/icons/closes.svg" alt="img" />
                                            </span>
                                        </Link>
                                    </div>
                                </div>
                            </div>

                            {/* Filtros */}
                            <div
                                className={`card${isFilterVisible ? " visible" : ""}`}
                                id="filter_inputs"
                                style={{ display: isFilterVisible ? "block" : "none" }}
                            >
                                <div className="card-body pb-0">
                                    <div className="row">
                                        <div className="col-lg-12 col-sm-12">

                                            {/* GRUPO 1 & 2: FILTRO POR FECHAS Y TOTAL NETO */}
                                            <div className="row align-items-start mb-4 border-bottom pb-3">

                                                {/* COLUMNA IZQUIERDA: FILTRO POR FECHAS */}
                                                <div className="col-lg-5 col-md-6 col-sm-12">
                                                    <h6 className="mb-3">📅 Filtro por Fechas</h6>
                                                    <div className="row">
                                                        <div className="col-lg-6 col-sm-6 col-12">
                                                            <div className="input-blocks">
                                                                <label>Fecha Inicio</label>
                                                                <input
                                                                    type="date"
                                                                    className="form-control"
                                                                    value={filtros.fechaInicio}
                                                                    onChange={(e) => handleFiltroChange('fechaInicio', e.target.value)}
                                                                />
                                                            </div>
                                                        </div>
                                                        <div className="col-lg-6 col-sm-6 col-12">
                                                            <div className="input-blocks">
                                                                <label>Fecha Fin</label>
                                                                <input
                                                                    type="date"
                                                                    className="form-control"
                                                                    value={filtros.fechaFin}
                                                                    onChange={(e) => handleFiltroChange('fechaFin', e.target.value)}
                                                                />
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* SEPARADOR VERTICAL */}
                                                <div className="col-lg-1 col-md-1 d-none d-md-flex align-items-center justify-content-center h-100">
                                                    <div className="vr" style={{ height: '100px' }}></div>
                                                </div>

                                                {/* COLUMNA DERECHA: FILTRO POR RANGO DE TOTAL NETO */}
                                                <div className="col-lg-6 col-md-5 col-sm-12">
                                                    <h6 className="mb-3">💵 Filtro por Total Neto</h6>
                                                    <div className="row">
                                                        <div className="col-lg-6 col-sm-6 col-12">
                                                            <div className="input-blocks">
                                                                <label>Total Neto Mín.</label>
                                                                <input
                                                                    type="number"
                                                                    className="form-control"
                                                                    placeholder="Mínimo"
                                                                    value={filtros.minTotalNeto}
                                                                    onChange={(e) => handleFiltroChange('minTotalNeto', e.target.value)}
                                                                />
                                                            </div>
                                                        </div>
                                                        <div className="col-lg-6 col-sm-6 col-12">
                                                            <div className="input-blocks">
                                                                <label>Total Neto Máx.</label>
                                                                <input
                                                                    type="number"
                                                                    className="form-control"
                                                                    placeholder="Máximo"
                                                                    value={filtros.maxTotalNeto}
                                                                    onChange={(e) => handleFiltroChange('maxTotalNeto', e.target.value)}
                                                                />
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>

                                            {/* GRUPO 3: FILTROS DE LISTAS DESPLEGABLES */}
                                            <div className="row align-items-end mb-3 pb-3 border-bottom">
                                                <div className="col-lg-12"><h6>🔍 Filtros por Atributo</h6></div>

                                                {/* Tipo Cliente */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Tipo Cotización</label>
                                                        <Select
                                                            className="select"
                                                            options={tipoClienteOptions}
                                                            value={filtros.tipoCliente}
                                                            onChange={(selected) => handleFiltroChange('tipoCliente', selected)}
                                                            placeholder="Seleccionar tipo"
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* Usuario */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Usuario</label>
                                                        <Select
                                                            className="select"
                                                            options={usuarios}
                                                            value={filtros.usuario}
                                                            onChange={(selected) => handleFiltroChange('usuario', selected)}
                                                            placeholder="Seleccionar usuario"
                                                            isSearchable
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* Cliente */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Cliente</label>
                                                        <Select
                                                            className="select"
                                                            options={clientes}
                                                            value={filtros.cliente}
                                                            onChange={(selected) => handleFiltroChange('cliente', selected)}
                                                            placeholder="Seleccionar cliente"
                                                            isSearchable
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* Botones de acción */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <div style={{ height: "20px", visibility: "hidden" }}>Label</div>
                                                        <div className="btn-group w-100">
                                                            <button
                                                                className="btn btn-primary me-2"
                                                                onClick={aplicarFiltros}
                                                            >
                                                                <i data-feather="search" className="feather-search me-1" />
                                                                Aplicar
                                                            </button>
                                                            <button
                                                                className="btn btn-secondary"
                                                                onClick={limpiarFiltros}
                                                            >
                                                                <i data-feather="x" className="feather-x me-1" />
                                                                Limpiar
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>

                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Tabla de Datos */}
                            <div className="table-responsive">
                                {loading ? (
                                    <div className="text-center p-4">
                                        <div className="spinner-border" role="status">
                                            <span className="visually-hidden">Cargando...</span>
                                        </div>
                                    </div>
                                ) : (
                                    <>
                                        <Table
                                            columns={columns}
                                            dataSource={cotizaciones}
                                            pagination={{
                                                current: currentPage + 1,
                                                pageSize: pageSize,
                                                total: totalElements,
                                                onChange: handlePageChange,
                                            }}
                                        />

                                        <div className="pagination-info mt-3 text-center">
                                            <span>
                                                Mostrando {cotizaciones.length} de {totalElements} cotizaciones
                                                (Página {currentPage + 1} de {totalPages})
                                            </span>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>


            {/* Modal de Detalle de Cotización */}
            <CotizacionDetailModal
                show={showDetailModal}
                handleClose={handleCloseDetailModal}
                cotizacion={selectedCotizacionForDetail}
            />

            {/* Modal de Actualizar Estado */}
            <ActualizarEstadoCotizacionModal
                show={showEstadoModal}
                handleClose={handleCloseEstadoModal}
                cotizacion={selectedCotizacionForEstado}
                onEstadoActualizado={handleEstadoActualizado}
            />
        </div>
    )
}

export default CotizacionesList;