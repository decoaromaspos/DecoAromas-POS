import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import Select from "react-select";
import withReactContent from "sweetalert2-react-content";
import Swal from "sweetalert2";
import { OverlayTrigger, Tooltip, Modal, Form } from "react-bootstrap";
import {
    Edit,
    PlusCircle,
    RotateCcw,
    Trash2,
    CheckCircle,
    AlertCircle,
    DollarSign,
    Calendar,
    Activity,
    XCircle,
    Tag,
    Layers,
} from "feather-icons-react/build/IconComponents";
import {
    getVentasPorAnio,
    crearVentaMensual,
    actualizarVentaMensual,
    eliminarVentaMensual
} from "../../services/ventaOnlineMensualService";

// --- CONSTANTES GLOBALES ---
const MySwal = withReactContent(Swal);

const NOMBRES_MESES = [
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
];

const VentaOnlineList = () => {
    
    // --- ESTADOS ---
    const [loading, setLoading] = useState(false);
    
    // Estado para el select del año. Valor inicial: Año actual.
    const [anioSeleccionado, setAnioSeleccionado] = useState({ 
        value: new Date().getFullYear(), 
        label: new Date().getFullYear().toString() 
    });
    
    // Array donde mezclaremos los 12 meses con la data del backend
    const [datosTabla, setDatosTabla] = useState([]); 
    
    // Estados del Modal
    const [showModal, setShowModal] = useState(false);
    const [modalMode, setModalMode] = useState("create"); // 'create' | 'update'
    const [currentVenta, setCurrentVenta] = useState({
        id: null,
        anio: new Date().getFullYear(),
        mes: 1,
        totalDetalle: 0,
        totalMayorista: 0,
        nombreMes: '' // Solo visual
    });

    // --- CONFIGURACIÓN DE SELECTORES ---
    const anioActual = new Date().getFullYear();
    const aniosOptions = [];
    // Generamos opciones desde 2023 hasta el año actual
    for (let i = 2023; i <= anioActual; i++) {
        aniosOptions.push({ value: i, label: i.toString() });
    }

    // --- LÓGICA DE NEGOCIO Y MEZCLA DE DATOS ---

    // Función auxiliar para determinar si un mes es futuro
    const esMesFuturo = (anio, mes) => {
        const hoy = new Date();
        // Si el año es mayor al actual, todo es futuro
        if (anio > hoy.getFullYear()) return true;
        // Si es el mismo año, comparamos el mes (mes en JS es 0-11)
        if (anio === hoy.getFullYear() && (mes - 1) > hoy.getMonth()) return true;
        return false;
    };

    // useCallback ahora es seguro porque MySwal y NOMBRES_MESES son constantes externas
    const cargarDatos = useCallback(async () => {
        setLoading(true);
        try {
            const anioValue = anioSeleccionado.value;
            
            // 1. Obtener datos reales de la BD usando tu servicio
            const dataBackend = await getVentasPorAnio(anioValue);
            
            // 2. Crear estructura base de 12 meses
            const mesesEstructura = Array.from({ length: 12 }, (_, i) => i + 1);
            
            // 3. Mezclar (Merge) datos del backend con la estructura base
            const datosProcesados = mesesEstructura.map(mesIndex => {
                // Buscamos si existe el mes en la respuesta del backend
                const registroEncontrado = dataBackend.find(d => d.mes === mesIndex);
                const esFuturo = esMesFuturo(anioValue, mesIndex);

                if (registroEncontrado) {
                    return {
                        ...registroEncontrado,
                        nombreMes: NOMBRES_MESES[mesIndex - 1],
                        estado: 'registrado',
                        esFuturo: false
                    };
                } else {
                    // Si no existe, creamos un objeto "placeholder" con estado pendiente
                    return {
                        id: null,
                        anio: anioValue,
                        mes: mesIndex,
                        nombreMes: NOMBRES_MESES[mesIndex - 1],
                        totalDetalle: 0,
                        totalMayorista: 0,
                        estado: 'pendiente',
                        esFuturo: esFuturo
                    };
                }
            });

            setDatosTabla(datosProcesados);

        } catch (error) {
            console.error(error);
            MySwal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudieron cargar las ventas online mensuales.',
                confirmButtonClass: "btn btn-danger",
            });
        } finally {
            setLoading(false);
        }
    }, [anioSeleccionado]); // Dependencia única: anioSeleccionado

    useEffect(() => {
        cargarDatos();
    }, [cargarDatos]);

    // --- MANEJO DEL MODAL ---

    const handleOpenModal = (mesData) => {
        setModalMode(mesData.id ? "update" : "create");
        setCurrentVenta({
            id: mesData.id,
            anio: mesData.anio,
            mes: mesData.mes,
            totalDetalle: mesData.totalDetalle || 0,
            totalMayorista: mesData.totalMayorista || 0,
            nombreMes: mesData.nombreMes
        });
        setShowModal(true);
    };

    const handleCloseModal = () => setShowModal(false);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        // Validación extra: Si el usuario pega un negativo, lo forzamos a 0 o positivo
        let numericValue = value === "" ? "" : parseFloat(value);
        if (numericValue < 0) numericValue = 0;
        
        setCurrentVenta(prev => ({
            ...prev,
            [name]: numericValue
        }));
    };

    // Bloquea caracteres no numéricos y signos negativos a nivel de teclado
    const preventNegativeInput = (e) => {
        // Bloqueamos 'e' (exponente), 'E', '+' y '-'
        if (["e", "E", "+", "-"].includes(e.key)) {
            e.preventDefault();
        }
    };

    const handleSave = async () => {
        // Validaciones simples frontend
        if (currentVenta.totalDetalle < 0 || currentVenta.totalMayorista < 0) {
            MySwal.fire({
                icon: 'warning',
                title: 'Atención',
                text: 'Los montos no pueden ser negativos.',
                confirmButtonClass: "btn btn-warning",
            });
            return;
        }

        const payload = {
            anio: currentVenta.anio,
            mes: currentVenta.mes,
            totalDetalle: Number(currentVenta.totalDetalle) || 0,
            totalMayorista: Number(currentVenta.totalMayorista) || 0
        };

        try {
            if (modalMode === 'create') {
                await crearVentaMensual(payload);
                MySwal.fire({
                    icon: 'success',
                    title: '¡Registrado!',
                    text: 'La venta mensual ha sido creada exitosamente.',
                    confirmButtonClass: "btn btn-success",
                });
            } else {
                await actualizarVentaMensual(payload);
                MySwal.fire({
                    icon: 'success',
                    title: '¡Actualizado!',
                    text: 'Los montos han sido actualizados correctamente.',
                    confirmButtonClass: "btn btn-success",
                });
            }
            
            handleCloseModal();
            cargarDatos(); // Recargar la tabla

        } catch (error) {
            console.error(error);
            // Intentamos mostrar el mensaje que viene del backend (ej: "Ya existe registro...")
            const msg = error.response?.data?.message || "Ocurrió un error al procesar la solicitud.";
            MySwal.fire({
                icon: 'error',
                title: 'Error',
                text: msg,
                confirmButtonClass: "btn btn-danger",
            });
        }
    };

    const handleDelete = (id) => {
        MySwal.fire({
            title: "¿Estás seguro?",
            text: "Se eliminarán los datos de ventas para este mes y volverá a estado pendiente.",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#dc3545",
            cancelButtonColor: "#6c757d",
            confirmButtonText: "Sí, eliminar",
            cancelButtonText: "Cancelar",
            customClass: {
                confirmButton: "btn btn-danger",
                cancelButton: "btn btn-secondary ms-2"
            }
        }).then(async (result) => {
            if (result.isConfirmed) {
                try {
                    await eliminarVentaMensual(id);
                    await MySwal.fire({
                        title: "¡Eliminado!",
                        text: "El registro ha sido eliminado.",
                        icon: "success",
                        confirmButtonClass: "btn btn-success"
                    });
                    cargarDatos();
                } catch (error) {
                    MySwal.fire({
                        title: "Error",
                        text: "No se pudo eliminar el registro.",
                        icon: "error",
                        confirmButtonClass: "btn btn-danger"
                    });
                }
            }
        });
    };

    // --- RENDERIZADO ---
    return (
        <div className="page-wrapper">
            <div className="content">
                
                {/* Cabecera */}
                <div className="page-header">
                    <div className="add-item d-flex">
                        <div className="page-title">
                            <h4>Ventas Online Mensuales</h4>
                            <h6>Gestión de ganancias detalle y mayorista</h6>
                        </div>
                    </div>
                    <ul className="table-top-head">
                        <li>
                            <OverlayTrigger placement="top" overlay={<Tooltip>Actualizar tabla</Tooltip>}>
                                <Link onClick={cargarDatos} style={{ cursor: "pointer" }}>
                                    <RotateCcw />
                                </Link>
                            </OverlayTrigger>
                        </li>
                    </ul>
                </div>

                {/* Filtros y Resumen */}
                <div className="card">
                    <div className="card-body">
                        <div className="row align-items-center">
                            {/* Selector de Año */}
                            <div className="col-lg-3 col-sm-6 col-12">
                                <div className="input-blocks">
                                    <label>Seleccionar Año</label>
                                    <div className="d-flex align-items-center">
                                        <div className="form-sort me-3">
                                            <Calendar className="info-img me-4" />
                                            <Select
                                                className="select"
                                                options={aniosOptions}
                                                value={aniosOptions.find(op => op.value === anioSeleccionado.value)}
                                                onChange={setAnioSeleccionado}
                                                placeholder="Año"
                                                isSearchable={false}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            {/* Badge de Total Anual */}
                            <div className="col-lg-9 d-flex justify-content-end">
                                <div className="bg-light border rounded p-3 d-flex align-items-center">
                                    <Activity className="text-success me-2" />
                                    <div>
                                        <h6 className="mb-0 text-muted">Total Anual (Online)</h6>
                                        <h4 className="mb-0 text-success fw-bold">
                                            {/* Cálculo dinámico del total anual en Frontend */}
                                            ${datosTabla.reduce((acc, curr) => acc + curr.totalDetalle + curr.totalMayorista, 0).toLocaleString('es-CL')}
                                        </h4>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Tabla */}
                <div className="card table-list-card">
                    <div className="card-body">
                        <div className="table-responsive">
                            <table className="table table-hover table-center mb-0">
                                <thead>
                                    <tr>
                                        <th>Mes</th>
                                        <th className="text-end">Total Detalle</th>
                                        <th className="text-end">Total Mayorista</th>
                                        <th className="text-end">Total General</th>
                                        <th className="text-center">Estado</th>
                                        <th className="text-center">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {loading ? (
                                        <tr>
                                            <td colSpan="6" className="text-center p-5">
                                                <div className="spinner-border text-primary" role="status">
                                                    <span className="visually-hidden">Cargando...</span>
                                                </div>
                                            </td>
                                        </tr>
                                    ) : (
                                        datosTabla.map((fila) => (
                                            <tr key={fila.mes}>
                                                <td className="fw-bold">{fila.nombreMes}</td>
                                                
                                                <td className="text-end">
                                                    {fila.estado === 'registrado' 
                                                        ? `$${fila.totalDetalle.toLocaleString('es-CL')}` 
                                                        : '-'}
                                                </td>
                                                
                                                <td className="text-end">
                                                    {fila.estado === 'registrado' 
                                                        ? `$${fila.totalMayorista.toLocaleString('es-CL')}` 
                                                        : '-'}
                                                </td>
                                                
                                                {/* Cálculo dinámico por fila */}
                                                <td className="text-end fw-bold text-success">
                                                    {fila.estado === 'registrado' 
                                                        ? `$${(fila.totalDetalle + fila.totalMayorista).toLocaleString('es-CL')}` 
                                                        : '-'}
                                                </td>
                                                
                                                <td className="text-center">
                                                    {fila.estado === 'registrado' ? (
                                                        <span className="badge bg-success">
                                                            <CheckCircle className="me-1" size={12}/> Registrado
                                                        </span>
                                                    ) : fila.esFuturo ? (
                                                        <span className="badge bg-light text-muted border">
                                                            Futuro
                                                        </span>
                                                    ) : (
                                                        <span className="badge bg-warning text-dark">
                                                            <AlertCircle className="me-1" size={12}/> Pendiente
                                                        </span>
                                                    )}
                                                </td>
                                                
                                                <td className="text-center">
                                                    <div className="action-table-data justify-content-center">
                                                        <div className="edit-delete-action">
                                                            {fila.estado === 'registrado' ? (
                                                                <>
                                                                    <td className="action-table-data">
                                                                        <OverlayTrigger placement="top" overlay={<Tooltip>Editar</Tooltip>}>
                                                                            <Link 
                                                                                to="#" 
                                                                                className="me-2 p-2"
                                                                                onClick={() => handleOpenModal(fila)}
                                                                            >
                                                                                <Edit className="feather-edit" />
                                                                            </Link>
                                                                        </OverlayTrigger>
                                                                        
                                                                        <OverlayTrigger placement="top" overlay={<Tooltip>Eliminar</Tooltip>}>
                                                                            <Link 
                                                                                to="#" 
                                                                                className="confirm-text p-2"
                                                                                onClick={() => handleDelete(fila.id)}
                                                                            >
                                                                                <Trash2 className="feather-trash-2 text-danger" />
                                                                            </Link>
                                                                        </OverlayTrigger>
                                                                    </td>
                                                                </>
                                                            ) : (
                                                                // Botón "Ingresar" solo si no es futuro
                                                                <td className="action-table-data">
                                                                <OverlayTrigger 
                                                                    placement="top" 
                                                                    overlay={<Tooltip>{fila.esFuturo ? "Mes no disponible" : "Ingresar ventas"}</Tooltip>}
                                                                >
                                                                    {fila.esFuturo ? (
                                                                        <span className="text-muted p-2">
                                                                            <XCircle className="feather-x-circle" style={{opacity: 0.3}} />
                                                                        </span>
                                                                    ) : (
                                                                        <Link 
                                                                            to="#" 
                                                                            className="text-success p-2"
                                                                            onClick={() => handleOpenModal(fila)}
                                                                        >
                                                                            <PlusCircle className="feather-plus-circle" />
                                                                        </Link>
                                                                    )}
                                                                </OverlayTrigger>
                                                                </td>
                                                            )}
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            {/* Modal de Creación / Edición */}
            <Modal show={showModal} onHide={handleCloseModal} centered>
                <Modal.Header closeButton>
                    <Modal.Title>
                        {modalMode === 'create' ? 'Ingresar Ventas' : 'Editar Ventas'}
                        <span className="text-muted ms-2 fs-6">
                            | {currentVenta.nombreMes} {currentVenta.anio}
                        </span>
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <div className="row">
                            <div className="col-12 mb-3">
                                {/* Agregado Ícono Tag */}
                                <Form.Label className="d-flex align-items-center">
                                    <Tag className="me-2" size={16} /> Total Venta Detalle
                                </Form.Label>
                                <div className="input-group">
                                    <span className="input-group-text"><DollarSign size={16} /></span>
                                    <Form.Control 
                                        type="number" 
                                        min="0"
                                        name="totalDetalle"
                                        value={currentVenta.totalDetalle}
                                        onChange={handleInputChange}
                                        onKeyDown={preventNegativeInput}
                                        placeholder="0"
                                    />
                                </div>
                            </div>
                            
                            <div className="col-12 mb-3">
                                {/* Agregado Ícono Layers */}
                                <Form.Label className="d-flex align-items-center">
                                    <Layers className="me-2" size={16} /> Total Venta Mayorista
                                </Form.Label>
                                <div className="input-group">
                                    <span className="input-group-text"><DollarSign size={16} /></span>
                                    <Form.Control 
                                        type="number" 
                                        min="0"
                                        name="totalMayorista"
                                        value={currentVenta.totalMayorista}
                                        onChange={handleInputChange}
                                        onKeyDown={preventNegativeInput}
                                        placeholder="0"
                                    />
                                </div>
                            </div>

                            {/* Cálculo en vivo del Total General dentro del Modal */}
                            <div className="col-12 mt-2">
                                <div className="alert alert-primary d-flex justify-content-between align-items-center mb-0">
                                    <strong className="d-flex align-items-center">
                                        <Activity className="me-2" size={18} /> Total General: 
                                    </strong> 
                                    <span className="fs-5 fw-bold">
                                        ${((Number(currentVenta.totalDetalle)||0) + (Number(currentVenta.totalMayorista)||0)).toLocaleString('es-CL')}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <button type="button" className="btn btn-cancel" onClick={handleCloseModal}>
                        Cancelar
                    </button>
                    <button type="button" className="btn btn-primary" onClick={handleSave}>
                        {modalMode === 'create' ? 'Guardar Ventas' : 'Actualizar Cambios'}
                    </button>
                </Modal.Footer>
            </Modal>
        </div>
    );
};

export default VentaOnlineList;