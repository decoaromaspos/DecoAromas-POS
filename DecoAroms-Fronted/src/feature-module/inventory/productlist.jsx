import {
	ChevronUp,
	Edit,
	Eye,
	Filter,
	PlusCircle,
	RotateCcw,
	Sliders,
	StopCircle,
	Inbox,
	Power,
} from "feather-icons-react/build/IconComponents";
import React, { useState, useEffect, useCallback, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link } from "react-router-dom";
import Select from "react-select";
import withReactContent from "sweetalert2-react-content";
import Swal from "sweetalert2";
import { all_routes } from "../../Router/all_routes";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import Table from "../../core/pagination/datatable";
import { setToogleHeader } from "../../core/redux/action";
import {
	getProductosByFiltrosPaginados,
	activarProducto,
	desactivarProducto,
} from "../../services/productoService";
import { getAromas } from "../../services/aromaService";
import { getFamilias } from "../../services/familiaService";
import ProductViewModal from "../../core/modals/inventory/productviewmodal";
import ProductStockModal from "../../core/modals/inventory/productstockmodal";
import { useAuth } from '../../context/AuthContext';
import ImageWithBasePath from '../../core/img/imagewithbasebath';
import ExportProductCsvModal from "../../core/modals/inventory/ExportProductCsvModal";

