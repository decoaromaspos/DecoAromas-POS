import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link } from "react-router-dom";
import {
	ChevronUp,
	RotateCcw,
	Filter,
	Search,
	X,
} from "feather-icons-react/build/IconComponents";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import { setToogleHeader } from "../../core/redux/action";
import { useDispatch, useSelector } from "react-redux";
import BarcodePrinter from "../../core/modals/inventory/barcodePrinter";
import { getProductosByFiltrosPaginados } from "../../services/productoService";
import { getBarcodeListByIds } from "../../services/barCodeService";
import { getAllBarcodes } from "../../services/barCodeService";

const PrintBarcode = () => {
	const dispatch = useDispatch();
	const data = useSelector((state) => state.toggle_header);

	// --- ESTADOS ---
	const [productos, setProductos] = useState([]);
	const [isLoadingProducts, setIsLoadingProducts] = useState(false);
	const [printList, setPrintList] = useState([]);
	const [isPrinting, setIsPrinting] = useState(false);

	// size inputs for exports (cm)
	const [exportWidthCm, setExportWidthCm] = useState(3);
	const [exportHeightCm, setExportHeightCm] = useState(1.2);

	// size inputs for list export (cm)
	const [listWidthCm, setListWidthCm] = useState(3);
	const [listHeightCm, setListHeightCm] = useState(1.2);

	// Estados para la paginación y filtros
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize] = useState(10000); // Se mantiene un tamaño grande para cargar "todo"

	// Búsqueda rápida (reactiva)
	const [valorInputBusqueda, setValorInputBusqueda] = useState("");

	// Filtros avanzados (aplicación manual)
	const [filtrosActivos, setFiltrosActivos] = useState({
		nombre: "",
		sku: "",
		codigoBarras: "",
	});
	const [filtrosForm, setFiltrosForm] = useState({
		sku: "",
		codigoBarras: "",
	});

	// Visibilidad del panel de filtros
	const [isFilterVisible, setIsFilterVisible] = useState(false);

	const searchTimeoutRef = useRef(null);

	// --- EFECTOS Y LÓGICA DE DATOS ---

	// Función centralizada para cargar productos
	const loadProductos = useCallback(
		async (page, filtros) => {
			setIsLoadingProducts(true);
			try {
				const pageNumber = Math.max(0, page - 1);
				const nombreFilter =
					filtros.nombre && filtros.nombre.trim().length > 2
						? filtros.nombre.trim()
						: null;
				const skuFilter =
					filtros.sku && filtros.sku.trim() !== "" ? filtros.sku.trim() : null;
				const codigoBarrasFilter =
					filtros.codigoBarras && filtros.codigoBarras.trim() !== ""
						? filtros.codigoBarras.trim()
						: null;

				const res = await getProductosByFiltrosPaginados(
					pageNumber,
					pageSize,
					"nombre",
					null, // aromaId
					null, // familiaId
					true, // activo
					nombreFilter,
					skuFilter,
					codigoBarrasFilter
				);
				setProductos(res.content || []);
			} catch (error) {
				console.error("Error cargando productos para PrintBarcode:", error);
				setProductos([]);
			} finally {
				setIsLoadingProducts(false);
			}
		},
		[pageSize]
	);

	// Efecto que reacciona a los filtros activos para recargar los datos
	useEffect(() => {
		loadProductos(currentPage, filtrosActivos);
	}, [currentPage, filtrosActivos, loadProductos]);

	// --- MANEJADORES DE EVENTOS (HANDLERS) ---

	// Búsqueda rápida por nombre (con debounce)
	const handleNombreBusquedaChange = (e) => {
		const value = e.target.value;
		setValorInputBusqueda(value);

		clearTimeout(searchTimeoutRef.current);
		searchTimeoutRef.current = setTimeout(() => {
			setCurrentPage(1);
			setFiltrosActivos((prev) => ({ ...prev, nombre: value }));
		}, 300); // 300ms de debounce
	};

	// Actualiza el estado del formulario de filtros avanzados sin recargar
	const handleFiltroAvanzadoChange = (field, value) => {
		setFiltrosForm((prev) => ({ ...prev, [field]: value }));
	};

	// Aplica los filtros del formulario y recarga la lista
	const aplicarFiltros = () => {
		setCurrentPage(1);
		setFiltrosActivos((prev) => ({
			...prev,
			sku: filtrosForm.sku,
			codigoBarras: filtrosForm.codigoBarras,
		}));
	};

	// Limpia los filtros avanzados y recarga la lista
	const limpiarFiltrosAvanzados = () => {
		setFiltrosForm({ sku: "", codigoBarras: "" });
		setCurrentPage(1);
		setFiltrosActivos((prev) => ({
			...prev,
			sku: "",
			codigoBarras: "",
		}));
	};

	// Toggle para mostrar/ocultar el panel de filtros
	const toggleFilterVisibility = () => {
		setIsFilterVisible(!isFilterVisible);
	};

	// Lógica para la lista de impresión (sin cambios)
	const handleAddToPrintList = (product) => {
		const existing = printList.find((i) => i.productoId === product.productoId);
		if (existing) {
			setPrintList(
				printList.map((i) =>
					i.productoId === product.productoId
						? { ...i, cantidad: i.cantidad + 1 }
						: i
				)
			);
		} else {
			setPrintList([
				...printList,
				{
					productoId: product.productoId,
					nombre: product.nombre,
					sku: product.sku,
					cantidad: 1,
				},
			]);
		}
	};

	const handleUpdateQuantity = (productoId, delta) => {
		const item = printList.find((i) => i.productoId === productoId);
		if (!item) return;
		const newQty = item.cantidad + delta;
		if (newQty < 1) {
			setPrintList(printList.filter((i) => i.productoId !== productoId));
			return;
		}
		setPrintList(
			printList.map((i) =>
				i.productoId === productoId ? { ...i, cantidad: newQty } : i
			)
		);
	};

	const handleRemoveFromPrintList = (productoId) => {
		setPrintList(printList.filter((i) => i.productoId !== productoId));
	};

	// Generar e imprimir el PDF de códigos de barras
	const handlePrintBarcode = async () => {
		if (printList.length === 0) return;
		setIsPrinting(true);
		try {
			const ids = printList.flatMap((item) =>
				Array(item.cantidad || 1).fill(item.productoId)
			);
			const blob = await getBarcodeListByIds(
				ids,
				parseFloat(listWidthCm) || 3,
				parseFloat(listHeightCm) || 1.2
			);
			const url = window.URL.createObjectURL(blob);
			window.open(url, "_blank");
			setTimeout(() => window.URL.revokeObjectURL(url), 10000);
		} catch (error) {
			console.error("Error generando PDF de códigos de barras:", error);
		} finally {
			setIsPrinting(false);
		}
	};

	// Exportar todos los códigos de barras
	const handleExport = async () => {
		try {
			const blob = await getAllBarcodes(
				parseFloat(exportWidthCm) || 3,
				parseFloat(exportHeightCm) || 1.2
			);
			const url = window.URL.createObjectURL(blob);
			window.open(url, "_blank");
			setTimeout(() => window.URL.revokeObjectURL(url), 10000);
		} catch (err) {
			console.error("Error exportando PDF", err);
		}
	};

	// --- RENDERIZADO DE TOOLTIPS ---
	const renderRefreshTooltip = (props) => (
		<Tooltip id="refresh-tooltip" {...props}>
			Refrescar
		</Tooltip>
	);
	const renderCollapseTooltip = (props) => (
		<Tooltip id="collapse-tooltip" {...props}>
			Colapsar
		</Tooltip>
	);

	// --- RENDERIZADO DEL COMPONENTE ---
	return (
		<div className="page-wrapper notes-page-wrapper">
			<div className="content">
				<div className="page-header">
					<div className="add-item d-flex">
						<div className="page-title">
							<h4>Imprimir Códigos de Barra</h4>
							<h6>Gestiona tus códigos de barra</h6>
						</div>
					</div>
					<div className="d-flex align-items-center">
						{/* Export size inputs (left of export button) */}
						<div className="me-2 d-flex align-items-center">
							<div className="me-2" style={{ minWidth: 110 }}>
								<label className="form-label small mb-0">Ancho (cm)</label>
								<input
									type="number"
									step="0.1"
									min="0.1"
									className="form-control form-control-sm"
									value={exportWidthCm}
									onChange={(e) => setExportWidthCm(e.target.value)}
								/>
							</div>
							<div style={{ minWidth: 110 }}>
								<label className="form-label small mb-0">Alto (cm)</label>
								<input
									type="number"
									step="0.1"
									min="0.1"
									className="form-control form-control-sm"
									value={exportHeightCm}
									onChange={(e) => setExportHeightCm(e.target.value)}
								/>
							</div>
						</div>
						<button
							className="btn-added-outline me-2"
							onClick={handleExport}
							type="button"
						>
							Exportar Todos los Códigos
						</button>
						<ul className="table-top-head">
							<li>
								<OverlayTrigger placement="top" overlay={renderRefreshTooltip}>
									<Link
										onClick={() =>
											loadProductos(currentPage, filtrosActivos)
										}
									>
										<RotateCcw />
									</Link>
								</OverlayTrigger>
							</li>
							<li>
								<OverlayTrigger placement="top" overlay={renderCollapseTooltip}>
									<Link
										id="collapse-header"
										className={data ? "active" : ""}
										onClick={() => dispatch(setToogleHeader(!data))}
									>
										<ChevronUp />
									</Link>
								</OverlayTrigger>
							</li>
						</ul>
					</div>
				</div>

				<div className="card">
					<div className="card-body">
						{/* --- SECCIÓN DE BÚSQUEDA Y FILTROS --- */}
						<div className="table-top">
							{/* Búsqueda rápida */}
							<div className="search-set">
								<div className="search-input">
									<input
										type="text"
										placeholder="Buscar por nombre..."
										className="form-control form-control-sm formsearch"
										value={valorInputBusqueda}
										onChange={handleNombreBusquedaChange}
									/>
									<Link to="#" className="btn btn-searchset">
										<Search />
									</Link>
								</div>
							</div>
							{/* Botón para filtros avanzados */}
							<div className="search-path">
								<Link
									className={`btn btn-filter ${isFilterVisible ? "setclose" : ""
										}`}
									id="filter_search"
									onClick={toggleFilterVisibility}
								>
									<Filter className="filter-icon" />
									<span>
										<X />
									</span>
								</Link>
							</div>
						</div>

						{/* --- PANEL DE FILTROS AVANZADOS (OCULTO) --- */}
						<div
							className={`card${isFilterVisible ? " visible" : ""}`}
							id="filter_inputs"
							style={{ display: isFilterVisible ? "block" : "none" }}
						>
							<div className="card-body pb-0">
								<div className="row">
									<div className="col-lg-3 col-sm-6 col-12">
										<div className="input-blocks">
											<label>SKU</label>
											<input
												type="text"
												className="form-control"
												placeholder="Buscar por SKU..."
												value={filtrosForm.sku}
												onChange={(e) =>
													handleFiltroAvanzadoChange("sku", e.target.value)
												}
											/>
										</div>
									</div>
									<div className="col-lg-3 col-sm-6 col-12">
										<div className="input-blocks">
											<label>Código de Barras</label>
											<input
												type="text"
												className="form-control"
												placeholder="Código de barras..."
												value={filtrosForm.codigoBarras}
												onChange={(e) =>
													handleFiltroAvanzadoChange(
														"codigoBarras",
														e.target.value
													)
												}
											/>
										</div>
									</div>
									<div className="col-lg-6">
										<div className="d-flex align-items-center justify-content-end h-100">
											<button
												className="btn btn-primary me-2"
												onClick={aplicarFiltros}
											>
												<Search className="me-1" size={18} />
												Aplicar
											</button>
											<button
												className="btn btn-secondary"
												onClick={limpiarFiltrosAvanzados}
											>
												<X className="me-1" size={18} />
												Limpiar
											</button>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>

				<div className="row">
					{/* --- LISTA DE PRODUCTOS --- */}
					<div className="col-lg-7">
						<div className="card" style={{ height: "525px" }} >
							<div className="card-body">
								<h6>Productos</h6>
								<div style={{ maxHeight: 420, overflowY: "auto" }}>
									{isLoadingProducts ? (
										<p>Cargando productos...</p>
									) : productos.length === 0 ? (
										<p className="text-muted">No se encontraron productos.</p>
									) : (
										productos.map((p) => (
											<div
												key={p.productoId}
												className="d-flex align-items-center justify-content-between mb-2 p-2 border-bottom"
											>
												<div>
													<div className="fw-bold">{p.nombre}</div>
													<div className="text-muted small">SKU: {p.sku}</div>
												</div>
												<button
													className="btn btn-primary btn-sm"
													onClick={() => handleAddToPrintList(p)}
												>
													Agregar
												</button>
											</div>
										))
									)}
								</div>
							</div>
						</div>
					</div>

					{/* --- LISTA PARA IMPRIMIR --- */}
					<div className="col-lg-5">
						<aside
							className="card p-3 d-flex flex-column"
							style={{ height: "525px" }} // <-- ALTURA FIJA PARA TODA LA TARJETA
						>
							<h6 className="mb-3">
								Lista para imprimir ({printList.length} items)
							</h6>

							{/* Esta es la zona que ahora tendrá scroll */}
							<div style={{ flex: 1, overflowY: "auto", minHeight: 0 }}>
								{printList.length === 0 ? (
									<p className="text-muted d-flex align-items-center justify-content-center h-100">
										No hay productos seleccionados.
									</p>
								) : (
									printList.map((item) => (
										<div
											key={item.productoId}
											className="d-flex align-items-center justify-content-between mb-2 p-2 border rounded"
										>
											<div>
												<div className="fw-bold">{item.nombre}</div>
												<div className="small text-muted">
													SKU: {item.sku}
												</div>
											</div>
											<div className="d-flex align-items-center">
												<button
													className="btn btn-sm btn-light me-2"
													onClick={() =>
														handleUpdateQuantity(item.productoId, -1)
													}
												>
													-
												</button>
												<div className="px-2">{item.cantidad}</div>
												<button
													className="btn btn-sm btn-light ms-2"
													onClick={() =>
														handleUpdateQuantity(item.productoId, 1)
													}
												>
													+
												</button>
												<button
													className="btn btn-sm btn-link text-danger ms-3"
													onClick={() =>
														handleRemoveFromPrintList(item.productoId)
													}
												>
													Eliminar
												</button>
											</div>
										</div>
									))
								)}
							</div>

							{/* Inputs for list export size (above print button) */}
							<div className="row g-2 mb-2">
								<div className="col-6">
									<label className="form-label small mb-1">Ancho (cm)</label>
									<input
										type="number"
										step="0.1"
										min="0.1"
										className="form-control form-control-sm"
										value={listWidthCm}
										onChange={(e) => setListWidthCm(e.target.value)}
									/>
								</div>
								<div className="col-6">
									<label className="form-label small mb-1">Alto (cm)</label>
									<input
										type="number"
										step="0.1"
										min="0.1"
										className="form-control form-control-sm"
										value={listHeightCm}
										onChange={(e) => setListHeightCm(e.target.value)}
									/>
								</div>
							</div>
							{/* El botón de imprimir ahora se queda fijo en la parte inferior */}
							<div className="mt-3">
								<button
									className="btn btn-success w-100"
									onClick={handlePrintBarcode}
									disabled={isPrinting || printList.length === 0}
								>
									<i className="fas fa-print me-2" />
									{isPrinting ? "Generando PDF..." : "Generar e Imprimir"}
								</button>
							</div>
						</aside>
					</div>
				</div>
				<BarcodePrinter />
			</div>
		</div>
	);
};

export default PrintBarcode;