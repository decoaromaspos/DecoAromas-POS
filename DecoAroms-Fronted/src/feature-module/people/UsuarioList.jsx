import {
  ChevronUp, Edit, Filter, Power, RotateCcw, Search, Sliders, StopCircle, Users
} from "feather-icons-react/build/IconComponents";
import React, { useState, useEffect, useCallback, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link } from "react-router-dom";
import Select from "react-select";
import withReactContent from "sweetalert2-react-content";
import Swal from "sweetalert2";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import Table from "../../core/pagination/datatable";
import { setToogleHeader } from "../../core/redux/action";
import { useAuth } from "../../context/AuthContext";
import {
  getUsuariosFiltrosPaginados, activarUsuario, desactivarUsuario
} from "../../services/usuarioService";
import EditUsuarioModal from "../../core/modals/peoples/EditUsuarioModal";
import CambiarRolModal from "../../core/modals/peoples/CambiarRolModal";


// Opciones para el filtro de roles
const ROL_OPTIONS = [
  { value: "ADMIN", label: "Administrador" },
  { value: "VENDEDOR", label: "Vendedor" },
];

const UsuarioList = () => {
  const dispatch = useDispatch();
  const { usuario: usuarioLogueado } = useAuth();
  const data = useSelector((state) => state.toggle_header);
  const MySwal = withReactContent(Swal);

  const [isFilterVisible, setIsFilterVisible] = useState(false);
  const [usuarios, setUsuarios] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const [showEditModal, setShowEditModal] = useState(false);
  const [showCambiarRolModal, setShowCambiarRolModal] = useState(false);
  const [selectedUsuario, setSelectedUsuario] = useState(null);

  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [estadoSeleccionado, setEstadoSeleccionado] = useState({
    value: true,
    label: "Activos",
  });

  const searchTimeoutRef = useRef(null);
  const nombreBusquedaRef = useRef(nombreBusqueda);
  nombreBusquedaRef.current = nombreBusqueda;

  const estadoOptions = [
    { value: true, label: "Activos" },
    { value: false, label: "Inactivos" },
    { value: null, label: "Todos" },
  ];

  const pageSizeOptions = [
    { value: 10, label: "10 por página" },
    { value: 20, label: "20 por página" },
    { value: 50, label: "50 por página" },
  ];

  // === ESTADOS PARA FILTROS AVANZADOS ===
  const [filtrosAvanzadosForm, setFiltrosAvanzadosForm] = useState({
    correoParcial: "",
    usernameParcial: "",
    rol: null,
  });

  const [filtrosAvanzadosAplicados, setFiltrosAvanzadosAplicados] = useState({
    correoParcial: "",
    usernameParcial: "",
    rol: null,
  });

  // =================================================================
  // === LÓGICA DE CARGA DE DATOS ===
  // =================================================================

  const cargarUsuarios = useCallback(
    async (page, size, sort = "usuarioId") => {
      const filtros = {
        nombreCompletoParcial: nombreBusquedaRef.current,
        activo: estadoSeleccionado.value,
        correoParcial: filtrosAvanzadosAplicados.correoParcial,
        usernameParcial: filtrosAvanzadosAplicados.usernameParcial,
        rol: filtrosAvanzadosAplicados.rol,
      };

      try {
        setLoading(true);
        const response = await getUsuariosFiltrosPaginados(
          page,
          size,
          sort,
          filtros.nombreCompletoParcial,
          filtros.correoParcial,
          filtros.usernameParcial,
          filtros.rol,
          filtros.activo
        );

        setUsuarios(response.content);
        setCurrentPage(response.pageNumber);
        setPageSize(response.pageSize);
        setTotalElements(response.totalElements);
        setTotalPages(response.totalPages);
      } catch (error) {
        console.error("Error al cargar usuarios:", error);
        setUsuarios([]);
        Swal.fire("Error", "No se pudieron cargar los usuarios.", "error");
      } finally {
        setLoading(false);
      }
    },
    [estadoSeleccionado.value, filtrosAvanzadosAplicados]
  );

  useEffect(() => {
    cargarUsuarios(currentPage, pageSize);
  }, [
    currentPage,
    pageSize,
    estadoSeleccionado.value,
    filtrosAvanzadosAplicados,
    cargarUsuarios,
  ]);

  // =================================================================
  // === HANDLERS DE FILTROS Y PAGINACIÓN ===
  // =================================================================

  const handleNombreBusquedaChange = (e) => {
    const valor = e.target.value;
    setNombreBusqueda(valor);
    clearTimeout(searchTimeoutRef.current);

    searchTimeoutRef.current = setTimeout(() => {
      if (currentPage !== 0) {
        setCurrentPage(0);
      } else {
        cargarUsuarios(0, pageSize);
      }
    }, 300);
  };

  const handleEstadoChange = (selected) => {
    setEstadoSeleccionado(selected);
    setCurrentPage(0);
  };

  const handleFiltroAvanzadoChange = (campo, valor) => {
    setFiltrosAvanzadosForm((prev) => ({
      ...prev,
      [campo]: valor,
    }));
  };

  const aplicarFiltros = () => {
    setFiltrosAvanzadosAplicados(filtrosAvanzadosForm);
    setCurrentPage(0);
  };

  const limpiarFiltrosAvanzados = () => {
    const filtrosLimpios = { correoParcial: "", usernameParcial: "", rol: null };
    setFiltrosAvanzadosForm(filtrosLimpios);
    setFiltrosAvanzadosAplicados(filtrosLimpios);
    setCurrentPage(0);
  };

  const handlePageChange = (page) => setCurrentPage(page - 1);

  const handlePageSizeChange = (selected) => {
    setPageSize(selected.value);
    setCurrentPage(0);
  };

  const toggleFilterVisibility = () => setIsFilterVisible(!isFilterVisible);



  // =================================================================
  // === HANDLERS PARA ABRIR Y CERRAR MODALES ===
  // =================================================================
  const handleEditClick = (usuario) => {
    setSelectedUsuario(usuario);
    setShowEditModal(true);
  };

  const handleCambiarRolClick = (usuario) => {
    setSelectedUsuario(usuario);
    setShowCambiarRolModal(true);
  };

  const handleCloseModals = () => {
    setShowEditModal(false);
    setShowCambiarRolModal(false);
    setSelectedUsuario(null);
  };

  const handleUsuarioActualizado = () => {
    cargarUsuarios(currentPage, pageSize);
  };

  // =================================================================
  // === LÓGICA DE ACCIONES (Activar/Desactivar) ===
  // =================================================================

  const handleActivarDesactivar = (usuario) => {
    if (usuarioLogueado.rol === 'VENDEDOR') {
      MySwal.fire("Acción no permitida", "No tienes permisos para realizar esta acción.", "error");
      return;
    }

    if (usuario.usuarioId === usuarioLogueado.usuarioId) { // Corregido para usar usuarioId consistentemente
      MySwal.fire("Acción no permitida", "No puedes desactivar tu propia cuenta.", "error");
      return;
    }
    if (usuario.rol === 'SUPER_ADMIN') {
      MySwal.fire("Acción no permitida", "No es posible desactivar a un Super Administrador.", "error");
      return;
    }
    if (usuarioLogueado.rol === 'ADMIN' && usuario.rol === 'ADMIN') {
      MySwal.fire("Acción no permitida", "Un Administrador no puede desactivar a otro.", "error");
      return;
    }

    // Si ninguna de las reglas anteriores lo impide, se procede con la confirmación
    const isCurrentlyActive = usuario.activo;
    const actionText = isCurrentlyActive ? "desactivar" : "activar";
    const serviceFunction = isCurrentlyActive ? desactivarUsuario : activarUsuario;

    MySwal.fire({
      title: "¿Estás seguro?",
      text: `¿Quieres ${actionText} al usuario "${usuario.nombreCompleto}"?`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: isCurrentlyActive ? "#dc3545" : "#198754",
      confirmButtonText: `Sí, ${actionText}`,
      cancelButtonText: "Cancelar",
    }).then(async (result) => {
      if (result.isConfirmed) {
        try {
          await serviceFunction(usuario.usuarioId);
          MySwal.fire(
            isCurrentlyActive ? "Desactivado" : "Activado",
            `El usuario ha sido ${actionText}.`,
            "success"
          );
          cargarUsuarios(currentPage, pageSize);
        } catch (error) {
          MySwal.fire("Error", `Hubo un error al ${actionText} el usuario.`, "error");
        }
      }
    });
  };

  // === CONFIGURACIÓN DE LA TABLA ===
  const columns = [
    {
      title: "Nombre Completo",
      dataIndex: "nombreCompleto",
      sorter: (a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto),
    },
    {
      title: "Username",
      dataIndex: "username",
      sorter: (a, b) => a.username.localeCompare(b.username),
    },
    {
      title: "Correo",
      dataIndex: "correo",
      sorter: (a, b) => a.correo.localeCompare(b.correo),
    },
    {
      title: "Rol",
      dataIndex: "rol",
      render: (rol) => {
        let badgeClass = 'bg-secondary';
        if (rol === 'ADMIN') badgeClass = 'bg-primary';
        if (rol === 'VENDEDOR') badgeClass = 'bg-info';
        if (rol === 'SUPER_ADMIN') badgeClass = 'bg-warning text-dark';
        return <span className={`badge ${badgeClass}`}>{rol}</span>;
      },
      sorter: (a, b) => a.rol.localeCompare(b.rol),
    },
    {
      title: "Estado",
      dataIndex: "activo",
      render: (activo) => (
        <span className={`badge ${activo ? "bg-success" : "bg-danger"}`}>
          {activo ? "Activo" : "Inactivo"}
        </span>
      ),
      sorter: (a, b) => a.activo - b.activo,
    },
    {
      title: "Acciones",
      render: (text, record) => {
        // Lógica de permisos para habilitar/deshabilitar botones
        const esMiPropiaCuenta = record.usuarioId === usuarioLogueado.usuarioId;
        const esSuperAdminTarget = record.rol === 'SUPER_ADMIN';
        const esAdminTarget = record.rol === 'ADMIN';

        const puedeVerAcciones = usuarioLogueado.rol === 'ADMIN' || usuarioLogueado.rol === 'SUPER_ADMIN';

        // Permisos para Editar Perfil y Cambiar Rol (tu lógica original, sin cambios)
        let puedeEditarYCambiarRol = false;
        if (puedeVerAcciones && !esMiPropiaCuenta && !esSuperAdminTarget) {
          if (usuarioLogueado.rol === 'SUPER_ADMIN') {
            puedeEditarYCambiarRol = true;
          } else if (usuarioLogueado.rol === 'ADMIN' && !esAdminTarget) {
            puedeEditarYCambiarRol = true;
          }
        }

        let puedeActivarDesactivar = false;
        if (puedeVerAcciones && !esMiPropiaCuenta && !esSuperAdminTarget) {
          if (usuarioLogueado.rol === 'SUPER_ADMIN') {
            puedeActivarDesactivar = true; // Super Admin puede con todos menos consigo mismo
          } else if (usuarioLogueado.rol === 'ADMIN' && !esAdminTarget) {
            puedeActivarDesactivar = true; // Admin puede con Vendedores
          }
        }

        if (!puedeVerAcciones) {
          return <td>-</td>;
        }

        return (
          <td className="action-table-data">
            <div className="edit-delete-action">
              {/* Botón de Editar Perfil */}
              <OverlayTrigger placement="top" overlay={<Tooltip>{puedeEditarYCambiarRol ? "Editar Perfil" : "No tienes permisos"}</Tooltip>}>
                <span className="d-inline-block">
                  <Link
                    className={`me-2 p-2 ${!puedeEditarYCambiarRol ? 'disabled-link' : ''}`}
                    to="#" onClick={(e) => { e.preventDefault(); if (puedeEditarYCambiarRol) handleEditClick(record); }}
                    style={!puedeEditarYCambiarRol ? { pointerEvents: "none" } : {}}
                  >
                    <Edit className="feather-edit" />
                  </Link>
                </span>
              </OverlayTrigger>

              {/* Botón de Cambiar Rol */}
              <OverlayTrigger placement="top" overlay={<Tooltip>{puedeEditarYCambiarRol ? "Cambiar Rol" : "No tienes permisos"}</Tooltip>}>
                <span className="d-inline-block">
                  <Link
                    className={`me-2 p-2 ${!puedeEditarYCambiarRol ? 'disabled-link' : ''}`}
                    to="#" onClick={(e) => { e.preventDefault(); if (puedeEditarYCambiarRol) handleCambiarRolClick(record); }}
                    style={!puedeEditarYCambiarRol ? { pointerEvents: "none" } : {}}
                  >
                    <Users className="feather-users" />
                  </Link>
                </span>
              </OverlayTrigger>

              {/* Botón de Activar/Desactivar */}
              <OverlayTrigger
                placement="top"
                overlay={
                  <Tooltip>
                    {puedeActivarDesactivar
                      ? (record.activo ? "Desactivar usuario" : "Activar usuario")
                      : "No tienes permisos"
                    }
                  </Tooltip>
                }
              >
                <span className="d-inline-block">
                  <Link
                    className={`confirm-text me-2 p-2 ${!puedeActivarDesactivar ? 'disabled-link' : ''}`}
                    to="#"
                    onClick={(e) => { e.preventDefault(); if (puedeActivarDesactivar) handleActivarDesactivar(record); }}
                    style={!puedeActivarDesactivar ? { pointerEvents: "none" } : {}}
                  >
                    <Power className={`feather-power ${record.activo ? "text-danger" : "text-success"}`} />
                  </Link>
                </span>
              </OverlayTrigger>
            </div>
          </td>
        );
      },
    },
  ];

  return (
    <div className="page-wrapper">
      <div className="content">
        <div className="page-header">
          <div className="add-item d-flex">
            <div className="page-title">
              <h4>Usuarios</h4>
              <h6>Gestión de usuarios del sistema</h6>
            </div>
          </div>
          <ul className="table-top-head">
            <li>
              <OverlayTrigger placement="top" overlay={<Tooltip>Actualizar</Tooltip>}>
                <Link onClick={() => cargarUsuarios(currentPage, pageSize)} style={{ cursor: "pointer" }}>
                  <RotateCcw />
                </Link>
              </OverlayTrigger>
            </li>
            <li>
              <OverlayTrigger placement="top" overlay={<Tooltip>Colapsar</Tooltip>}>
                <Link id="collapse-header" className={data ? "active" : ""} onClick={(e) => { e.preventDefault(); dispatch(setToogleHeader(!data)); }}>
                  <ChevronUp />
                </Link>
              </OverlayTrigger>
            </li>
          </ul>
          {/* SE ELIMINA EL BOTÓN DE CREAR USUARIO PARA EL ROL ADMIN */}
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
                    value={nombreBusqueda}
                    onChange={handleNombreBusquedaChange}
                  />
                  <Link to="#" className="btn btn-searchset"><Search /></Link>
                </div>
              </div>

              <div className="d-flex align-items-center">
                <div className="form-sort me-3">
                  <StopCircle className="info-img" />
                  <Select
                    className="select"
                    options={estadoOptions}
                    value={estadoSeleccionado}
                    onChange={handleEstadoChange}
                  />
                </div>
                <div className="form-sort">
                  <Sliders className="info-img" />
                  <Select
                    className="select"
                    options={pageSizeOptions}
                    value={pageSizeOptions.find((opt) => opt.value === pageSize)}
                    onChange={handlePageSizeChange}
                  />
                </div>
                <div className="search-path ms-3">
                  <Link className={`btn btn-filter ${isFilterVisible ? "setclose" : ""}`} id="filter_search" onClick={toggleFilterVisibility}>
                    <Filter className="filter-icon" />
                    <span><i className="feather-x" /></span>
                  </Link>
                </div>
              </div>
            </div>

            {/* === FILTROS AVANZADOS (Desplegable) === */}
            <div
              className={`card${isFilterVisible ? " visible" : ""}`}
              id="filter_inputs"
              style={{ display: isFilterVisible ? "block" : "none" }}
            >
              <div className="card-body pb-0">
                <div className="row">
                  <div className="col-lg-12 col-sm-12">
                    <div className="row align-items-end">

                      <div className="col-lg-3 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Username</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por username..."
                            value={filtrosAvanzadosForm.usernameParcial}
                            onChange={(e) => handleFiltroAvanzadoChange("usernameParcial", e.target.value)}
                          />
                        </div>
                      </div>

                      <div className="col-lg-3 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Correo</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por correo..."
                            value={filtrosAvanzadosForm.correoParcial}
                            onChange={(e) => handleFiltroAvanzadoChange("correoParcial", e.target.value)}
                          />
                        </div>
                      </div>

                      <div className="col-lg-3 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Rol</label>
                          <Select
                            className="select"
                            options={ROL_OPTIONS}
                            value={ROL_OPTIONS.find(opt => opt.value === filtrosAvanzadosForm.rol) || null}
                            onChange={(selected) => handleFiltroAvanzadoChange("rol", selected ? selected.value : null)}
                            placeholder="Seleccionar rol"
                            isClearable
                          />
                        </div>
                      </div>

                      <div className="col-lg-3 col-sm-6 col-12 ms-auto">
                        <div className="input-blocks">
                          <div style={{ height: "20px", visibility: "hidden" }}>Label</div>
                          <div className="btn-group w-100">
                            <button className="btn btn-primary me-2" onClick={aplicarFiltros} disabled={loading}>
                              <i
                                data-feather="search"
                                className="feather-search me-1"
                              />
                              Aplicar
                            </button>
                            <button className="btn btn-secondary" onClick={limpiarFiltrosAvanzados} disabled={loading}>
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
                <div className="text-center p-4"><div className="spinner-border" role="status"><span className="visually-hidden">Cargando...</span></div></div>
              ) : (
                <>
                  <Table
                    columns={columns}
                    dataSource={usuarios}
                    pagination={{
                      current: currentPage + 1,
                      pageSize: pageSize,
                      total: totalElements,
                      onChange: (page) => handlePageChange(page),
                    }}
                  />
                  <div className="pagination-info mt-3 text-center">
                    <span>Mostrando {usuarios.length} de {totalElements} usuarios (Página {currentPage + 1} de {totalPages})</span>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {selectedUsuario && (
        <>
          <EditUsuarioModal
            show={showEditModal}
            handleClose={handleCloseModals}
            usuario={selectedUsuario}
            onUsuarioUpdated={handleUsuarioActualizado}
          />
          <CambiarRolModal
            show={showCambiarRolModal}
            handleClose={handleCloseModals}
            usuario={selectedUsuario}
            onRolActualizado={handleUsuarioActualizado}
          />
        </>
      )}
    </div>
  );
};

export default UsuarioList;