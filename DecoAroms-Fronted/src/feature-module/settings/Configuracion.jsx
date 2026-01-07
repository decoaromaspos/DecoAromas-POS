import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { TrendingUp, Printer, User, Save, Database, DownloadCloud, UploadCloud, PlayCircle } from 'feather-icons-react';
import configuracionService from '../../services/configuracionService';
import backupService from '../../services/BackupService';
import { all_routes } from "../../Router/all_routes";
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';
import { useAuth } from '../../context/AuthContext';
import Select from "react-select";

const mySwal = withReactContent(Swal);

const Configuracion = () => {
    const route = all_routes;
    const { usuario } = useAuth();
    const esSuperAdmin = usuario?.rol === 'SUPER_ADMIN';
    const esSuperAdminOrAdmin = usuario?.rol === 'SUPER_ADMIN' || usuario?.rol === 'ADMIN';
    const esUsuarioNoSuperAdmin = usuario?.rol !== 'SUPER_ADMIN';

    // Estados para los valores de los inputs
    const [metaMensual, setMetaMensual] = useState('');
    const [ipImpresora, setIpImpresora] = useState('');

    // Estados para saber si los datos fueron inicializados
    const [isMetaInicializada, setIsMetaInicializada] = useState(true);
    const [isIpInicializada, setIsIpInicializada] = useState(true);

    // Estados para manejar la carga y guardado
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);

    // ESTADOS PARA BACKUP/RESTAURACIÓN
    const [backupList, setBackupList] = useState([]); // Lista de nombres de archivos disponibles
    const [selectedBackup, setSelectedBackup] = useState(null); // Archivo seleccionado para restaurar
    const [isBackingUp, setIsBackingUp] = useState(false); // Estado para la creación de backup
    const [isRestoring, setIsRestoring] = useState(false); // Estado para la restauración

    // useEffect para cargar los datos iniciales y la lista de backups
    useEffect(() => {
        const cargarConfiguracionesYBackups = async () => {
            try {
                setIsLoading(true);

                // 1. CARGA INDEPENDIENTE DE CONFIGURACIONES BÁSICAS (IP y META)
                const [meta, ip] = await Promise.all([
                    configuracionService.getConfiguracion('META_MENSUAL'),
                    configuracionService.getConfiguracion('IP_IMPRESORA'),
                ]);

                // Lógica de inicialización de Meta e IP (MANTENIDA)
                if (meta === null || meta === '') {
                    setIsMetaInicializada(false);
                    setMetaMensual('0');
                } else {
                    setIsMetaInicializada(true);
                    setMetaMensual(meta);
                }

                if (ip === null || ip === '') {
                    setIsIpInicializada(false);
                    setIpImpresora('');
                } else {
                    setIsIpInicializada(true);
                    setIpImpresora(ip);
                }

                // 2. CARGA CONDICIONAL Y AISLADA DE BACKUPS
                if (esSuperAdmin) {
                    try {
                        const backups = await backupService.listBackups();
                        const options = backups.map(name => ({
                            value: name,
                            label: `${name.replace('decoaromas_', '').replace('.dump', '')} (Formato Custom)`
                        }));
                        setBackupList(options);
                    } catch (backupError) {
                        console.error('No se pudo cargar la lista de backups (posiblemente la carpeta no existe):', backupError);
                        mySwal.fire('Advertencia', 'No se pudo listar backups. Verifique la carpeta.', 'warning');
                        setBackupList([]); // Aseguramos que la lista esté vacía.
                    }
                }

            } catch (error) {
                mySwal.fire('Error', 'No se pudo cargar la configuración o la lista de backups.', 'error');
            } finally {
                setIsLoading(false);
            }
        };

        cargarConfiguracionesYBackups();
    }, [esSuperAdmin]);

    // Manejador para guardar la meta mensual
    const handleGuardarMeta = async () => {
        if (isNaN(metaMensual) || metaMensual < 0) {
            mySwal.fire('Atención', 'Por favor, introduce un valor numérico válido.', 'warning');
            return;
        }
        setIsSaving(true);
        try {
            await configuracionService.actualizarMetaMensual(metaMensual);
            mySwal.fire({
                title: '¡Éxito!',
                text: 'La meta mensual ha sido actualizada.',
                icon: 'success',
                timer: 2000,
                showConfirmButton: false
            });
            setIsMetaInicializada(true); // Marcamos como configurado después de guardar
        } catch (error) {
            mySwal.fire('Error', 'No se pudo actualizar la meta mensual.', 'error');
        } finally {
            setIsSaving(false);
        }
    };

    /**
     * Valida si un string tiene el formato de una IP v4.
     * @param {string} ip - El string de IP a validar.
     * @returns {boolean} - True si es un formato IPv4 válido, false de lo contrario.
     */
    const esIpV4Valida = (ip) => {
        // Regex para validar IPv4 (números de 0-255 en 4 bloques)
        const regexIpV4 = /^(?:(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        return regexIpV4.test(ip);
    };

    /**
     * Manejador para el input de IP.
     * Filtra caracteres no válidos (solo permite números y puntos).
     */
    const handleIpChange = (e) => {
        // Reemplaza cualquier caracter que NO sea un número (0-9) o un punto (.)
        const valorFiltrado = e.target.value.replace(/[^0-9.]/g, '');
        setIpImpresora(valorFiltrado);
    };

    // Manejador para guardar la IP de la impresora
    const handleGuardarIp = async () => {
        const ipTrimmed = ipImpresora.trim(); // Quitamos espacios al inicio/final

        if (!ipTrimmed) {
            mySwal.fire('Atención', 'La dirección IP no puede estar vacía.', 'warning');
            return;
        }

        // VALIDACIÓN: Verificar formato IPv4
        if (!esIpV4Valida(ipTrimmed)) {
            mySwal.fire(
                'Formato Incorrecto',
                'La dirección IP no tiene un formato IPv4 válido (Ej: 192.168.1.50).',
                'error'
            );
            return;
        }

        // Si pasa las validaciones, procedemos a guardar
        setIsSaving(true);
        try {
            await configuracionService.actualizarIpImpresora(ipTrimmed);
            mySwal.fire({
                title: '¡Éxito!',
                text: 'La IP de la impresora ha sido actualizada.',
                icon: 'success',
                timer: 2000,
                showConfirmButton: false
            });
            setIsIpInicializada(true);
        } catch (error) {
            mySwal.fire('Error', 'No se pudo actualizar la IP de la impresora.', 'error');
        } finally {
            setIsSaving(false);
        }
    };

    // Manejador para restaurar la base de datos
    const handleGenerarBackup = async () => {
        const confirmacion = await mySwal.fire({
            title: 'Confirmar Generación de Backup',
            text: '¿Deseas iniciar la creación de un nuevo archivo de respaldo de la base de datos?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Sí, generar backup',
            cancelButtonText: 'Cancelar',
        });

        if (!confirmacion.isConfirmed) {
            return;
        }

        setIsBackingUp(true);
        try {
            const result = await backupService.createBackup();
            mySwal.fire({
                title: '¡Éxito!',
                text: result, // Muestra el mensaje de éxito del backend
                icon: 'success',
                timer: 3000,
                showConfirmButton: false
            });
            // Recargar la lista de backups para mostrar el nuevo archivo
            const updatedBackups = await backupService.listBackups();
            const options = updatedBackups.map(name => ({
                value: name,
                label: `${name.replace('decoaromas_', '').replace('.dump', '')} (Formato Custom)`
            }));
            setBackupList(options);

        } catch (error) {
            const mensajeError = error.response?.data || error.message || 'Error desconocido al crear el backup.';
            mySwal.fire('Error', 'No se pudo generar el backup: ' + mensajeError, 'error');
        } finally {
            setIsBackingUp(false);
        }
    };

    // Manejador para restaurar la base de datos
    const handleRestaurarBaseDatos = async () => {
        if (!selectedBackup) {
            mySwal.fire('Atención', 'Por favor, selecciona un archivo de backup para restaurar.', 'warning');
            return;
        }

        const filename = selectedBackup.value;

        const confirmacion = await mySwal.fire({
            title: '¡PELIGRO! Confirmación de Restauración',
            html: `
                <p>Estás a punto de **ELIMINAR TODOS LOS DATOS** y reemplazarlos con el contenido del archivo:</p>
                <p><strong>${filename}</strong></p>
                <p class="text-danger">Esta acción es irreversible y podría causar la pérdida de datos recientes.</p>
                <p>Para proceder, escribe la palabra **RESTAURAR** a continuación:</p>
            `,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'RESTAURAR', // Texto modificado
            cancelButtonText: 'Cancelar',
            input: 'text',
            inputPlaceholder: 'Escribe "RESTAURAR"',
            inputValidator: (value) => {
                // Validación para la palabra clave RESTAURAR (seguridad)
                if (value !== 'RESTAURAR') {
                    return 'Debes escribir "RESTAURAR" para confirmar';
                }
            }
        });

        if (!confirmacion.isConfirmed) {
            return;
        }

        setIsRestoring(true);
        try {
            const result = await backupService.restoreBackup(filename);
            mySwal.fire({
                title: '¡Éxito!',
                text: result,
                icon: 'success',
                timer: 3000,
                showConfirmButton: false
            });
            // Limpiar la selección
            setSelectedBackup(null);

        } catch (error) {
            const mensajeError = error.response?.data?.error || error.response?.data || error.message || 'No se pudo restaurar la base de datos.';
            mySwal.fire('Error', 'Fallo en la restauración: ' + mensajeError, 'error');
        } finally {
            setIsRestoring(false);
        }
    };


    if (isLoading) {
        return <div className="page-wrapper"><div className="content"><p>Cargando configuración...</p></div></div>;
    }

    return (
        <div className="page-wrapper">
            <div className="content">
                <div className="page-header">
                    <div className="page-title">
                        <h4>Configuración General</h4>
                        <h6>Ajusta los parámetros de tu aplicación</h6>
                    </div>
                </div>

                <div className="row">
                    {/* Tarjeta para Meta de Ganancias */}
                    {esSuperAdminOrAdmin && (
                        <div className="col-lg-6 col-md-12 mb-4">
                            <div className="card h-100">
                                <div className="card-body">
                                    <div className="d-flex align-items-start mb-3">
                                        <TrendingUp className="me-3 text-primary" size={40} />
                                        <div>
                                            <h5 className="card-title">Meta de Ganancias Mensual</h5>
                                            <p className="card-text text-muted">Establece el objetivo de ganancias para el mes en curso.</p>
                                        </div>
                                    </div>
                                    <div className="form-group mb-3">
                                        <label>
                                            Monto de la meta ($)
                                            {!isMetaInicializada && <span className="badge bg-warning ms-2">Sin configurar</span>}
                                        </label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={metaMensual}
                                            onChange={(e) => setMetaMensual(e.target.value)}
                                            placeholder="Ej: 5000000"
                                            min="0"
                                        />
                                    </div>
                                    <button onClick={handleGuardarMeta} className="btn btn-primary mt-auto" disabled={isSaving}>
                                        <Save size={16} className="me-2" />
                                        {isSaving ? 'Guardando...' : 'Guardar Meta'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Tarjeta para IP de la Impresora */}
                    <div className="col-lg-6 col-md-12 mb-4">
                        <div className="card h-100">
                            <div className="card-body">
                                <div className="d-flex align-items-start mb-3">
                                    <Printer className="me-3 text-secondary" size={40} />
                                    <div>
                                        <h5 className="card-title">Impresora Térmica</h5>
                                        <p className="card-text text-muted">Configura la dirección IP de la impresora de tickets en tu red local.</p>
                                    </div>
                                </div>
                                <div className="form-group mb-3">
                                    <label>
                                        Dirección IP de la impresora
                                        {!isIpInicializada && <span className="badge bg-warning ms-2">Sin configurar</span>}
                                    </label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={ipImpresora}
                                        onChange={handleIpChange}
                                        placeholder="Ej: 192.168.1.50"
                                        maxLength={15}
                                    />
                                </div>
                                <button onClick={handleGuardarIp} className="btn btn-primary" disabled={isSaving}>
                                    <Save size={16} className="me-2" />
                                    {isSaving ? 'Guardando...' : 'Guardar IP'}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Generar Backup */}
                    {esSuperAdminOrAdmin && (
                        <div className="col-lg-6 col-md-12 mb-4">
                            <div className="card h-100 border-success">
                                <div className="card-body d-flex flex-column">
                                    <div className="d-flex align-items-start mb-3">
                                        <DownloadCloud className="me-3 text-success" size={40} />
                                        <div>
                                            <h5 className="card-title">Generar Backup Instantáneo</h5>
                                            <p className="card-text text-muted">
                                                Crea un archivo de respaldo de la base de datos actual en el servidor.
                                            </p>
                                            <p className="card-text text-muted mb-2">La base de datos quedará guardada en la siguiente ubicación del servidor:</p>
                                            <div className="alert alert-info p-2 mb-3" role="alert" style={{ fontSize: '0.95rem' }}>
                                                <div className="small mb-0">
                                                    <code>C:\Backups\decoaromas</code>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <button
                                        onClick={handleGenerarBackup}
                                        className="btn btn-success mt-auto"
                                        disabled={isBackingUp || isSaving || isRestoring}
                                    >
                                        <PlayCircle size={16} className="me-2" />
                                        {isBackingUp ? 'Creando Backup...' : 'Generar Backup Ahora'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Restaurar Base de Datos */}
                    {esSuperAdmin && (
                        <div className="col-lg-6 col-md-12 mb-4">
                            <div className="card h-100 border-warning">
                                <div className="card-body d-flex flex-column">
                                    <div className="d-flex align-items-start mb-3">
                                        <Database className="me-3 text-warning" size={40} />
                                        <div>
                                            <h5 className="card-title">Restaurar Base de Datos</h5>
                                            <p className="card-text text-muted">
                                                Restaura la base de datos desde un archivo **.dump** disponible en el servidor.
                                                <strong className="text-danger"> ¡Acción irreversible!</strong>
                                            </p>
                                        </div>
                                    </div>
                                    <div className="form-group mb-3 flex-grow-1">
                                        <label>Seleccionar Archivo de Respaldo (.dump)</label>
                                        {/* Componente Select para elegir el archivo */}
                                        <Select
                                            options={backupList}
                                            value={selectedBackup}
                                            onChange={setSelectedBackup}
                                            placeholder="Busca y selecciona un archivo .dump..."
                                            isClearable={true}
                                            isDisabled={isRestoring || isSaving}
                                        />
                                        <small className="form-text text-muted d-block mt-2">
                                            Archivos encontrados: {backupList.length}
                                        </small>
                                    </div>
                                    <button
                                        onClick={handleRestaurarBaseDatos}
                                        className="btn btn-warning mt-auto"
                                        disabled={isRestoring || isSaving || !selectedBackup}
                                    >
                                        <UploadCloud size={16} className="me-2" />
                                        {isRestoring ? 'Restaurando...' : 'Restaurar Base de Datos'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Tarjeta para Perfil de Usuario */}
                    {esUsuarioNoSuperAdmin && (
                        <div className="col-lg-6 col-md-12 mb-4">
                            <div className="card  h-100">
                                <div className="card-body">
                                    <div className="d-flex align-items-start mb-3">
                                        <User className="me-3 text-success" size={40} />
                                        <div>
                                            <h5 className="card-title">Mi Perfil</h5>
                                            <p className="card-text text-muted">Edita tu información personal, nombre de usuario y contraseña.</p>
                                        </div>
                                    </div>
                                    <Link to={route.profile} className="btn btn-outline-success">
                                        Editar mi Perfil
                                    </Link>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Configuracion;