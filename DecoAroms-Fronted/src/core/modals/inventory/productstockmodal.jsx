import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { toast } from "react-toastify"; // Asumimos que ya está instalado y configurado
import {
    actualizarStockProducto, 
    incrementarStockProducto, 
    decrementarStockProducto 
} from '../../../services/productoService';
import { useAuth } from "../../../context/AuthContext";



const ProductStockModal = ({ show, handleClose, product, onStockUpdated }) => {
    const [currentStock, setCurrentStock] = useState(product?.stock || 0); 
    const [operationType, setOperationType] = useState('set'); 
    const [value, setValue] = useState(''); 
    const [isUpdating, setIsUpdating] = useState(false);
    const { usuario } = useAuth();
    const CURRENT_USER_ID = usuario.usuarioId;
    
    // Sincronizar el estado local cuando cambia el producto (al abrir el modal)
    useEffect(() => {
        if (product) {
            setCurrentStock(product.stock);
            setValue(''); // Limpiar el valor al cambiar de producto o abrir
        }
    }, [product]);

    // Función para manejar el cierre del modal, restableciendo estados
    const handleModalClose = () => {
        setOperationType('set');
        setValue('');
        handleClose();
    };


    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!product || isUpdating) return;
        
        const numericValue = Number(value);
        const productId = product.productoId;
        
        if (isNaN(numericValue) || (operationType === 'set' && numericValue < 0)) {
            toast.error("Por favor, ingrese una cantidad válida y positiva.");
            return;
        }

        setIsUpdating(true);
        let successMessage = "";
        let finalStock = currentStock;

        try {
            if (operationType === 'set') {
                // Opción 1: Establecer un valor específico 
                await actualizarStockProducto(productId, numericValue, CURRENT_USER_ID); 
                
                finalStock = numericValue;
                successMessage = `Stock actualizado a: ${numericValue}`;
                
            } else {
                // Opción 2: Ajustar (Incrementar o Decrementar)
                if (numericValue === 0) {
                    toast.info("No se realizó ningún cambio.");
                    setIsUpdating(false);
                    return;
                } else if (numericValue > 0) {
                    // Incrementar 
                    await incrementarStockProducto(productId, numericValue, CURRENT_USER_ID); 
                    
                    finalStock = currentStock + numericValue;
                    successMessage = `Stock incrementado en: ${numericValue}. Nuevo Stock: ${finalStock}`;
                } else {
                    // Decrementar
                    const decrementValue = Math.abs(numericValue);
                    


                    // En caso de no permitir stock negativo, elimninar este bloque                                                     OJO con stock negativo
                    //if (currentStock < decrementValue) {
                        //toast.warn("No se puede retirar más stock del disponible.");
                        //setIsUpdating(false);
                        //return;
                    //}
                    
                    await decrementarStockProducto(productId, decrementValue, CURRENT_USER_ID); 

                    finalStock = currentStock - decrementValue;
                    successMessage = `Stock decrementado en: ${decrementValue}. Nuevo Stock: ${finalStock}`;
                }
            }

            // Muestra notificación de éxito
            toast.success(` ¡Éxito! ${successMessage}`);
            
            // Llama a la función de callback para actualizar la lista principal
            if (onStockUpdated) {
                // Pasamos el nuevo stock final
                onStockUpdated(productId, finalStock); 
            }

            // Cerrar el modal
            handleModalClose();

        } catch (error) {
            console.error('Error al actualizar stock:', error);
            // Muestra notificación de error
            // Asumimos que el error.response.data.message contiene el mensaje del servidor
            const errorMessage = error.response?.data?.message || error.message || 'Intente de nuevo.';
            toast.error(` Error al actualizar el stock: ${errorMessage}`);
        } finally {
            setIsUpdating(false);
        }
    };

    if (!product) return null;

    // Determinar la etiqueta del campo de entrada
    let inputLabel = '';
    if (operationType === 'set') {
        inputLabel = 'Nuevo Stock Total';
    } else {
        inputLabel = 'Cantidad a Ingresar (+) o Retirar (-)';
    }

    return (
        <>
            {/* Contenedor principal del modal con estilos heredados */}
            <div 
                className={`modal fade ${show ? 'show d-block' : ''}`}
                tabIndex="-1"
                aria-hidden={!show}
                style={{ 
                    display: show ? 'block' : 'none', 
                    zIndex: 1055 // Asegura que esté sobre el backdrop
                }} 
            >
                <div className="modal-dialog modal-dialog-centered modal-lg custom-modal-two">
                    <div className="modal-content">
                        <div className="page-wrapper-new p-0">
                            <div className="content">
                                
                                {/* === HEADER - Estilo similar al de ver producto === */}
                                <div className="modal-header border-0 custom-modal-header">
                                    <div className="page-title">
                                        <h4>Gestionar Stock</h4>
                                        <small className="text-muted">
                                            Producto: {product.nombre} (SKU: {product.sku})
                                        </small>
                                    </div>
                                    <button
                                        type="button"
                                        className="btn-close"
                                        onClick={handleModalClose}
                                        aria-label="Close"
                                        style={{ /* Estilos de btn-close heredados */
                                            borderRadius: '50%', transition: 'all 0.2s ease-in-out', 
                                            background: 'transparent url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e") center/1em auto no-repeat', opacity: '0.7' 
                                        }}
                                        onMouseEnter={(e) => { e.target.style.backgroundColor = '#FF4D4D'; e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%23ffffff%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")'; e.target.style.opacity = '1'; }}
                                        onMouseLeave={(e) => { e.target.style.backgroundColor = 'transparent'; e.target.style.backgroundImage = 'url("data:image/svg+xml,%3csvg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 16 16%27 fill=%27%236c757d%27%3e%3cpath d=%27M.293.293a1 1 0 0 1 1.414 0L8 6.586 14.293.293a1 1 0 1 1 1.414 1.414L9.414 8l6.293 6.293a1 1 0 0 1-1.414 1.414L8 9.414l-6.293 6.293a1 1 0 0 1-1.414-1.414L6.586 8 .293 1.707a1 1 0 0 1 0-1.414z%27/%3e%3c/svg%3e")'; e.target.style.opacity = '0.7'; }}
                                    ></button>
                                </div>

                                <form onSubmit={handleSubmit}>
                                    <div className="modal-body custom-modal-body">
                                        <div className="row">
                                            {/* Columna de Stock Actual */}
                                            <div className="col-12 mb-4">
                                                <h5 className="text-muted">Stock Actual: 
                                                    <span className={`fw-bold ms-2 ${currentStock <= 0 ? "text-danger" : currentStock <= 10 ? "text-warning" : "text-success"}`}>
                                                        {currentStock} Unidades
                                                    </span>
                                                </h5>
                                            </div>
                                            
                                            {/* Columna de Selección de Tipo de Operación */}
                                            <div className="col-lg-12 mb-4">
                                                <h6 className="text-primary mb-3">Seleccione el tipo de gestión:</h6>
                                                
                                                <div className="d-flex flex-wrap gap-4">
                                                    {/* Opción 1: Establecer un valor específico (actualizarStockProducto) */}
                                                    <div className="form-check">
                                                        <input
                                                            className="form-check-input"
                                                            type="radio"
                                                            name="operationType"
                                                            id="setOption"
                                                            value="set"
                                                            checked={operationType === 'set'}
                                                            onChange={(e) => {
                                                                setOperationType(e.target.value);
                                                                setValue('');
                                                            }}
                                                        />
                                                        <label className="form-check-label fw-bold" htmlFor="setOption">
                                                            Establecer Stock Total
                                                            <small className="d-block text-muted fw-normal"> (Ajusta al valor exacto que ingreses)</small>
                                                        </label>
                                                    </div>

                                                    {/* Opción 2: Ajustar (Incrementar/Decrementar) */}
                                                    <div className="form-check">
                                                        <input
                                                            className="form-check-input"
                                                            type="radio"
                                                            name="operationType"
                                                            id="adjustOption"
                                                            value="adjust"
                                                            checked={operationType === 'adjust'}
                                                            onChange={(e) => {
                                                                setOperationType(e.target.value);
                                                                setValue('');
                                                            }}
                                                        />
                                                        <label className="form-check-label fw-bold" htmlFor="adjustOption">
                                                            Añadir/Retirar Stock
                                                            <small className="d-block text-muted fw-normal"> (Usa valor positivo para añadir, negativo para retirar)</small>
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>

                                            {/* Columna de Entrada de Valor (Dinámica) */}
                                            <div className="col-lg-6 col-md-8 col-sm-12">
                                                <div className="input-blocks">
                                                    <label className="fw-bold">{inputLabel}</label>
                                                    <input
                                                        type="number"
                                                        className="form-control"
                                                        placeholder={operationType === 'set' ? "Ej: 150" : "Ej: 10 (Añadir) o -5 (Retirar)"}
                                                        value={value}
                                                        onChange={(e) => setValue(e.target.value)}
                                                        required
                                                        min={operationType === 'set' ? "0" : undefined} // El stock total no puede ser negativo
                                                    />
                                                    {operationType === 'adjust' && (
                                                        <small className="text-info mt-1 d-block">
                                                            Ingrese un valor negativo (ej: -5) para decrementar stock.
                                                        </small>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* === FOOTER - Con botones Cancelar y Guardar === */}
                                    <div className="modal-footer custom-modal-footer">
                                        <button
                                            type="button"
                                            className="btn btn-cancel" 
                                            onClick={handleModalClose}
                                            disabled={isUpdating}
                                        >
                                            Cancelar
                                        </button>
                                        <button
                                            type="submit"
                                            className="btn btn-primary"
                                            disabled={isUpdating || !value.toString().trim()}
                                        >
                                            {isUpdating ? 'Guardando...' : 'Confirmar Actualización'}
                                        </button>
                                    </div>
                                </form>

                            </div>
                        </div>
                    </div>
                </div>
            </div>
            {/* Backdrop */}
            {show && <div className="modal-backdrop fade show"></div>}
        </>
    );
};

// ... (PropTypes)
ProductStockModal.propTypes = {
    show: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    product: PropTypes.shape({
        productoId: PropTypes.number.isRequired,
        nombre: PropTypes.string,
        sku: PropTypes.string,
        stock: PropTypes.number,
    }),
    onStockUpdated: PropTypes.func
};

ProductStockModal.defaultProps = {
    onStockUpdated: () => {}
};

export default ProductStockModal;