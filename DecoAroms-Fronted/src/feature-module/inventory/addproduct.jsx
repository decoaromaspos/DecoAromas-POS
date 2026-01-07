import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Link } from "react-router-dom";
import Select from "react-select";
import { all_routes } from "../../Router/all_routes";
import AddFamilyList from '../../core/modals/inventory/addfamilylist';
import AddCategoryList from '../../core/modals/inventory/addcategorylist';
import {
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  Info,
  LifeBuoy,
  PlusCircle,
} from "feather-icons-react/build/IconComponents";
import { useDispatch, useSelector } from "react-redux";
import { setToogleHeader } from "../../core/redux/action";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import { toast } from "react-toastify";
import { checkSkuDisponible, checkNombreDisponible, crearProducto } from "../../services/productoService";
import { getAromas } from "../../services/aromaService";
import { getFamilias } from "../../services/familiaService";
import { useAuth } from "../../context/AuthContext";

// Helper para debounce
const debounce = (func, delay) => {
  let timeout;
  return (...args) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), delay);
  };
};

// Estilo para los asteriscos obligatorios
const requiredAsteriskStyle = { color: 'red', marginLeft: '5px' };

const AddProduct = () => {
  const route = all_routes;
  const { usuario } = useAuth();
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);

  // Estados del formulario
  const [formData, setFormData] = useState({
    nombre: "",
    descripcion: "",
    sku: "",
    precioDetalle: "",
    precioMayorista: "",
    stock: "",
    costo: "",
    familiaId: null,
    aromaId: null,
    usuarioId: usuario.usuarioId,
  });

  // Estados de listas de opciones
  const [aromasOptions, setAromasOptions] = useState([]);
  const [familiasOptions, setFamiliasOptions] = useState([]);

  // Estados de validación
  const [skuValidationMessage, setSkuValidationMessage] = useState("");
  const [skuAvailable, setSkuAvailable] = useState(false);
  const [nombreValidationMessage, setNombreValidationMessage] = useState("");
  const [nombreAvailable, setNombreAvailable] = useState(false);
  const [errors, setErrors] = useState({});


  // Función para refrescar las listas de aromas y familias
  const refreshDropdowns = useCallback(async () => {
    try {
      const aromas = await getAromas();
      const familias = await getFamilias();

      const mappedAromas = aromas.map((a) => ({
        value: a.aromaId,
        label: a.nombre,
        ...a,
      }));
      const mappedFamilias = familias.map((f) => ({
        value: f.familiaId,
        label: f.nombre,
        ...f,
      }));

      setAromasOptions(mappedAromas);
      setFamiliasOptions(mappedFamilias);
      console.log("Dropdowns actualizados!");
    } catch (error) {
      toast.error("Error al refrescar las listas de aromas/familias.");
      console.error("Error al recargar datos de dropdown:", error);
    }
  }, []);


  // Función para la carga inicial.
  useEffect(() => {
    refreshDropdowns();
  }, [refreshDropdowns]);

  // Envolver checkNombre en useCallback para estabilizar la función
  const checkNombre = useCallback(async (nombre) => {
    if (!nombre) {
      setNombreValidationMessage("");
      setNombreAvailable(false);
      return;
    }
    try {
      const response = await checkNombreDisponible(nombre);
      setNombreValidationMessage(response.message);
      setNombreAvailable(response.available);
      if (response.available) setErrors((prev) => ({ ...prev, nombre: "" }));
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setNombreValidationMessage(error.response.data.message);
        setNombreAvailable(error.response.data.available);
        setErrors((prev) => ({ ...prev, nombre: error.response.data.message }));
      } else {
        setNombreValidationMessage("Error al verificar el nombre.");
        setNombreAvailable(false);
        console.error("Error en checkNombreDisponible:", error);
      }
    }
  }, []); // Sin dependencias externas porque solo usa setters

  // Usar useMemo para crear la función debounced
  const checkNombreAvailabilityDebounced = useMemo(
    () => debounce(checkNombre, 500),
    [checkNombre]
  );

  // Función para manejar el cambio en los inputs de texto
  const handleChange = (e) => {
    const { name, value } = e.target;

    // Limpiar el error de ese campo
    if (errors[name]) { setErrors((prev) => ({ ...prev, [name]: "" })); }

    // Si el cambio es en el SKU, también limpiar el mensaje de validación
    if (name === 'sku') {
      const cleanedValue = value.replace(/\s/g, '').toUpperCase();

      setFormData((prev) => ({ ...prev, [name]: cleanedValue }));
      setSkuValidationMessage("");
      setSkuAvailable(false);
      checkSkuAvailabilityDebounced(cleanedValue);
    } else if (name === 'nombre') {
      setFormData((prev) => ({ ...prev, [name]: value }));
      setNombreValidationMessage("");
      setNombreAvailable(false);
      checkNombreAvailabilityDebounced(value);
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleNumericChange = (e) => {
    const { name, value } = e.target;
    // Permite solo números enteros no negativos
    const regex = /^\d*$/;
    if (regex.test(value)) {
      if (errors[name]) { setErrors((prev) => ({ ...prev, [name]: "" })); }
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  // Función para manejar el cambio en los Selects (Aroma y Familia)
  const handleSelectChange = (selectedOption, { name }) => {
    setFormData((prev) => ({
      ...prev,
      [name]: selectedOption ? selectedOption.value : null,
    }));

    // Limpiar el error de ese campo
    if (errors[name]) { setErrors((prev) => ({ ...prev, [name]: "" })); }
  };

  // Envolver checkSku en useCallback
  const checkSku = useCallback(async (sku) => {
    if (!sku) {
      setSkuValidationMessage("");
      setSkuAvailable(false);
      return;
    }
    if (/\s/.test(sku)) {
      setSkuValidationMessage("El SKU no puede contener espacios.");
      setSkuAvailable(false);
      setErrors((prev) => ({ ...prev, sku: "El SKU no puede contener espacios." }));
      return;
    }

    try {
      const response = await checkSkuDisponible(sku);
      // Respuesta 200 OK
      setSkuValidationMessage(response.message);
      setSkuAvailable(response.available);
      if (response.available) setErrors((prev) => ({ ...prev, sku: "" }));
    } catch (error) {
      // Respuesta 409 Conflict
      if (error.response && error.response.status === 409) {
        setSkuValidationMessage(error.response.data.message);
        setSkuAvailable(error.response.data.available);
        setErrors((prev) => ({ ...prev, sku: error.response.data.message }));
      } else {
        setSkuValidationMessage("Error al verificar SKU.");
        setSkuAvailable(false);
        console.error("Error en checkSkuDisponible:", error);
      }
    }
  }, []); // Sin dependencias externas

  // Usar useMemo para la versión debounced
  const checkSkuAvailabilityDebounced = useMemo(
    () => debounce(checkSku, 500),
    [checkSku]
  );

  // Función de validación de formulario antes de enviar
  const validateForm = () => {
    let newErrors = {};
    let isValid = true;
    const skuRegex = /^\S*$/;

    const requiredFields = ["nombre", "sku", "precioDetalle", "precioMayorista", "stock", "costo"];

    requiredFields.forEach((field) => {
      if (!formData[field] || String(formData[field]).trim() === "") {
        newErrors[field] = "Este campo es obligatorio.";
        isValid = false;
      }
    });


    const numericFields = ["precioDetalle", "precioMayorista", "stock", "costo"];
    numericFields.forEach((field) => {
      const value = Number(formData[field]);
      if (formData[field] && (isNaN(value) || value < 0)) {
        newErrors[field] = "Debe ser un número positivo o cero.";
        isValid = false;
      }
    });

    if (formData.sku && !skuRegex.test(formData.sku)) {
      newErrors.sku = "El SKU no puede contener espacios.";
      isValid = false;
    }

    // Validación de disponibilidad del nombre
    if (!nombreAvailable && formData.nombre) {
      newErrors.nombre = nombreValidationMessage || "Debe validar la disponibilidad del nombre.";
      isValid = false;
    }

    if (!skuAvailable && formData.sku) {
      newErrors.sku = skuValidationMessage || "Debe validar la disponibilidad del SKU.";
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  // Manejador del submit del formulario
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (validateForm()) {
      const productoData = {
        ...formData,
        sku: formData.sku.replace(/\s/g, '').toUpperCase(),
        precioDetalle: Number(formData.precioDetalle),
        precioMayorista: Number(formData.precioMayorista),
        stock: Number(formData.stock),
        costo: Number(formData.costo),
      };

      try {
        const response = await crearProducto(productoData);
        toast.success("!Producto creado exitosamente!",
          { position: "top-right", autoClose: 3000 });
        // Limpiar formulario
        setFormData({
          nombre: "",
          descripcion: "",
          sku: "",
          precioDetalle: "",
          precioMayorista: "",
          stock: "",
          costo: "",
          familiaId: null,
          aromaId: null,
          usuarioId: usuario.usuarioId,
        });
        console.log("Producto Creado:", response);

      } catch (response) {
        const { status, data } = response;

        if (status === 400 && data.details) {
          // Notificacion peligro. Faltan parametros (data.datails existe)
          const errores = Object.values(data.details).join(' ');
          toast.warn(`Campos incompletos: ${errores}`, {
            position: "top-right",
            autoClose: 6000,
          });
        } else if (status === 409 && data.error) {
          // Notificación peligro: SKU usado (status 409, data.error existe)
          toast.warn(`Conflicto: ${data.error}`, {
            position: "top-right",
            autoClose: 6000,
          });
        } else if (data.error) {
          // Notificación error genérico con mensaje del backend
          toast.error(`Error al crear producto: ${data.error}`, {
            position: "top-right",
          });
        } else {
          // Notificación de error desconocido
          toast.error("Ocurrió un error inesperado. Inténtelo de nuevo.", {
            position: "top-right",
          });
        }
      }
    } else {
      toast.error("Por favor, complete todos los campos obligatorios y corrija los errores.");
    }
  };


  // Tooltip para el colapso
  const renderCollapseTooltip = (props) => (
    <Tooltip id="refresh-tooltip" {...props}>
      Collapse
    </Tooltip>
  );

  return (
    <div className="page-wrapper">
      <div className="content">
        <div className="page-header">
          <div className="add-item d-flex">
            <div className="page-title">
              <h4>Nuevo Producto</h4>
              <h6>Crear nuevo producto</h6>
            </div>
          </div>
          <ul className="table-top-head">
            <li>
              <div className="page-btn">
                <Link to={route.productlist} className="btn btn-secondary">
                  <ArrowLeft className="me-2" />
                  Volver a Productos
                </Link>
              </div>
            </li>
            <li>
              <OverlayTrigger placement="top" overlay={renderCollapseTooltip}>
                <Link
                  data-bs-toggle="tooltip"
                  data-bs-placement="top"
                  title="Collapse"
                  id="collapse-header"
                  className={data ? "active" : ""}
                  onClick={() => {
                    dispatch(setToogleHeader(!data));
                  }}
                >
                  {data ? <ChevronUp className="feather-chevron-up" /> : <ChevronDown className="feather-chevron-down" />}
                </Link>
              </OverlayTrigger>
            </li>
          </ul>
        </div>
        {/* /add */}
        <form onSubmit={handleSubmit}>
          <div className="card">
            <div className="card-body add-product pb-0">
              <div
                className="accordion-card-one accordion"
                id="accordionExample"
              >
                <div className="accordion-item">
                  <div className="accordion-header" id="headingOne">
                    <div
                      className="accordion-button"
                      data-bs-toggle="collapse"
                      data-bs-target="#collapseOne"
                      aria-controls="collapseOne"
                    >
                      <div className="addproduct-icon">
                        <h5>
                          <Info className="add-info" />
                          <span>Información de Producto</span>
                        </h5>
                        <Link to="#">
                          <ChevronDown className="chevron-down-add" />
                        </Link>
                      </div>
                    </div>
                  </div>
                  <div
                    id="collapseOne"
                    className="accordion-collapse collapse show"
                    aria-labelledby="headingOne"
                    data-bs-parent="#accordionExample"
                  >
                    <div className="accordion-body">
                      <div className="row">
                        <div className="col-lg-6 col-sm-6 col-12">
                          <div className="mb-3 add-product">
                            <label className="form-label">
                              Nombre de Producto <span style={requiredAsteriskStyle}>*</span>
                            </label>
                            <input
                              type="text"
                              className={`form-control ${errors.nombre ? 'is-invalid' : nombreAvailable && formData.nombre ? 'is-valid' : ''}`}
                              placeholder="Ingresar Nombre de Producto"
                              name="nombre"
                              value={formData.nombre}
                              onChange={handleChange}
                            />
                            {/* Mensaje de validación para el nombre */}
                            {nombreValidationMessage && (
                              <div className={`mt-1 ${nombreAvailable ? 'text-success' : 'text-danger'}`}>
                                {nombreValidationMessage}
                              </div>
                            )}
                            {errors.nombre && !nombreValidationMessage && <div className="invalid-feedback">{errors.nombre}</div>}
                          </div>
                        </div>
                        <div className="col-lg-6 col-sm-6 col-12">
                          <div className="input-blocks add-product list">
                            <label>SKU <span style={requiredAsteriskStyle}>*</span></label>
                            <input
                              type="text"
                              className={`form-control list ${errors.sku ? 'is-invalid' : skuAvailable && formData.sku ? 'is-valid' : ''}`}
                              placeholder="Ingresar SKU"
                              name="sku"
                              value={formData.sku}
                              onChange={handleChange}
                            />
                            {skuValidationMessage && (
                              <div
                                className={`mt-1 ${skuAvailable ? 'text-success' : 'text-danger'}`}
                              >
                                {skuValidationMessage}
                              </div>
                            )}

                          </div>
                        </div>
                      </div>

                      <div className="addservice-info">
                        <div className="row">
                          <div className="col-lg-6 col-sm-6 col-12">
                            <div className="mb-3 add-product">
                              <div className="add-newplus">
                                <label className="form-label">
                                  Tipo de Aroma
                                </label>
                                <Link
                                  data-bs-toggle="modal"
                                  data-bs-target="#add-category"
                                  type="button"
                                >
                                  <PlusCircle className="plus-down-add" />
                                  <span>Nuevo Aroma</span>
                                </Link>
                              </div>
                              <Select
                                className={`select ${errors.aromaId ? 'is-invalid-select' : ''}`}
                                options={aromasOptions}
                                placeholder="Seleccionar (Opcional)"
                                name="aromaId"
                                onChange={handleSelectChange}
                                isClearable
                              />
                              {errors.aromaId && <div className="invalid-feedback d-block">{errors.aromaId}</div>}
                            </div>
                          </div>

                          <div className="col-lg-6 col-sm-6 col-12">
                            <div className="mb-3 add-product">
                              <div className="add-newplus">
                                <label className="form-label">
                                  Tipo de Familia
                                </label>
                                <Link
                                  data-bs-toggle="modal"
                                  data-bs-target="#add-family"
                                  type="button"
                                >
                                  <PlusCircle className="plus-down-add" />
                                  <span>Nueva Familia</span>
                                </Link>
                              </div>
                              <Select
                                className={`select ${errors.familiaId ? 'is-invalid-select' : ''}`}
                                options={familiasOptions}
                                placeholder="Seleccionar (Opcional)"
                                name="familiaId"
                                onChange={handleSelectChange}
                                isClearable
                              />
                              {errors.familiaId && <div className="invalid-feedback d-block">{errors.familiaId}</div>}
                            </div>
                          </div>
                        </div>
                      </div>


                      {/* Editor */}
                      <div className="col-lg-12">
                        <div className="input-blocks summer-description-box transfer mb-3">
                          <label>Descripción</label>
                          <textarea
                            className="form-control h-100"
                            maxLength={250}
                            rows={5}
                            name="descripcion"
                            value={formData.descripcion}
                            onChange={handleChange}
                            placeholder="Ingresar Descripción (máx. 250 carácteres)"
                          />
                          <p className="mt-1">{formData.descripcion.length} / 250 carácteres.</p>
                        </div>
                      </div>
                      {/* /Editor */}
                    </div>
                  </div>
                </div>
              </div>
              <div
                className="accordion-card-one accordion"
                id="accordionExample2"
              >
                <div className="accordion-item">
                  <div className="accordion-header" id="headingTwo">
                    <div
                      className="accordion-button"
                      data-bs-toggle="collapse"
                      data-bs-target="#collapseTwo"
                      aria-controls="collapseTwo"
                    >
                      <div className="text-editor add-list">
                        <div className="addproduct-icon list icon">
                          <h5>
                            <LifeBuoy className="add-info" />
                            <span>Precios &amp; Stocks</span>
                          </h5>
                          <Link to="#">
                            <ChevronDown className="chevron-down-add" />
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div
                    id="collapseTwo"
                    className="accordion-collapse collapse show"
                    aria-labelledby="headingTwo"
                    data-bs-parent="#accordionExample2"
                  >
                    <div className="accordion-body">
                      <div className="tab-content" id="pills-tabContent">
                        <div
                          className="tab-pane fade show active"
                          id="pills-home"
                          role="tabpanel"
                          aria-labelledby="pills-home-tab"
                        >
                          <div className="row">
                            <div className="col-lg-6 col-sm-6 col-12">
                              <div className="input-blocks add-product">
                                <label>Precio Detalle <span style={requiredAsteriskStyle}>*</span></label>
                                <input
                                  type="text"
                                  inputMode="numeric"
                                  pattern="[0-9]*"
                                  className={`form-control ${errors.precioDetalle ? 'is-invalid' : ''}`}
                                  name="precioDetalle"
                                  value={formData.precioDetalle}
                                  onChange={handleNumericChange}
                                  placeholder="Ingresar Precio Detalle"
                                />
                                {errors.precioDetalle && <div className="invalid-feedback">{errors.precioDetalle}</div>}
                              </div>
                            </div>
                            <div className="col-lg-6 col-sm-6 col-12">
                              <div className="input-blocks add-product">
                                <label>Precio Mayorista <span style={requiredAsteriskStyle}>*</span></label>
                                <input
                                  type="text"
                                  inputMode="numeric"
                                  pattern="[0-9]*"
                                  className={`form-control ${errors.precioMayorista ? 'is-invalid' : ''}`}
                                  name="precioMayorista"
                                  value={formData.precioMayorista}
                                  onChange={handleNumericChange}
                                  placeholder="Ingresar Precio Mayorista"
                                />
                                {errors.precioMayorista && <div className="invalid-feedback">{errors.precioMayorista}</div>}
                              </div>
                            </div>
                          </div>


                          <div className="row">
                            <div className="col-lg-6 col-sm-6 col-12">
                              <div className="input-blocks add-product">
                                <label>Stock <span style={requiredAsteriskStyle}>*</span></label>
                                <input
                                  type="text"
                                  inputMode="numeric"
                                  pattern="[0-9]*"
                                  className={`form-control ${errors.stock ? 'is-invalid' : ''}`}
                                  name="stock"
                                  value={formData.stock}
                                  onChange={handleNumericChange}
                                  placeholder="Ingresar Stock"
                                />
                                {errors.stock && <div className="invalid-feedback">{errors.stock}</div>}
                              </div>
                            </div>
                            <div className="col-lg-6 col-sm-6 col-12">
                              <div className="input-blocks add-product">
                                <label>Costo<span style={requiredAsteriskStyle}>*</span></label>
                                <input
                                  type="text"
                                  inputMode="numeric"
                                  pattern="[0-9]*"
                                  className={`form-control ${errors.costo ? 'is-invalid' : ''}`}
                                  name="costo"
                                  value={formData.costo}
                                  onChange={handleNumericChange}
                                  placeholder="Ingresar Costo"
                                />
                                {errors.costo && <div className="invalid-feedback">{errors.costo}</div>}
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className="col-lg-12">
            <div className="btn-addproduct mb-4">
              <Link to={route.productlist} type="button" className="btn btn-cancel me-2">
                Cancelar
              </Link>
              <button type="submit" className="btn btn-submit">
                Guardar Producto
              </button>
            </div>
          </div>
        </form>
      </div>

      <AddCategoryList id="add-category" onDataUpdated={refreshDropdowns} />
      <AddFamilyList id="add-family" onDataUpdated={refreshDropdowns} />
    </div>
  );
};

export default AddProduct;