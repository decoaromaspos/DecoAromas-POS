import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import { ChevronUp, RotateCcw, Sliders, Filter, Eye, Lock, Unlock } from 'feather-icons-react/build/IconComponents';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import Table from '../../core/pagination/datatable';
import OpenCashboxModal from './opencashboxmodal';
import CloseCashboxModal from './closecashboxmodal';
import CashboxViewModal from './cashboxviewmodal';
import {
  getCajasByFiltrosPaginados,
  getCajaAbierta
} from '../../services/cajaService';
import { getUsuarios } from '../../services/usuarioService';
import { useAuth } from "../../context/AuthContext";


// Opciones de filtros
const ESTADO_OPTIONS = [
  { value: 'ABIERTA', label: 'Abierta' },
  { value: 'CERRADA', label: 'Cerrada' },
];

const CUADRADA_OPTIONS = [
  { value: true, label: 'Sí (Cuadrada)' },
  { value: false, label: 'No (Con diferencia)' },
];

const pageSizeOptions = [
  { value: 5, label: "5 por página" },
  { value: 10, label: "10 por página" },
  { value: 20, label: "20 por página" },
  { value: 50, label: "50 por página" },
];

// Función de ayuda para formatear la fecha/hora
const formatearFechaHora = (fechaISO) => {
  if (!fechaISO) return 'N/A';
  const fecha = new Date(fechaISO);
  return fecha.toLocaleDateString('es-CL') + ' ' + fecha.toLocaleTimeString('es-CL', {
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Función de ayuda para formatear moneda (asume CLP/pesos chilenos o similar)
const formatearMoneda = (monto) => {
  if (monto === null || monto === undefined) return 'N/A';
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0
  }).format(monto);
};


const Cashboxes = () => {
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);
  const { usuario } = useAuth();
  const CURRENT_USER_ID = usuario.usuarioId;
  const isSuperAdmin = usuario && usuario.rol === 'SUPER_ADMIN';

  const [isFilterVisible, setIsFilterVisible] = useState(false);
  const [cajas, setCajas] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingHeader, setLoadingHeader] = useState(false);
  const [cajaAbiertaData, setCajaAbiertaData] = useState(null);

  // Estados para modales
  const [showOpenModal, setShowOpenModal] = useState(false);
  const [showCloseModal, setShowCloseModal] = useState(false);

  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedCaja, setSelectedCaja] = useState(null);

  // Estados para filtros de Cajas
  const [filtros, setFiltros] = useState({
    fechaInicio: '',
    fechaFin: '',
    estado: null,
    cuadrada: null,
    usuario: null
  });


  // Función para comprobar si hay una caja abierta (envuelta en useCallback)
  const comprobarCajaAbierta = useCallback(async () => {
    setLoadingHeader(true);
    try {
      const caja = await getCajaAbierta();
      // Si la llamada es exitosa (status 200 OK)
      setCajaAbiertaData(caja);
    } catch (error) {
      // Manejar el caso 404 Not Found (no hay caja abierta)
      if (error.response && error.response.status === 404) {
        setCajaAbiertaData(null);
      } else {
        console.error('Error al comprobar caja abierta:', error);
        // Manejar otros errores (ej. de red, 500)
        setCajaAbiertaData(null);
      }
    } finally {
      setLoadingHeader(false);
    }
  }, [setLoadingHeader, setCajaAbiertaData]);

  // Función para cargar usuarios (envuelta en useCallback)
  const cargarUsuarios = useCallback(async () => {
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
  }, [setUsuarios]); // --- AÑADIDO --- (Dependencias estables)

  // Función principal para cargar cajas con filtros combinados (envuelta en useCallback)
  const cargarCajas = useCallback(async (page = currentPage, size = pageSize) => {
    try {
      setLoading(true);

      const params = {
        fechaInicio: filtros.fechaInicio || null,
        fechaFin: filtros.fechaFin || null,
        estado: filtros.estado ? filtros.estado.value : null,
        cuadrada: filtros.cuadrada ? filtros.cuadrada.value : null,
        usuarioId: filtros.usuario ? filtros.usuario.value : null,
      };

      const response = await getCajasByFiltrosPaginados(
        page,
        size,
        'fechaApertura',
        params.fechaInicio,
        params.fechaFin,
        params.estado,
        params.cuadrada,
        params.usuarioId
      );

      setCajas(response.content);
      setCurrentPage(response.pageNumber);
      setPageSize(response.pageSize);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Error al cargar cajas:', error);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, filtros, setCajas, setCurrentPage, setPageSize, setTotalElements, setTotalPages, setLoading]);

  // Cargar datos que no cambian (usuarios, estado de caja) solo al montar
  useEffect(() => {
    cargarUsuarios();
    comprobarCajaAbierta();
  }, [cargarUsuarios, comprobarCajaAbierta]); // Deps son estables (solo setters)

  // Cargar cajas solo al montar. Las cargas posteriores se manejan por eventos (paginación, filtros).
  useEffect(() => {
    cargarCajas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // <-- Intencionalmente vacío para que solo se ejecute al montar


  // Función para aplicar filtros
  const aplicarFiltros = () => {
    setCurrentPage(0);
    cargarCajas(0, pageSize);
  };

  // Función para limpiar filtros
  const limpiarFiltros = () => {
    setFiltros({
      fechaInicio: '',
      fechaFin: '',
      estado: null,
      cuadrada: null,
      usuario: null
    });
    setCurrentPage(0);
    cargarCajas(0, pageSize);
  };

  // Función para cambiar de página
  const handlePageChange = (page) => {
    const newPage = page - 1;
    setCurrentPage(newPage);
    cargarCajas(newPage, pageSize);
  };

  // Función para cambiar el tamaño de página
  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(0);
    cargarCajas(0, size);
  };

  // Función para manejar cambios en los filtros
  const handleFiltroChange = (campo, valor) => {
    setFiltros(prev => ({
      ...prev,
      [campo]: valor
    }));
  };

  // --- NUEVA FUNCIÓN PARA ABRIR EL MODAL DE VISUALIZACIÓN ---
  const handleViewCaja = (caja) => {
    setSelectedCaja(caja); // Guarda la caja seleccionada
    setShowViewModal(true); // Abre el modal
  };

  // --- CIERRE DEL MODAL DE VISUALIZACIÓN ---
  const handleCloseViewModal = () => {
    setShowViewModal(false);
    setSelectedCaja(null);
  };


  const toggleFilterVisibility = () => {
    setIsFilterVisible((prevVisibility) => !prevVisibility);
  };

  const renderRefreshTooltip = (props) => (
    <Tooltip id="refresh-tooltip" {...props}>
      Actualizar Tablas y Estado de Caja
    </Tooltip>
  );

  const renderCollapseTooltip = (props) => (
    <Tooltip id="collapse-tooltip" {...props}>
      Colapsar
    </Tooltip>
  );

  const renderViewTooltip = (props) => (
    <Tooltip id="view-tooltip" {...props}>
      Ver Detalle
    </Tooltip>
  );

  const renderCerrarCajaTooltip = (props) => (
    <Tooltip id="view-tooltip" {...props}>
      Cerrar Caja
    </Tooltip>
  );


  const handleOpenCloseFromView = (caja) => {
    setCajaAbiertaData(caja); // Seteamos la data necesaria para el modal de cierre
    setShowCloseModal(true);  // Abrimos el modal de cierre
  };


  // Lógica para el botón de Abrir/Cerrar Caja
  const handleCajaAction = () => {
    if (cajaAbiertaData) {
      // Si hay caja abierta: la acción es CERRAR
      setShowCloseModal(true);
    } else {
      // Si NO hay caja abierta: la acción es ABRIR
      setShowOpenModal(true);
    }
  };

  // Función para refrescar todo (tabla y estado de caja)
  const handleFullRefresh = () => {
    comprobarCajaAbierta();
    cargarCajas();
  };


  // Definición de las columnas de la tabla 
  const columns = useMemo(() => [
    {
      title: "Fecha Apertura",
      dataIndex: "fechaApertura",
      sorter: (a, b) => new Date(a.fechaApertura) - new Date(b.fechaApertura),
      key: "fechaApertura",
      width: '18%',
      render: (fecha) => formatearFechaHora(fecha),
    },
    {
      title: "Fecha Cierre",
      dataIndex: "fechaCierre",
      sorter: (a, b) => new Date(a.fechaCierre) - new Date(b.fechaCierre),
      key: "fechaCierre",
      width: '18%',
      render: (fecha) => formatearFechaHora(fecha),
    },
    {
      title: "Efectivo Apertura",
      dataIndex: "efectivoApertura",
      sorter: (a, b) => a.efectivoApertura - b.efectivoApertura,
      key: "efectivoApertura",
      width: '12%',
      render: (monto) => formatearMoneda(monto),
    },
    {
      title: "Efectivo Cierre",
      dataIndex: "efectivoCierre",
      sorter: (a, b) => (a.efectivoCierre || 0) - (b.efectivoCierre || 0),
      key: "efectivoCierre",
      width: '12%',
      render: (monto) => formatearMoneda(monto),
    },
    {
      title: "Estado",
      dataIndex: "estado",
      sorter: (a, b) => a.estado.localeCompare(b.estado),
      key: "estado",
      width: '8%',
      render: (estado) => (
        <span className={`badge ${estado === 'ABIERTA' ? 'bg-success' : 'bg-danger'}`}>
          {estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'}
        </span>
      ),
    },
    {
      title: "Usuario",
      dataIndex: "nombreUsuario",
      sorter: (a, b) => a.nombreUsuario.localeCompare(b.nombreUsuario),
      key: "nombreUsuario",
      width: '15%',
    },
    {
      title: "Caja Cuadrada",
      dataIndex: "diferenciaReal",
      sorter: (a, b) => a.diferenciaReal - b.diferenciaReal,
      key: "cuadrada",
      width: '8%',
      render: (diferenciaReal) => {
        const esCuadrada = diferenciaReal === 0;
        return (
          <span className={`badge ${esCuadrada ? 'bg-success' : 'bg-warning'}`}>
            {esCuadrada ? 'Sí' : 'NO'}
          </span>
        );
      },
    },
    {
      title: "Acciones",
      dataIndex: "actions",
      key: "actions",
      width: '9%',
      render: (text, record) => (
        <td className="action-table-data">
          <div className="edit-delete-action">

            <OverlayTrigger placement="top" overlay={renderViewTooltip}>
              {/* Botón de Ver (Ojo) */}
              <Link
                className="btn-sm me-2 p-2"
                onClick={() => handleViewCaja(record)}
              >
                <Eye size={16} />
              </Link>
            </OverlayTrigger>

            {/* Botón de Cerrar (Solo si está ABIERTA y no es SUPER_ADMIN) */}
            {record.estado === "ABIERTA" && !isSuperAdmin && (
              <OverlayTrigger placement="top" overlay={renderCerrarCajaTooltip}>
                <Link
                  className="btn btn-sm btn-danger"
                  onClick={() => handleOpenCloseFromView(record)}
                  title="Cerrar Caja"
                >
                  <Lock size={16} />
                </Link>
              </OverlayTrigger>
            )}
          </div>
        </td>
      ),
    },
  ], [isSuperAdmin]);


  return (
    <div>
      <div className="page-wrapper">
        <div className="content">
          <div className="page-header">
            <div className="page-title me-auto">
              <h4>Gestión de Cajas</h4>
              <h6>Historial de aperturas y cierres de cajas</h6>
            </div>

            {/* Nueva estructura para los botones de la cabecera */}
            <div className="d-flex align-items-center">
              <ul className="table-top-head">
                <li>
                  {/* Botón de Refrescar (Actualizar todo) */}
                  <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                    <Link
                      data-bs-toggle="tooltip"
                      data-bs-placement="top"
                      onClick={handleFullRefresh}
                      style={{ cursor: 'pointer' }}
                    >
                      <RotateCcw />
                    </Link>
                  </OverlayTrigger>
                </li>
                <li>
                  {/* Botón de Colapsar */}
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

              {/* Botón dinámico de Abrir/Cerrar Caja (Oculto para SUPER_ADMIN) */}
              {!isSuperAdmin && (
                <div className="page-btn ms-3">
                  <button
                    className={`btn ${cajaAbiertaData ? 'btn-danger-light' : 'btn-added'}`}
                    onClick={handleCajaAction}
                    type="button"
                    disabled={loadingHeader}
                    style={{
                      backgroundColor: cajaAbiertaData ? '#ffcccc' : '',
                      color: cajaAbiertaData ? '#dc3545' : 'white',
                      border: '1px solid ' + (cajaAbiertaData ? '#dc3545' : 'transparent'),
                      transition: 'all 0.3s ease'
                    }}
                  >
                    {loadingHeader ? (
                      'Cargando...'
                    ) : (
                      <>
                        {cajaAbiertaData ? <Lock className="me-2" /> : <Unlock className="me-2" />}
                        {cajaAbiertaData ? 'Cerrar Caja' : 'Abrir Caja'}
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>

          </div>

          <div className="card table-list-card">
            <div className="card-body">
              <div className="table-top">
                <div>
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
                      <div className="row align-items-end">

                        {/* Filtro por Fechas */}
                        <div className="col-lg-2 col-sm-6 col-12">
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

                        <div className="col-lg-2 col-sm-6 col-12">
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

                        {/* Filtro por Estado */}
                        <div className="col-lg-2 col-sm-6 col-12">
                          <div className="input-blocks">
                            <label>Estado</label>
                            <Select
                              className="select"
                              options={ESTADO_OPTIONS}
                              value={filtros.estado}
                              onChange={(selected) => handleFiltroChange('estado', selected)}
                              placeholder="Seleccionar estado"
                              isClearable
                            />
                          </div>
                        </div>

                        {/* Filtro por Cuadrada */}
                        <div className="col-lg-2 col-sm-6 col-12">
                          <div className="input-blocks">
                            <label>Caja Cuadrada</label>
                            <Select
                              className="select"
                              options={CUADRADA_OPTIONS}
                              value={filtros.cuadrada}
                              onChange={(selected) => handleFiltroChange('cuadrada', selected)}
                              placeholder="Seleccionar si es cuadrada"
                              isClearable
                            />
                          </div>
                        </div>

                        {/* Filtro por Usuario */}
                        <div className="col-lg-2 col-sm-6 col-12">
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

                        {/* Botones de acción */}
                        <div className="col-lg-2 col-sm-6 col-12">
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
                      dataSource={cajas}
                      pagination={{
                        current: currentPage + 1,
                        pageSize: pageSize,
                        total: totalElements,
                        onChange: handlePageChange,
                      }}
                    />

                    <div className="pagination-info mt-3 text-center">
                      <span>
                        Mostrando {cajas.length} de {totalElements} cajas
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

      {/* MODAL ABRIR CAJA */}
      <OpenCashboxModal
        show={showOpenModal}
        onClose={() => setShowOpenModal(false)}
        onSuccess={handleFullRefresh} // Ejecuta recarga tras éxito
        usuarioId={CURRENT_USER_ID} // Pasar el ID del usuario logueado
      />

      {/* MODAL CIERRE CAJA */}
      <CloseCashboxModal
        show={showCloseModal}
        onClose={() => setShowCloseModal(false)}
        onSuccess={handleFullRefresh} // Ejecuta recarga tras éxito
        cajaInfo={cajaAbiertaData} // Pasar la info completa de la caja abierta
      />

      {/* MODAL VER DETALLE CAJA */}
      <CashboxViewModal
        show={showViewModal}
        handleClose={handleCloseViewModal}
        caja={selectedCaja} // Pasar el ID de la caja seleccionada
        onOpenCloseModal={handleOpenCloseFromView}
      />

    </div>
  )
}

export default Cashboxes;