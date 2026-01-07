import React, { useState, useEffect } from "react";
import {
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  User, 
  Key
} from "feather-icons-react/build/IconComponents";
import Swal from "sweetalert2";
import { all_routes } from "../../Router/all_routes";
import { useAuth } from "../../context/AuthContext";
import {
  actualizarUsuario,
  checkCorreoDisponible,
  checkUsernameDisponible
} from "../../services/usuarioService";
import { setToogleHeader } from "../../core/redux/action";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";

import CambiarPasswordModal from "../../core/modals/peoples/CambiarPasswordModal";

const emailValidator = (email) => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

const Profile = () => {
  const route = all_routes;
  const { usuario, actualizarDatosUsuario } = useAuth();
  const dispatch = useDispatch();
  const data = useSelector((state) => state.toggle_header);

  const [originalUsuario, setOriginalUsuario] = useState(null);
  const [formData, setFormData] = useState({
    nombre: "",
    apellido: "",
    correo: "",
    username: "",
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  const [availabilityStatus, setAvailabilityStatus] = useState({
    correo: { checked: false, available: false, message: '' },
    username: { checked: false, available: false, message: '' },
  });

  useEffect(() => {
    if (usuario) {
      const initialData = {
        nombre: usuario.nombre || "",
        apellido: usuario.apellido || "",
        correo: usuario.correo || "",
        username: usuario.username || "",
      };
      setFormData(initialData);
      setOriginalUsuario(initialData);
    }
  }, [usuario]);

  // --- Hooks de validación en tiempo real para correo y username (igual que en EditUsuarioModal) ---
  useEffect(() => {
    const currentCorreo = formData.correo.trim();
    if (!originalUsuario || currentCorreo === originalUsuario.correo || !emailValidator(currentCorreo)) {
      setAvailabilityStatus(prev => ({ ...prev, correo: { checked: false } }));
      return;
    }
    const handler = setTimeout(async () => {
      try {
        const res = await checkCorreoDisponible(currentCorreo);
        setAvailabilityStatus(prev => ({ ...prev, correo: { checked: true, ...res } }));
      } catch (error) {
        setAvailabilityStatus(prev => ({ ...prev, correo: { checked: true, available: false, message: error.response?.data?.message } }));
      }
    }, 500);
    return () => clearTimeout(handler);
  }, [formData.correo, originalUsuario]);

  useEffect(() => {
    const currentUsername = formData.username.trim();
    if (!originalUsuario || currentUsername === originalUsuario.username || !currentUsername) {
      setAvailabilityStatus(prev => ({ ...prev, username: { checked: false } }));
      return;
    }
    const handler = setTimeout(async () => {
      try {
        const res = await checkUsernameDisponible(currentUsername);
        setAvailabilityStatus(prev => ({ ...prev, username: { checked: true, ...res } }));
      } catch (error) {
        setAvailabilityStatus(prev => ({ ...prev, username: { checked: true, available: false, message: error.response?.data?.message } }));
      }
    }, 500);
    return () => clearTimeout(handler);
  }, [formData.username, originalUsuario]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: null }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if ((availabilityStatus.correo.checked && !availabilityStatus.correo.available) ||
      (availabilityStatus.username.checked && !availabilityStatus.username.available)) {
      return;
    }

    setIsSubmitting(true);
    try {
      const dataToSend = {
        nombre: formData.nombre.trim(),
        apellido: formData.apellido.trim(),
        correo: formData.correo.trim(),
        username: formData.username.trim(),
      };

      const updatedUser = await actualizarUsuario(usuario.usuarioId, dataToSend);

      actualizarDatosUsuario(updatedUser);

      Swal.fire("¡Perfil Actualizado!", "Tus datos han sido guardados.", "success");

    } catch (error) {
      Swal.fire("Error", error?.response?.data?.message || "No se pudo actualizar el perfil.", "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Función para resetear el formulario a los valores originales
  const handleCancel = () => {
    setFormData(originalUsuario);
    setAvailabilityStatus({ correo: { checked: false }, username: { checked: false } });
    setErrors({});
  };

    // Tooltip para el colapso
  const renderCollapseTooltip = (props) => (
    <Tooltip id="refresh-tooltip" {...props}>
      Collapse
    </Tooltip>
  );


  return (
    <>
      <style>
        {`
        .profile-user-icon {
            width: 120px;
            height: 120px;
            border-radius: 50%;
            background-color: #fff; /* Fondo blanco */
            display: flex;
            justify-content: center;
            align-items: center;
            border: 4px solid #fff;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
            color: #7367f0; /* Color del ícono */
        }
        `}
      </style>
      <div className="page-wrapper">
        <div className="content">
          <div className="page-header">
            <div className="page-title">
              <h4>Mi Perfil</h4>
              <h6>Gestión de tu perfil de usuario</h6>
            </div>

          <ul className="table-top-head">
            <li>
              <div className="page-btn">
                <Link to={route.configuracion} className="btn btn-secondary">
                  <ArrowLeft className="me-2" />
                  Volver a Configuración
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
          <div className="card">
            <div className="card-body">
              <form onSubmit={handleSubmit}>
                <div className="profile-set">
                  <div className="profile-top">
                    <div className="profile-content">
                      <div>
                        <div className="profile-user-icon">
                          <User size={70} strokeWidth={1.5} />
                        </div>
                      </div>
                      <div className="profile-contentname">
                        <h2>{`${formData.nombre} ${formData.apellido}`}</h2>
                        <h4>Actualiza tus datos personales.</h4>
                      </div>
                    </div>
                    <div className="ms-auto">
                      <button type="button" className="btn btn-light" onClick={() => setShowPasswordModal(true)}>
                        <Key className="me-2" size={16} />
                        Cambiar Contraseña
                      </button>
                    </div>
                  </div>
                </div>
                <div className="row">
                  <div className="col-lg-6 col-sm-12">
                    <div className="input-blocks">
                      <label className="form-label">Nombre</label>
                      <input type="text" name="nombre" className="form-control" value={formData.nombre} onChange={handleChange} />
                    </div>
                  </div>
                  <div className="col-lg-6 col-sm-12">
                    <div className="input-blocks">
                      <label className="form-label">Apellido</label>
                      <input type="text" name="apellido" className="form-control" value={formData.apellido} onChange={handleChange} />
                    </div>
                  </div>
                  <div className="col-lg-6 col-sm-12">
                    <div className="input-blocks">
                      <label>Username</label>
                      <input type="text" name="username" className="form-control" value={formData.username} onChange={handleChange} />
                      {availabilityStatus.username.checked && (
                        <small className={availabilityStatus.username.available ? 'text-success' : 'text-danger'}>
                          {availabilityStatus.username.message}
                        </small>
                      )}
                    </div>
                  </div>
                  <div className="col-lg-6 col-sm-12">
                    <div className="input-blocks">
                      <label>Email</label>
                      <input type="email" name="correo" className="form-control" value={formData.correo} onChange={handleChange} />
                      {availabilityStatus.correo.checked && (
                        <small className={availabilityStatus.correo.available ? 'text-success' : 'text-danger'}>
                          {availabilityStatus.correo.message}
                        </small>
                      )}
                    </div>
                  </div>
                  <div className="col-12">
                    <button type="submit" className="btn btn-submit me-2" disabled={isSubmitting}>
                      {isSubmitting ? "Guardando..." : "Guardar Cambios"}
                    </button>
                    <button type="button" className="btn btn-cancel" onClick={handleCancel} >
                      Cancelar
                    </button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      {usuario && (
        <CambiarPasswordModal
          show={showPasswordModal}
          handleClose={() => setShowPasswordModal(false)}
          userId={usuario.usuarioId}
        />
      )}
    </>
  );
};

export default Profile;