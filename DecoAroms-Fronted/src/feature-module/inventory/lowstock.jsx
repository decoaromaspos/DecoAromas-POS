import React, { useState, useEffect, useCallback, useRef } from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import { ChevronUp, RotateCcw, Sliders, Filter, Eye, Edit, Inbox } from 'feather-icons-react/build/IconComponents';
import { useDispatch, useSelector } from 'react-redux';
import { setToogleHeader } from '../../core/redux/action';
import Select from 'react-select';
import { all_routes } from "../../Router/all_routes";
import EditLowStock from '../../core/modals/inventory/editlowstock';
import Table from '../../core/pagination/datatable';
import {
  getProductosBajoStockByFiltrosPaginados,
  getProductosFueraStockByFiltrosPaginados
} from '../../services/stockProductosService';
import { getAromas } from '../../services/aromaService';
import { getFamilias } from '../../services/familiaService';
import ProductViewModal from "../../core/modals/inventory/productviewmodal";
import ProductStockModal from "../../core/modals/inventory/productstockmodal";
import { useAuth } from "../../context/AuthContext";

const LowStock = () => {
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

  const [activeTab, setActiveTab] = useState('bajo-stock');
  const [isFilterVisible, setIsFilterVisible] = useState(false);
  const [productos, setProductos] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const [showModal, setShowModal] = useState(false); // Estado para abrir/cerrar el modal
  const [selectedProduct, setSelectedProduct] = useState(null); // Producto a mostrar en el modal
  const [showStockModal, setShowStockModal] = useState(false);

  const handleOpenStockModal = (productData) => {
    setSelectedProduct(productData);
    setShowStockModal(true);
  };

  const handleCloseStockModal = () => {
    setShowStockModal(false);
    setSelectedProduct(null);
  };

  const handleStockUpdate = () => {
    cargarProductos(currentPage, pageSize, filtrosAplicados);
  };



  // Estados para los filtros aplicados (valores que se usan para la API)
  const [filtrosAplicados, setFiltrosAplicados] = useState({
    aroma: null,
    familia: null,
    nombre: "",
    umbral: 10 // Valor por defecto para bajo stock
  });

  // Estados para los filtros en el formulario (valores temporales en los inputs)
  const [filtrosFormulario, setFiltrosFormulario] = useState({
    aroma: null,
    familia: null,
    nombre: "",
    umbral: 10
  });

  // Estados para opciones de select
  const [aromass, setAromas] = useState([]);
  const [familias, setFamilias] = useState([]);

  // Ref para el timeout de la búsqueda en tiempo real (debounce)
  const searchTimeoutRef = useRef(null);

    /**
   * Función principal para cargar productos usando los métodos unificados.
   * Envuelto en useCallback para evitar warnings en useEffect.
   * @param {number} page - Número de página.
   * @param {number} size - Tamaño de página.
   * @param {object} filtrosToApply - Los filtros APLICADOS a usar en la llamada a la API.
   */
  const cargarProductos = useCallback(async (page, size, filtrosToApply) => {
    try {
      setLoading(true);
      let response;
      const aromaId = filtrosToApply.aroma?.value || null;
      const familiaId = filtrosToApply.familia?.value || null;
      const nombre = filtrosToApply.nombre.trim() === "" ? null : filtrosToApply.nombre.trim();
      const sortBy = 'stock';

      if (activeTab === 'bajo-stock') {
        response = await getProductosBajoStockByFiltrosPaginados(
          page,
          size,
          sortBy,
          aromaId,
          familiaId,
          nombre,
          filtrosToApply.umbral 
        );
      } else {
        response = await getProductosFueraStockByFiltrosPaginados(
          page,
          size,
          sortBy,
          aromaId,
          familiaId,
          nombre
        );
      }

      setProductos(response.content);
      setCurrentPage(response.pageNumber);
      setPageSize(response.pageSize);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Error al cargar productos:', error);
      setProductos([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [activeTab]); // Dependencia vital: cambia el comportamiento según la pestaña

  // Cargar opciones al montar el componente
  useEffect(() => {
    cargarAromas();
    cargarFamilias();
  }, []);

  // Efecto PRINCIPAL para cargar productos
  // Se dispara al montar, al cambiar página, size, filtros O al cambiar activeTab (porque recrea cargarProductos)
  useEffect(() => {
    cargarProductos(currentPage, pageSize, filtrosAplicados);
  }, [cargarProductos, currentPage, pageSize, filtrosAplicados]);

  // Función para cargar aromas y familias
  const cargarAromas = async () => {
    try {
      const aromasData = await getAromas();
      const opcionesAromas = aromasData.map(aroma => ({ value: aroma.aromaId, label: aroma.nombre }));
      setAromas(opcionesAromas);
    } catch (error) {
      console.error('Error al cargar aromas:', error);
    }
  };

  const cargarFamilias = async () => {
    try {
      const familiasData = await getFamilias();
      const opcionesFamilias = familiasData.map(familia => ({ value: familia.familiaId, label: familia.nombre }));
      setFamilias(opcionesFamilias);
    } catch (error) {
      console.error('Error al cargar familias:', error);
    }
  };

  // Función para APLICAR los filtros del formulario a los filtros aplicados.
  // Esto desencadenará la recarga de productos si la página se resetea a 0.
  const aplicarFiltros = () => {
    clearTimeout(searchTimeoutRef.current); // Detener cualquier búsqueda en tiempo real pendiente

    // 1. Aplicar todos los filtros del formulario
    setFiltrosAplicados(prev => ({
      ...prev,
      aroma: filtrosFormulario.aroma,
      familia: filtrosFormulario.familia,
      nombre: filtrosFormulario.nombre,
      umbral: filtrosFormulario.umbral,
    }));

    // 2. Resetear la paginación a la página 0.
    if (currentPage === 0) {
      // Si ya estamos en la página 0, forzamos la recarga con los nuevos filtros
      cargarProductos(0, pageSize, {
        ...filtrosFormulario,
        aroma: filtrosFormulario.aroma, // Aseguramos usar el estado actual del formulario
        familia: filtrosFormulario.familia,
        nombre: filtrosFormulario.nombre,
        umbral: filtrosFormulario.umbral,
      });
    } else {
      // El useEffect al cambiar currentPage a 0 se encargará de la carga
      setCurrentPage(0);
    }
  };

  // Función para limpiar filtros
  const limpiarFiltros = () => {
    clearTimeout(searchTimeoutRef.current);
    const newFiltros = {
      aroma: null,
      familia: null,
      nombre: "",
      umbral: 10
    };

    // 1. Limpiar filtros del formulario
    setFiltrosFormulario(newFiltros);

    // 2. Aplicar filtros limpios
    setFiltrosAplicados(newFiltros);

    // 3. Resetear y cargar
    setCurrentPage(0);
    // El useEffect se encargará de la recarga
  };

  // Función para cambiar de página (sin cambios)
  const handlePageChange = (page) => {
    const newPage = page - 1;
    if (newPage !== currentPage) {
      setCurrentPage(newPage);
    }
  };

  // Función para cambiar el tamaño de página (sin cambios)
  const handlePageSizeChange = (selected) => {
    const size = selected.value;
    if (size !== pageSize) {
      setPageSize(size);
      setCurrentPage(0); // Forzar a la primera página al cambiar el tamaño
    }
  };

  /**
   * Manejador de cambios para filtros de SELECT y UMERAL.
   * Solo actualiza el estado del FORMULARIO (no aplica filtros a la tabla).
   */
  const handleFiltroFormularioChange = (campo, valor) => {
    setFiltrosFormulario(prev => ({
      ...prev,
      [campo]: valor
    }));
  };

  /**
   * Manejador de cambios en el input de texto de búsqueda (nombre).
   * Implementa la búsqueda en tiempo real con un debounce, actualizando `filtrosAplicados`.
   */
  const handleNombreChange = (e) => {
    const valor = e.target.value;

    // 1. Actualizar el valor en el formulario inmediatamente
    setFiltrosFormulario(prev => ({ ...prev, nombre: valor }));

    // 2. Limpiar el timeout anterior
    clearTimeout(searchTimeoutRef.current);

    // 3. Definir un nuevo timeout (debounce)
    searchTimeoutRef.current = setTimeout(() => {
      // 4. Cuando el usuario se detiene de escribir, APLICAR el filtro de nombre

      // Si el valor aplicado ya es el mismo, no hacemos nada (optimización)
      if (filtrosAplicados.nombre === valor) return;

      const newFiltrosAplicados = {
        ...filtrosAplicados,
        nombre: valor
      };

      // La única diferencia es el nombre. Mantenemos el resto de filtros Aplicados.
      setFiltrosAplicados(newFiltrosAplicados);

      // Resetear la página a 0 para una nueva búsqueda
      if (currentPage !== 0) {
        setCurrentPage(0);
      } else {
        // Si ya estamos en la página 0, forzamos la recarga con los filtros ya aplicados
        cargarProductos(0, pageSize, newFiltrosAplicados);
      }
    }, 300); // Debounce de 300ms
  };


  const toggleFilterVisibility = () => {
    setIsFilterVisible((prevVisibility) => !prevVisibility);
  };

  const handleTabChange = (tab) => {
    if (activeTab !== tab) {
      setActiveTab(tab);
      
      // === LÓGICA MOVIDA DEL useEffect ===
      // 1. Sincronizar formulario (manteniendo aroma/familia, limpiando nombre)
      setFiltrosFormulario({
        aroma: filtrosAplicados.aroma,
        familia: filtrosAplicados.familia,
        nombre: "", 
        umbral: 10
      });

      // 2. Definir nuevos filtros aplicados
      const newFiltros = {
        aroma: filtrosAplicados.aroma,
        familia: filtrosAplicados.familia,
        nombre: "",
        umbral: tab === 'bajo-stock' ? 10 : filtrosAplicados.umbral
      };

      // 3. Aplicar filtros y resetear página
      // Esto disparará el useEffect principal automáticamente
      setFiltrosAplicados(newFiltros);
      setCurrentPage(0);
    }
  };

  // === GESTIÓN DEL MODAL DE DETALLES ===
  const handleShowModal = (product) => {
    setSelectedProduct(product); // Guarda el producto seleccionado
    setShowModal(true); // Abre el modal
  };

  const handleCloseModal = () => {
    setShowModal(false); // Cierra el modal
    setSelectedProduct(null); // Limpia el producto seleccionado
  };

  const route = all_routes;

  // Opciones para el tamaño de página (sin cambios)
  const pageSizeOptions = [
    { value: 5, label: "5 por página" },
    { value: 10, label: "10 por página" },
    { value: 20, label: "20 por página" },
    { value: 50, label: "50 por página" },
  ];

  // Tooltips (sin cambios)
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

  // Columnas (sin cambios, salvo por usar filtrosAplicados.umbral para el color)
  const columns = [
    { title: "SKU", dataIndex: "sku", sorter: (a, b) => a.sku.localeCompare(b.sku), key: "sku", width: '10%' },
    { title: "Nombre", dataIndex: "nombre", sorter: (a, b) => a.nombre.localeCompare(b.nombre), key: "nombre", width: '35%' },
    { title: "Aroma", dataIndex: "aromaNombre", sorter: (a, b) => a.aromaNombre.localeCompare(b.aromaNombre), key: "aroma", width: '15%' },
    { title: "Familia", dataIndex: "familiaNombre", sorter: (a, b) => a.familiaNombre.localeCompare(b.familiaNombre), key: "familia", width: '15%' },
    {
      title: "Stock",
      dataIndex: "stock",
      sorter: (a, b) => a.stock - b.stock,
      key: "stock",
      width: '10%',
      render: (stock) => {
        let className = '';
        if (stock <= 0) {
          className = 'text-danger fw-bold';
        } else if (stock > 0 && stock <= filtrosAplicados.umbral) { // Usamos el umbral APLICADO
          className = 'text-warning fw-bold';
        }

        return (
          <span className={className}>
            {stock}
          </span>
        );
      },
    },
    {
      title: 'Acciones',
      dataIndex: 'actions',
      key: 'actions',
      render: (text, record) => (
        <td className="action-table-data">
          <div className="edit-delete-action">

            {/* Botón de Ver */}
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip>Ver producto</Tooltip>}
            >
              <Link
                className="me-2 p-2"
                to="#"
                onClick={(e) => {
                  e.preventDefault(); // Evitar comportamiento por defecto del link
                  handleShowModal(record); // Llama a la función para abrir el modal
                }}
              >
                <Eye className="feather-view" />
              </Link>
            </OverlayTrigger>


            {/* Botón de Editar */}
            {esAdmin && (
              <OverlayTrigger
                placement="top"
                overlay={<Tooltip>Editar producto</Tooltip>}
              >
                <Link
                  className="me-2 p-2"
                  to={`${route.editproduct}/${record.productoId}`}
                >
                  <Edit className="feather-edit" />
                </Link>
              </OverlayTrigger>
            )}

            {/* Botón de Stock */}
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip>Editar stock</Tooltip>}
            >
              <Link
                className="me-2 p-2"
                to="#"
                onClick={(e) => {
                  e.preventDefault();
                  handleOpenStockModal(record); // Llama a la función para abrir el modal de stock
                }}
              >
                <Inbox className="feather-edit text-warning" />
              </Link>
            </OverlayTrigger>
          </div>
        </td>
      ),
      width: '15%',
      align: 'left',
    },
  ];

  // Componente renderizado de la tabla y filtros
  const renderTableContent = () => (
    <>
      <div className="table-top">
        <div className="search-set">
          <div className="search-input">
            <input
              type="text"
              placeholder="Buscar por nombre..."
              className="form-control form-control-sm formsearch"
              value={filtrosFormulario.nombre} // Usa el estado del formulario
              onChange={handleNombreChange} // Búsqueda en tiempo real
              onKeyPress={(e) => {
                if (e.key === 'Enter') aplicarFiltros(); // Forzar aplicación de filtros (incluye nombre)
              }}
            />
            <Link
              to="#"
              className="btn btn-searchset"
              onClick={aplicarFiltros} // Aplicar filtros al hacer click
            >
              <i data-feather="search" className="feather-search" />
            </Link>
          </div>
        </div>
        <div className="d-flex align-items-center">
          <div className="form-sort me-3">
            <Sliders className="info-img" />
            <Select
              className="select"
              options={pageSizeOptions}
              value={pageSizeOptions.find(opt => opt.value === pageSize)}
              onChange={handlePageSizeChange}
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

      {/* Sección de Filtros Desplegable */}
      <div
        className={`card${isFilterVisible ? " visible" : ""}`}
        id="filter_inputs"
        style={{ display: isFilterVisible ? "block" : "none" }}
      >
        <div className="card-body pb-0">
          <div className="row">
            <div className="col-lg-12 col-sm-12">
              <div className="row align-items-end">
                {/* Input de Umbral (Solo para Bajo Stock) */}
                {activeTab === 'bajo-stock' && (
                  <div className="col-lg-2 col-sm-6 col-12">
                    <div className="input-blocks">
                      <label>Umbral de Stock</label>
                      <input
                        type="number"
                        className="form-control"
                        placeholder="Umbral"
                        value={filtrosFormulario.umbral} // Usa el estado del formulario
                        onChange={(e) => handleFiltroFormularioChange('umbral', parseInt(e.target.value) || 0)}
                        min="0"
                      />
                    </div>
                  </div>
                )}

                {/* Filtro por Aroma */}
                <div className="col-lg-2 col-sm-6 col-12">
                  <div className="input-blocks">
                    <label>Aroma</label>
                    <Select
                      className="select"
                      options={aromass}
                      value={filtrosFormulario.aroma} // Usa el estado del formulario
                      onChange={(selected) => handleFiltroFormularioChange('aroma', selected)}
                      placeholder="Seleccionar aroma"
                      isSearchable
                      isClearable
                    />
                  </div>
                </div>

                {/* Filtro por Familia */}
                <div className="col-lg-2 col-sm-6 col-12">
                  <div className="input-blocks">
                    <label>Familia</label>
                    <Select
                      className="select"
                      options={familias}
                      value={filtrosFormulario.familia} // Usa el estado del formulario
                      onChange={(selected) => handleFiltroFormularioChange('familia', selected)}
                      placeholder="Seleccionar familia"
                      isSearchable
                      isClearable
                    />
                  </div>
                </div>

                {/* Botones de acción (Buscar/Limpiar) */}
                <div className="col-lg-2 col-sm-6 col-12 ms-auto">
                  <div className="input-blocks">
                    <div style={{ height: "20px", visibility: "hidden" }}>Label</div>
                    <div className="btn-group w-100">
                      <button
                        className="btn btn-primary me-2"
                        onClick={aplicarFiltros} // APLICAR FILTROS
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

      {/* Tabla de Productos */}
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
              dataSource={productos}
              pagination={{
                current: currentPage + 1,
                pageSize: pageSize,
                total: totalElements,
                onChange: handlePageChange,
              }}
            />

            <div className="pagination-info mt-3 text-center">
              <span>
                Mostrando {productos.length} de {totalElements} productos
                (Página {currentPage + 1} de {totalPages})
              </span>
            </div>
          </>
        )}
      </div>
    </>
  );


  return (
    <div>
      <div className="page-wrapper">
        <div className="content">
          <div className="page-header">
            <div className="page-title me-auto">
              <h4>Gestión de Bajo Stock</h4>
              <h6>Control de productos con stock bajo y fuera de stock</h6>
            </div>
            <ul className="table-top-head">
              <li>
                <OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
                  <Link
                    data-bs-toggle="tooltip"
                    data-bs-placement="top"
                    // Recarga con los filtros actualmente APLICADOS
                    onClick={() => cargarProductos(currentPage, pageSize, filtrosAplicados)}
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

          <div className="table-tab">
            <ul className="nav nav-pills" id="pills-tab" role="tablist">
              <li className="nav-item" role="presentation">
                <button
                  className={`nav-link ${activeTab === 'bajo-stock' ? 'active' : ''}`}
                  onClick={() => handleTabChange('bajo-stock')}
                >
                  Bajo Stock
                </button>
              </li>
              <li className="nav-item" role="presentation">
                <button
                  className={`nav-link ${activeTab === 'fuera-stock' ? 'active' : ''}`}
                  onClick={() => handleTabChange('fuera-stock')}
                >
                  Fuera de Stock
                </button>
              </li>
            </ul>

            <div className="tab-content" id="pills-tabContent">
              {/* Pestaña Bajo Stock */}
              <div
                className={`tab-pane fade ${activeTab === 'bajo-stock' ? 'show active' : ''}`}
                id="pills-home"
                role="tabpanel"
                aria-labelledby="pills-home-tab"
              >
                <div className="card table-list-card">
                  <div className="card-body">
                    {activeTab === 'bajo-stock' && renderTableContent()}
                  </div>
                </div>
              </div>

              {/* Pestaña Fuera de Stock */}
              <div
                className={`tab-pane fade ${activeTab === 'fuera-stock' ? 'show active' : ''}`}
                id="pills-profile"
                role="tabpanel"
                aria-labelledby="pills-profile-tab"
              >
                <div className="card table-list-card">
                  <div className="card-body">
                    {activeTab === 'fuera-stock' && renderTableContent()}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <EditLowStock />

      {/* === RENDERIZAR EL MODAL DE VISTA DE PRODUCTO === */}
      <ProductViewModal
        show={showModal}
        handleClose={handleCloseModal}
        product={selectedProduct}
      />

      {/* === RENDERIZAR EL MODAL DE STOCK DE PRODUCTO === */}
      <ProductStockModal
        show={showStockModal}
        handleClose={handleCloseStockModal}
        product={selectedProduct}
        onStockUpdated={handleStockUpdate}
      />
    </div>
  )
}

export default LowStock;