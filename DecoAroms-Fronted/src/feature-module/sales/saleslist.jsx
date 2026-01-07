import React, { useState, useEffect } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import { ChevronUp, RotateCcw, Sliders, Filter, Eye, Trash2, FileText, Printer, Edit3 } from 'feather-icons-react';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import AsyncSelect from 'react-select/async';
import Table from '../../core/pagination/datatable';
import SaleDetailModal from './saledetailmodal';
import ActualizarDocumentoModal from './ActualizarDocumentoModal';
import {
    getVentasByFiltrosPaginados,
    deleteVenta,
    imprimirComprobanteVenta
} from '../../services/ventaService';
import { getClientesActivos, getClienteById } from '../../services/clienteService';
import { getUsuarios, getUsuarioById } from '../../services/usuarioService';
import { getProductosByNombreParcial } from '../../services/productoService';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import { PDFDownloadLink } from '@react-pdf/renderer';
import SaleComprobantePDF from './SaleComprobantePDF';
import { useAuth } from "../../context/AuthContext";
import ExportSaleCsvModal from './ExportSaleCsvModal';
import { MEDIOS_PAGO } from "../../utils/medioPago";


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
    const fecha = new Date(fechaISO);
    return fecha.toLocaleDateString('es-CL') + ' ' + fecha.toLocaleTimeString('es-CL', {
        hour: '2-digit',
        minute: '2-digit'
    });
};


