import React, { useState, useEffect, useCallback, useMemo } from "react"; // <--- Agregado useMemo
import { Link, useParams, useNavigate } from "react-router-dom";
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
import {
  getProductoById,
  actualizarProducto,
  checkSkuDisponible,
  checkNombreDisponible
} from "../../services/productoService";
import { getAromas } from "../../services/aromaService";
import { getFamilias } from "../../services/familiaService";
import PropTypes from 'prop-types';
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

const EditProduct = () => {
  const route = all_routes;
  const { id } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);
  const { usuario, isAuthenticated, loading: authLoading } = useAuth();
  const isVendedor = usuario && usuario.rol === 'VENDEDOR';

  const [formData, setFormData] = useState({
    nombre: "",
    descripcion: "",
    sku: "",
    precioDetalle: "",
    precioMayorista: "",
    costo: "",
    familiaId: null,
    aromaId: null,
  });

  const [aromasOptions, setAromasOptions] = useState([]);
  const [familiasOptions, setFamiliasOptions] = useState([]);
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(true);

  // Estados para validación de SKU y Nombre
  const [originalSku, setOriginalSku] = useState("");
  const [skuValidationMessage, setSkuValidationMessage] = useState("");
  const [skuAvailable, setSkuAvailable] = useState(true);

  const [originalNombre, setOriginalNombre] = useState("");
  const [nombreValidationMessage, setNombreValidationMessage] = useState("");
  const [nombreAvailable, setNombreAvailable] = useState(true);

  useEffect(() => {
    // --- Guardias de Auth/Rol dentro del Effect ---
    if (authLoading) return;
    if (!isAuthenticated) return;

    if (isVendedor) {
      setIsLoading(false);
      return;
    }

    const fetchInitialData = async () => {
      try {
        const [productoData, aromas, familias] = await Promise.all([
          getProductoById(id),
          getAromas(),
          getFamilias(),
        ]);

        setFormData({
          nombre: productoData.nombre || "",
          descripcion: productoData.descripcion || "",
          sku: productoData.sku || "",
          precioDetalle: productoData.precioDetalle || "",
          precioMayorista: productoData.precioMayorista || "",
          costo: productoData.costo || "",
          familiaId: productoData.familiaId || null,
          aromaId: productoData.aromaId || null,
        });

        setOriginalSku(productoData.sku);
        setSkuAvailable(true);
        setSkuValidationMessage("SKU original del producto.");

        setOriginalNombre(productoData.nombre);
        setNombreAvailable(true);
        setNombreValidationMessage("Nombre original del producto.");

        const mappedAromas = aromas.map((a) => ({ value: a.aromaId, label: a.nombre }));
        const mappedFamilias = familias.map((f) => ({ value: f.familiaId, label: f.nombre }));
        setAromasOptions(mappedAromas);
        setFamiliasOptions(mappedFamilias);

      } catch (error) {
        toast.error("Error al cargar los datos del producto.");
        console.error("Error al cargar datos:", error);
        navigate(route.productlist);
      } finally {
        setIsLoading(false);
      }
    };

    fetchInitialData();
  }, [id, navigate, route.productlist, authLoading, isAuthenticated, isVendedor]);

  const refreshDropdowns = useCallback(async () => {
    try {
      const aromas = await getAromas();
      const familias = await getFamilias();
      const mappedAromas = aromas.map((a) => ({ value: a.aromaId, label: a.nombre }));
      const mappedFamilias = familias.map((f) => ({ value: f.familiaId, label: f.nombre }));
      setAromasOptions(mappedAromas);
      setFamiliasOptions(mappedFamilias);
    } catch (error) {
      toast.error("Error al refrescar las listas.");
    }
  }, []);

  // --- Lógica de Validación y Debounce Corregida ---

  // Estabilizamos la función checkNombre con useCallback
  const checkNombre = useCallback(async (nombre) => {
    const trimmedNombre = nombre.trim();
    if (!trimmedNombre) {
      setNombreValidationMessage("");
      setNombreAvailable(false);
      return;
    }
    // Compara con originalNombre (dependencia)
    if (trimmedNombre.toLowerCase() === originalNombre.toLowerCase()) {
      setNombreValidationMessage("Nombre original del producto.");
      setNombreAvailable(true);
      setErrors((prev) => ({ ...prev, nombre: "" }));
      return;
    }
    try {
      const response = await checkNombreDisponible(trimmedNombre);
      setNombreValidationMessage(response.message);
      setNombreAvailable(response.available);
      if (response.available) setErrors((prev) => ({ ...prev, nombre: "" }));
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setNombreValidationMessage(error.response.data.message);
        setNombreAvailable(error.response.data.available);
      } else {
        setNombreValidationMessage("Error al verificar el nombre.");
        setNombreAvailable(false);
      }
    }
  }, [originalNombre]); // Dependencia necesaria

  // Estabilizamos la función checkSku con useCallback
  const checkSku = useCallback(async (sku) => {
    if (!sku) {
      setSkuValidationMessage("");
      setSkuAvailable(false);
      return;
    }
    // Compara con originalSku (dependencia)
    if (sku === originalSku) {
      setSkuValidationMessage("SKU original del producto.");
      setSkuAvailable(true);
      setErrors((prev) => ({ ...prev, sku: "" }));
      return;
    }
    try {
      const response = await checkSkuDisponible(sku);
      setSkuValidationMessage(response.message);
      setSkuAvailable(response.available);
      setErrors((prev) => ({ ...prev, sku: "" }));
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setSkuValidationMessage(error.response.data.message);
        setSkuAvailable(error.response.data.available);
      } else {
        setSkuValidationMessage("Error al verificar SKU.");
        setSkuAvailable(false);
      }
    }
  }, [originalSku]); // Dependencia necesaria

  // Usamos useMemo para crear las instancias debounced
  const checkSkuAvailabilityDebounced = useMemo(
    () => debounce(checkSku, 500),
    [checkSku]
  );

  const checkNombreAvailabilityDebounced = useMemo(
    () => debounce(checkNombre, 500),
    [checkNombre]
  );


  const handleChange = (e) => {
    const { name, value } = e.target;
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));

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
    const regex = /^\d*$/;
    if (regex.test(value)) {
      if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSelectChange = (selectedOption, { name }) => {
    setFormData((prev) => ({
      ...prev,
      [name]: selectedOption ? selectedOption.value : null,
    }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: "" }));
  };

  const validateForm = () => {
    let newErrors = {};
    let isValid = true;
    const requiredFields = ["nombre", "sku", "precioDetalle", "precioMayorista", "costo"];

    requiredFields.forEach((field) => {
      if (!formData[field] || String(formData[field]).trim() === "") {
        newErrors[field] = "Este campo es obligatorio.";
        isValid = false;
      }
    });

    const numericFields = ["precioDetalle", "precioMayorista", "costo"];
    numericFields.forEach((field) => {
      const value = Number(formData[field]);
      if (isNaN(value) || value < 0) {
        newErrors[field] = "Debe ser un número positivo o cero.";
        isValid = false;
      }
    });

    if (!nombreAvailable) {
      newErrors.nombre = nombreValidationMessage || "Este nombre no está disponible.";
      isValid = false;
    }

    if (!skuAvailable) {
      newErrors.sku = skuValidationMessage || "Este SKU no está disponible.";
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };


  const ErrorToast = ({ title, errors }) => (
    <div>
      <strong>{title}</strong>
      <ul style={{ paddingLeft: '20px', marginBottom: 0, marginTop: '5px' }}>
        {errors.map((err, index) => <li key={index}>{err}</li>)}
      </ul>
    </div>
  );

  ErrorToast.propTypes = {
    title: PropTypes.string.isRequired,
    errors: PropTypes.arrayOf(PropTypes.string).isRequired
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const finalSku = formData.sku.replace(/\s/g, '').toUpperCase();

    if (validateForm()) {
      const productoData = {
        nombre: formData.nombre,
        descripcion: formData.descripcion,
        sku: finalSku,
        precioDetalle: Number(formData.precioDetalle),
        precioMayorista: Number(formData.precioMayorista),
        costo: Number(formData.costo),
        familiaId: formData.familiaId,
        aromaId: formData.aromaId,
      };

      try {
        await actualizarProducto(id, productoData);
        toast.success("¡Producto actualizado exitosamente!");
        navigate(route.productlist);
      } catch (error) {
        if (error.response && error.response.status === 400 && error.response.data.details) {
          const validationErrors = error.response.data.details;
          const errorList = Object.values(validationErrors);
          toast.error(<ErrorToast title="Error de validación:" errors={errorList} />);
        } else {
          const errorMessage = error.response?.data?.error || "Ocurrió un error inesperado.";
          toast.error(`Error al actualizar: ${errorMessage}`);
        }
      }
    } else {
      toast.error("Por favor, corrija los errores en el formulario.");
    }
  };

  if (authLoading) {
    return (
      <div className="page-wrapper"><div className="content"><h4>Verificando acceso...</h4></div></div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  if (isVendedor) {
    return (
      <div className="page-wrapper">
        <div className="content">
          <div className="text-center" style={{ padding: '100px 0' }}>
            <h2 className="text-danger">🚫 Acceso Denegado</h2>
            <p className="lead">
              Tu rol de <strong>VENDEDOR</strong> no tiene permiso para editar productos.
            </p>
            <Link to={route.productlist} className="btn btn-primary btn-lg mt-3">
              Volver a la Lista
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="page-wrapper"><div className="content"><h4>Cargando datos del producto...</h4></div></div>
    );
  }

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
              <h4>Editar Producto</h4>
              <h6>Actualizar datos del producto</h6>
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
                  id="collapse-header"
                  className={data ? "active" : ""}
                  onClick={() => {
                    dispatch(setToogleHeader(!data));
                  }}
                >
                  {data ? <ChevronUp /> : <ChevronDown />}
                </Link>
              </OverlayTrigger>
            </li>
          </ul>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="card">
            <div className="card-body add-product pb-0">
              <div className="accordion-card-one accordion" id="accordionExample">
                <div className="accordion-item">
                  <div className="accordion-header" id="headingOne">
                    <div
                      className="accordion-button"
                      data-bs-toggle="collapse"
                      data-bs-target="#collapseOne"
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
                  <div id="collapseOne" className="accordion-collapse collapse show">
                    <div className="accordion-body">
                      <div className="row">
                        <div className="col-lg-6 col-sm-6 col-12">
                          <div className="mb-3 add-product">
                            <label className="form-label">Nombre de Producto <span style={requiredAsteriskStyle}>*</span></label>
                            <input
                              type="text"
                              className={`form-control ${errors.nombre ? 'is-invalid' :
                                nombreAvailable && formData.nombre.trim() !== originalNombre ? 'is-valid' : ''}`}
                              name="nombre"
                              value={formData.nombre}
                              onChange={handleChange}
                            />
                            {nombreValidationMessage && (
                              <div className={`mt-1 small ${nombreAvailable ? 'text-success' : 'text-danger'}`}>
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
                              className={`form-control list ${errors.sku ? 'is-invalid' :
                                skuAvailable && formData.sku !== originalSku ? 'is-valid' : ''}`}
                              name="sku"
                              value={formData.sku}
                              onChange={handleChange}
                            />
                            {skuValidationMessage && (
                              <div className={`mt-1 small ${skuAvailable ? 'text-success' : 'text-danger'}`}>
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
                                name="aromaId"
                                value={aromasOptions.find(option => option.value === formData.aromaId)}
                                onChange={(opt) => handleSelectChange(opt, { name: 'aromaId' })}
                                placeholder="Seleccionar Aroma"
                                isClearable={true}
                                isSearchable={true}
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
                                name="familiaId"
                                value={familiasOptions.find(option => option.value === formData.familiaId)}
                                onChange={(opt) => handleSelectChange(opt, { name: 'familiaId' })}
                                placeholder="Seleccionar Familia"
                                isClearable={true}
                                isSearchable={true}
                              />
                              {errors.familiaId && <div className="invalid-feedback d-block">{errors.familiaId}</div>}
                            </div>
                          </div>
                        </div>
                      </div>

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
                          <p className="mt-1 text-muted small">{formData.descripcion ? formData.descripcion.length : 0} / 250 caracteres.</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="accordion-card-one accordion" id="accordionExample2">
                <div className="accordion-item">
                  <div className="accordion-header" id="headingTwo">
                    <div
                      className="accordion-button"
                      data-bs-toggle="collapse"
                      data-bs-target="#collapseTwo"
                    >
                      <div className="text-editor add-list">
                        <div className="addproduct-icon list icon">
                          <h5>
                            <LifeBuoy className="add-info" />
                            <span>Precios</span>
                          </h5>
                          <Link to="#">
                            <ChevronDown className="chevron-down-add" />
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div id="collapseTwo" className="accordion-collapse collapse show">
                    <div className="accordion-body">
                      <div className="row">
                        <div className="col-lg-4 col-sm-6 col-12">
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
                        <div className="col-lg-4 col-sm-6 col-12">
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

                        <div className="col-lg-4 col-sm-6 col-12">
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
          <div className="col-lg-12">
            <div className="btn-addproduct mb-4">
              <Link to={route.productlist} type="button" className="btn btn-cancel me-2">
                Cancelar
              </Link>
              <button type="submit" className="btn btn-submit">
                Actualizar Producto
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

export default EditProduct;