import React, { useState, useEffect, useCallback } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { ChevronUp, PlusCircle, RotateCcw, StopCircle, Sliders, Edit, Trash2, Power } from 'feather-icons-react/build/IconComponents';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import AddCategoryList from '../../core/modals/inventory/addcategorylist';
import EditCategoryList from '../../core/modals/inventory/editcategorylist';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import Table from '../../core/pagination/datatable';
import {
    getAromasByFiltrosPaginados,
    desactivarAroma,
    activarAroma,
    eliminarAroma
} from '../../services/aromaService'; 
import { useAuth } from '../../context/AuthContext';

const CategoryList = () => {
    const dispatch = useDispatch();
    const data = useSelector((state) => state.toggle_header);
    const { usuario } = useAuth();
    const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

    const [aromas, setAromas] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [aromaEditando, setAromaEditando] = useState(null);
    const [loading, setLoading] = useState(false);
    
    const [filtros, setFiltros] = useState({
        estado: 'activo', // 'activo', 'inactivo', 'todos'
        nombre: ""
    });

    // Función para mapear el filtro de estado a isDeleted (booleano o null)
    const mapEstadoToIsDeleted = (estado) => {
        switch (estado) {
            case 'activo':
                return false; // isDeleted = false (Activo)
            case 'inactivo':
                return true;  // isDeleted = true (Inactivo)
            case 'todos':
            default:
                return null;  // isDeleted = null (Ignorar el filtro de estado)
        }
    };

    // Función para cargar aromas con filtros y paginación
    const cargarAromas = useCallback(async (page, size, sort = 'nombre') => {
        try {
            setLoading(true);
            
            const isDeletedFilter = mapEstadoToIsDeleted(filtros.estado);
            const nombreFilter = filtros.nombre.trim(); 

            const response = await getAromasByFiltrosPaginados(
                page,
                size,
                sort,
                isDeletedFilter,
                nombreFilter
            );

            setAromas(response.content);
            setCurrentPage(response.pageNumber);
            setPageSize(response.pageSize);
            setTotalElements(response.totalElements);
            setTotalPages(response.totalPages);

        } catch (error) {
            console.error('Error al cargar aromas:', error);
        } finally {
            setLoading(false);
        }
    }, [filtros.estado, filtros.nombre]); // Depende de los filtros

    // 1. Efecto principal para la carga y el filtrado
    // Se ejecuta inmediatamente cuando cambia cargarAromas (por cambio de filtros) o el pageSize
    useEffect(() => {
        cargarAromas(0, pageSize);
    }, [cargarAromas, pageSize]);


    // Función para cambiar de página (no reinicia filtros, solo cambia la paginación)
    const handlePageChange = (page) => {
        const newPage = page - 1;
        setCurrentPage(newPage);
        cargarAromas(newPage, pageSize); 
    };

    // Función para cambiar el tamaño de página
    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(0); // Se reinicia en el useEffect superior
    };

    // Función para manejar cambios en el filtro de estado
    const handleEstadoChange = (selected) => {
        setFiltros(prev => ({
            ...prev,
            estado: selected.value
        }));
        setCurrentPage(0); // Se reinicia en el useEffect superior
    };

    // Función para manejar cambios en la búsqueda por nombre (responsiva)
    const handleNombreChange = (e) => {
        setFiltros(prev => ({
            ...prev,
            nombre: e.target.value
        }));
        setCurrentPage(0); 
    };

    // Función para recargar después de crear/editar/activar/desactivar
    const handleAromaRefreshed = () => {
        cargarAromas(currentPage, pageSize); 
    };

    const handleEditarAroma = (aroma) => {
        setAromaEditando({
            aromaId: aroma.aromaId,
            nombre: aroma.nombre,
            isDeleted: aroma.isDeleted,
            cantidadProductosAsociados: aroma.cantidadProductosAsociados
        });
        setTimeout(() => {
        }, 50);
    };

    const MySwal = withReactContent(Swal);
    
    // Alerta de confirmación para desactivar
    const showDesactivarAlert = (aroma) => {
      MySwal.fire({
        title: '¿Desactivar aroma?',
        text: `¿Estás seguro de que quieres desactivar el aroma "${aroma.nombre}"?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ffc107',
        confirmButtonText: 'Sí, desactivar',
        cancelButtonText: 'Cancelar'
      }).then((result) => {
        if (result.isConfirmed) {
          handleDesactivarAroma(aroma);
        }
      });
    };
    
    // Función para desactivar un aroma
    const handleDesactivarAroma = async (aroma) => {
        try {
            await desactivarAroma(aroma.aromaId);
            MySwal.fire({
                title: '¡Desactivado!',
                text: `El aroma ${aroma.nombre} ha sido desactivado.`,
                icon: 'success',
                confirmButtonText: 'OK'
            }).then(() => {
                handleAromaRefreshed();
            });
        } catch (error) {
            console.error('Error al desactivar aroma:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo desactivar el aroma.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };
    
    // Alerta de confirmación para activar
    const showActivarAlert = (aroma) => {
      MySwal.fire({
        title: '¿Activar aroma?',
        text: `¿Estás seguro de que quieres activar el aroma "${aroma.nombre}"?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        confirmButtonText: 'Sí, activar',
        cancelButtonText: 'Cancelar'
      }).then((result) => {
        if (result.isConfirmed) {
          handleActivarAroma(aroma);
        }
      });
    };
    
    // Función para activar un aroma
    const handleActivarAroma = async (aroma) => {
        try {
            await activarAroma(aroma.aromaId);
            MySwal.fire({
                title: '¡Activado!',
                text: `El aroma ${aroma.nombre} ha sido activado.`,
                icon: 'success',
                confirmButtonText: 'OK'
            }).then(() => {
                handleAromaRefreshed();
            });
        } catch (error) {
            console.error('Error al activar aroma:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo activar el aroma.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };
    
    // Alerta de confirmación para eliminar permanentemente
    const showEliminarAlert = (aroma) => {
      MySwal.fire({
        title: '¿Eliminar permanentemente?',
        html: `¿Estás seguro de que quieres eliminar permanentemente el aroma "<strong>${aroma.nombre}</strong>"?<br><br><small>Esta acción no se puede deshacer.</small>`,
        icon: 'error',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
      }).then((result) => {
        if (result.isConfirmed) {
          handleEliminarAroma(aroma);
        }
      });
    };
    
    // Función para eliminar permanentemente un aroma
    const handleEliminarAroma = async (aroma) => {
        try {
            await eliminarAroma(aroma.aromaId);
            MySwal.fire({
                title: '¡Eliminado!',
                text: `El aroma ${aroma.nombre} ha sido eliminado permanentemente.`,
                icon: 'success',
                confirmButtonText: 'OK' 
            }).then(() => {
                handleAromaRefreshed();
            });
        } catch (error) {
            console.error('Error al eliminar aroma:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo eliminar el aroma.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };
    
    const openModal = (modalId) => {
        const modalElement = document.getElementById(modalId);
        if (modalElement) {
            if (window.bootstrap && window.bootstrap.Modal) {
                const modal = new window.bootstrap.Modal(modalElement);
                modal.show();
            } else {
                modalElement.style.display = 'block';
                modalElement.classList.add('show');
            }
        }
    };



    // Opciones para el estado
    const estadoOptions = [
        { value: 'activo', label: "Activos" },
        { value: 'inactivo', label: "Inactivos" },
        { value: 'todos', label: "Todos los estados" },
    ];

    // Opciones para el tamaño de página
    const pageSizeOptions = [
        { value: 5, label: "5 por página" },
        { value: 10, label: "10 por página" },
        { value: 20, label: "20 por página" },
        { value: 50, label: "50 por página" },
    ];

    const renderRefreshTooltip = (props) => (
        <Tooltip id="refresh-tooltip" {...props}>
            Actualizar
        </Tooltip>
    );

    const renderCollapseTooltip = (props) => (
        <Tooltip id="refresh-tooltip" {...props}>
            Colapsar
        </Tooltip>
    );

    const columns = [
        {
            title: "Nombre",
            dataIndex: "nombre",
            sorter: (a, b) => a.nombre.localeCompare(b.nombre),
            key: "nombre",
            width: esAdmin ? '30%' : '50%',
        },
        {
            title: "Estado",
            dataIndex: "isDeleted",
            render: (isDeleted) => (
                <span className={`badge ${!isDeleted ? 'bg-success' : 'bg-danger'}`}>
                    {!isDeleted ? 'Activo' : 'Inactivo'}
                </span>
            ),
            sorter: (a, b) => a.isDeleted - b.isDeleted,
            key: "estado",
            width: '25%',
        },
        {
            title: "Productos Asociados",
            dataIndex: "cantidadProductosAsociados",
            key: "productosAsociados",
            width: '25%',
            render: (cantidad) => (
                <span className={cantidad > 0 ? 'text-info fw-bold' : ''}>
                    {cantidad}
                </span>
            ),
        },
    ];

    // Si es admin, añade la columna de Acciones
    if (esAdmin) {
        columns.push({
            title: 'Acciones',
            dataIndex: 'actions',
            key: 'actions',
            render: (text, record) => (
                <td className="action-table-data">
                    <div className="edit-delete-action">
                        {/* Botón Editar */}
                        <OverlayTrigger placement="top" overlay={<Tooltip>Editar aroma</Tooltip>}>
                            <Link
                                className="me-2 p-2"
                                to="#"
                                data-bs-toggle="modal"
                                data-bs-target="#edit-category"
                                onClick={() => {
                                    handleEditarAroma(record);
                                    setTimeout(() => openModal('edit-category'), 50);
                                }}
                            >
                                <Edit className="feather-edit" />
                            </Link>
                        </OverlayTrigger>

                        {/* Botón Desactivar (si está activo) */}
                        {!record.isDeleted && (
                            <OverlayTrigger placement="top" overlay={<Tooltip>Desactivar aroma</Tooltip>}>
                                <Link
                                    className="me-2 p-2 text-danger"
                                    to="#"
                                    onClick={() => showDesactivarAlert(record)}
                                >
                                    <Power className="feather-power" style={{ opacity: 0.7, transform: 'scale(0.9)' }} />
                                </Link>
                            </OverlayTrigger>
                        )}

                        {/* Botones Activar y Eliminar (si está inactivo) */}
                        {record.isDeleted && (
                            <>
                                <OverlayTrigger placement="top" overlay={<Tooltip>Activar aroma</Tooltip>}>
                                    <Link
                                        className="me-2 p-2 text-success"
                                        to="#"
                                        onClick={() => showActivarAlert(record)}
                                    >
                                        <Power className="feather-power" />
                                    </Link>
                                </OverlayTrigger>

                                {record.cantidadProductosAsociados === 0 ? (
                                    <OverlayTrigger placement="top" overlay={<Tooltip>Eliminar permanentemente</Tooltip>}>
                                        <Link
                                            className="confirm-text p-2"
                                            to="#"
                                            onClick={() => showEliminarAlert(record)}
                                        >
                                            <Trash2 className="feather-trash-2" />
                                        </Link>
                                    </OverlayTrigger>
                                ) : (
                                    <OverlayTrigger
                                        placement="top"
                                        overlay={
                                            <Tooltip>
                                                No se puede eliminar permanentemente. Existen {record.cantidadProductosAsociados} productos asociados a este aroma. Reasigne los productos.
                                            </Tooltip>
                                        }
                                    >
                                        <span className="p-2 text-muted" style={{ cursor: 'not-allowed', opacity: 0.5 }}>
                                            <Trash2 className="feather-trash-2" />
                                        </span>
                                    </OverlayTrigger>
                                )}
                            </>
                        )}
                    </div>
                </td>
            ),
            width: '20%',
            align: 'left',
        });
    }

    return (
        <div>
            <div className="page-wrapper">
                <div className="content">
                    <div className="page-header">
                        <div className="add-item d-flex">
                            <div className="page-title">
                                <h4>Aromas</h4>
                                <h6>Gestión de aromas</h6>
                            </div>
                        </div>
                        <ul className="table-top-head">
                            <li>
                                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                                    <Link
                                        data-bs-toggle="tooltip"
                                        data-bs-placement="top"
                                        onClick={() => handleAromaRefreshed()}
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
                        {esAdmin && (
                            <div className="page-btn">
                                <button
                                    className="btn btn-added"
                                    data-bs-toggle="modal"
                                    data-bs-target="#add-category"
                                    onClick={() => openModal('add-category')}
                                    type="button"
                                >
                                    <PlusCircle className="me-2" />
                                    Crear Nuevo Aroma
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="card table-list-card">
                        <div className="card-body">
                            <div className="table-top">
                                <div className="search-set">
                                    <div className="search-input">
                                        <input
                                            type="text"
                                            placeholder="Buscar por nombre..."
                                            className="form-control form-control-sm formsearch"
                                            value={filtros.nombre}
                                            onChange={handleNombreChange}
                                        />
                                        <Link
                                            to="#"
                                            className="btn btn-searchset"
                                        >
                                            <i data-feather="search" className="feather-search" />
                                        </Link>
                                    </div>
                                </div>

                                <div className="search-path">
                                    <div className="form-sort me-3">
                                        <StopCircle className="info-img" />
                                        <Select
                                            options={estadoOptions}
                                            className="select"
                                            placeholder="Seleccionar estado"
                                            value={estadoOptions.find(opt => opt.value === filtros.estado)}
                                            onChange={handleEstadoChange}
                                        />
                                    </div>
                                </div>

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
                                            dataSource={aromas}
                                            pagination={{
                                                current: currentPage + 1,
                                                pageSize: pageSize,
                                                total: totalElements,
                                                onChange: handlePageChange,
                                            }}
                                        />

                                        <div className="pagination-info mt-3 text-center">
                                            <span>
                                                Mostrando {aromas.length} de {totalElements} aromas
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
            <AddCategoryList onAromaCreado={handleAromaRefreshed} />
            <EditCategoryList
                aromaEditando={aromaEditando}
                onAromaActualizado={handleAromaRefreshed}
            />
        </div>
    )
}

export default CategoryList;