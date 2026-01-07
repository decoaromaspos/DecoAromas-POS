import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { Link, useLocation } from 'react-router-dom';
import Select from 'react-select';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';
import Table from "../../core/pagination/datatable";
import axios from 'axios';
import {
    Trash2,
    UserPlus,
    PlusCircle,
    MinusCircle,
    RefreshCcw,
    Home,
    FileText,
    DollarSign,
    Columns
} from 'feather-icons-react';
import { Modal, ButtonGroup, Button, ListGroup } from 'react-bootstrap';
import { getCajaAbierta } from '../../services/cajaService';
import { getClientesActivos } from '../../services/clienteService';
import { getAromas } from '../../services/aromaService';
import { getFamilias } from '../../services/familiaService';
import { crearVenta, imprimirComprobanteVenta } from '../../services/ventaService';
import { getProductosByFiltrosPaginados, getProductoByCodigoBarras } from '../../services/productoService';
import { crearCotizacion } from '../../services/cotizacionService';
import OpenCashboxModal from './opencashboxmodal';
import CreateClientModal from "../../core/modals/peoples/CreateClientModal";
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import { useAuth } from "../../context/AuthContext";
import { all_routes } from "../../Router/all_routes";
import { PDFDownloadLink } from '@react-pdf/renderer';
import CotizacionComprobantePDF from './CotizacionComprobantePDF';
import CloseCashboxModal from './closecashboxmodal';
import { MEDIOS_PAGO } from '../../utils/medioPago';


const MySwal = withReactContent(Swal);

const TIPOS_DOCUMENTO = [
    { value: 'BOLETA', label: 'Boleta' },
    { value: 'FACTURA', label: 'Factura' }
];

const TIPOS_VENTA = [
    { value: 'DETALLE', label: 'Detalle (Precio Normal)' },
    { value: 'MAYORISTA', label: 'Mayorista (Precio Especial)' }
];



