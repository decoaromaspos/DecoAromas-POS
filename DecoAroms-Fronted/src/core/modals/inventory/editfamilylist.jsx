import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { actualizarFamilia, checkNombreDisponible } from '../../../services/familiaService';
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

const EditFamilyList = ({ familiaEditando, onFamiliaActualizada }) => {
  const [nombre, setNombre] = useState('');
  const [actualizando, setActualizando] = useState(false);
  const [originalNombre, setOriginalNombre] = useState('');
  const [nombreValidationMessage, setNombreValidationMessage] = useState('');
  const [nombreAvailable, setNombreAvailable] = useState(true);

  useEffect(() => {
    if (familiaEditando) {
      const nombreOriginal = familiaEditando.nombre || '';
      setNombre(nombreOriginal);
      setOriginalNombre(nombreOriginal);
      setNombreAvailable(true);
      setNombreValidationMessage('Nombre original de la familia.');
    }
  }, [familiaEditando]);

  // Envolver en useCallback
  const checkFamiliaNombre = useCallback(async (nombreFamilia) => {
    const trimmedNombre = nombreFamilia.trim();
    if (!trimmedNombre) {
      setNombreValidationMessage("El nombre es requerido.");
      setNombreAvailable(false);
      return;
    }
    // Usamos originalNombre que es una dependencia externa
    if (trimmedNombre.toLowerCase() === originalNombre.toLowerCase()) {
      setNombreValidationMessage("Nombre original de la familia.");
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
        console.error("Error en checkNombreDisponible de Familia:", error);
      }
    }
  }, [originalNombre]); // Dependencia necesaria

  // Usar useMemo para la función debounced
  const checkFamiliaNombreDebounced = useMemo(
    () => debounce(checkFamiliaNombre, 500),
    [checkFamiliaNombre]
  );

  const handleNombreChange = (e) => {
    const value = e.target.value;
    setNombre(value);
    setNombreValidationMessage("");
    setNombreAvailable(false);
    checkFamiliaNombreDebounced(value);
  };

  const closeBootstrapModal = () => {
    const closeButton = document.querySelector('#edit-family [data-bs-dismiss="modal"]');
    if (closeButton) {
      closeButton.click();
    }
  };

  const handleActualizarFamilia = async () => {
    if (!nombre.trim() || !familiaEditando || !nombreAvailable) {
        if (!nombreAvailable) {
            MySwal.fire({
                title: 'Nombre no válido',
                text: nombreValidationMessage || 'El nombre ingresado ya está en uso o no es válido.',
                icon: 'error',
                confirmButtonText: 'OK'
            });
        }
        return;
    }

    try {
      setActualizando(true);
      closeBootstrapModal();
      await new Promise(resolve => setTimeout(resolve, 300));

      await actualizarFamilia(familiaEditando.familiaId, nombre.trim());
      
      await MySwal.fire({
        title: '¡Éxito!',
        text: `La familia ha sido actualizada correctamente`,
        icon: 'success',
        confirmButtonText: 'OK'
      });

      if (onFamiliaActualizada) {
        onFamiliaActualizada();
      }
    } catch (error) {
      console.error('Error al actualizar familia:', error);
      closeBootstrapModal();
      await new Promise(resolve => setTimeout(resolve, 300));
      
      MySwal.fire({
        title: 'Error',
        text: 'No se pudo actualizar la familia. Intente nuevamente.',
        icon: 'error',
        confirmButtonText: 'OK'
      });
    } finally {
      setActualizando(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleActualizarFamilia();
  };

  return (
    <div>
      <div className="modal fade" id="edit-family">
        <div className="modal-dialog modal-dialog-centered custom-modal-two">
          <div className="modal-content">
            <div className="page-wrapper-new p-0">
              <div className="content">
                <div className="modal-header border-0 custom-modal-header">
                  <div className="page-title">
                    <h4>Editar Familia</h4>
                    {familiaEditando && (
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
                          <label>Nuevo Nombre de la Familia<span style={{ color: 'red', marginLeft: '5px' }}>*</span></label>
                          <input
                            type="text"
                            className={`form-control ${!nombreAvailable ? 'is-invalid' : 
                                nombre.trim().toLowerCase() !== originalNombre.toLowerCase() && nombre.trim() !== '' ? 'is-valid' : ''}`}
                            placeholder="Ingrese el nuevo nombre de la familia"
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
                      {actualizando ? 'Actualizando...' : 'Actualizar Familia'}
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

EditFamilyList.propTypes = {
  familiaEditando: PropTypes.shape({
    familiaId: PropTypes.number,
    nombre: PropTypes.string
  }),
  onFamiliaActualizada: PropTypes.func
};

EditFamilyList.defaultProps = {
  familiaEditando: null,
  onFamiliaActualizada: () => {}
};

export default EditFamilyList;