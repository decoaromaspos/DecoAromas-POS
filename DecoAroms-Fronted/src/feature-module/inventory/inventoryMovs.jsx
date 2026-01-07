import React, { useState, useEffect } from 'react'
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import { ChevronUp, RotateCcw, Sliders, Filter } from 'feather-icons-react/build/IconComponents';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import AsyncSelect from 'react-select/async';
import Table from '../../core/pagination/datatable'
import {
  getMovimientosByFiltrosPaginados
} from '../../services/movimientosInventarioService';
import { getUsuariosConSuperAdmin } from '../../services/usuarioService';
import { getProductosByNombreParcial } from '../../services/productoService';

const InventoryMovs = () => {
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);

  const [isFilterVisible, setIsFilterVisible] = useState(false);
  const [movimientos, setMovimientos] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  // Estados para filtros
  const [filtros, setFiltros] = useState({
    fechaInicio: '',
    fechaFin: '',
    tipo: null,
    motivo: null,
    usuario: null,
    producto: null
  });

  // Cargar usuarios al montar el componente
  useEffect(() => {
    cargarUsuarios();
    cargarMovimientos();
    // eslint-disable-next-line
  }, []);

  // Función para cargar usuarios
  const cargarUsuarios = async () => {
    try {
      const usuariosData = await getUsuariosConSuperAdmin();
      const opcionesUsuarios = usuariosData.map(usuario => ({
        value: usuario.usuarioId,
        label: `${usuario.username} - ${usuario.nombreCompleto}`
      }));
      setUsuarios(opcionesUsuarios);
    } catch (error) {
      console.error('Error al cargar usuarios:', error);
    }
  };

  // --- LOGICA DEL ASYNC SELECT (PRODUCTOS) ---
  const promiseOptions = (inputValue) => {
    // Solo buscamos si hay texto
    if (!inputValue) {
      return Promise.resolve([]);
    }

    return getProductosByNombreParcial(inputValue)
      .then((listaProductos) => {
        // Mapeamos la respuesta del backend { productoId, nombre } 
        // al formato de react-select { value, label }
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

  // Función principal para cargar movimientos con filtros combinados
  const cargarMovimientos = async (page = currentPage, size = pageSize, filtrosParaUsar = filtros) => {
    try {
      setLoading(true);

      const params = {
        fechaInicio: filtrosParaUsar.fechaInicio || null,
        fechaFin: filtrosParaUsar.fechaFin || null,
        tipo: filtrosParaUsar.tipo ? filtrosParaUsar.tipo.value : null,
        motivo: filtrosParaUsar.motivo ? filtrosParaUsar.motivo.value : null,
        usuarioId: filtrosParaUsar.usuario ? filtrosParaUsar.usuario.value : null,
        productoId: filtrosParaUsar.producto ? filtrosParaUsar.producto.value : null
      };

      const response = await getMovimientosByFiltrosPaginados(
        page,
        size,
        'fecha',
        params.fechaInicio,
        params.fechaFin,
        params.tipo,
        params.motivo,
        params.usuarioId,
        params.productoId
      );

      setMovimientos(response.content);
      setCurrentPage(response.pageNumber);
      setPageSize(response.pageSize);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Error al cargar movimientos:', error);
    } finally {
      setLoading(false);
    }
  };

  // Función para aplicar filtros
  const aplicarFiltros = () => {
    setCurrentPage(0);
    cargarMovimientos(0, pageSize);
  };

  // Función para limpiar filtros
  const limpiarFiltros = () => {
    const filtrosLimpios = {
      fechaInicio: '',
      fechaFin: '',
      tipo: null,
      motivo: null,
      usuario: null,
      producto: null
    };

    setFiltros(filtrosLimpios);
    setCurrentPage(0);
    cargarMovimientos(0, pageSize, filtrosLimpios);
  };

  // Función para cambiar de página
  const handlePageChange = (page) => {
    const newPage = page - 1;
    setCurrentPage(newPage);
    cargarMovimientos(newPage, pageSize);
  };

  // Función para cambiar el tamaño de página
  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(0);
    cargarMovimientos(0, size);
  };

  // Función para manejar cambios en los filtros
  const handleFiltroChange = (campo, valor) => {
    setFiltros(prev => ({
      ...prev,
      [campo]: valor
    }));
  };

  const toggleFilterVisibility = () => {
    setIsFilterVisible((prevVisibility) => !prevVisibility);
  };

  // Opciones para el tamaño de página
  const pageSizeOptions = [
    { value: 5, label: "5 por página" },
    { value: 10, label: "10 por página" },
    { value: 20, label: "20 por página" },
    { value: 50, label: "50 por página" },
  ];

  // Opciones para tipo
  const tipoOptions = [
    { value: 'ENTRADA', label: 'Entrada' },
    { value: 'SALIDA', label: 'Salida' },
  ];

  // Opciones para motivo
  const motivoOptions = [
    { value: 'VENTA', label: 'Venta' },
    { value: 'AJUSTE_VENTA', label: 'Ajuste de Venta' },
    { value: 'PRODUCCION', label: 'Producción' },
    { value: 'NUEVO_STOCK', label: 'Nuevo Stock' },
    { value: 'CORRECCION', label: 'Corrección' },
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

  // Función para formatear la fecha
  const formatearFecha = (fechaISO) => {
    const fecha = new Date(fechaISO);
    return fecha.toLocaleDateString('es-CL') + ' ' + fecha.toLocaleTimeString('es-CL', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const columns = [
    {
      title: "Fecha",
      dataIndex: "fecha",
      sorter: (a, b) => new Date(a.fecha) - new Date(b.fecha),
      key: "fecha",
      width: '20%',
      render: (fecha) => formatearFecha(fecha),
    },
    {
      title: "Tipo",
      dataIndex: "tipo",
      sorter: (a, b) => a.tipo.localeCompare(b.tipo),
      key: "tipo",
      width: '10%',
      render: (tipo) => (
        <span className={`badge ${tipo === 'ENTRADA' ? 'bg-success' : 'bg-danger'}`}>
          {tipo === 'ENTRADA' ? 'Entrada' : 'Salida'}
        </span>
      ),
    },
    {
      title: "Motivo",
      dataIndex: "motivo",
      sorter: (a, b) => a.motivo.localeCompare(b.motivo),
      key: "motivo",
      width: '15%',
      render: (motivo) => {
        const motivosTraducidos = {
          'VENTA': 'Venta',
          'AJUSTE_VENTA': 'Ajuste Venta',
          'PRODUCCION': 'Producción',
          'NUEVO_STOCK': 'Nuevo Stock',
          'CORRECCION': 'Corrección'
        };
        return motivosTraducidos[motivo] || motivo;
      },
    },
    {
      title: "Cantidad",
      dataIndex: "cantidad",
      sorter: (a, b) => a.cantidad - b.cantidad,
      key: "cantidad",
      width: '10%',
      render: (cantidad, record) => (
        <span className={record.tipo === 'ENTRADA' ? 'text-success fw-bold' : 'text-danger fw-bold'}>
          {record.tipo === 'ENTRADA' ? '+' : '-'}{cantidad}
        </span>
      ),
    },
    {
      title: "Producto",
      dataIndex: "productoNombre",
      sorter: (a, b) => a.productoNombre.localeCompare(b.productoNombre),
      key: "producto",
      width: '25%',
    },
    {
      title: "Usuario",
      dataIndex: "nombreCompleto",
      sorter: (a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto),
      key: "usuario",
      width: '20%',
    },
  ];


  return (
    <div>
      <div className="page-wrapper">
        <div className="content">
          <div className="page-header">
            <div className="page-title me-auto">
              <h4>Movimientos de Inventario</h4>
              <h6>Historial de entradas y salidas de productos</h6>
            </div>
            <ul className="table-top-head">
              <li>
                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                  <Link
                    data-bs-toggle="tooltip"
                    data-bs-placement="top"
                    onClick={() => cargarMovimientos()}
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
                  {/* No hay búsqueda por texto en esta vista */}
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

                        {/* 1. Fecha Inicio */}
                        <div className="col-lg-3 col-sm-6 col-12">
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

                        {/* 2. Fecha Fin */}
                        <div className="col-lg-3 col-sm-6 col-12">
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

                        {/* 3. Filtro por Producto (ASYNC SELECT) */}
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

                        {/* 4. Filtro por Usuario */}
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

                        {/* 5. Filtro por Tipo */}
                        <div className="col-lg-3 col-sm-6 col-12">
                          <div className="input-blocks">
                            <label>Tipo</label>
                            <Select
                              className="select"
                              options={tipoOptions}
                              value={filtros.tipo}
                              onChange={(selected) => handleFiltroChange('tipo', selected)}
                              placeholder="Seleccionar tipo"
                              isClearable
                            />
                          </div>
                        </div>

                        {/* 6. Filtro por Motivo */}
                        <div className="col-lg-3 col-sm-6 col-12">
                          <div className="input-blocks">
                            <label>Motivo</label>
                            <Select
                              className="select"
                              options={motivoOptions}
                              value={filtros.motivo}
                              onChange={(selected) => handleFiltroChange('motivo', selected)}
                              placeholder="Seleccionar motivo"
                              isClearable
                            />
                          </div>
                        </div>

                        {/* 7. Botones de acción */}
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
                      dataSource={movimientos}
                      pagination={{
                        current: currentPage + 1,
                        pageSize: pageSize,
                        total: totalElements,
                        onChange: handlePageChange,
                      }}
                    />

                    <div className="pagination-info mt-3 text-center">
                      <span>
                        Mostrando {movimientos.length} de {totalElements} movimientos
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
    </div>
  )
}

export default InventoryMovs;