import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { actualizarAroma, checkNombreDisponible } from '../../../services/aromaService';
import withReactContent from 'sweetalert2-react-content';
import Swal from 'sweetalert2';
import PropTypes from 'prop-types';

const MySwal = withReactContent(Swal);

const debounce = (func, delay) => {
  let timeout;
  return (...args) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), delay);
  };
};

const EditCategoryList = ({ aromaEditando, onAromaActualizado }) => {
  const [nombre, setNombre] = useState('');
  const [actualizando, setActualizando] = useState(false);
  const [originalNombre, setOriginalNombre] = useState('');
  const [nombreValidationMessage, setNombreValidationMessage] = useState('');
  const [nombreAvailable, setNombreAvailable] = useState(true);

  useEffect(() => {
    if (aromaEditando) {
      const nombreOriginal = aromaEditando.nombre || '';
      setNombre(nombreOriginal);
      setOriginalNombre(nombreOriginal);
      setNombreAvailable(true);
      setNombreValidationMessage('Nombre original del aroma.');
    }
  }, [aromaEditando]);

  // Envolver en useCallback
  const checkAromaNombre = useCallback(async (nombreAroma) => {
    const trimmedNombre = nombreAroma.trim();
    if (!trimmedNombre) {
      setNombreValidationMessage("El nombre es requerido.");
      setNombreAvailable(false);
      return;
    }
    if (trimmedNombre.toLowerCase() === originalNombre.toLowerCase()) {
      setNombreValidationMessage("Nombre original del aroma.");
      setNombreAvailable(true);
      return;
    }
    try {
      const response = await checkNombreDisponible(trimmedNombre);
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
  }, [originalNombre]); // Necesario incluir la dependencia

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
    const closeButton = document.querySelector('#edit-category [data-bs-dismiss="modal"]');
    if (closeButton) {
      closeButton.click();
    }
  };

  const handleActualizarAroma = async () => {
    if (!nombre.trim() || !aromaEditando) {
      return;
    }

    if (!nombreAvailable) {
        MySwal.fire({
            title: 'Nombre no válido',
            text: nombreValidationMessage || 'El nombre que ingresaste ya está en uso o no es válido.',
            icon: 'error',
            confirmButtonText: 'OK'
        });
        return;
    }

    try {
      setActualizando(true);
      closeBootstrapModal();
      await new Promise(resolve => setTimeout(resolve, 300));

      await actualizarAroma(aromaEditando.aromaId, nombre.trim());
      
      await MySwal.fire({
        title: '¡Éxito!',
        text: `El aroma ha sido actualizado correctamente`,
        icon: 'success',
        confirmButtonText: 'OK'
      });

      if (onAromaActualizado) {
        onAromaActualizado();
      }
    } catch (error) {
      console.error('Error al actualizar aroma:', error);
      closeBootstrapModal();
      await new Promise(resolve => setTimeout(resolve, 300));
      
      MySwal.fire({
        title: 'Error',
        text: 'No se pudo actualizar el aroma. Intente nuevamente.',
        icon: 'error',
        confirmButtonText: 'OK'
      });
    } finally {
      setActualizando(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleActualizarAroma();
  };

  return (
    <div>
      <div className="modal fade" id="edit-category">
        <div className="modal-dialog modal-dialog-centered custom-modal-two">
          <div className="modal-content">
            <div className="page-wrapper-new p-0">
              <div className="content">
                <div className="modal-header border-0 custom-modal-header">
                  <div className="page-title">
                    <h4>Editar Aroma</h4>
                    {aromaEditando && (
                      <small className="text-muted">
                        Editando: {originalNombre}
                      </small>
                    )}
                  </div>
                  <button
                    type="button"
                    className="btn-close"
                    data-bs-dismiss="modal"
                    aria-label="Close"
                  ></button>
                </div>
                <form onSubmit={handleSubmit}>
                  <div className="modal-body custom-modal-body">
                    <div className="row">
                      <div className="col-lg-12 col-sm-12 col-12">
                        <div className="input-blocks">
                          <label>Nuevo Nombre del Aroma<span style={{ color: 'red', marginLeft: '5px' }}>*</span></label>
                          <input
                            type="text"
                            className={`form-control ${!nombreAvailable ? 'is-invalid' : 
                                nombre.trim().toLowerCase() !== originalNombre.toLowerCase() && nombre.trim() !== '' ? 'is-valid' : ''}`}
                            placeholder="Ingrese el nuevo nombre del aroma"
                            value={nombre}
                            onChange={handleNombreChange}
                            required
                          />
                          {nombreValidationMessage && (
                            <div className={`mt-1 small ${nombreAvailable ? 'text-success' : 'text-danger'}`}>
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
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={actualizando || !nombre.trim() || !nombreAvailable || nombre.trim().toLowerCase() === originalNombre.toLowerCase()}
                    >
                      {actualizando ? 'Actualizando...' : 'Actualizar Aroma'}
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

EditCategoryList.propTypes = {
  aromaEditando: PropTypes.shape({
    aromaId: PropTypes.number,
    nombre: PropTypes.string
  }),
  onAromaActualizado: PropTypes.func
};

EditCategoryList.defaultProps = {
  aromaEditando: null,
  onAromaActualizado: () => {}
};

export default EditCategoryList;