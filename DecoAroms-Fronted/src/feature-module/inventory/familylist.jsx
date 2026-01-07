import React, { useState, useEffect, useCallback } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { ChevronUp, PlusCircle, RotateCcw, Sliders, Edit, Trash2, Power, StopCircle } from 'feather-icons-react/build/IconComponents';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import AddFamilyList from '../../core/modals/inventory/addfamilylist';
import EditFamilyList from '../../core/modals/inventory/editfamilylist';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import Table from '../../core/pagination/datatable';
import {
    getFamiliasByFiltrosPaginados,
    desactivarFamilia,
    activarFamilia,
    eliminarFamilia
} from '../../services/familiaService';
import { useAuth } from '../../context/AuthContext';

const FamilyList = () => {
    const dispatch = useDispatch();
    const data = useSelector((state) => state.toggle_header);
    const { usuario } = useAuth();
    const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

    const [familias, setFamilias] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [familiaEditando, setFamiliaEditando] = useState(null);
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


    // Función para cargar familias con filtros
    const cargarFamilias = useCallback(async (page, size, sort = 'nombre') => {
        try {
            setLoading(true);

            const isDeletedFilter = mapEstadoToIsDeleted(filtros.estado);
            const nombreFilter = filtros.nombre.trim();

            const response = await getFamiliasByFiltrosPaginados(
                page,
                size,
                sort,
                isDeletedFilter, // false, true o null
                nombreFilter
            );

            setFamilias(response.content);
            setCurrentPage(response.pageNumber);
            setPageSize(response.pageSize);
            setTotalElements(response.totalElements);
            setTotalPages(response.totalPages);

        } catch (error) {
            console.error('Error al cargar familias:', error);
        } finally {
            setLoading(false);
        }
    }, [filtros.estado, filtros.nombre]); // Depende de los filtros



    useEffect(() => {
        cargarFamilias(0, pageSize);
    }, [cargarFamilias, pageSize]);



    // Función para recargar después de crear/editar/activar/desactivar
    const handleFamiliaRefreshed = () => {
        cargarFamilias(currentPage, pageSize);
    };

    const handleFamiliaCreada = () => {
        handleFamiliaRefreshed();
    };

    const handleFamiliaActualizada = () => {
        handleFamiliaRefreshed();
    };


    // Función para cambiar de página
    const handlePageChange = (page) => {
        const newPage = page - 1;
        setCurrentPage(newPage);
        // Cuando cambiamos de página, llamamos directamente a cargarFamilias
        cargarFamilias(newPage, pageSize);
    };

    // Función para cambiar el tamaño de página
    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(0); // El cambio de pageSize dispara el useEffect superior
    };

    // Función para manejar cambios en el filtro de estado
    const handleEstadoChange = (selected) => {
        setFiltros(prev => ({
            ...prev,
            estado: selected.value
        }));
        setCurrentPage(0); // El cambio de estado dispara el useEffect superior
    };

    // Implementa la búsqueda en tiempo real (inmediata) **
    const handleNombreChange = (e) => {
        setFiltros(prev => ({
            ...prev,
            nombre: e.target.value
        }));
        setCurrentPage(0);
    };

    // Función para manejar la edición
    const handleEditarFamilia = (familia) => {
        setFamiliaEditando({
            familiaId: familia.familiaId,
            nombre: familia.nombre,
            isDeleted: familia.isDeleted,
            cantidadProductosAsociados: familia.cantidadProductosAsociados
        });
        setTimeout(() => {
            openModal('edit-family');
        }, 50);
    };

    // Función para abrir el modal (necesaria para el manejo en React)
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


    // Función para desactivar una familia
    const handleDesactivarFamilia = async (familia) => {
        try {
            await desactivarFamilia(familia.familiaId);
            MySwal.fire({
                title: '¡Desactivada!',
                text: `La familia ${familia.nombre} ha sido desactivada.`,
                icon: 'success',
                confirmButtonText: 'OK'
            }).then(() => {
                handleFamiliaRefreshed(); // Usar la función de refresco
            });
        } catch (error) {
            console.error('Error al desactivar familia:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo desactivar la familia.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };

    // Función para activar una familia
    const handleActivarFamilia = async (familia) => {
        try {
            await activarFamilia(familia.familiaId);
            MySwal.fire({
                title: '¡Activada!',
                text: `La familia ${familia.nombre} ha sido activada.`,
                icon: 'success',
                confirmButtonText: 'OK'
            }).then(() => {
                handleFamiliaRefreshed(); // Usar la función de refresco
            });
        } catch (error) {
            console.error('Error al activar familia:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo activar la familia.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };

    // Función para eliminar permanentemente una familia
    const handleEliminarFamilia = async (familia) => {
        try {
            await eliminarFamilia(familia.familiaId);
            MySwal.fire({
                title: '¡Eliminada!',
                text: `La familia ${familia.nombre} ha sido eliminada permanentemente.`,
                icon: 'success',
                confirmButtonText: 'OK'
            }).then(() => {
                handleFamiliaRefreshed(); // Usar la función de refresco
            });
        } catch (error) {
            console.error('Error al eliminar familia:', error);
            MySwal.fire({
                title: 'Error',
                text: 'No se pudo eliminar la familia.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
    };

    const MySwal = withReactContent(Swal);

    // Alerta de confirmación para desactivar
    const showDesactivarAlert = (familia) => {
        MySwal.fire({
            title: '¿Desactivar familia?',
            text: `¿Estás seguro de que quieres desactivar la familia "${familia.nombre}"?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#ffc107',
            confirmButtonText: 'Sí, desactivar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                handleDesactivarFamilia(familia);
            }
        });
    };

    // Alerta de confirmación para activar
    const showActivarAlert = (familia) => {
        MySwal.fire({
            title: '¿Activar familia?',
            text: `¿Estás seguro de que quieres activar la familia "${familia.nombre}"?`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            confirmButtonText: 'Sí, activar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                handleActivarFamilia(familia);
            }
        });
    };

    // Alerta de confirmación para eliminar permanentemente
    const showEliminarAlert = (familia) => {
        MySwal.fire({
            title: '¿Eliminar permanentemente?',
            html: `¿Estás seguro de que quieres eliminar permanentemente la familia "<strong>${familia.nombre}</strong>"?<br><br><small>Esta acción no se puede deshacer.</small>`,
            icon: 'error',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                handleEliminarFamilia(familia);
            }
        });
    };


    // Opciones para el estado
    const estadoOptions = [
        { value: 'activo', label: "Activas" },
        { value: 'inactivo', label: "Inactivas" },
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
                    {!isDeleted ? 'Activa' : 'Inactiva'}
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
                        {/* Botón Editar - Siempre visible */}
                        <OverlayTrigger placement="top" overlay={<Tooltip>Editar familia</Tooltip>}>
                            <Link
                                className="me-2 p-2"
                                to="#"
                                data-bs-toggle="modal"
                                data-bs-target="#edit-family"
                                onClick={() => handleEditarFamilia(record)}
                            >
                                <Edit className="feather-edit" />
                            </Link>
                        </OverlayTrigger>

                        {/* Para familias activas */}
                        {!record.isDeleted && (
                            <OverlayTrigger placement="top" overlay={<Tooltip>Desactivar familia</Tooltip>}>
                                <Link
                                    className="me-2 p-2 text-danger"
                                    to="#"
                                    onClick={() => showDesactivarAlert(record)}
                                >
                                    <Power className="feather-power" style={{ opacity: 0.7, transform: 'scale(0.9)' }} />
                                </Link>
                            </OverlayTrigger>
                        )}

                        {/* Para familias inactivas */}
                        {record.isDeleted && (
                            <>
                                {/* Botón Activar */}
                                <OverlayTrigger placement="top" overlay={<Tooltip>Activar familia</Tooltip>}>
                                    <Link
                                        className="me-2 p-2 text-success"
                                        to="#"
                                        onClick={() => showActivarAlert(record)}
                                    >
                                        <Power className="feather-power" />
                                    </Link>
                                </OverlayTrigger>

                                {/* Botón Eliminar - Solo si no tiene productos asociados */}
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
                                                No se puede eliminar permanentemente. Existen {record.cantidadProductosAsociados} productos asociados a esta familia. Reasigne los productos.
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
                                <h4>Familias</h4>
                                <h6>Gestión de familias de productos</h6>
                            </div>
                        </div>
                        <ul className="table-top-head">
                            <li>
                                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                                    <Link
                                        data-bs-toggle="tooltip"
                                        data-bs-placement="top"
                                        onClick={() => handleFamiliaRefreshed()} // Usamos la función de refresco
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
                                    data-bs-target="#add-family"
                                    onClick={() => openModal('add-family')}
                                    type="button"
                                >
                                    <PlusCircle className="me-2" />
                                    Crear Nueva Familia
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
                                            onChange={handleNombreChange} // Búsqueda en tiempo real
                                        // ** onKeyPress y onClick eliminados para evitar llamadas redundantes **
                                        />
                                        <Link
                                            to="#"
                                            className="btn btn-searchset"
                                            onClick={() => cargarFamilias(0, pageSize)} // Se puede dejar un botón de búsqueda manual, aunque no es necesario
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
                                            dataSource={familias}
                                            pagination={{
                                                current: currentPage + 1,
                                                pageSize: pageSize,
                                                total: totalElements,
                                                onChange: handlePageChange,
                                            }}
                                        />

                                        <div className="pagination-info mt-3 text-center">
                                            <span>
                                                Mostrando {familias.length} de {totalElements} familias
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
            <AddFamilyList onDataUpdated={handleFamiliaCreada} />
            <EditFamilyList
                familiaEditando={familiaEditando}
                onFamiliaActualizada={handleFamiliaActualizada}
            />
        </div>
    )
}

export default FamilyList;