const ProductList = () => {
	const dispatch = useDispatch();
	const data = useSelector((state) => state.toggle_header);
	const { usuario } = useAuth();
	const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

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
	const [showExportModal, setShowExportModal] = useState(false);

	const handleOpenStockModal = (productData) => {
		setSelectedProduct(productData);
		setShowStockModal(true);
	};

	const handleCloseStockModal = () => {
		setShowStockModal(false);
		setSelectedProduct(null);
	};

	const handleStockUpdate = () => {
		cargarProductos(currentPage, pageSize);
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

	// === ESTADOS PARA FILTROS REACTIVOS ===
	const [nombreBusqueda, setNombreBusqueda] = useState("");
	const [valorInputBusqueda, setValorInputBusqueda] = useState("");
	const [estadoSeleccionado, setEstadoSeleccionado] = useState({
		value: true,
		label: "Activos",
	}); // Por defecto: Activos (true)

	// === ESTADOS PARA FILTROS AVANZADOS (NO REACTIVOS - REQUIEREN BOTÓN APLICAR) ===
	const [filtrosAvanzadosForm, setFiltrosAvanzadosForm] = useState({
		sku: "",
		codigoBarras: "",
		aromaId: null, // ID del aroma
		familiaId: null, // ID de la familia
	});

	// Estado que almacena los filtros avanzados APLICADOS (los que se usan en la API)
	const [filtrosAvanzadosAplicados, setFiltrosAvanzadosAplicados] = useState({
		sku: "",
		codigoBarras: "",
		aromaId: null,
		familiaId: null,
	});

	// Ref para el timeout de la búsqueda en tiempo real (debounce)
	const searchTimeoutRef = useRef(null);
	// Opciones de Selects
	const [aromasOptions, setAromasOptions] = useState([]);
	const [familiasOptions, setFamiliasOptions] = useState([]);

	// Opciones para el estado (activo/inactivo/todos)
	const estadoOptions = [
		{ value: true, label: "Activos" },
		{ value: false, label: "Inactivos" },
		{ value: null, label: "Todos los estados" },
	];

	// Función principal unificada para cargar productos con todos los filtros
	const cargarProductos = useCallback(
		async (page, size, sort = "nombre") => {
			// Usamos los filtros aplicados y los reactivos (nombre y estado)
			const filtros = {
				aromaId: filtrosAvanzadosAplicados.aromaId,
				familiaId: filtrosAvanzadosAplicados.familiaId,
				activo: estadoSeleccionado.value,
				nombre: nombreBusqueda,
				sku: filtrosAvanzadosAplicados.sku,
				codigoBarras: filtrosAvanzadosAplicados.codigoBarras,
			};

			try {
				setLoading(true);

				const response = await getProductosByFiltrosPaginados(
					page,
					size,
					sort,
					filtros.aromaId,
					filtros.familiaId,
					filtros.activo,
					filtros.nombre,
					filtros.sku,
					filtros.codigoBarras
				);

				setProductos(response.content);
				setCurrentPage(response.pageNumber);
				setPageSize(response.pageSize);
				setTotalElements(response.totalElements);
				setTotalPages(response.totalPages);
			} catch (error) {
				console.error("Error al cargar productos:", error);
				setProductos([]);
				setTotalElements(0);
				setTotalPages(0);
			} finally {
				setLoading(false);
			}
		},
		[estadoSeleccionado.value, nombreBusqueda, filtrosAvanzadosAplicados]
	);

	// Cargar maestros (aromas y familias) al montar el componente
	useEffect(() => {
		const cargarMaestros = async () => {
			try {
				const [aromasData, familiasData] = await Promise.all([
					getAromas(),
					getFamilias(),
				]);

				const opcionesAromas = aromasData.map((aroma) => ({
					value: aroma.aromaId,
					label: aroma.nombre,
				}));
				setAromasOptions(opcionesAromas);

				const opcionesFamilias = familiasData.map((familia) => ({
					value: familia.familiaId,
					label: familia.nombre,
				}));
				setFamiliasOptions(opcionesFamilias);
			} catch (error) {
				console.error("Error al cargar maestros:", error);
			}
		};
		cargarMaestros();
	}, []);

	// Efecto para recargar productos al cambiar PAGINACIÓN o FILTROS REACTIVOS
	useEffect(() => {
		cargarProductos(currentPage, pageSize);
	}, [cargarProductos, currentPage, pageSize]);

	// Handler para la BÚSQUEDA RÁPIDA (por Nombre) - IMPLEMENTACIÓN REACTIVA CON DEBOUNCE
	const handleNombreBusquedaChange = (e) => {
		const valor = e.target.value;
		setValorInputBusqueda(valor); // 1. Actualiza el valor del input para la UI inmediatamente

		clearTimeout(searchTimeoutRef.current);

		// Establece un temporizador para actualizar el estado de búsqueda y la página
		searchTimeoutRef.current = setTimeout(() => {
			setNombreBusqueda(valor);
			setCurrentPage(0);
		}, 300); // Debounce de 300ms
	};

	// Handler para el FILTRO REACTIVO de ESTADO
	const handleEstadoChange = (selected) => {
		// Al cambiar el estado, reiniciamos la página a 0.
		// El useEffect se encargará de llamar a cargarProductos
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

	//Función para APLICAR FILTROS AVANZADOS (Activada por el botón "Aplicar").
	const aplicarFiltros = () => {
		// Actualizamos el estado de los filtros APLICADOS.
		setFiltrosAvanzadosAplicados(filtrosAvanzadosForm);

		// Si no estamos en la página 0, reseteamos la paginación.
		if (currentPage !== 0) {
			setCurrentPage(0);
		}
	};

	// Función para LIMPIAR FILTROS AVANZADOS
	const limpiarFiltrosAvanzados = () => {
		const filtrosLimpios = {
			sku: "",
			codigoBarras: "",
			aromaId: null,
			familiaId: null,
		};

		setFiltrosAvanzadosForm(filtrosLimpios);
		setFiltrosAvanzadosAplicados(filtrosLimpios);

		if (currentPage !== 0) {
			setCurrentPage(0);
		}
	};

	// Función para cambiar de página
	const handlePageChange = (page) => {
		// La paginación sigue siendo reactiva
		const newPage = page - 1;
		if (newPage !== currentPage) {
			setCurrentPage(newPage);
		}
	};

	// Función para cambiar el tamaño de página
	const handlePageSizeChange = (selected) => {
		// El cambio de tamaño de página sigue siendo reactivo
		const size = selected.value;
		if (size !== pageSize) {
			setPageSize(size);
			setCurrentPage(0); // Forzar a la primera página al cambiar el tamaño
		}
	};

	const toggleFilterVisibility = () => {
		setIsFilterVisible((prevVisibility) => !prevVisibility);
	};

	const route = all_routes;

	// Opciones para el tamaño de página
	const pageSizeOptions = [
		{ value: 10, label: "10 por página" },
		{ value: 20, label: "20 por página" },
		{ value: 50, label: "50 por página" },
	];

	const MySwal = withReactContent(Swal);

	// Maneja la acción de activar o desactivar un producto.
	const handleActivarDesactivar = (producto) => {
		const isCurrentlyActive = producto.activo;
		const actionText = isCurrentlyActive ? "desactivar" : "activar";
		const serviceFunction = isCurrentlyActive
			? desactivarProducto
			: activarProducto;
		const confirmButtonClass = isCurrentlyActive ? "btn-danger" : "btn-success";
		const confirmButtonColor = isCurrentlyActive ? "#dc3545" : "#198754";
		const successTitle = isCurrentlyActive ? "Desactivado!" : "Activado!";
		const successText = isCurrentlyActive
			? `El producto ${producto.nombre} ha sido desactivado.`
			: `El producto ${producto.nombre} ha sido activado.`;

		MySwal.fire({
			title: "¿Estás seguro?",
			text: `¿Quieres ${actionText} el producto "${producto.nombre}"?`,
			icon: "warning",
			showCancelButton: true,
			confirmButtonColor: confirmButtonColor,
			confirmButtonText: `Sí, ${actionText}lo!`,
			cancelButtonColor: "#6c757d",
			cancelButtonText: "Cancelar",
		}).then(async (result) => {
			if (result.isConfirmed) {
				try {
					await serviceFunction(producto.productoId);
					MySwal.fire({
						title: successTitle,
						text: successText,
						icon: "success",
						confirmButtonText: "OK",
						customClass: {
							confirmButton: `btn ${confirmButtonClass}`,
						},
					}).then(() => {
						// Recargar la lista para reflejar el cambio de estado
						cargarProductos(currentPage, pageSize);
					});
				} catch (error) {
					console.error(`Error al ${actionText} producto:`, error);
					MySwal.fire({
						title: "Error",
						text: `Hubo un error al intentar ${actionText} el producto.`,
						icon: "error",
						confirmButtonText: "Cerrar",
						customClass: {
							confirmButton: "btn btn-danger",
						},
					});
				}
			} else {
				MySwal.close();
			}
		});
	};

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

	const renderExcelTooltip = (props) => (
		<Tooltip id="excel-tooltip" {...props}>
			Exportar a CSV
		</Tooltip>
	);

	// Columna para la tabla
	const columns = [
		{
			title: "SKU",
			dataIndex: "sku",
			sorter: (a, b) => a.sku.localeCompare(b.sku),
		},
		{
			title: "Nombre",
			dataIndex: "nombre",
			sorter: (a, b) => a.nombre.localeCompare(b.nombre),
		},
		{
			title: "Stock",
			dataIndex: "stock",
			sorter: (a, b) => a.stock - b.stock,
			key: "stock",
			render: (stock) => {
				let className = "";
				if (stock <= 0) {
					className = "text-danger fw-bold";
				} else if (stock >= 1 && stock <= 10) {
					className = "text-warning fw-bold";
				}

				return <span className={className}>{stock}</span>;
			},
		},
		{
			title: "Precio Detalle",
			dataIndex: "precioDetalle",
			render: (text) => `$${(text ?? 0).toLocaleString("es-CL")}`,
			sorter: (a, b) => a.precioDetalle - b.precioDetalle,
		},
		{
			title: "Precio Mayorista",
			dataIndex: "precioMayorista",
			render: (text) => `$${(text ?? 0).toLocaleString("es-CL")}`,
			sorter: (a, b) => a.precioMayorista - b.precioMayorista,
		},
		{
			title: "Aroma",
			dataIndex: "aromaNombre",
			sorter: (a, b) => a.aromaNombre.localeCompare(b.aromaNombre),
		},
		{
			title: "Familia",
			dataIndex: "familiaNombre",
			sorter: (a, b) => a.familiaNombre.localeCompare(b.familiaNombre),
		},
		{
			title: "Activo",
			dataIndex: "activo",
			render: (text) => (
				<span className={`badge ${text ? "bg-success" : "bg-danger"}`}>
					{text ? "Sí" : "No"}
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

						{/* Botón de Activar/Desactivar  */}
						{esAdmin && (
							<OverlayTrigger
								placement="top"
								overlay={
									record.activo ? (
										<Tooltip>Desactivar producto</Tooltip>
									) : (
										<Tooltip>Activar producto</Tooltip>
									)
								}
							>
								<Link
									className="confirm-text me-2 p-2"
									to="#"
									onClick={() => handleActivarDesactivar(record)} // Llama a la nueva función
								>
									{record.activo ? (
										<Power
											className="feather-power text-danger"
											title="Desactivar Producto"
										/>
									) : (
										<Power
											className="feather-edit text-success"
											title="Activar Producto"
										/> // Usamos CheckSquare para representar 'Activar'
									)}
								</Link>
							</OverlayTrigger>
						)}
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
							<h4>Productos</h4>
							<h6>Gestión de productos</h6>
						</div>
					</div>
					<ul className="table-top-head">
						<li>
							<OverlayTrigger placement="top" overlay={renderExcelTooltip}>
								<Link
									onClick={(e) => {
										e.preventDefault();
										setShowExportModal(true);
									}}
									style={{ cursor: 'pointer' }}
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
									// Recarga con los filtros actualmente APLICADOS
									onClick={() => cargarProductos(currentPage, pageSize)}
									style={{ cursor: "pointer" }}
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
									onClick={(e) => {
										e.preventDefault();
										dispatch(setToogleHeader(!data));
									}}
								>
									<ChevronUp />
								</Link>
							</OverlayTrigger>
						</li>
					</ul>
					{esAdmin && (
						<div className="page-btn">
							<Link to={route.addproduct} className="btn btn-added">
								<PlusCircle className="me-2 iconsize" />
								Crear Nuevo Producto
							</Link>
						</div>
					)}
				</div>

				{/* /product list */}
				<div className="card table-list-card">
					<div className="card-body">
						<div className="table-top">
							{/* === BÚSQUEDA RÁPIDA (Nombre) - REACTIVA CON DEBOUNCE === */}
							<div className="search-set">
								<div className="search-input">
									<input
										type="text"
										placeholder="Buscar por nombre..."
										className="form-control form-control-sm formsearch"
										value={valorInputBusqueda}
										onChange={handleNombreBusquedaChange} // Reactivo con debounce
									/>
									<Link to="#" className="btn btn-searchset">
										<i data-feather="search" className="feather-search" />
									</Link>
								</div>
							</div>

							{/* === ESTADO REACTIVO Y TAMAÑO DE PÁGINA === */}
							<div className="d-flex align-items-center">
								{/* Selector de Estado (Activo/Inactivo/Todos) - REACTIVO */}
								<div className="form-sort me-3">
									<StopCircle className="info-img" />
									<Select
										className="select"
										options={estadoOptions}
										value={estadoSeleccionado}
										onChange={handleEstadoChange} // Reactivo, recarga la tabla
										placeholder="Filtrar por estado"
									/>
								</div>

								{/* Selector de tamaño de página */}
								<div className="form-sort">
									<Sliders className="info-img" />
									<Select
										className="select"
										options={pageSizeOptions}
										value={pageSizeOptions.find(
											(opt) => opt.value === pageSize
										)}
										onChange={handlePageSizeChange}
										placeholder="Tamaño de página"
									/>
								</div>

								{/* Botón de FILTROS AVANZADOS */}
								<div className="search-path ms-3">
									<Link
										className={`btn btn-filter ${isFilterVisible ? "setclose" : ""
											}`}
										id="filter_search"
										onClick={toggleFilterVisibility}
										style={{ cursor: "pointer" }}
									>
										<Filter className="filter-icon" />
										<span>
											<i data-feather="x" className="feather-x" />
										</span>
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
											{/* Filtro por SKU */}
											<div className="col-lg-2 col-sm-6 col-12">
												<div className="input-blocks">
													<label>SKU</label>
													<input
														type="text"
														className="form-control"
														placeholder="Buscar por SKU..."
														value={filtrosAvanzadosForm.sku} // Usa el estado del formulario
														onChange={(e) =>
															handleFiltroAvanzadoChange("sku", e.target.value)
														} // NO recarga
													/>
												</div>
											</div>

											{/* Filtro por Código de Barras */}
											<div className="col-lg-2 col-sm-6 col-12">
												<div className="input-blocks">
													<label>Código de Barras</label>
													<input
														type="text"
														className="form-control"
														placeholder="Código de barras..."
														value={filtrosAvanzadosForm.codigoBarras} // Usa el estado del formulario
														onChange={(e) =>
															handleFiltroAvanzadoChange(
																"codigoBarras",
																e.target.value
															)
														} // NO recarga
													/>
												</div>
											</div>

											{/* Filtro por Aroma */}
											<div className="col-lg-2 col-sm-6 col-12">
												<div className="input-blocks">
													<label>Aroma</label>
													<Select
														className="select"
														options={aromasOptions}
														value={aromasOptions.find(
															(opt) =>
																opt.value === filtrosAvanzadosForm.aromaId
														) || null} // Usa el estado del formulario
														onChange={(selected) =>
															handleFiltroAvanzadoChange(
																"aromaId",
																selected ? selected.value : null
															)
														} // NO recarga
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
														options={familiasOptions}
														value={familiasOptions.find(
															(opt) =>
																opt.value === filtrosAvanzadosForm.familiaId
														) || null} // Usa el estado del formulario
														onChange={(selected) =>
															handleFiltroAvanzadoChange(
																"familiaId",
																selected ? selected.value : null
															)
														} // NO recarga
														placeholder="Seleccionar familia"
														isSearchable
														isClearable
													/>
												</div>
											</div>

											{/* Botones de acción */}
											<div className="col-lg-2 col-sm-6 col-12 ms-auto">
												<div className="input-blocks">
													<div style={{ height: "20px", visibility: "hidden" }}>Label</div>
													<div className="btn-group w-100">
														<button
															className="btn btn-primary me-2"
															onClick={aplicarFiltros} // APLICAR FILTROS
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
											onChange: (page) => handlePageChange(page),
											showSizeChanger: false,
										}}
									/>

									{/* Información de paginación */}
									<div className="pagination-info mt-3 text-center">
										<span>
											Mostrando {productos.length} de {totalElements} productos
											(Página {currentPage + 1} de {totalPages})
										</span>
									</div>
								</>
							)}
						</div>
					</div>
				</div>
				{/* /product list */}
			</div>

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

			<ExportProductCsvModal
				show={showExportModal}
				handleClose={() => setShowExportModal(false)}
				aromasOptions={aromasOptions}
				familiasOptions={familiasOptions}
			/>
		</div>
	);
};

export default ProductList;