const Pos = () => {
    const { usuario, isAuthenticated, loading: authLoading } = useAuth();
    const route = all_routes;
    const barcodeInputRef = useRef(null);
    const location = useLocation(); // Hook para leer el state de navegación
    const isSuperAdmin = usuario && usuario.rol === 'SUPER_ADMIN';

    // --- ESTADOS CRÍTICOS ---
    const [isPageLoading, setIsPageLoading] = useState(false); // <-- CAMBIO: Iniciar en false
    const [isCajaVerificada, setIsCajaVerificada] = useState(false); // <-- AÑADIDO: Nuevo estado
    const [isCajaAbierta, setIsCajaAbierta] = useState(false);
    const [cajaInfo, setCajaInfo] = useState(null);
    const [showCajaModal, setShowCajaModal] = useState(false);

    // --- ESTADOS DE VENTA (Carrito) ---
    const [cart, setCart] = useState([]);
    const [selectedClient, setSelectedClient] = useState(null);
    const [tipoVenta, setTipoVenta] = useState('DETALLE');
    const [ventaSubtotal, setVentaSubtotal] = useState(0);
    const [valorDescuentoGlobal, setValorDescuentoGlobal] = useState(0);
    const [tipoDescuentoGlobal, setTipoDescuentoGlobal] = useState('PORCENTAJE');
    const [ventaTotal, setVentaTotal] = useState(0);
    const [cotizacionIdOrigen, setCotizacionIdOrigen] = useState(null); // NUEVO: Para guardar ID de cotización

    // --- ESTADOS DEL MODAL DE PAGO (MODIFICADO PARA PAGO MIXTO) ---
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [tipoDocumento, setTipoDocumento] = useState('BOLETA');

    // -- ESTADO DEL MODAL DE CIERRE DE CAJA --
    const [showCloseModal, setShowCloseModal] = useState(false);

    // --- Estados para gestionar múltiples pagos ---
    const [pagos, setPagos] = useState([]); // Almacena la lista de pagos [{ medioPago, monto }]
    const [totalPagado, setTotalPagado] = useState(0);
    const [montoFaltante, setMontoFaltante] = useState(0);
    const [vuelto, setVuelto] = useState(0);

    // Estados para el formulario de agregar pago
    const [currentPaymentMethod, setCurrentPaymentMethod] = useState(null);
    const [currentPaymentAmount, setCurrentPaymentAmount] = useState('');

    // --- ESTADOS DE PRODUCTOS / FILTROS ---
    const [productos, setProductos] = useState([]);
    const [clientes, setClientes] = useState([]);
    const [aromas, setAromas] = useState([]);
    const [familias, setFamilias] = useState([]);
    const [selectedAroma, setSelectedAroma] = useState(null);
    const [selectedFamilia, setSelectedFamilia] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [activeSearchFilter, setActiveSearchFilter] = useState('');
    const [barcodeQuery, setBarcodeQuery] = useState('');


    // --- ESTADOS DE PAGINACION
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 12;

    // --- ESTADOS PARA ABIR MODAL DE CLIENTE
    const [showCreateClientModal, setShowCreateClientModal] = useState(false);
    const CURRENT_USER_ID = usuario?.usuarioId;



    // --- FUNCION PARA OBTENER PRECIO CORRECTO SEGUN TIPO DE VENTA ---
    const getPriceForProduct = (product) => {
        const price = tipoVenta === 'MAYORISTA'
            ? product.precioMayorista
            : product.precioDetalle;

        return price ?? 0;
    };


    // --- LÓGICA DEL CARRITO ---
    const updateVentaTotales = useCallback(() => {
        // Calcular el subtotal a partir de los items del carrito con sus descuentos unitarios
        const subtotal = cart.reduce((acc, item) => {
            let descuentoUnitarioAplicado = 0;
            if (item.valorDescuentoUnitario > 0) {
                if (item.tipoDescuentoUnitario === 'PORCENTAJE') {
                    descuentoUnitarioAplicado = item.precioUnitario * (item.valorDescuentoUnitario / 100);
                } else { // VALOR
                    descuentoUnitarioAplicado = item.valorDescuentoUnitario;
                }
            }
            const precioFinalUnitario = item.precioUnitario - descuentoUnitarioAplicado;
            return acc + (precioFinalUnitario * item.cantidad);
        }, 0);

        setVentaSubtotal(subtotal);

        // Calcular el descuento global sobre el subtotal
        let descuentoGlobalAplicado = 0;
        if (valorDescuentoGlobal > 0) {
            if (tipoDescuentoGlobal === 'PORCENTAJE') {
                descuentoGlobalAplicado = subtotal * (valorDescuentoGlobal / 100);
            } else { // VALOR
                descuentoGlobalAplicado = valorDescuentoGlobal;
            }
        }

        // Calcular el total final
        const total = subtotal - descuentoGlobalAplicado;
        setVentaTotal(total > 0 ? total : 0); // Asegurarse de que el total no sea negativo
    }, [cart, valorDescuentoGlobal, tipoDescuentoGlobal]);


    const loadProductos = useCallback(async () => {
        if (!isCajaAbierta) return;
        setIsPageLoading(true); // Usamos el loading de página general
        try {
            const pageNumber = currentPage - 1;
            const res = await getProductosByFiltrosPaginados(
                pageNumber, pageSize, 'nombre',
                selectedAroma?.value || null,
                selectedFamilia?.value || null,
                true,
                activeSearchFilter.length > 2 ? activeSearchFilter : null,
                null, null
            );
            setProductos(res.content || []);
            setTotalPages(res.totalPages || 1);
            setTotalElements(res.totalElements || 0);
        } catch (error) {
            console.error("Error al cargar productos:", error);
            // No mostramos alerta aquí para no ser intrusivos al filtrar/paginar
        } finally {
            setIsPageLoading(false);
        }
    }, [isCajaAbierta, currentPage, pageSize, selectedAroma, selectedFamilia, activeSearchFilter]);


    // --- EFECTO PARA PRECARGAR COTIZACIÓN ---
    useEffect(() => {
        // Solo ejecutar si la caja está abierta, los clientes cargados y hay un estado de cotización
        if (!location.state?.cotizacion || !isCajaAbierta || clientes.length === 0) {
            return;
        }

        const cotizacion = location.state.cotizacion;

        MySwal.fire({
            title: `Cargando Cotización #${cotizacion.cotizacionId}`,
            text: 'Por favor, espere mientras se cargan los datos...',
            icon: 'info',
            allowOutsideClick: false,
            didOpen: () => {
                MySwal.showLoading();
            }
        });

        // 1. Setear ID de origen
        setCotizacionIdOrigen(cotizacion.cotizacionId);

        // 2. Setear datos generales
        setTipoVenta(cotizacion.tipoCliente);
        setValorDescuentoGlobal(cotizacion.valorDescuentoGlobal);
        setTipoDescuentoGlobal(cotizacion.tipoDescuentoGlobal);

        // 3. Setear Cliente (si existe)
        if (cotizacion.clienteId) {
            const clienteEncontrado = clientes.find(c => c.clienteId === cotizacion.clienteId);
            if (clienteEncontrado) {
                // Mapeamos el cliente al formato que espera react-select
                const fullName = `${clienteEncontrado.nombre} ${clienteEncontrado.apellido || ''}`.trim();
                setSelectedClient({
                    value: clienteEncontrado.clienteId,
                    clienteId: clienteEncontrado.clienteId,
                    label: `${fullName} (${clienteEncontrado.rut})`,
                    tipo: clienteEncontrado.tipo
                });
            }
        }

        // 4. Cargar productos en el carrito (requiere validar stock actual)
        const cargarDetalles = async () => {
            const nuevoCarrito = [];
            let stockInsuficiente = false;

            for (const detalle of cotizacion.detalles) {
                try {
                    // Buscamos el producto para obtener stock actualizado y precios base
                    const productoDB = await getProductoByCodigoBarras(detalle.codigoBarras);

                    if (!productoDB) {
                        stockInsuficiente = true;
                        MySwal.fire('Producto no encontrado', `El producto "${detalle.productoNombre}" (SKU: ${detalle.codigoBarras}) no se encontró y no será agregado.`, 'warning');
                        continue; // Saltar este producto
                    }

                    const cantidadACargar = detalle.cantidad;

                    if (productoDB.stock < cantidadACargar) {
                        stockInsuficiente = true;
                        MySwal.fire('Stock Insuficiente', `El stock de "${productoDB.nombre}" es ${productoDB.stock}, pero la cotización pedía ${cantidadACargar}. Se ajustará al stock disponible.`, 'warning');
                    }

                    const cantidadFinal = Math.min(productoDB.stock, cantidadACargar);

                    if (cantidadFinal > 0) {
                        nuevoCarrito.push({
                            productoId: productoDB.productoId,
                            nombre: productoDB.nombre,
                            sku: productoDB.sku,
                            cantidad: cantidadFinal, // Cantidad ajustada al stock
                            precioUnitario: detalle.precioUnitario, // Usamos el precio guardado en la cotización
                            precioBaseDetalle: productoDB.precioDetalle, // Guardamos los precios base actuales
                            precioBaseMayorista: productoDB.precioMayorista,
                            stock: productoDB.stock, // Stock actual
                            valorDescuentoUnitario: detalle.valorDescuentoUnitario, // Descuento guardado
                            tipoDescuentoUnitario: detalle.tipoDescuentoUnitario, // Tipo de descuento guardado
                        });
                    }

                } catch (error) {
                    console.error("Error cargando detalle de cotización:", error);
                    stockInsuficiente = true;
                    MySwal.fire('Error', `Hubo un problema al cargar el producto "${detalle.productoNombre}".`, 'error');
                }
            }

            setCart(nuevoCarrito);
            // MySwal.close(); // <-- ELIMINADO: No cerramos, actualizamos.

            if (stockInsuficiente) {
                MySwal.fire({
                    title: 'Cotización Cargada con Ajustes',
                    text: 'Se cargaron los productos de la cotización. Revisa el carrito, algunos items pueden tener cantidades ajustadas por falta de stock.',
                    icon: 'warning',
                    showConfirmButton: true // <-- AÑADIDO: Esto oculta el spinner
                });
            } else {
                MySwal.fire({
                    title: 'Cotización Cargada',
                    text: `Se cargaron ${nuevoCarrito.length} productos de la Cotización #${cotizacion.cotizacionId}.`,
                    icon: 'success',
                    showConfirmButton: true // <-- AÑADIDO: Esto oculta el spinner
                });
            }

            // Limpiamos el state de location para que no se recargue al refrescar
            window.history.replaceState({}, document.title);
        };

        cargarDetalles();

        // Dependemos de location.state (para el primer trigger), isCajaAbierta y clientes (para que los datos estén listos)
    }, [location.state, isCajaAbierta, clientes]);


    useEffect(() => {
        if (authLoading) return;
        if (!isAuthenticated) { setIsPageLoading(false); return; }

        const loadInitialData = async () => {
            // setIsPageLoading(true); // <-- ELIMINADO: loadProductos manejará su propio spinner
            try {
                const cajaData = await getCajaAbierta();
                const [aromasData, familiasData, clientesData] = await Promise.all([
                    getAromas(), getFamilias(), getClientesActivos()
                ]);

                setAromas(aromasData || []);
                setFamilias(familiasData || []);
                setClientes(clientesData || []);

                if (cajaData && cajaData.cajaId) {
                    setIsCajaAbierta(true);
                    setCajaInfo(cajaData);
                } else {
                    setIsCajaAbierta(false);
                }
            } catch (error) {
                if (error.response && error.response.status === 404) {
                    setIsCajaAbierta(false);
                } else if (!axios.isCancel(error)) {
                    console.error("Error al cargar datos iniciales:", error);
                    MySwal.fire('Error', 'No se pudieron cargar los datos iniciales.', 'error');
                }
            } finally {
                // ELIMINADO: El 'finally' que seteaba isPageLoading(false) condicionalmente
                // if (!location.state?.cotizacion) {
                //     setIsPageLoading(false);
                // }
                setIsCajaVerificada(true); // <-- AÑADIDO: Indicar que la verificación inicial terminó
            }
        };

        loadInitialData();
    }, [isAuthenticated, authLoading, location.state]); // location.state aquí para que sepa si debe manejar el loading



    useEffect(() => {
        // CORRECCIÓN: Siempre cargar la lista de productos,
        loadProductos();
    }, [loadProductos]); // Dependencia 'location.state' eliminada



    useEffect(() => {
        updateVentaTotales();
    }, [updateVentaTotales]);


    // Actualiza precios del carrito al cambiar tipo de venta
    useEffect(() => {
        // Este efecto se ejecutará cada vez que el estado 'tipoVenta' cambie.
        // Actualizamos el carrito usando la forma funcional de 'setCart'.
        setCart(currentCart => {
            // Si el carrito está vacío, .map() devuelve un array vacío, lo cual es seguro.
            // No necesitamos el 'if (cart.length > 0)' aquí afuera.
            return currentCart.map(item => {
                // Determina el nuevo precio basado en el tipo de venta seleccionado
                const newPrice = tipoVenta === 'MAYORISTA'
                    ? item.precioBaseMayorista
                    : item.precioBaseDetalle;

                // Retorna el item actualizado con el nuevo precio unitario
                return { ...item, precioUnitario: newPrice };
            });
        });
        // Este efecto se ejecutará cada vez que el estado 'tipoVenta' cambie.
    }, [tipoVenta]);



    // --- LÓGICA DE CÁLCULO DE PAGOS MEJORADA ---
    // Este efecto ahora calcula correctamente el total entregado por el cliente, lo que falta y el vuelto.
    useEffect(() => {
        // Suma de todo el dinero que el cliente ha entregado (puede ser mayor al total de la venta)
        const totalEntregado = pagos.reduce((acc, pago) => acc + pago.monto, 0);
        setTotalPagado(totalEntregado); // `totalPagado` ahora es "Total Recibido"

        // Calculamos el saldo. Si es negativo, es vuelto. Si es positivo, es lo que falta.
        const saldo = ventaTotal - totalEntregado;

        if (saldo <= 0) {
            // El cliente ha pagado el total o ha pagado de más
            setMontoFaltante(0);
            setVuelto(Math.abs(saldo)); // El vuelto es el valor absoluto del saldo negativo
        } else {
            // El cliente aún debe dinero
            setMontoFaltante(saldo);
            setVuelto(0); // No hay vuelto si aún falta pagar
        }

    }, [pagos, ventaTotal]);

    // --- EFECTO PARA AJUSTAR MONTO MÁXIMO DE PAGOS ELECTRÓNICOS ---
    useEffect(() => {
        // Si se selecciona un método de pago y NO es efectivo...
        if (currentPaymentMethod && currentPaymentMethod.value !== 'EFECTIVO') {
            const montoActual = parseInt(currentPaymentAmount, 10);
            const montoFaltanteRedondeado = Math.ceil(montoFaltante);

            // ...y si el monto ingresado supera lo que falta por pagar...
            if (!isNaN(montoActual) && montoActual > montoFaltanteRedondeado) {

                // ...lo ajustamos automáticamente al máximo permitido (lo que falta).
                setCurrentPaymentAmount(montoFaltanteRedondeado.toString());

                // Opcional: Notificación sutil de que el monto fue ajustado.
                MySwal.fire({
                    toast: true,
                    position: 'top-end',
                    icon: 'info',
                    title: 'Monto ajustado al faltante',
                    showConfirmButton: false,
                    timer: 2500, // La notificación desaparece sola
                });
            }
        }
    }, [currentPaymentAmount, currentPaymentMethod, montoFaltante]);


    // --- LÓGICA DE CARGA DE DATOS ESTÁTICOS Y PRODUCTOS ---
    const loadInitialData = useCallback(async () => {
        try {
            // Cargar datos estáticos
            const [aromasData, familiasData, clientesData] = await Promise.all([
                getAromas(),
                getFamilias(),
                getClientesActivos(),
            ]);

            setAromas(aromasData || []);
            setFamilias(familiasData || []);
            setClientes(clientesData || []);

        } catch (error) {
            console.error("Error al cargar datos iniciales:", error);
            MySwal.fire('Error', 'No se pudieron cargar los datos de filtros y clientes.', 'error');
        }
    }, []);


    // --- Handler para el input de monto que solo acepta enteros ---
    const handleAmountChange = (e) => {
        // Elimina cualquier caracter que no sea un dígito (puntos, comas, etc.)
        const value = e.target.value.replace(/\D/g, '');
        setCurrentPaymentAmount(value);
    };

    // --- Función para abrir el modal de pago y resetear estados ---
    const handleOpenPaymentModal = () => {
        if (cart.length === 0) {
            MySwal.fire('Advertencia', 'El carrito está vacío.', 'warning');
            return;
        }
        // Al abrir, el monto a pagar por defecto es el total que falta (redondeado a entero)
        setCurrentPaymentAmount(Math.ceil(montoFaltante > 0 ? montoFaltante : ventaTotal).toString());
        setShowPaymentModal(true);
    };

    // --- FUNCIÓN DE BÚSQUEDA POR NOMBRE (On Submit) ---
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        setActiveSearchFilter(searchQuery);
        setCurrentPage(1); // Siempre volver a la primera página al aplicar un filtro
    };


    // --- FUNCIÓN PARA CAMBIAR PÁGINA ---
    const handlePageChange = (page) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    };

    const handleAddToCart = (product) => {
        // VALIDACIÓN DE STOCK ANTES DE AGREGAR
        if (product.stock <= 0) {
            MySwal.fire('Sin Stock', `El producto "${product.nombre}" tiene stock de ${product.stock} y no puede ser agregado.`, 'warning');
            return;
        }

        const existingItem = cart.find(item => item.productoId === product.productoId);

        // Obtener el precio según el tipo de venta
        const price = getPriceForProduct(product);

        let newQuantity = 1;
        if (existingItem) {
            newQuantity = existingItem.cantidad + 1;
        }

        // VALIDACIÓN DE STOCK AL AGREGAR
        if (newQuantity > product.stock) {
            MySwal.fire('Stock Insuficiente', `Solo quedan ${product.stock} unidades de este producto.`, 'warning');
            return;
        }

        if (existingItem) {
            setCart(
                cart.map(item =>
                    item.productoId === product.productoId
                        ? { ...item, cantidad: item.cantidad + 1, precioUnitario: price }
                        : item
                )
            );
        } else {
            setCart([
                ...cart,
                {
                    productoId: product.productoId,
                    nombre: product.nombre,
                    sku: product.sku,
                    cantidad: 1,
                    precioUnitario: price,
                    precioBaseDetalle: product.precioDetalle,
                    precioBaseMayorista: product.precioMayorista,
                    stock: product.stock,
                    valorDescuentoUnitario: 0,
                    tipoDescuentoUnitario: 'PORCENTAJE',
                },
            ]);
        }
    };


    // --- FUNCIÓN PARA ACTUALIZAR CANTIDAD (+ / -) ---
    const handleUpdateQuantity = (item, delta) => {
        const newQuantity = item.cantidad + delta;

        // VALIDACIÓN: No permitir cantidad menor a 1
        if (newQuantity < 1) {
            handleRemoveFromCart(item.productoId);
            return;
        }

        // VALIDACIÓN: No permitir cantidad mayor al stock
        if (newQuantity > item.stock) {
            MySwal.fire('Stock Insuficiente', `Se alcanzó el stock máximo para ${item.nombre}.`, 'warning');
            return;
        }

        setCart(
            cart.map(i =>
                i.productoId === item.productoId
                    ? { ...i, cantidad: newQuantity }
                    : i
            )
        );
    };

    // --- FUNCIÓN PARA MANEJO MANUAL DE CANTIDAD ---
    const handleManualQuantityChange = (e, item) => {
        const inputValue = e.target.value;

        // Si el usuario borra el input, no hacemos nada momentáneamente
        // para permitirle escribir, o podríamos forzar a 1.
        if (inputValue === '') return;

        let newQuantity = parseInt(inputValue, 10);

        // Si no es un número válido, ignoramos
        if (isNaN(newQuantity)) return;

        // Validación 1: No permitir negativos ni cero
        if (newQuantity < 1) {
            MySwal.fire({
                toast: true,
                position: 'top-end',
                icon: 'warning',
                title: 'Cantidad inválida',
                text: 'La cantidad mínima es 1.',
                showConfirmButton: false,
                timer: 2000
            });
            // Forzamos a 1 para corregir
            newQuantity = 1;
        }

        // Validación 2: Stock Máximo
        if (newQuantity > item.stock) {
            MySwal.fire({
                icon: 'warning',
                title: 'Stock Insuficiente',
                text: `Solo existen ${item.stock} unidades disponibles de "${item.nombre}".`,
                confirmButtonColor: '#d33',
                confirmButtonText: 'Entendido'
            });
            // Ajustamos al máximo stock posible
            newQuantity = item.stock;
        }

        // Actualizamos el carrito con la cantidad validada
        setCart(
            cart.map(i =>
                i.productoId === item.productoId
                    ? { ...i, cantidad: newQuantity }
                    : i
            )
        );
    };

    // Función unificada para actualizar cualquier campo de un item del carrito
    const handleUpdateCartItem = (productoId, field, value) => {
        setCart(cart.map(item => {
            if (item.productoId === productoId) {
                const updatedItem = { ...item, [field]: value };

                // --- VALIDACIONES ---
                if (field === 'valorDescuentoUnitario') {
                    const numericValue = parseFloat(value) || 0;
                    if (numericValue < 0) {
                        return { ...item, valorDescuentoUnitario: 0 }; // No permitir negativos
                    }
                    if (updatedItem.tipoDescuentoUnitario === 'PORCENTAJE' && numericValue > 100) {
                        MySwal.fire('Descuento Inválido', 'El porcentaje no puede ser mayor a 100.', 'warning');
                        return { ...item, valorDescuentoUnitario: 100 }; // Limitar a 100
                    }
                    if (updatedItem.tipoDescuentoUnitario === 'VALOR' && numericValue > item.precioUnitario) {
                        MySwal.fire('Descuento Inválido', `El descuento no puede superar el precio de ${formatCurrency(item.precioUnitario)}.`, 'warning');
                        return { ...item, valorDescuentoUnitario: item.precioUnitario }; // Limitar al precio
                    }
                    return { ...item, valorDescuentoUnitario: numericValue };
                }
                // Al cambiar de tipo, reseteamos el valor a 0 para evitar confusiones
                if (field === 'tipoDescuentoUnitario') {
                    return { ...item, tipoDescuentoUnitario: value, valorDescuentoUnitario: 0 };
                }
                return updatedItem;
            }
            return item;
        }));
    };

    // Handler para el descuento global con validación
    const handleDescuentoGlobalChange = (value) => {
        const numericValue = parseFloat(value) || 0;

        if (numericValue < 0) {
            setValorDescuentoGlobal(0);
            return;
        }
        if (tipoDescuentoGlobal === 'PORCENTAJE' && numericValue > 100) {
            MySwal.fire('Descuento Inválido', 'El porcentaje no puede ser mayor a 100.', 'warning');
            setValorDescuentoGlobal(100);
            return;
        }
        if (tipoDescuentoGlobal === 'VALOR' && numericValue > ventaSubtotal) {
            MySwal.fire('Descuento Inválido', `El descuento no puede superar el subtotal de ${formatCurrency(ventaSubtotal)}.`, 'warning');
            setValorDescuentoGlobal(ventaSubtotal);
            return;
        }
        setValorDescuentoGlobal(numericValue);
    };


    const handleRemoveFromCart = (productoId) => {
        setCart(cart.filter(item => item.productoId !== productoId));
    };


    const handleClearCart = () => {
        setCart([]);
        setValorDescuentoGlobal(0);
        setTipoDescuentoGlobal('PORCENTAJE');
        setSelectedClient(null);
        setCotizacionIdOrigen(null); // Limpiar ID de cotización
    };


    // --- LÓGICA DE ESCANEO DE CÓDIGO DE BARRAS ---
    const handleBarcodeSubmit = async (e) => {
        e.preventDefault();
        if (!barcodeQuery.trim()) return;

        try {
            const product = await getProductoByCodigoBarras(barcodeQuery.trim());

            if (product) {
                handleAddToCart(product);
                setBarcodeQuery('');
            } else {
                MySwal.fire('Producto No Encontrado', 'No se encontró ningún producto con ese código de barras.', 'warning');
            }
        } catch (error) {
            console.error("Error al buscar por código de barras:", error);
            MySwal.fire('Error de Búsqueda', 'Ocurrió un error al buscar el producto.', 'error');
        } finally {
            if (barcodeInputRef.current) {
                barcodeInputRef.current.focus();
            }
        }
    };


    // --- LÓGICA DE CERRAR CAJA (Modal para Cajero) ---
    const handleCerrarCaja = () => {
        setShowCloseModal(true);
    };

    // --- NUEVA FUNCIÓN PARA CERRAR EL MODAL DE PAGO Y LIMPIAR ESTADOS ---
    const handleClosePaymentModal = () => {
        setShowPaymentModal(false);
        setPagos([]);
        setCurrentPaymentMethod(null);
        setCurrentPaymentAmount('');
        setTipoDocumento('BOLETA');
        setVuelto(0);
    };

    // --- Lógica para añadir un pago a la lista ---
    const handleAddPago = () => {
        const montoIngresado = parseInt(currentPaymentAmount, 10);
        // Usamos Math.ceil para redondear SIEMPRE hacia arriba
        const montoFaltanteRedondeado = Math.ceil(montoFaltante);

        if (!currentPaymentMethod || !currentPaymentAmount || isNaN(montoIngresado) || montoIngresado <= 0) {
            MySwal.fire('Error', 'Debe seleccionar un medio de pago y un monto válido.', 'error');
            return;
        }

        // Si el medio de pago NO es efectivo y el monto ingresado es mayor a lo que falta...
        if (currentPaymentMethod.value !== 'EFECTIVO' && montoIngresado > montoFaltanteRedondeado) {
            MySwal.fire({
                icon: 'error',
                title: 'Monto Excedido',
                text: `El pago con ${currentPaymentMethod.label} no puede ser mayor al monto faltante redondeado de ${formatCurrency(montoFaltanteRedondeado)}.`,
            });
            // Ajustamos el monto en el input para sugerir el valor correcto al usuario.
            setCurrentPaymentAmount(montoFaltanteRedondeado.toString());
            return;
        }

        // Validación para no duplicar medios de pago 
        if (pagos.some(p => p.medioPago === currentPaymentMethod.value)) {
            MySwal.fire('Medio de Pago Duplicado', 'Ya has agregado un pago con este método. Elimina el pago anterior para modificarlo.', 'warning');
            return;
        }

        setPagos(prevPagos => [...prevPagos, {
            medioPago: currentPaymentMethod.value,
            monto: montoIngresado,
            label: currentPaymentMethod.label,
        }]);

        // Limpiamos el formulario para el siguiente pago.
        setCurrentPaymentMethod(null);

        // Actualizamos el campo de monto con lo que sigue faltando, si aplica.
        const nuevoMontoFaltante = montoFaltante - montoIngresado;
        setCurrentPaymentAmount(nuevoMontoFaltante > 0 ? Math.ceil(nuevoMontoFaltante).toString() : '');
    };

    // --- Lógica para remover un pago de la lista ---
    const handleRemovePago = (indexToRemove) => {
        setPagos(pagos.filter((_, index) => index !== indexToRemove));
        // El useEffect se encargará de recalcular el vuelto y el faltante automáticamente.
    };

    // Opciones de pago que se filtran dinámicamente
    const availablePaymentMethods = useMemo(() => {
        const usedMethods = pagos.map(p => p.medioPago);
        return MEDIOS_PAGO.filter(option => !usedMethods.includes(option.value));
    }, [pagos]);

    // Verifica si alguno de los pagos registrados es 'EFECTIVO'
    const hayPagoEfectivo = useMemo(() => {
        return pagos.some(p => p.medioPago === 'EFECTIVO');
    }, [pagos]);


    // --- LÓGICA DE CREAR VENTA (Payment) ---
    const handleCrearVenta = async () => {
        // La validación principal ahora es que el monto faltante sea CERO.
        if (montoFaltante > 0) {
            MySwal.fire('Pago Incompleto', `Aún falta por pagar ${formatCurrency(montoFaltante)}.`, 'warning');
            return;
        }

        // --- NOTA IMPORTANTE ---
        // Tu backend AHORA debe ser capaz de recibir un array de `pagos`
        // donde la suma de `monto` puede ser SUPERIOR al `totalNeto` de la venta.
        const ventaData = {
            tipoCliente: tipoVenta,
            valorDescuentoGlobal: valorDescuentoGlobal,
            tipoDescuentoGlobal: tipoDescuentoGlobal,
            usuarioId: CURRENT_USER_ID,
            clienteId: selectedClient ? selectedClient.clienteId : null,
            tipoDocumento: tipoDocumento,
            cotizacionId: cotizacionIdOrigen, // <-- ID DE LA COTIZACIÓN DE ORIGEN
            detalles: cart.map(item => ({
                productoId: item.productoId,
                cantidad: item.cantidad,
                valorDescuentoUnitario: item.valorDescuentoUnitario || 0,
                tipoDescuentoUnitario: item.tipoDescuentoUnitario
            })),
            // Se envía el array de pagos tal cual fue ingresado.
            pagos: pagos.map(({ medioPago, monto }) => ({ medioPago, monto }))
        };

        try {
            const saleResult = await crearVenta(ventaData);

            MySwal.fire({
                title: '¡Venta Completada!',
                text: `Total: ${formatCurrency(saleResult.totalNeto)}. ¿Desea imprimir el comprobante?`,
                icon: 'success',
                showCancelButton: true,
                confirmButtonText: 'Imprimir Comprobante',
                cancelButtonText: 'Siguiente Venta',
            }).then(async (result) => {
                if (result.isConfirmed) {
                    try {
                        await imprimirComprobanteVenta(saleResult.ventaId);
                        MySwal.fire('Impreso', 'El comprobante se ha enviado a la impresora.', 'success');
                    } catch (err) {
                        MySwal.fire('Error', 'No se pudo imprimir el comprobante.', 'error');
                    }
                }
            });

            // Limpiar todo para la siguiente venta
            handleClearCart();
            setVentaSubtotal(0);
            setVentaTotal(0);
            handleClosePaymentModal();
            setCotizacionIdOrigen(null); // Limpiar ID de cotización

        } catch (error) {
            let errorMessage = 'Error al procesar la venta.';
            if (error.response && error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message;
            }
            MySwal.fire('Error', errorMessage, 'error');
        }
    };


    // --- LÓGICA DE CREAR COTIZACIÓN (NUEVA FUNCIÓN) ---
    const handleCrearCotizacion = async () => {
        // 1. Validar carrito
        if (cart.length === 0) {
            MySwal.fire('Carrito Vacío', 'No se puede crear una cotización sin productos.', 'warning');
            return;
        }

        // 2. Construir cotizacionData (similar a ventaData pero sin pagos/documento)
        const cotizacionData = {
            tipoCliente: tipoVenta,
            valorDescuentoGlobal: valorDescuentoGlobal,
            tipoDescuentoGlobal: tipoDescuentoGlobal,
            usuarioId: CURRENT_USER_ID,
            clienteId: selectedClient ? selectedClient.clienteId : null,
            detalles: cart.map(item => ({
                productoId: item.productoId,
                cantidad: item.cantidad,
                valorDescuentoUnitario: item.valorDescuentoUnitario || 0,
                tipoDescuentoUnitario: item.tipoDescuentoUnitario
            }))
        };

        // 3. Mostrar Swal de carga
        MySwal.fire({
            title: 'Guardando Cotización...',
            text: 'Por favor, espere.',
            allowOutsideClick: false,
            didOpen: () => {
                MySwal.showLoading();
            }
        });

        try {
            // 4. Llamar al servicio
            // (Asegúrate que tu servicio 'crearCotizacion' devuelva la cotización creada)
            const cotizacionGuardada = await crearCotizacion(cotizacionData);

            // 5. Mostrar Swal de éxito con PDF
            MySwal.fire({
                title: '¡Cotización Guardada!',
                icon: 'success',
                html: (
                    <div>
                        <p>La cotización #{cotizacionGuardada.cotizacionId} se ha creado exitosamente.</p>
                        {/* Usamos el CotizacionComprobantePDF (el que recibe el objeto completo)
                            y el formateador de fecha que ya teníamos en CotizacionesList.jsx
                        */}
                        <PDFDownloadLink
                            document={<CotizacionComprobantePDF cotizacion={cotizacionGuardada} />}
                            fileName={`Cotizacion_${formatearFechaParaNombreArchivo(cotizacionGuardada.fechaEmision)}_${(cotizacionGuardada.clienteNombre || 'NA').replace(/[^a-zA-Z0-9 ]/g, '').replace(/ /g, '_')}.pdf`}
                            className="btn btn-success mt-2" // <-- CAMBIO: de btn-primary a btn-success
                        >
                            {({ loading }) => (
                                loading ? 'Preparando PDF...' : (
                                    // <-- CAMBIO: Añadido Fragment e Icono
                                    <>
                                        <FileText size={16} className="me-1" />
                                        Descargar PDF
                                    </>
                                )
                            )}
                        </PDFDownloadLink>
                    </div>
                )
            });

        } catch (error) {
            let errorMessage = 'Error al guardar la cotización.';
            if (error.response && error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message;
            }
            MySwal.fire('Error', errorMessage, 'error');
        }
    };


    // Función de formato de moneda
    const formatCurrency = (amount) => {
        if (amount === null || amount === undefined) return "$0";

        const numericAmount = Number(amount);

        // Si la parte decimal es .0, no mostramos decimales.
        if (numericAmount % 1 === 0) {
            return `$${numericAmount.toLocaleString('es-CL', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 0,
            })}`;
        }

        // Si tiene decimales relevantes, mostramos dos decimales.
        return `$${numericAmount.toLocaleString('es-CL', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        })}`;
    };

    // --- NUEVA FUNCIÓN (copiada de CotizacionesList.jsx) ---
    // Función para formatear la fecha para nombres de archivo (YYYY-MM-DD_HH-mm)
    const formatearFechaParaNombreArchivo = (fechaISO) => {
        if (!fechaISO) return 'fecha_desconocida';
        try {
            const fecha = new Date(fechaISO);

            // Obtenemos las partes de la fecha/hora
            const year = fecha.getFullYear();
            const month = String(fecha.getMonth() + 1).padStart(2, '0');
            const day = String(fecha.getDate()).padStart(2, '0');
            const hours = String(fecha.getHours()).padStart(2, '0');
            const minutes = String(fecha.getMinutes()).padStart(2, '0');

            // Formato: AAAA-MM-DD_HH-mm (Usamos guion en vez de dos puntos para compatibilidad de archivos)
            return `${year}-${month}-${day}_${hours}-${minutes}`;
        } catch (e) {
            console.error("Error formateando fecha para archivo:", e);
            return 'fecha_invalida';
        }
    };


    const calcularSubtotalItem = (item) => {
        let precioFinalUnitario = item.precioUnitario;

        if (item.valorDescuentoUnitario > 0) {
            if (item.tipoDescuentoUnitario === 'PORCENTAJE') {
                const montoDescuento = item.precioUnitario * (item.valorDescuentoUnitario / 100);
                precioFinalUnitario -= montoDescuento;
            } else { // 'VALOR'
                precioFinalUnitario -= item.valorDescuentoUnitario;
            }
        }

        return precioFinalUnitario * item.cantidad;
    };



    const columns = [
        {
            title: 'SKU',
            dataIndex: 'sku',
            sorter: (a, b) => a.sku.localeCompare(b.sku),
            width: '5%',
        },
        {
            title: 'Nombre',
            dataIndex: 'nombre',
            sorter: (a, b) => a.nombre.localeCompare(b.nombre),
            width: '25%',
        },
        {
            title: "Stock",
            dataIndex: "stock",
            sorter: (a, b) => a.stock - b.stock,
            key: "stock",
            render: (stock) => {
                let className = "text-center"; // Centramos por defecto
                if (stock <= 0) {
                    className += " text-danger fw-bold";
                } else if (stock >= 1 && stock <= 10) {
                    className += " text-warning fw-bold";
                }

                return <span className={className}>{stock}</span>;
            },
            width: '5%',
        },
        {
            title: "Precio Detalle",
            dataIndex: "precioDetalle",
            render: (text) => formatCurrency(text ?? 0),
            sorter: (a, b) => a.precioDetalle - b.precioDetalle,
            align: 'right',
            width: '10%',
        },
        {
            title: "Precio Mayorista",
            dataIndex: "precioMayorista",
            render: (text) => formatCurrency(text ?? 0),
            sorter: (a, b) => a.precioMayorista - b.precioMayorista,
            align: 'right',
            width: '10%',
        },
        {
            title: "Aroma",
            dataIndex: "aromaNombre",
            sorter: (a, b) => (a.aromaNombre ?? '').localeCompare(b.aromaNombre ?? ''),
        },
        {
            title: "Familia",
            dataIndex: "familiaNombre",
            sorter: (a, b) => (a.familiaNombre ?? '').localeCompare(b.familiaNombre ?? ''),
        },
        {
            title: "Acciones",
            key: "acciones",
            width: 80, // ancho fijo
            align: 'center',
            render: (text, record) => ( // 'record' es el objeto completo del producto
                <div className="edit-delete-action d-flex justify-content-center">
                    {/* Botón de Add */}
                    <button
                        className="btn btn-sm btn-success"
                        onClick={() => handleAddToCart(record)}
                        title={`Agregar a ${formatCurrency(getPriceForProduct(record))}`}
                    >
                        <PlusCircle size={16} />
                    </button>
                </div>
            ),
        },
    ];



    // RENDERIZADO CONDICIONAL MIENTRAS SE VALIDA LA SESIÓN
    if (authLoading || !isCajaVerificada) {
        return (
            <div className="page-wrapper pos-pg-wrapper ms-0">
                <div className="content pos-design p-0 d-flex justify-content-center align-items-center" style={{ height: '80vh' }}>
                    <div className="text-center">
                        <h2>Verificando sesión y cargando POS...</h2>
                        <div className="spinner-border text-primary" role="status">
                            <span className="visually-hidden">Loading...</span>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    // Si después de cargar, el usuario no está autenticado, no mostrar nada.
    if (!isAuthenticated) {
        return null;
    }

    // --- Cláusula de Guarda para SUPER_ADMIN ---
    // Este bloque se ejecuta DESPUÉS de saber que está autenticado pero ANTES de chequear la caja o mostrar el POS.
    if (isSuperAdmin) {
        return (
            <div className="page-wrapper pos-pg-wrapper ms-0">
                <div className="content pos-design p-0 d-flex justify-content-center align-items-center" style={{ height: '80vh' }}>
                    <div className="text-center">
                        <h2 className="text-danger">🚫 Acceso Denegado</h2>
                        <p className="lead">
                            Tu rol de <strong>SUPER_ADMIN</strong> no tiene permitido el acceso a la vista de Punto de Venta (POS).
                        </p>
                        <p>Esta vista está reservada para roles de Cajero o Vendedor.</p>
                        <Link to={route.dashboard} className="btn btn-primary btn-lg mt-3">
                            Volver al Inicio
                        </Link>
                    </div>
                </div>
            </div>
        );
    }

    // Si la caja no está abierta, se muestra el mensaje de bloqueo
    // NOTA: Un SUPER_ADMIN nunca llegará a este punto gracias a la guarda anterior.
    if (!isCajaAbierta) {
        return (
            <div className="page-wrapper pos-pg-wrapper ms-0">
                <div className="content pos-design p-0 d-flex justify-content-center align-items-center" style={{ height: '80vh' }}>
                    <div className="text-center">
                        <h2 className="text-danger">❌ No hay Caja Abierta</h2>
                        <p>Necesitas iniciar tu turno para realizar ventas.</p>
                        <button
                            className="btn btn-primary btn-lg mt-3"
                            onClick={() => setShowCajaModal(true)}
                        >
                            Abrir Caja Ahora
                        </button>
                    </div>
                </div>

                {/* MODAL REUTILIZABLE DE ABRIR CAJA */}
                <OpenCashboxModal
                    show={showCajaModal}
                    onClose={() => setShowCajaModal(false)}
                    onSuccess={() => window.location.reload()}
                    usuarioId={CURRENT_USER_ID} // Pasar el ID del usuario logueado
                />
            </div>
        );
    }

    // --- VISTA PRINCIPAL DEL POS (Caja Abierta) ---
    return (
        <div className="page-wrapper pos-pg-wrapper ms-0">
            <div className="content pos-design p-0">
                <div className="row align-items-start pos-wrapper">

                    {/* ZONA DE PRODUCTOS (Izquierda) */}
                    <div className="col-md-12 col-lg-8">
                        <div className="pos-products px-3 pt-3 pb-0 border-end border-light">
                            <div className="search-and-filters d-flex flex-column flex-md-row justify-content-between mb-3 bg-white rounded">
                                <div className="input-block me-3 mb-2 mb-md-0 flex-grow-1">
                                    <form onSubmit={handleBarcodeSubmit} className="d-flex">
                                        <input
                                            type="text"
                                            className="form-control me-2"
                                            placeholder="Escanear o Ingresar Código de Barras (Enter)"
                                            value={barcodeQuery}
                                            onChange={(e) => setBarcodeQuery(e.target.value)}
                                            ref={barcodeInputRef}
                                        />
                                        <button type="submit" className="btn btn-dark">
                                            <PlusCircle size={20} />
                                        </button>
                                    </form>
                                </div>
                                <div className="d-flex flex-grow-2">
                                    <div className="input-block me-2 flex-grow-1">
                                        <form onSubmit={handleSearchSubmit} className="d-flex">
                                            <input
                                                type="text"
                                                className="form-control me-2"
                                                placeholder="Buscar producto por nombre (Enter)"
                                                value={searchQuery}
                                                onChange={(e) => setSearchQuery(e.target.value)}
                                            />
                                            <button type="submit" className="btn btn-secondary">
                                                <i className="fa fa-search"></i>
                                            </button>
                                        </form>
                                    </div>
                                    <div className="input-block me-2" style={{ minWidth: '150px' }}>
                                        <Select
                                            options={aromas.map(a => ({ value: a.aromaId, label: a.nombre }))}
                                            classNamePrefix="select"
                                            placeholder="Filtro Aroma"
                                            onChange={setSelectedAroma}
                                            value={selectedAroma}
                                            isClearable
                                        />
                                    </div>
                                    <div className="input-block" style={{ minWidth: '150px' }}>
                                        <Select
                                            options={familias.map(f => ({ value: f.familiaId, label: f.nombre }))}
                                            classNamePrefix="select"
                                            placeholder="Filtro Familia"
                                            onChange={setSelectedFamilia}
                                            value={selectedFamilia}
                                            isClearable
                                        />
                                    </div>
                                </div>
                            </div>
                            <div className="table-responsive">
                                {isPageLoading ? (
                                    <div className="text-center p-4">
                                        <div className="spinner-border" role="status">
                                            <span className="visually-hidden">Cargando...</span>
                                        </div>
                                    </div>
                                ) : (
                                    <>
                                        <Table
                                            columns={columns}
                                            dataSource={productos}
                                            rowKey="productoId"
                                            pagination={{
                                                current: currentPage,
                                                pageSize: pageSize,
                                                total: totalElements,
                                                onChange: (page) => handlePageChange(page),
                                                showSizeChanger: false,
                                                showTotal: (total, range) => `Mostrando ${range[0]}-${range[1]} de ${total} productos`,
                                            }}
                                        />
                                        <div className="pagination-info mt-3 text-center">
                                            <span>
                                                Mostrando {productos.length} productos en esta página. Total: {totalElements}
                                                (Página {currentPage} de {totalPages})
                                            </span>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* ZONA DE VENTA / CARRITO (Derecha) */}
                    <div className="col-md-12 col-lg-4 ps-0">
                        <div className="btn-row d-sm-flex align-items-center justify-content-end mb-3">

                            {/* Botón para volver al inicio */}
                            <Link to={route.dashboard} className="btn btn-info me-2 mb-xs-3 d-flex align-items-center">
                                <Home className="feather-16 me-1" />
                                Volver al inicio
                            </Link>

                            <button
                                className="btn btn-danger me-2 mb-xs-3"
                                onClick={handleCerrarCaja}
                            >
                                🔒 Cerrar Caja
                            </button>
                            <Link to={route.saleslist} className="btn btn-primary mb-xs-3">
                                <span className="me-1 d-flex align-items-center"><RefreshCcw className="feather-16" /></span>
                                Ventas Recientes
                            </Link>
                            <Link to={route.cotizaciones} className="btn btn-success mb-xs-3">
                                <span className="me-1 d-flex align-items-center"><Columns className="feather-16" /></span>
                                Ver Cotizaciones
                            </Link>
                        </div>
                        <aside className="product-order-list">
                            <div className="head d-flex align-items-center justify-content-between w-100">
                                <div>
                                    <h5>Lista de Pedido</h5>
                                    <span>Cajero: {usuario.nombreCompleto}</span>
                                </div>
                                <Link className="confirm-text" to="#" onClick={handleClearCart}>
                                    <Trash2 className="feather-16 text-danger me-1" />
                                    Vaciar Carrito
                                </Link>
                            </div>
                            <div className="customer-info block-section">
                                <h6>Tipo de Venta</h6>
                                <div className="input-block d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <Select
                                            options={TIPOS_VENTA}
                                            className="select"
                                            placeholder="Seleccionar Tipo"
                                            // defaultValue={{ value: 'DETALLE', label: 'Detalle (Precio Normal)' }}
                                            value={TIPOS_VENTA.find(t => t.value === tipoVenta)} // Controlado
                                            onChange={(option) => setTipoVenta(option ? option.value : 'DETALLE')}
                                        />
                                    </div>
                                </div>
                                <h6>Cliente</h6>
                                <div className="input-block d-flex align-items-center">
                                    <div className="flex-grow-1">
                                        <Select
                                            options={clientes.map(c => {
                                                const fullName = `${c.nombre} ${c.apellido || ''}`.trim();

                                                return {
                                                    value: c.clienteId,
                                                    clienteId: c.clienteId,
                                                    label: `${fullName} (${c.rut})`, // Usamos el nombre completo formateado
                                                    tipo: c.tipo
                                                };
                                            })}
                                            className="select"
                                            placeholder="Buscar Cliente"
                                            value={selectedClient} // Controlado
                                            onChange={setSelectedClient}
                                            isClearable
                                        />
                                    </div>
                                    <OverlayTrigger placement="top" overlay={<Tooltip>Nuevo Cliente</Tooltip>}>
                                        <Link
                                            to="#"
                                            className="btn btn-primary btn-icon ms-2"
                                            onClick={(e) => {
                                                e.preventDefault();
                                                setShowCreateClientModal(true);
                                            }}
                                        >
                                            <UserPlus className="feather-16" />
                                        </Link>
                                    </OverlayTrigger>
                                </div>
                            </div>
                            <div className="product-added block-section">
                                <h6 className="d-flex align-items-center mb-3">
                                    Productos ({cart.length})
                                    {cotizacionIdOrigen && <span className="badge bg-info ms-2">Cargado de Cotización #{cotizacionIdOrigen}</span>}
                                </h6>
                                <div className="product-wrap" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                                    {cart.length === 0 ? (
                                        <p className="text-muted text-center">Aún no hay productos en el carrito.</p>
                                    ) : (
                                        cart.map((item) => (
                                            <div key={item.productoId} className="product-list d-flex align-items-center justify-content-between">
                                                <div className="d-flex align-items-center product-info">
                                                    <div>
                                                        <h6>{item.nombre}</h6>
                                                        <p>{formatCurrency(item.precioUnitario)} x {item.cantidad} | Stock: {item.stock}</p>

                                                        {/* Input para descuento unitario */}
                                                        <div className="d-flex align-items-center mt-1">
                                                            <label htmlFor={`discount-${item.productoId}`} className="form-label me-2 mb-0 small">Desc. (-)</label>
                                                            <input
                                                                type="number"
                                                                id={`discount-${item.productoId}`}
                                                                className="form-control form-control-sm"
                                                                style={{ width: '80px' }}
                                                                value={item.valorDescuentoUnitario}
                                                                onChange={(e) => handleUpdateCartItem(item.productoId, 'valorDescuentoUnitario', e.target.value)}
                                                                min="0"
                                                                step="1"
                                                                placeholder="0"
                                                            />
                                                            <ButtonGroup className="ms-2">
                                                                <Button
                                                                    size="sm"
                                                                    variant={item.tipoDescuentoUnitario === 'PORCENTAJE' ? 'primary' : 'outline-primary'}
                                                                    onClick={() => handleUpdateCartItem(item.productoId, 'tipoDescuentoUnitario', 'PORCENTAJE')}
                                                                >%</Button>
                                                                <Button
                                                                    size="sm"
                                                                    variant={item.tipoDescuentoUnitario === 'VALOR' ? 'primary' : 'outline-primary'}
                                                                    onClick={() => handleUpdateCartItem(item.productoId, 'tipoDescuentoUnitario', 'VALOR')}
                                                                >$</Button>
                                                            </ButtonGroup>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div className="qty-item text-center d-flex align-items-center justify-content-center">
                                                    <Link to="#" className="dec d-flex justify-content-center align-items-center" onClick={() => handleUpdateQuantity(item, -1)}>
                                                        <MinusCircle className="feather-14" />
                                                    </Link>

                                                    {/* --- INPUT MODIFICADO --- */}
                                                    <input
                                                        type="number"
                                                        className="form-control text-center mx-2"
                                                        value={item.cantidad}
                                                        onChange={(e) => handleManualQuantityChange(e, item)}
                                                        onClick={(e) => e.target.select()} // UX: Selecciona todo el texto al hacer click para facilitar escribir encima
                                                        min="1"
                                                        max={item.stock}

                                                    />
                                                    <Link to="#" className="inc d-flex justify-content-center align-items-center" onClick={() => handleUpdateQuantity(item, 1)}>
                                                        <PlusCircle className="feather-14" />
                                                    </Link>
                                                </div>
                                                <div className="text-end total-price">
                                                    {formatCurrency(calcularSubtotalItem(item))}
                                                </div>
                                                <div className="action ms-2">
                                                    <Link to="#" className="btn-icon delete-icon" onClick={() => handleRemoveFromCart(item.productoId)}>
                                                        <Trash2 className="feather-14 text-danger" />
                                                    </Link>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                            <div className="block-section">
                                <div className="order-total">
                                    <table className="table table-responsive table-borderless">
                                        <tbody>
                                            <tr>
                                                <td>Sub Total</td>
                                                <td className="text-end">{formatCurrency(ventaSubtotal)}</td>
                                            </tr>
                                            <tr>
                                                <td>Descuento Global</td>
                                                <td className="text-end d-flex justify-content-end align-items-center">
                                                    <input
                                                        type="number"
                                                        value={valorDescuentoGlobal}
                                                        onChange={(e) => handleDescuentoGlobalChange(e.target.value)}
                                                        className="form-control form-control-sm text-end d-inline"
                                                        min="0"
                                                        style={{ maxWidth: '80px' }}
                                                    />
                                                    <ButtonGroup className="ms-2">
                                                        <Button
                                                            size="sm"
                                                            variant={tipoDescuentoGlobal === 'PORCENTAJE' ? 'primary' : 'outline-primary'}
                                                            onClick={() => { setTipoDescuentoGlobal('PORCENTAJE'); setValorDescuentoGlobal(0); }}
                                                        >%</Button>
                                                        <Button
                                                            size="sm"
                                                            variant={tipoDescuentoGlobal === 'VALOR' ? 'primary' : 'outline-primary'}
                                                            onClick={() => { setTipoDescuentoGlobal('VALOR'); setValorDescuentoGlobal(0); }}
                                                        >$</Button>
                                                    </ButtonGroup>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td className="fw-bold fs-5">Total</td>
                                                <td className="text-end fw-bold fs-5">{formatCurrency(ventaTotal)}</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <div className="d-grid btn-block">
                                <button
                                    className="btn btn-secondary btn-lg"
                                    onClick={handleOpenPaymentModal}
                                >
                                    Pagar: {formatCurrency(ventaTotal)}
                                </button>

                                {/* --- BOTÓN DE COTIZACIÓN MODIFICADO --- */}
                                {cart.length > 0 && (
                                    <button
                                        className="btn btn-outline-primary btn-lg mt-2 d-flex align-items-center justify-content-center"
                                        onClick={handleCrearCotizacion} // <-- Llama a la nueva función
                                    >
                                        <FileText size={18} className="me-2" />
                                        {cotizacionIdOrigen
                                            ? "Guardar Como Nueva Cotización y Generar PDF"
                                            : "Guardar Cotización y Generar PDF"
                                        }
                                    </button>
                                )}
                                {/* --- FIN DE BOTÓN MODIFICADO --- */}

                            </div>
                        </aside>
                    </div>
                </div>
            </div>

            {/* --- CAMBIO CLAVE: MODAL DE PAGO CON INTERFAZ MEJORADA --- */}
            <Modal show={showPaymentModal} onHide={handleClosePaymentModal} centered size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>Finalizar Pago</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {/* SECCIÓN DE TOTALES MEJORADA */}
                    <div className="row text-center mb-4">
                        <div className="col-4">
                            <h5 className="text-muted">Total Venta</h5>
                            <h3 className="fw-bold">{formatCurrency(ventaTotal)}</h3>
                        </div>
                        <div className="col-4">
                            <h5 className="text-muted">Total Recibido</h5>
                            <h3 className="text-info fw-bold">{formatCurrency(totalPagado)}</h3>
                        </div>
                        <div className="col-4">
                            <h5 className="text-muted">Faltante / Vuelto</h5>
                            {montoFaltante > 0 ? (
                                <h3 className="text-danger fw-bold">{formatCurrency(montoFaltante)}</h3>
                            ) : (
                                // 'hayPagoEfectivo ? vuelto : 0'
                                <h3 className="text-success fw-bold">
                                    {formatCurrency(hayPagoEfectivo ? vuelto : 0)}
                                </h3>
                            )}
                        </div>
                    </div>

                    <hr />

                    {/* FORMULARIO PARA AÑADIR UN PAGO */}
                    <div className="row align-items-end p-3 bg-light rounded border">
                        <div className="col-md-5">
                            <label className="fw-bold text-dark">Medio de Pago</label>
                            <Select
                                options={availablePaymentMethods}
                                className='text-dark'
                                classNamePrefix="select"
                                placeholder="Seleccione..."
                                value={currentPaymentMethod}
                                onChange={setCurrentPaymentMethod}
                                isDisabled={montoFaltante <= 0 && vuelto > 0} // Deshabilitar si ya se pagó todo y hay vuelto
                            />
                        </div>
                        <div className="col-md-5">
                            <label className="fw-bold text-dark">Monto</label>
                            <div className="input-group">
                                <span className="input-group-text"><DollarSign size={16} /></span>
                                <input
                                    type="text"
                                    pattern="\d*"
                                    className="form-control"
                                    placeholder="0"
                                    value={currentPaymentAmount}
                                    onChange={handleAmountChange}
                                    disabled={montoFaltante <= 0 && vuelto > 0}
                                />
                            </div>
                        </div>
                        <div className="col-md-2 d-grid">
                            <button className="btn btn-primary" onClick={handleAddPago} disabled={montoFaltante <= 0 && vuelto > 0}>
                                Agregar
                            </button>
                        </div>
                    </div>

                    {/* LISTA DE PAGOS AÑADIDOS */}
                    <div className="mt-4">
                        <h6 className="fw-bold">Pagos Registrados</h6>
                        {pagos.length === 0 ? (
                            <p className="text-muted">Aún no se han agregado pagos.</p>
                        ) : (
                            <ListGroup>
                                {pagos.map((pago, index) => (
                                    <ListGroup.Item key={index} className="d-flex justify-content-between align-items-center">
                                        <div>
                                            <strong>{pago.label}:</strong> {formatCurrency(pago.monto)}
                                            {pago.medioPago === 'EFECTIVO' && vuelto > 0 && (
                                                <small className="d-block text-muted">
                                                    (Entregado: {formatCurrency(pago.monto)} → Cubre: {formatCurrency(pago.monto - vuelto)})
                                                </small>
                                            )}
                                        </div>
                                        <button className="btn btn-icon btn-sm" onClick={() => handleRemovePago(index)}>
                                            <Trash2 className="text-danger" size={16} />
                                        </button>
                                    </ListGroup.Item>
                                ))}
                            </ListGroup>
                        )}
                    </div>

                    {/* VUELTO: AHORA SE MUESTRA EN LA PARTE SUPERIOR */}
                    {vuelto > 0 && (
                        <>
                            {hayPagoEfectivo ? (
                                // Caso 1: Vuelto real con efectivo
                                <div className="alert alert-success mt-4 text-center">
                                    <span className="fs-4">Vuelto a entregar:</span>
                                    <span className="fs-3 fw-bold ms-2">{formatCurrency(vuelto)}</span>
                                </div>
                            ) : (
                                // Caso 2: Vuelto "virtual" por redondeo (sin efectivo)
                                <div className="alert alert-info mt-4 text-center">
                                    <span className="fs-5">Ajuste por redondeo (no se entrega):</span>
                                    {/* Mostramos el decimal solo como información */}
                                    <span className="fs-4 fw-bold ms-2">{formatCurrency(vuelto)}</span>
                                    <p className="fs-6 mb-0">
                                        El vuelto real a entregar es <strong>$0</strong> (pago sin efectivo).
                                    </p>
                                </div>
                            )}
                        </>
                    )}


                    <hr className="mt-4" />

                    {/* TIPO DE DOCUMENTO */}
                    <div className="input-block mb-3">
                        <label className="fw-bold">Tipo de Documento</label>
                        <Select
                            options={TIPOS_DOCUMENTO}
                            classNamePrefix="select"
                            defaultValue={TIPOS_DOCUMENTO[0]}
                            onChange={(option) => setTipoDocumento(option ? option.value : 'BOLETA')}
                        />
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <button className="btn btn-secondary" onClick={handleClosePaymentModal}>
                        Cancelar
                    </button>
                    <button
                        className="btn btn-success"
                        onClick={handleCrearVenta}
                        disabled={montoFaltante > 0} // El botón se activa solo cuando el pago está completo o hay vuelto.
                    >
                        Confirmar Venta
                    </button>
                </Modal.Footer>
            </Modal>


            {/* === MODAL DE CREACIÓN DE CLIENTE === */}
            <CreateClientModal
                show={showCreateClientModal}
                handleClose={() => setShowCreateClientModal(false)}
                onClientCreated={loadInitialData}
            />

            {/* MODAL DE CIERRE DE CAJA */}
            <CloseCashboxModal
                show={showCloseModal}
                onClose={() => setShowCloseModal(false)}
                onSuccess={() => {
                    // Al cerrar con éxito, refrescamos o redirigimos
                    window.location.reload();
                }}
                cajaInfo={cajaInfo} // Pasamos la info que ya tenemos cargada en el POS
            />
        </div>
    );
};

export default Pos;