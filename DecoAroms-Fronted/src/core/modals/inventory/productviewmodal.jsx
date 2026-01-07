import React from "react";
import PropTypes from 'prop-types'; 
import { all_routes } from "../../../Router/all_routes";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";

const ProductViewModal = ({ show, handleClose, product }) => {
  const navigate = useNavigate();
  const route = all_routes;
  const { usuario } = useAuth();
  const esAdmin = usuario?.rol === 'ADMIN' || usuario?.rol === 'SUPER_ADMIN';

  // Función de formato de moneda
  const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return "N/A"; 
    
    const numericAmount = Number(amount);
    
    // Si la parte decimal es .0, no mostramos decimales.
    if (numericAmount % 1 === 0) {
        return `$${numericAmount.toLocaleString(undefined, {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        })}`;
    }
    
    // Si tiene decimales relevantes, mostramos dos decimales.
    return `$${numericAmount.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  // Si no hay producto, no renderiza el contenido del modal
  if (!product) return null; 

  const modalId = `view-product-${product.productoId}`;

  return (
    <>
    <div 
        className={`modal fade ${show ? 'show d-block' : ''}`}
        id={modalId}
        tabIndex="-1"
        aria-labelledby={`${modalId}Label`}
        aria-hidden={!show}
        style={{ 
            display: show ? 'block' : 'none', 
            zIndex: 1055
        }} 
    >
      <div className="modal-dialog modal-dialog-centered modal-lg custom-modal-two">
        <div className="modal-content">
          <div className="page-wrapper-new p-0">
            <div className="content">
              {/* === HEADER === */}
              <div className="modal-header border-0 custom-modal-header">
                <div className="page-title">
                  <h4>Ver Producto</h4>
                  <small className="text-muted">
                    {product.nombre}
                  </small>
                </div>
                {/* Botón de cerrar con estilos */}
                <button
                  type="button"
                  className="btn-close"
                  onClick={handleClose} 
                  aria-label="Close"
                  style={{
                    borderRadius: '50%',
                    transition: 'all 0.2s ease-in-out',
                    background: 'transparent url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e") center/1em auto no-repeat',
                    opacity: '0.7' 
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.backgroundColor = '#FF4D4D';
                    e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%23ffffff%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")';
                    e.target.style.opacity = '1';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.backgroundColor = 'transparent';
                    e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")';
                    e.target.style.opacity = '0.7';
                  }}
                ></button>
              </div>

              {/* === BODY === */}
              <div className="modal-body custom-modal-body">
                <div className="row">
                  {/* Bloque 1: Identificadores (Mitad izquierda) */}
                  <div className="col-lg-6 col-md-6 col-sm-12">
                    <h6 className="mb-3 text-primary">Información General</h6>
                    <div className="row">
                        <div className="col-12 mb-3">
                            <label className="form-label mb-0 fw-bold">Nombre</label>
                            <p>{product.nombre}</p>
                        </div>
                        <div className="col-12 mb-3">
                            <label className="form-label mb-0 fw-bold">Descripción</label>
                            <p className="text-muted">{product.descripcion || "Sin descripción"}</p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">SKU</label>
                            <p>{product.sku}</p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Cód. Barras</label>
                            <p>{product.codigoBarras || "N/A"}</p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">ID Interno</label>
                            <p>{product.productoId}</p>
                        </div>
                    </div>
                  </div>
                  
                  {/* Bloque 2: Precios y Stock (Mitad derecha) */}
                  <div className="col-lg-6 col-md-6 col-sm-12">
                    <h6 className="mb-3 text-primary">Precios e Inventario</h6>
                    <div className="row">
                        <div className="col-12 mb-3">
                            <label className="form-label mb-0 fw-bold">Estado</label>
                            <p>
                                <span className={`badge ${product.activo ? "bg-success" : "bg-danger"}`}>
                                    {product.activo ? "Activo" : "Inactivo"}
                                </span>
                            </p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Precio Detalle</label>
                            <p className="fs-5 text-dark">{formatCurrency(product.precioDetalle)}</p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Precio Mayorista</label>
                            <p className="fs-5 text-dark">{formatCurrency(product.precioMayorista)}</p>
                        </div>
                        <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Stock Actual</label>
                            <p 
                                className={`fs-5 fw-bold ${product.stock <= 0 ? "text-danger" : product.stock <= 10 ? "text-warning" : "text-success"}`}
                            >
                                {product.stock}
                            </p>
                        </div>
                         <div className="col-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Costo</label>
                            <p className="fs-5 text-dark">{formatCurrency(product.costo)}</p>
                        </div>
                    </div>
                  </div>

                  {/* Bloque 3: Clasificación */}
                  <div className="col-lg-12 col-md-12 col-sm-12 mt-3">
                     <h6 className="mb-3 text-primary">Clasificación</h6>
                     <div className="row">
                        <div className="col-lg-6 col-md-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Familia</label>
                            <p>{product.familiaNombre || "N/A"}</p>
                        </div>
                        <div className="col-lg-6 col-md-6 mb-3">
                            <label className="form-label mb-0 fw-bold">Aroma</label>
                            <p>{product.aromaNombre || "N/A"}</p>
                        </div>
                     </div>
                  </div>
                </div>
              </div>
              
              {/* === FOOTER - CON BOTÓN CERRAR/CANCELAR === */}
              <div className="modal-footer custom-modal-footer">
                <button
                  type="button"
                  className="btn btn-cancel" // Estilo de "Cancelar"
                  onClick={handleClose}
                >
                  Cancelar
                </button>
                {esAdmin && (
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => navigate(`${route.editproduct}/${product.productoId}`)} // <-- Cambia esto
                  >
                    Editar Producto
                  </button>
                 )}
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>
    {show && <div className="modal-backdrop fade show"></div>}
    </>
  );
};

ProductViewModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    product: PropTypes.shape({
        productoId: PropTypes.number,
        nombre: PropTypes.string,
        descripcion: PropTypes.string,
        sku: PropTypes.string,
        codigoBarras: PropTypes.string,
        precioDetalle: PropTypes.number,
        precioMayorista: PropTypes.number,
        costo: PropTypes.number,
        stock: PropTypes.number,
        familiaNombre: PropTypes.string,
        aromaNombre: PropTypes.string,
        activo: PropTypes.bool,
    }),
};

export default ProductViewModal;