const SalesList = () => {
    const dispatch = useDispatch();
    const data = useSelector((state) => state.toggle_header);
    const { usuario } = useAuth();
    const CURRENT_USER_ID = usuario?.usuarioId;
    const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

    // El 'setter' setIsFilterVisible ya está implícitamente en uso a través de toggleFilterVisibility
    const [isFilterVisible, setIsFilterVisible] = useState(false);
    const [ventas, setVentas] = useState([]);
    const [usuarios, setUsuarios] = useState([]);
    const [clientes, setClientes] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);

    // Estados para el Modal de Detalle
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [selectedSale, setSelectedSale] = useState(null);

    // --- ESTADO PARA EL NUEVO MODAL DE DOCUMENTO ---
    const [showDocumentoModal, setShowDocumentoModal] = useState(false);
    const [selectedSaleForDocumento, setSelectedSaleForDocumento] = useState(null);

    const [showExportModal, setShowExportModal] = useState(false);

    // Estados para filtros
    const [filtros, setFiltros] = useState({
        fechaInicio: '',
        fechaFin: '',
        tipoCliente: null,
        tipoDocumento: null,
        medioPago: null,
        minTotalNeto: '',
        maxTotalNeto: '',
        usuario: null,
        cliente: null,
        numDocParcial: '',
        pendienteAsignacion: null,
        producto: null
    });

    // Opciones estáticas para ComboBoxes
    const tipoClienteOptions = [
        { value: 'DETALLE', label: 'Detalle' },
        { value: 'MAYORISTA', label: 'Mayorista' },
    ];

    const tipoDocumentoOptions = [
        { value: 'BOLETA', label: 'Boleta' },
        { value: 'FACTURA', label: 'Factura' },
    ];

    const estadoDocumentoOptions = [
        { value: true, label: 'Pendientes (Sin Asignar)' },
        { value: false, label: 'Emitidos (Asignados)' },
    ];

    const getMedioPagoLabel = (pagoEnumValue) => {
        const opcion = MEDIOS_PAGO.find(opt => opt.value === pagoEnumValue);
        return opcion ? opcion.label : pagoEnumValue; // Si no lo encuentra, muestra el valor original
    };

    const toggleFilterVisibility = () => {
        setIsFilterVisible((prevVisibility) => !prevVisibility);
    };

    // Cargar datos iniciales al montar
    useEffect(() => {
        cargarUsuarios();
        cargarClientes();
        cargarVentas();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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

    // --- 4. LOGICA ASYNC SELECT (PRODUCTOS) ---
    const promiseOptions = (inputValue) => {
        if (!inputValue) {
            return Promise.resolve([]);
        }

        return getProductosByNombreParcial(inputValue)
            .then((listaProductos) => {
                // Mapeamos: { productoId, nombre } -> { value, label }
                return listaProductos.map((prod) => ({
                    value: prod.productoId,
                    label: prod.nombre
                }));
            })
            .catch((error) => {
                console.error("Error buscando productos:", error);
                return [];
            });
    };

    const resolveNombres = async (ventasData) => {
        if (!ventasData || ventasData.length === 0) return [];

        if (ventasData[0].usuarioNombre && (ventasData[0].clienteNombre || ventasData[0].clienteId === null)) {
            return ventasData.map(v => ({
                ...v,
                nombreCliente: v.clienteNombre || 'N/A'
            }));
        }

        const ventasConNombres = await Promise.all(ventasData.map(async (venta) => {
            let nombreUsuario = 'N/A';
            try {
                const usuario = await getUsuarioById(venta.usuarioId);
                nombreUsuario = usuario.nombreCompleto || 'Usuario Desconocido';
            } catch (e) {
                console.warn(`No se pudo cargar usuario ${venta.usuarioId}:`, e.message);
            }

            let nombreCliente = 'N/A';
            if (venta.clienteId) {
                try {
                    const cliente = await getClienteById(venta.clienteId);
                    nombreCliente = `${cliente.nombre} ${cliente.apellido || ''} (${cliente.rut})`;
                } catch (e) {
                    console.warn(`No se pudo cargar cliente ${venta.clienteId}:`, e.message);
                }
            }

            return {
                ...venta,
                nombreUsuario: nombreUsuario,
                nombreCliente: nombreCliente,
            };
        }));

        return ventasConNombres;
    };

    // Función principal para cargar ventas con filtros combinados
    const cargarVentas = async (page = currentPage, size = pageSize, filtrosParaUsar = filtros) => {
        try {
            setLoading(true);

            const params = {
                fechaInicio: filtrosParaUsar.fechaInicio || null,
                fechaFin: filtrosParaUsar.fechaFin || null,
                tipoCliente: filtrosParaUsar.tipoCliente ? filtrosParaUsar.tipoCliente.value : null,
                tipoDocumento: filtrosParaUsar.tipoDocumento ? filtrosParaUsar.tipoDocumento.value : null,
                medioPago: filtrosParaUsar.medioPago ? filtrosParaUsar.medioPago.value : null,
                minTotalNeto: filtrosParaUsar.minTotalNeto ? parseFloat(filtrosParaUsar.minTotalNeto) : null,
                maxTotalNeto: filtrosParaUsar.maxTotalNeto ? parseFloat(filtrosParaUsar.maxTotalNeto) : null,
                usuarioId: filtrosParaUsar.usuario ? filtrosParaUsar.usuario.value : null,
                clienteId: filtrosParaUsar.cliente ? filtrosParaUsar.cliente.value : null,
                numDocParcial: filtrosParaUsar.numDocParcial || null,
                pendienteAsignacion: filtrosParaUsar.pendienteAsignacion ? filtrosParaUsar.pendienteAsignacion.value : null,
                productoId: filtrosParaUsar.producto ? filtrosParaUsar.producto.value : null // <--- EXTRACT ID
            };

            const response = await getVentasByFiltrosPaginados(
                page,
                size,
                'fecha',
                params.fechaInicio,
                params.fechaFin,
                params.tipoCliente,
                params.tipoDocumento,
                params.medioPago,
                params.minTotalNeto,
                params.maxTotalNeto,
                params.usuarioId,
                params.clienteId,
                params.numDocParcial,
                params.pendienteAsignacion,
                params.productoId
            );

            const ventasConNombres = await resolveNombres(response.content);

            setVentas(ventasConNombres);
            setCurrentPage(response.pageNumber);
            setPageSize(response.pageSize);
            setTotalElements(response.totalElements);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Error al cargar ventas:', error);
        } finally {
            setLoading(false);
        }
    };

    const aplicarFiltros = () => {
        setCurrentPage(0);
        cargarVentas(0, pageSize);
    };

    const limpiarFiltros = () => {
        const filtrosLimpios = {
            fechaInicio: '',
            fechaFin: '',
            tipoCliente: null,
            tipoDocumento: null,
            medioPago: null,
            minTotalNeto: '',
            maxTotalNeto: '',
            usuario: null,
            cliente: null,
            numDocParcial: '',
            pendienteAsignacion: null,
            producto: null
        };

        setFiltros(filtrosLimpios);
        setCurrentPage(0);
        cargarVentas(0, pageSize, filtrosLimpios);
    };

    const handlePageChange = (page) => {
        const newPage = page - 1;
        setCurrentPage(newPage);
        cargarVentas(newPage, pageSize);
    };

    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(0);
        cargarVentas(0, size);
    };

    const handleFiltroChange = (campo, valor) => {
        setFiltros(prev => ({
            ...prev,
            [campo]: valor
        }));
    };

    const handleShowDetailModal = (sale) => {
        setSelectedSale(sale);
        setShowDetailModal(true);
    };

    const handleCloseDetailModal = () => {
        setShowDetailModal(false);
        setSelectedSale(null);
    };

    const handleEliminarVenta = async (ventaId) => {
        const result = await MySwal.fire({
            title: '¿Anular esta Venta?',
            text: `La Venta #${ventaId} será eliminada permanentemente. ¡Esta acción es irreversible! El stock de los productos vendidos se DEVOLVERÁ automáticamente al inventario.`,
            icon: 'warning', // Icono de advertencia
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Sí, Anular Venta y Devolver Stock',
            cancelButtonText: 'Cancelar',
        });

        if (result.isConfirmed) {
            try {
                await deleteVenta(ventaId, CURRENT_USER_ID);
                await MySwal.fire(
                    '¡Anulada!',
                    `La Venta #${ventaId} ha sido anulada y el stock se ha devuelto al inventario.`,
                    'success'
                );

                cargarVentas(); // Recargar la lista
            } catch (error) {
                console.error('Error al anular venta:', error);
                await MySwal.fire(
                    'Error',
                    'Hubo un problema al anular la venta. Inténtalo de nuevo.',
                    'error'
                );
            }
        }
    };

    const handleImprimirComprobante = async (venta) => {
        try {
            await imprimirComprobanteVenta(venta.ventaId);
            await MySwal.fire(
                'Impresión',
                `Comprobante de Venta #${venta.ventaId} enviado a la impresora.`,
                'success'
            );
        } catch (error) {
            console.error('Error al imprimir comprobante:', error);
            await MySwal.fire(
                'Error',
                'No se pudo imprimir el comprobante. Inténtalo de nuevo.',
                'error'
            );
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
    const renderDeleteTooltip = (props) => (<Tooltip id="delete-tooltip" {...props}>Eliminar Venta</Tooltip>);
    const renderDocumentoTooltip = (props) => (<Tooltip id="doc-tooltip" {...props}>Asignar/Editar Documento</Tooltip>);
    const renderExcelTooltip = (props) => (
        <Tooltip id="excel-tooltip" {...props}>
            Exportar a CSV
        </Tooltip>
    );


    // Columnas para la tabla
    const columns = [
        {
            title: "ID",
            dataIndex: "ventaId",
            sorter: (a, b) => a.ventaId - b.ventaId,
            key: "ventaId",
            width: '5%',
        },
        {
            title: "Fecha",
            dataIndex: "fecha",
            sorter: (a, b) => new Date(a.fecha) - new Date(b.fecha),
            key: "fecha",
            width: '15%',
            render: (fecha) => formatearFecha(fecha),
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
            width: '15%',
        },
        {
            title: "Cliente",
            dataIndex: "clienteNombre",
            sorter: (a, b) => (a.clienteNombre || 'N/A').localeCompare(b.clienteNombre || 'N/A'),
            key: "nombreCliente",
            width: '15%',
            render: (nombreCliente) => nombreCliente || 'N/A',
        },
        {
            title: "Documento",
            dataIndex: "tipoDocumento",
            sorter: (a, b) => a.tipoDocumento.localeCompare(b.tipoDocumento),
            key: "tipoDocumento",
            width: '10%',
            render: (tipoDocumento) => (
                <span
                    className={`badge ${tipoDocumento === 'FACTURA' ? 'bg-primary' : 'bg-success'}`}
                    style={{ width: '65px', display: 'inline-block', textTransform: 'uppercase' }}
                >
                    {tipoDocumento}
                </span>
            ),
        },
        {
            title: "Número Documento",
            dataIndex: "numeroDocumento",
            sorter: (a, b) => (a.numeroDocumento || '').localeCompare(b.numeroDocumento || ''),
            key: "numeroDocumento",
            width: '15%',
            render: (numeroDocumento) => (
                numeroDocumento ? (
                    <span className="fw-bold">{numeroDocumento}</span>
                ) : (
                    <span className="badge bg-warning text-dark">(sin asignar)</span>
                )
            ),
        },
        {
            title: "Medio Pago",
            dataIndex: "pagos",
            key: "pagos",
            width: '15%',
            render: (pagos) => {
                if (!pagos || pagos.length === 0) {
                    return <span className="text-muted">N/A</span>;
                }
                if (pagos.length === 1) {
                    return getMedioPagoLabel(pagos[0].medioPago);
                }
                return <span className="badge bg-success">Pago Mixto</span>;
            },
        },
        {
            title: "Total",
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
                <div className="text-center d-flex justify-content-center">
                    <OverlayTrigger placement="top" overlay={renderViewTooltip}>
                        <Link
                            className="btn btn-sm btn-primary me-2"
                            onClick={() => handleShowDetailModal(record)}
                        >
                            <Eye className='feather-16' />
                        </Link>
                    </OverlayTrigger>

                    <OverlayTrigger placement="top" overlay={renderDocumentoTooltip}>
                        <Link
                            className="btn btn-sm btn-success me-1"
                            onClick={() => handleShowDocumentoModal(record)}
                        >
                            <Edit3 className='feather-16' />
                        </Link>
                    </OverlayTrigger>

                    <OverlayTrigger placement="top" overlay={(props) => (<Tooltip id="print-tooltip" {...props}>Imprimir (Térmica)</Tooltip>)}>
                        <Link
                            className="btn btn-sm btn-info me-1"
                            onClick={() => handleImprimirComprobante(record)}
                        >
                            <Printer className='feather-16' />
                        </Link>
                    </OverlayTrigger>

                    <PDFDownloadLink
                        document={<SaleComprobantePDF venta={record} />}
                        fileName={`Comprobante_Venta_${record.ventaId}.pdf`}
                    >
                        {({ loading }) => (
                            <OverlayTrigger placement="top" overlay={(props) => (<Tooltip id="pdf-tooltip" {...props}>Generar PDF</Tooltip>)}>
                                <button
                                    className="btn btn-sm btn-warning me-1"
                                    disabled={loading}
                                    style={{ border: 'none' }}
                                >
                                    {loading ? 'Cargando...' : <FileText className='feather-16' />}
                                </button>

                            </OverlayTrigger>
                        )}
                    </PDFDownloadLink>

                    {esAdmin && (
                        <OverlayTrigger placement="top" overlay={renderDeleteTooltip}>
                            <Link
                                className="btn btn-sm btn-danger"
                                onClick={() => handleEliminarVenta(record.ventaId)}
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

    const handleShowDocumentoModal = (venta) => {
        setSelectedSaleForDocumento(venta);
        setShowDocumentoModal(true);
    };

    const handleCloseDocumentoModal = () => {
        setSelectedSaleForDocumento(null);
        setShowDocumentoModal(false);
    };

    const handleDocumentoActualizado = () => {
        handleCloseDocumentoModal();
        cargarVentas(currentPage, pageSize);
    };


    return (
        <div>
            <div className="page-wrapper">
                <div className="content">
                    <div className="page-header">
                        <div className="page-title me-auto">
                            <h4>Lista de Ventas</h4>
                            <h6>Gestionar y revisar el historial de ventas</h6>
                        </div>
                        <ul className="table-top-head">
                            <li>
                                <OverlayTrigger placement="top" overlay={renderExcelTooltip}>
                                    <Link
                                        onClick={() => setShowExportModal(true)}
                                    >
                                        <ImageWithBasePath src="assets/img/icons/excel.svg" alt="img" />
                                    </Link>
                                </OverlayTrigger>
                            </li>
                            <li>
                                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                                    <Link
                                        data-bs-toggle="tooltip"
                                        data-bs-placement="top"
                                        onClick={() => cargarVentas()}
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

                                            {/* GRUPO 1 & 2: FILTRO POR FECHAS Y TOTAL NETO (Diseño de Doble Columna) */}
                                            <div className="row align-items-start mb-4 border-bottom pb-3">
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

                                                <div className="col-lg-1 col-md-1 d-none d-md-flex align-items-center justify-content-center h-100">
                                                    <div className="vr" style={{ height: '100px' }}></div>
                                                </div>

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

                                            {/* FILA 2: ENTIDADES PRINCIPALES (Producto, Cliente, Usuario, Pago) */}
                                            <div className="row align-items-end mb-3">
                                                <div className="col-lg-12"><h6>🔍 Filtros por Atributo</h6></div>

                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Producto</label>
                                                        <AsyncSelect
                                                            cacheOptions
                                                            defaultOptions
                                                            loadOptions={promiseOptions}
                                                            value={filtros.producto}
                                                            onChange={(selected) => handleFiltroChange('producto', selected)}
                                                            placeholder="Buscar producto..."
                                                            isClearable
                                                            noOptionsMessage={() => "Escribe para buscar..."}
                                                            loadingMessage={() => "Cargando..."}
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

                                                {/* Medio de Pago */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Medio de Pago</label>
                                                        <Select
                                                            className="select"
                                                            options={MEDIOS_PAGO}
                                                            value={filtros.medioPago}
                                                            onChange={(selected) => handleFiltroChange('medioPago', selected)}
                                                            placeholder="Seleccionar medio de pago"
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>
                                            </div>

                                            {/* FILA 3: DETALLES DE DOCUMENTO */}
                                            <div className="row align-items-end mb-3 pb-3 border-bottom">

                                                {/* Tipo Venta */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Tipo Venta</label>
                                                        <Select
                                                            className="select"
                                                            options={tipoClienteOptions}
                                                            value={filtros.tipoCliente}
                                                            onChange={(selected) => handleFiltroChange('tipoCliente', selected)}
                                                            placeholder="Seleccionar tipo de venta"
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* Tipo Documento */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Tipo Documento</label>
                                                        <Select
                                                            className="select"
                                                            options={tipoDocumentoOptions}
                                                            value={filtros.tipoDocumento}
                                                            onChange={(selected) => handleFiltroChange('tipoDocumento', selected)}
                                                            placeholder="Boleta o Factura"
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* Estado Asignación */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>Estado Asignación</label>
                                                        <Select
                                                            className="select"
                                                            options={estadoDocumentoOptions}
                                                            value={filtros.pendienteAsignacion}
                                                            onChange={(selected) => handleFiltroChange('pendienteAsignacion', selected)}
                                                            placeholder="Pendiente o Emitido"
                                                            isClearable
                                                        />
                                                    </div>
                                                </div>

                                                {/* N° Documento Parcial */}
                                                <div className="col-lg-3 col-sm-6 col-12">
                                                    <div className="input-blocks">
                                                        <label>N° Documento (ej: B123)</label>
                                                        <input
                                                            type="text"
                                                            className="form-control"
                                                            placeholder="Buscar por número..."
                                                            value={filtros.numDocParcial}
                                                            onChange={(e) => handleFiltroChange('numDocParcial', e.target.value)}
                                                        />
                                                    </div>
                                                </div>


                                                <div className="col-lg-3 col-sm-6 col-12 ms-auto">
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
                                            dataSource={ventas}
                                            pagination={{
                                                current: currentPage + 1,
                                                pageSize: pageSize,
                                                total: totalElements,
                                                onChange: handlePageChange,
                                            }}
                                        />

                                        <div className="pagination-info mt-3 text-center">
                                            <span>
                                                Mostrando {ventas.length} de {totalElements} ventas
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

            <SaleDetailModal
                show={showDetailModal}
                handleClose={handleCloseDetailModal}
                venta={selectedSale}
            />

            <ActualizarDocumentoModal
                show={showDocumentoModal}
                handleClose={handleCloseDocumentoModal}
                venta={selectedSaleForDocumento}
                onDocumentoActualizado={handleDocumentoActualizado}
            />

            <ExportSaleCsvModal
                show={showExportModal}
                handleClose={() => setShowExportModal(false)}
                usuarios={usuarios}
                clientes={clientes}
            />
        </div>
    )
}

export default SalesList;