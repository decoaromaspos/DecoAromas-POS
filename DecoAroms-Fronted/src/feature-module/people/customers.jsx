import {
  ChevronUp,
  Edit,
  Filter,
  PlusCircle,
  RotateCcw,
  Sliders,
  Power,
  Search,
  StopCircle,
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
import {
  getClientesFiltrosPaginados,
  activarCliente,
  desactivarCliente,
  exportarClientesCSV
} from "../../services/clienteService";
import CreateClientModal from "../../core/modals/peoples/CreateClientModal";
import EditClientModal from "../../core/modals/peoples/EditClientModal";
import ImageWithBasePath from '../../core/img/imagewithbasebath';

const TIPO_OPTIONS = [
  { value: "DETALLE", label: "Detalle" },
  { value: "MAYORISTA", label: "Mayorista" },
  { value: null, label: "Todos los Tipos" },
];

const ClienteList = () => {
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header); // Para colapsar header

  const MySwal = withReactContent(Swal);

  const [isFilterVisible, setIsFilterVisible] = useState(false);
  const [clientes, setClientes] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [estadoSeleccionado, setEstadoSeleccionado] = useState({
    value: true,
    label: "Activos",
  });

  const searchTimeoutRef = useRef(null);
  const nombreBusquedaRef = useRef(nombreBusqueda);
  nombreBusquedaRef.current = nombreBusqueda;

  // Opciones para el estado (activo/inactivo/todos)
  const estadoOptions = [
    { value: true, label: "Activos" },
    { value: false, label: "Inactivos" },
    { value: null, label: "Todos" },
  ];

  // Opciones para el tamaño de página
  const pageSizeOptions = [
    { value: 10, label: "10 por página" },
    { value: 20, label: "20 por página" },
    { value: 50, label: "50 por página" },
  ];

  // === ESTADOS PARA FILTROS AVANZADOS (NO REACTIVOS - REQUIEREN BOTÓN APLICAR) ===
  // Estado del formulario (lo que el usuario escribe, no se aplica aún)
  const [filtrosAvanzadosForm, setFiltrosAvanzadosForm] = useState({
    rutParcial: "",
    correoParcial: "",
    telefonoParcial: "",
    ciudadParcial: "", // <-- MODIFICACIÓN: Añadido
    tipo: null,
  });

  // Estado que almacena los filtros avanzados APLICADOS (los que se usan en la API)
  const [filtrosAvanzadosAplicados, setFiltrosAvanzadosAplicados] = useState({
    rutParcial: "",
    correoParcial: "",
    telefonoParcial: "",
    ciudadParcial: "", // <-- MODIFICACIÓN: Añadido
    tipo: null,
  });

  // =================================================================
  // === LÓGICA DE CARGA DE DATOS ===
  // =================================================================

  const cargarClientes = useCallback(
    async (page, size, sort = "nombre") => {
      // Combina filtros reactivos y aplicados
      const filtros = {
        nombreCompletoParcial: nombreBusquedaRef.current,
        activo: estadoSeleccionado.value,
        rutParcial: filtrosAvanzadosAplicados.rutParcial,
        correoParcial: filtrosAvanzadosAplicados.correoParcial,
        telefonoParcial: filtrosAvanzadosAplicados.telefonoParcial,
        ciudadParcial: filtrosAvanzadosAplicados.ciudadParcial, // <-- MODIFICACIÓN: Añadido
        tipo: filtrosAvanzadosAplicados.tipo,
      };

      try {
        setLoading(true);

        // <-- MODIFICACIÓN: Se añade 'filtros.ciudadParcial' al final
        const response = await getClientesFiltrosPaginados(
          page,
          size,
          sort,
          filtros.nombreCompletoParcial,
          filtros.rutParcial,
          filtros.correoParcial,
          filtros.telefonoParcial,
          filtros.tipo,
          filtros.activo,
          filtros.ciudadParcial // <-- Añadido según la nueva firma
        );

        setClientes(response.content);
        setCurrentPage(response.pageNumber);
        setPageSize(response.pageSize);
        setTotalElements(response.totalElements);
        setTotalPages(response.totalPages);
      } catch (error) {
        console.error("Error al cargar clientes:", error);
        setClientes([]);
        setTotalElements(0);
        setTotalPages(0);
        Swal.fire("Error", "No se pudieron cargar los clientes.", "error");
      } finally {
        setLoading(false);
      }
    },
    [estadoSeleccionado.value, filtrosAvanzadosAplicados]
  );

  // Efecto para recargar clientes al cambiar PAGINACIÓN o FILTROS
  useEffect(() => {
    cargarClientes(currentPage, pageSize);
  }, [
    currentPage, pageSize, estadoSeleccionado.value,
    filtrosAvanzadosAplicados, cargarClientes
  ]);

  // =================================================================
  // === HANDLERS DE MODALES DE EDICIÓN ===
  // =================================================================

  // Abre el modal de edición y establece el cliente
  const handleEditClick = (client) => {
    // Almacenamos el cliente completo en el estado para pasarlo al modal
    setSelectedClient(client);
    setShowEditModal(true);
  };

  // Cierra el modal de edición y limpia el cliente
  const handleCloseEditModal = () => {
    setShowEditModal(false);
    setSelectedClient(null);
  };

  // Callback llamado después de que un cliente se ha actualizado exitosamente
  const handleClientUpdated = () => {
    cargarClientes(currentPage, pageSize);
  };

  // =================================================================
  // === HANDLERS DE FILTROS Y PAGINACIÓN (EXISTENTES) ===
  // =================================================================

  // Handler para la BÚSQUEDA RÁPIDA (Nombre Completo) - DEBOUNCE
  const handleNombreBusquedaChange = (e) => {
    const valor = e.target.value;
    setNombreBusqueda(valor); // Actualiza el input visualmente de inmediato
    clearTimeout(searchTimeoutRef.current);

    searchTimeoutRef.current = setTimeout(() => {
      if (currentPage !== 0) {
        setCurrentPage(0);
      } else {
        cargarClientes(0, pageSize);
      }
    }, 300);
  };

  // Handler para el FILTRO REACTIVO de ESTADO
  const handleEstadoChange = (selected) => {
    setEstadoSeleccionado(selected);
    setCurrentPage(0);
  };

  // Handler para cambios en filtros avanzados (Solo actualiza el estado del FORMULARIO)
  const handleFiltroAvanzadoChange = (campo, valor) => {
    setFiltrosAvanzadosForm((prev) => ({
      ...prev,
      [campo]: valor,
    }));
  };

  // Función para APLICAR FILTROS AVANZADOS (Activada por el botón "Aplicar").
  const aplicarFiltros = () => {
    // 1. Actualizamos el estado de los filtros aplicados.
    setFiltrosAvanzadosAplicados(filtrosAvanzadosForm);

    // 2. Si no estamos en la página 0, vamos a ella.
    // El useEffect se encargará de cargar los datos correctos
    // ya sea por el cambio de 'currentPage' o por el cambio de 'filtrosAvanzadosAplicados'.
    if (currentPage !== 0) {
      setCurrentPage(0);
    }
    // Si ya estábamos en la página 0, el useEffect se disparará igualmente
    // porque 'filtrosAvanzadosAplicados' cambió.
  };

  // Función para LIMPIAR FILTROS AVANZADOS
  const limpiarFiltrosAvanzados = () => {
    const filtrosLimpios = {
      rutParcial: "",
      correoParcial: "",
      telefonoParcial: "",
      ciudadParcial: "",
      tipo: null
    };

    setFiltrosAvanzadosForm(filtrosLimpios);
    setFiltrosAvanzadosAplicados(filtrosLimpios);

    if (currentPage !== 0) {
      setCurrentPage(0);
    }
  };

  // Paginación
  const handlePageChange = (page) => {
    setCurrentPage(page - 1);
  };

  // Cambio de tamaño de página
  const handlePageSizeChange = (selected) => {
    setPageSize(selected.value);
    setCurrentPage(0);
  };

  const toggleFilterVisibility = () => {
    setIsFilterVisible((prevVisibility) => !prevVisibility);
  };

  // =================================================================
  // === LÓGICA DE ACCIONES (Activar/Desactivar) (EXISTENTE) ===
  // =================================================================

  // Maneja la acción de activar o desactivar un cliente.
  const handleActivarDesactivar = (cliente) => {
    const isCurrentlyActive = cliente.activo;
    const actionText = isCurrentlyActive ? "desactivar" : "activar";
    const serviceFunction = isCurrentlyActive ? desactivarCliente : activarCliente;

    MySwal.fire({
      title: "¿Estás seguro?",
      text: `¿Quieres ${actionText} al cliente "${cliente.nombre} ${cliente.apellido}"?`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: isCurrentlyActive ? "#dc3545" : "#198754",
      confirmButtonText: `Sí, ${actionText}`,
      cancelButtonText: "Cancelar",
    }).then(async (result) => {
      if (result.isConfirmed) {
        try {
          await serviceFunction(cliente.clienteId);
          MySwal.fire(
            isCurrentlyActive ? "Desactivado" : "Activado",
            `El cliente ha sido ${isCurrentlyActive ? "desactivado" : "activado"}.`,
            "success"
          );
          cargarClientes(currentPage, pageSize);
        } catch (error) {
          MySwal.fire("Error", `Hubo un error al ${actionText} el cliente.`, "error");
        }
      }
    });
  };

  // =================================================================
  // === LÓGICA DE EXPORTACIÓN ===
  // =================================================================

  const handleExportarCSV = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await exportarClientesCSV();
      MySwal.fire("Éxito", "El archivo CSV se ha generado correctamente.", "success");
    } catch (error) {
      MySwal.fire("Error", "No se pudo generar el archivo CSV.", "error");
    } finally {
      setLoading(false);
    }
  };

  // =================================================================
  // === CONFIGURACIÓN DE LA TABLA (EXISTENTE) ===
  // =================================================================

  const renderRefreshTooltip = (props) => (
    <Tooltip id="refresh-tooltip" {...props}>
      Actualizar
    </Tooltip>
  );

  const renderCollapseTooltip = (props) => (
    <Tooltip id="collapse-tooltip" {...props}>
      Colapsar
    </Tooltip>
  );
  const renderExcelTooltip = (props) => (
    <Tooltip id="excel-tooltip" {...props}>
      Exportar a CSV
    </Tooltip>
  );

  // Columnas para la tabla
  const columns = [
    {
      title: "RUT",
      dataIndex: "rut",
      sorter: (a, b) => a.rut.localeCompare(b.rut),
    },
    {
      title: "Nombre Completo",
      dataIndex: "nombreCompleto",
      render: (text, record) => `${record.nombre} ${record.apellido || ''}`.trim(),
      sorter: (a, b) => {
        const fullNameA = `${a.nombre} ${a.apellido || ''}`.trim();
        const fullNameB = `${b.nombre} ${b.apellido || ''}`.trim();
        return fullNameA.localeCompare(fullNameB);
      },
    },
    {
      title: "Correo",
      dataIndex: "correo",
      render: (text) => text || "N/A",
      sorter: (a, b) => (a.correo || "").localeCompare(b.correo || ""),
    },
    {
      title: "Teléfono",
      dataIndex: "telefono",
      render: (text) => text || "N/A",
      sorter: (a, b) => (a.telefono || "").localeCompare(b.telefono || ""),
    },
    {
      title: "Ciudad",
      dataIndex: "ciudad",
      render: (text) => text || "N/A",
      sorter: (a, b) => (a.ciudad || "").localeCompare(b.ciudad || ""),
    },
    {
      title: "Tipo",
      dataIndex: "tipo",
      render: (tipo) => (
        <span className={`badge bg-${tipo === "MAYORISTA" ? "primary" : "info"}`}>
          {tipo}
        </span>
      ),
      sorter: (a, b) => a.tipo.localeCompare(b.tipo),
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
      dataIndex: "action",
      render: (text, record) => (
        <td className="action-table-data">
          <div className="edit-delete-action">

            {/* 5. Botón de Editar (ABRE MODAL) */}
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip>Editar cliente</Tooltip>}
            >
              <Link
                className="me-2 p-2"
                to="#" // Cambiado a '#' para evitar la navegación
                onClick={() => handleEditClick(record)} // Llama al handler para abrir el modal
              >
                <Edit className="feather-edit" />
              </Link>
            </OverlayTrigger>

            {/* Botón de Activar/Desactivar */}
            <OverlayTrigger
              placement="top"
              overlay={record.activo ? (
                <Tooltip>Desactivar cliente</Tooltip>
              ) : (
                <Tooltip>Activar cliente</Tooltip>
              )}
            >
              <Link
                className="confirm-text me-2 p-2"
                to="#"
                onClick={() => handleActivarDesactivar(record)}
              >
                {/* Ícono Power de Feather-icons */}
                <Power
                  className={`feather-power ${record.activo ? "text-danger" : "text-success"}`}
                  title={record.activo ? "Desactivar Cliente" : "Activar Cliente"}
                />
              </Link>
            </OverlayTrigger>
          </div>
        </td>
      ),
    },
  ];

  return (
    <div className="page-wrapper">
      <div className="content">
        <div className="page-header">
          <div className="add-item d-flex">
            <div className="page-title">
              <h4>Clientes</h4>
              <h6>Gestión de la cartera de clientes</h6>
            </div>
          </div>
          <ul className="table-top-head">
            <li>
              <OverlayTrigger placement="top" overlay={renderExcelTooltip}>
                <Link
                  onClick={handleExportarCSV}
                  style={{ cursor: 'pointer' }}
                >
                  <ImageWithBasePath src="assets/img/icons/excel.svg" alt="img" />
                </Link>
              </OverlayTrigger>
            </li>
            <li>
              <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                <Link onClick={() => cargarClientes(currentPage, pageSize)} style={{ cursor: "pointer" }}>
                  <RotateCcw />
                </Link>
              </OverlayTrigger>
            </li>
            <li>
              <OverlayTrigger placement="top" overlay={renderCollapseTooltip}>
                <Link id="collapse-header" className={data ? "active" : ""} onClick={(e) => { e.preventDefault(); dispatch(setToogleHeader(!data)); }}>
                  <ChevronUp />
                </Link>
              </OverlayTrigger>
            </li>
          </ul>
          <div className="page-btn">
            <button className="btn btn-added" onClick={() => setShowCreateModal(true)}>
              <PlusCircle className="me-2 iconsize" />
              Crear Nuevo Cliente
            </button>
          </div>
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

            {/* === FILTROS AVANZADOS (Desplegable) - NO REACTIVOS === */}
            <div
              className={`card${isFilterVisible ? " visible" : ""}`}
              id="filter_inputs"
              style={{ display: isFilterVisible ? "block" : "none" }}
            >
              <div className="card-body pb-0">
                <div className="row">
                  <div className="col-lg-12 col-sm-12">
                    <div className="row align-items-end">

                      {/* Filtro por RUT Parcial */}
                      <div className="col-lg-2 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>RUT</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por RUT..."
                            value={filtrosAvanzadosForm.rutParcial}
                            onChange={(e) =>
                              handleFiltroAvanzadoChange("rutParcial", e.target.value)
                            }
                          />
                        </div>
                      </div>

                      {/* Filtro por Correo Parcial */}
                      <div className="col-lg-2 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Correo</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por correo..."
                            value={filtrosAvanzadosForm.correoParcial}
                            onChange={(e) =>
                              handleFiltroAvanzadoChange("correoParcial", e.target.value)
                            }
                          />
                        </div>
                      </div>

                      {/* Filtro por Teléfono Parcial */}
                      <div className="col-lg-2 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Teléfono</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por teléfono..."
                            value={filtrosAvanzadosForm.telefonoParcial}
                            onChange={(e) =>
                              handleFiltroAvanzadoChange("telefonoParcial", e.target.value)
                            }
                          />
                        </div>
                      </div>

                      {/* <-- MODIFICACIÓN: Nuevo filtro "Ciudad" --> */}
                      <div className="col-lg-2 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Ciudad</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="Buscar por ciudad..."
                            value={filtrosAvanzadosForm.ciudadParcial}
                            onChange={(e) =>
                              handleFiltroAvanzadoChange("ciudadParcial", e.target.value)
                            }
                          />
                        </div>
                      </div>

                      {/* Filtro por Tipo */}
                      <div className="col-lg-2 col-sm-6 col-12">
                        <div className="input-blocks">
                          <label>Tipo</label>
                          <Select
                            className="select"
                            options={TIPO_OPTIONS.filter(opt => opt.value !== null)} // Excluimos "Todos" para este filtro
                            value={TIPO_OPTIONS.find(opt => opt.value === filtrosAvanzadosForm.tipo)}
                            onChange={(selected) =>
                              handleFiltroAvanzadoChange(
                                "tipo",
                                selected ? selected.value : null
                              )
                            }
                            placeholder="Seleccionar tipo"
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
                              disabled={loading}
                            >
                              <i
                                data-feather="search"
                                className="feather-search me-2"
                              />
                              Aplicar
                            </button>
                            <button
                              className="btn btn-secondary"
                              onClick={limpiarFiltrosAvanzados}
                              disabled={loading}
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

            {/* /Filtros */}
            <div className="table-responsive">
              {loading ? (
                <div className="text-center p-4"><div className="spinner-border" role="status"><span className="visually-hidden">Cargando...</span></div></div>
              ) : (
                <>
                  <Table
                    columns={columns}
                    dataSource={clientes}
                    pagination={{
                      current: currentPage + 1,
                      pageSize: pageSize,
                      total: totalElements,
                      onChange: (page) => handlePageChange(page),
                    }}
                  />
                  <div className="pagination-info mt-3 text-center">
                    <span>Mostrando {clientes.length} de {totalElements} clientes (Página {currentPage + 1} de {totalPages})</span>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <CreateClientModal
        show={showCreateModal}
        handleClose={() => setShowCreateModal(false)}
        onClientCreated={() => cargarClientes(0, pageSize)}
      />

      <EditClientModal
        show={showEditModal}
        handleClose={handleCloseEditModal}
        client={selectedClient}
        onClientUpdated={handleClientUpdated}
      />
    </div>
  );
};


export default ClienteList;