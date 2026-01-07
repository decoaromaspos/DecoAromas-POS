import React, { useState, useCallback, useMemo } from 'react';
import { crearFamilia, checkNombreDisponible } from '../../../services/familiaService';
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

const AddFamilyList = ({ onDataUpdated }) => {
  const [nombre, setNombre] = useState('');
  const [creando, setCreando] = useState(false);

  // --- NUEVOS ESTADOS PARA LA VALIDACIÓN ---
  const [nombreValidationMessage, setNombreValidationMessage] = useState("");
  const [nombreAvailable, setNombreAvailable] = useState(false);

  // Envolver en useCallback
  const checkFamiliaNombre = useCallback(async (nombreFamilia) => {
    if (!nombreFamilia.trim()) {
      setNombreValidationMessage("");
      setNombreAvailable(false);
      return;
    }
    try {
      const response = await checkNombreDisponible(nombreFamilia);
      setNombreValidationMessage(response.message);
      setNombreAvailable(response.available);
    } catch (error) {
      if (error.response && error.response.status === 409) {
        setNombreValidationMessage(error.response.data.message);
        setNombreAvailable(error.response.data.available);
      } else {
        setNombreValidationMessage("Error al verificar el nombre.");
        setNombreAvailable(false);
        console.error("Error en checkNombreDisponible de Familia:", error);
      }
    }
  }, []); // Sin dependencias externas

  // Usar useMemo para la función debounced
  const checkFamiliaNombreDebounced = useMemo(
    () => debounce(checkFamiliaNombre, 500),
    [checkFamiliaNombre]
  );

  const handleNombreChange = (e) => {
    const value = e.target.value;
    setNombre(value);
    setNombreValidationMessage(""); // Limpiar mensaje al escribir
    setNombreAvailable(false);      // Resetear disponibilidad
    checkFamiliaNombreDebounced(value);
  };

  // Función para cerrar el modal de Bootstrap
  const closeBootstrapModal = () => {
    const closeButton = document.querySelector('#add-family [data-bs-dismiss="modal"]');
    if (closeButton) {
      closeButton.click();
    }
  };

  const handleCrearFamilia = async () => {
    if (!nombre.trim() || !nombreAvailable) {
      MySwal.fire({
        title: 'Error',
        text: 'El nombre de la familia es requerido y debe estar disponible.',
        icon: 'error',
        confirmButtonText: 'OK'
      });
      return;
    }

    setCreando(true);
    closeBootstrapModal();
    await new Promise(resolve => setTimeout(resolve, 300));

    try {
      await crearFamilia(nombre.trim());
      await MySwal.fire({
        title: '¡Éxito!',
        text: `La familia "${nombre}" ha sido creada correctamente`,
        icon: 'success',
        confirmButtonText: 'OK'
      });
      
      onDataUpdated();
      handleClose(); // Limpia todos los estados
    } catch (error) {
      console.error('Error al crear familia:', error);
      MySwal.fire({
        title: 'Error',
        text: 'No se pudo crear la familia. Intente nuevamente.',
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
    handleCrearFamilia();
  };

  return (
    <div>
      <div className="modal fade" id="add-family">
        <div className="modal-dialog modal-dialog-centered custom-modal-two">
          <div className="modal-content">
            <div className="page-wrapper-new p-0">
              <div className="content">
                <div className="modal-header border-0 custom-modal-header">
                  <div className="page-title">
                    <h4>Crear Familia</h4>
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
                          <label>Nombre de la Familia<span style={{ color: 'red', marginLeft: '5px' }}>*</span></label>
                          <input
                            type="text"
                            className={`form-control ${!nombreAvailable && nombre.trim() !== '' && nombreValidationMessage ? 'is-invalid' : nombreAvailable ? 'is-valid' : ''}`}
                            placeholder="Ingrese el nombre de la familia"
                            value={nombre}
                            onChange={handleNombreChange}
                            required
                          />
                          {/* --- MENSAJE DE VALIDACIÓN EN TIEMPO REAL --- */}
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
                      {creando ? 'Creando...' : 'Crear Familia'}
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

AddFamilyList.propTypes = {
  onDataUpdated: PropTypes.func
};

AddFamilyList.defaultProps = {
  onDataUpdated: () => {}
};

export default AddFamilyList;