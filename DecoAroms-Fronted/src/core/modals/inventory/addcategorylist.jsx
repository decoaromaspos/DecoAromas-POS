import React, { useState, useCallback, useMemo } from 'react';
import { crearAroma, checkNombreDisponible } from '../../../services/aromaService';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import PropTypes from 'prop-types';

const MySwal = withReactContent(Swal);

// Helper para debounce
const debounce = (func, delay) => {
  let timeout;
  return (...args) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), delay);
  };
};

// Aceptamos tanto onDataUpdated como onAromaCreado para compatibilidad
const AddCategoryList = ({ onDataUpdated, onAromaCreado }) => {
  const [nombre, setNombre] = useState('');
  const [creando, setCreando] = useState(false);
  const [nombreValidationMessage, setNombreValidationMessage] = useState("");
  const [nombreAvailable, setNombreAvailable] = useState(false);

  // Envolver en useCallback
  const checkAromaNombre = useCallback(async (nombreAroma) => {
    if (!nombreAroma.trim()) {
      setNombreValidationMessage("");
      setNombreAvailable(false);
      return;
    }
    try {
      const response = await checkNombreDisponible(nombreAroma);
      setNombreValidationMessage(response.message);
      setNombreAvailable(response.available);
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setNombreValidationMessage(error.response.data.message);
        setNombreAvailable(error.response.data.available);
      } else {
        setNombreValidationMessage("Error al verificar el nombre.");
        setNombreAvailable(false);
        console.error("Error en checkNombreDisponible de Aroma:", error);
      }
    }
  }, []); // Sin dependencias externas

  // Usar useMemo para la función debounced
  const checkAromaNombreDebounced = useMemo(
    () => debounce(checkAromaNombre, 500),
    [checkAromaNombre]
  );

  const handleNombreChange = (e) => {
    const value = e.target.value;
    setNombre(value);
    setNombreValidationMessage("");
    setNombreAvailable(false);
    checkAromaNombreDebounced(value);
  };

  const closeBootstrapModal = () => {
    const closeButton = document.querySelector('#add-category [data-bs-dismiss="modal"]');
    if (closeButton) {
      closeButton.click();
    }
  };

  const handleCrearAroma = async () => {
    if (!nombre.trim() || !nombreAvailable) {
      MySwal.fire({
        title: 'Error',
        text: 'El nombre del aroma es requerido y debe estar disponible.',
        icon: 'error',
        confirmButtonText: 'OK'
      });
      return;
    }

    setCreando(true);
    closeBootstrapModal();
    await new Promise(resolve => setTimeout(resolve, 500));

    try {
      await crearAroma(nombre.trim());
      await MySwal.fire({
        title: '¡Éxito!',
        text: `El aroma "${nombre}" ha sido creado correctamente`,
        icon: 'success',
        confirmButtonText: 'OK'
      });
      
      // Llamar a cualquiera de las dos funciones de actualización si existen
      if (onDataUpdated) onDataUpdated();
      if (onAromaCreado) onAromaCreado();

      handleClose();
    } catch (error) {
      console.error('Error al crear aroma:', error);
      MySwal.fire({
        title: 'Error',
        text: 'No se pudo crear el aroma. Intente nuevamente.',
        icon: 'error',
        confirmButtonText: 'OK'
      });
    } finally {
      setCreando(false);
    }
  };

  const handleClose = () => {
    setNombre('');
    setNombreValidationMessage('');
    setNombreAvailable(false);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleCrearAroma();
  };

  return (
    <div>
      <div className="modal fade" id="add-category">
        <div className="modal-dialog modal-dialog-centered custom-modal-two">
          <div className="modal-content">
            <div className="page-wrapper-new p-0">
              <div className="content">
                <div className="modal-header border-0 custom-modal-header">
                  <div className="page-title">
                    <h4>Crear Aroma</h4>
                  </div>
                  <button
                    type="button"
                    className="btn-close"
                    data-bs-dismiss="modal"
                    aria-label="Close"
                    onClick={handleClose}
                  ></button>
                </div>
                <form onSubmit={handleSubmit}>
                  <div className="modal-body custom-modal-body">
                    <div className="row">
                      <div className="col-lg-12 col-sm-12 col-12">
                        <div className="input-blocks">
                          <label>Nombre del Aroma<span style={{ color: 'red', marginLeft: '5px' }}>*</span></label>
                          <input
                            type="text"
                            className={`form-control ${!nombreAvailable && nombre.trim() !== '' && nombreValidationMessage ? 'is-invalid' : nombreAvailable ? 'is-valid' : ''}`}
                            placeholder="Ingrese el nombre del aroma"
                            value={nombre}
                            onChange={handleNombreChange}
                            required
                          />
                          {nombreValidationMessage && (
                            <div className={`mt-1 ${nombreAvailable ? 'text-success' : 'text-danger'}`} style={{ fontSize: '0.875em' }}>
                              {nombreValidationMessage}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="modal-footer custom-modal-footer">
                    <button
                      type="button"
                      className="btn btn-cancel"
                      data-bs-dismiss="modal"
                      onClick={handleClose}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={creando || !nombre.trim() || !nombreAvailable}
                    >
                      {creando ? 'Creando...' : 'Crear Aroma'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

AddCategoryList.propTypes = {
  onDataUpdated: PropTypes.func,
  onAromaCreado: PropTypes.func
};

AddCategoryList.defaultProps = {
  onDataUpdated: null,
  onAromaCreado: null
};

export default AddCategoryList;