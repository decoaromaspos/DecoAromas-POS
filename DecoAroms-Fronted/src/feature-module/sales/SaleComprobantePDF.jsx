import React from 'react';
import { Document, Page, Text, View, StyleSheet, Image } from '@react-pdf/renderer';
import PropTypes from 'prop-types';

// Logo en Base64
const LOGO_BASE64 = 'data:image/jpeg;base64,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';

const BASE_COL_PROPS = {
    borderStyle: 'solid',
    borderWidth: 1,
    borderColor: '#000',
    borderTopWidth: 0,
    borderLeftWidth: 0,
    padding: 4,
    fontSize: 7.5,
};

// Función de utilidad para formatear moneda
const formatCurrency = (amount) => {
    if (amount === null || amount === undefined) return '$0';
    
    const options = {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    };
    
    const absoluteAmount = Math.abs(amount);
    const formatted = new Intl.NumberFormat('es-CL', options).format(absoluteAmount); // "1.781"

    if (amount < 0) {
        return `-$${formatted}`; // Resultado: "-$1.781"
    } else {
        return `$${formatted}`; // Resultado: "$1.781"
    }
};

const styles = StyleSheet.create({
    page: {
        flexDirection: 'column',
        backgroundColor: '#ffffff',
        padding: 30,
        fontFamily: 'Helvetica',
    },
    header: {
        fontSize: 18,
        textAlign: 'center',
        marginBottom: 10,
        fontWeight: 'bold',
        color: '#000000ff',
    },
    subheader: {
        fontSize: 14,
        textAlign: 'center',
        marginBottom: 20,
        fontWeight: 'normal',
    },
    logo: {
        width: 60,
        height: 60,
        alignSelf: 'center',
        marginBottom: 10,
    },
    infoContainer: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 20,
        borderBottomWidth: 1,
        borderBottomColor: '#aaa',
        paddingBottom: 10,
    },
    infoBlock: {
        fontSize: 9,
        lineHeight: 1.5,
    },
    table: {
        display: 'table',
        width: 'auto',
        marginBottom: 10,
        borderStyle: 'solid',
        borderWidth: 1,
        borderColor: '#000',
    },
    tableRow: {
        margin: 'auto',
        flexDirection: 'row',
    },
    tableColHeader: {
        ...BASE_COL_PROPS,
        backgroundColor: '#EAEAEA',
        fontWeight: 'bold',
        textAlign: 'center',
        borderBottomWidth: 1,
    },
    // --- ESTILOS DE COLUMNA ACTUALIZADOS (8 columnas) ---
    colWidth: { width: '5%' },           // #
    colWidthCode: { width: '15%' },      // Cód. Barras
    colWidthProd: { width: '30%' },      // Producto
    colWidthQty: { width: '8%' },        // Cant.
    colWidthPrice: { width: '11%' },     // P. Unitario
    colWidthSubtBruto: { width: '11%' }, // Subtotal Bruto (NUEVO)
    colWidthDiscount: { width: '10%' },  // Descuento
    colWidthSubt: { width: '10%' },      // Monto Final
    // Total: 5 + 15 + 30 + 8 + 11 + 11 + 10 + 10 = 100%
    
    colData: {
        ...BASE_COL_PROPS,
        textAlign: 'right',
        borderBottomWidth: 0,
    },
    colDataLeft: {
        ...BASE_COL_PROPS,
        textAlign: 'left',
        borderBottomWidth: 0,
    },
    totalsContainer: {
        alignSelf: 'flex-end',
        width: '45%',
        marginTop: 10,
    },
    totalRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingVertical: 2,
        fontSize: 10,
    },
    finalTotalRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingVertical: 5,
        borderTopWidth: 1,
        borderTopColor: '#000',
        fontSize: 12,
        fontWeight: 'bold',
    },
    changeRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingVertical: 3,
        fontSize: 11,
        fontWeight: 'bold',
        color: '#333',
    },
    footer: {
        position: 'absolute',
        bottom: 30,
        left: 30,
        right: 30,
        textAlign: 'center',
        fontSize: 8,
        color: '#666',
    }
});

const MEDIOS_PAGO = [
    { value: 'EFECTIVO', label: 'Efectivo' },
    { value: 'TRANSFERENCIA', label: 'Transferencia' },
    { value: 'MERCADO_PAGO', label: 'Mercado Pago' },
    { value: 'BCI', label: 'BCI' },
    { value: 'BOTON_DE_PAGO', label: 'Botón de Pago' },
];

const TIPOS_DOCUMENTO = [
    { value: 'BOLETA', label: 'Boleta' },
    { value: 'FACTURA', label: 'Factura' }
];

const getPagoLabel = (pagoEnum) => {
    return MEDIOS_PAGO.find(pago => pago.value === pagoEnum)?.label || pagoEnum;
};

const SaleComprobantePDF = ({ venta }) => {
    const currentYear = new Date().getFullYear();
    const descuentoGlobalLabel = venta.tipoDescuentoGlobal === 'PORCENTAJE' && venta.valorDescuentoGlobal > 0
        ? `Descuento Global (${venta.valorDescuentoGlobal}%):`
        : 'Descuento Global:';

    // --- INICIO CÁLCULO NETO/IVA ---
    // Calculamos Neto e IVA en base al TotalNeto (Total Venta)
    const totalNetoVenta = venta.totalNeto || 0;
    const netoBase = totalNetoVenta / 1.19;
    const iva = totalNetoVenta - netoBase;
    
    return (
        <Document>
            <Page size="A4" style={styles.page}>
                
                <Image style={styles.logo} src={LOGO_BASE64} /> 
                <Text style={styles.header}>DecoAromas - Fragancias que Cautivan</Text>
                <Text style={styles.subheader}>Comprobante de Venta #{venta.ventaId}</Text>

                <View style={styles.infoContainer}>
                    <View style={styles.infoBlock}>
                        <Text>Fecha: {new Date(venta.fecha).toLocaleDateString('es-CL')}</Text>
                        <Text>Hora: {new Date(venta.fecha).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })}</Text>
                        <Text>Vendedor: {venta.usuarioNombre}</Text> 
                    </View>
                    <View style={styles.infoBlock}>
                        <Text>Cliente: {venta.clienteNombre || 'N/A'}</Text>
                        <Text>
                            Documento: {TIPOS_DOCUMENTO.find(doc => doc.value === venta.tipoDocumento)?.label || venta.tipoDocumento}
                        </Text>
                    </View>
                </View>

                {/* --- TABLA DE PRODUCTOS ACTUALIZADA --- */}
                <View style={styles.table}>
                    <View style={styles.tableRow} fixed>
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidth, borderLeftWidth: 1 }}>#</Text>
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthCode, textAlign: 'left' }}>Cód. Barras</Text>
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthProd, textAlign: 'left' }}>Producto</Text>
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthQty }}>Cant.</Text>
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthPrice }}>P. Unitario</Text> 
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthSubtBruto }}>Subtotal</Text> 
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthDiscount }}>Descuento</Text> 
                        <Text style={{ ...styles.tableColHeader, ...styles.colWidthSubt, borderRightWidth: 1 }}>Monto Final</Text>
                    </View>
                    
                    {venta.detalles.map((detalle, index) => {
                        return (
                            <View style={styles.tableRow} key={index}>
                                <Text style={{ ...styles.colData, ...styles.colWidth, textAlign: 'center', borderLeftWidth: 1 }}>{index + 1}</Text>
                                <Text style={{ ...styles.colDataLeft, ...styles.colWidthCode }}>{detalle.codigoBarras}</Text> 
                                <Text style={{ ...styles.colDataLeft, ...styles.colWidthProd }}>{detalle.productoNombre}</Text>
                                <Text style={{ ...styles.colData, ...styles.colWidthQty }}>{detalle.cantidad}</Text>
                                <Text style={{ ...styles.colData, ...styles.colWidthPrice }}>{formatCurrency(detalle.precioUnitario)}</Text>
                                {/* NUEVA CELDA: Subtotal Bruto */}
                                <Text style={{ ...styles.colData, ...styles.colWidthSubtBruto }}>
                                    {formatCurrency(detalle.subtotalBruto)}
                                </Text>
                                {/* CELDA MODIFICADA: Descuento Calculado */}
                                <Text style={{ ...styles.colData, ...styles.colWidthDiscount, color: '#d9534f' }}>
                                    {detalle.montoDescuentoUnitarioCalculado > 0 ? formatCurrency(detalle.montoDescuentoUnitarioCalculado * -1) : formatCurrency(0)}
                                </Text>
                                {/* CELDA MODIFICADA: Monto Final (detalle.subtotal es el neto) */}
                                <Text style={{ ...styles.colData, ...styles.colWidthSubt, fontWeight: 'bold', borderRightWidth: 1 }}>
                                    {formatCurrency(detalle.subtotal)}
                                </Text>
                            </View>
                        );
                    })}
                </View>

                {/* --- SECCIÓN DE TOTALES ACTUALIZADA --- */}
                <View style={styles.totalsContainer}>
                    <View style={styles.totalRow}>
                        <Text>Subtotal (Bruto):</Text>
                        <Text>{formatCurrency(venta.totalBruto)}</Text>
                    </View>
                    
                    {/* NUEVA FILA: Descuentos Unitarios */}
                    {venta.totalDescuentosUnitarios > 0 && (
                        <View style={styles.totalRow}>
                            <Text>Descuentos Unitarios:</Text>
                            <Text style={{ color: '#d9534f' }}>
                                {formatCurrency(venta.totalDescuentosUnitarios * -1)}
                            </Text>
                        </View>
                    )}

                    {/* Fila de Descuento Global (corregida) */}
                    {venta.montoDescuentoGlobalCalculado > 0 && (
                        <View style={styles.totalRow}>
                            <Text>{descuentoGlobalLabel}</Text>
                            <Text style={{ color: '#d9534f' }}>
                                {formatCurrency(venta.montoDescuentoGlobalCalculado * -1)}
                            </Text>
                        </View>
                    )}
                    
                    <View style={styles.finalTotalRow}>
                        <Text>TOTAL VENTA:</Text>
                        <Text>{formatCurrency(venta.totalNeto)}</Text>
                    </View>

                    {/* --- INICIO DE LA CORRECCIÓN: Neto e IVA --- */}
                    {/* Este bloque muestra el desglose del total (Neto + IVA) */}
                    <View style={{ borderTopWidth: 1, borderTopColor: '#aaa', paddingTop: 5, marginTop: 5 }}>
                        <View style={styles.totalRow}>
                            <Text>Neto:</Text>
                            <Text>{formatCurrency(netoBase)}</Text>
                        </View>
                        <View style={styles.totalRow}>
                            <Text>IVA (19%):</Text>
                            <Text>{formatCurrency(iva)}</Text>
                        </View>
                    </View>
                    {/* --- FIN DE LA CORRECCIÓN --- */}

                    {/* Sección de Pagos (sin cambios) */}
                    <View style={{ marginTop: 10, borderTopWidth: 1, borderTopColor: '#eee', paddingTop: 5 }}>
                        {venta.pagos.map((pago, index) => (
                            <View style={styles.totalRow} key={index}>
                                <Text>Pagado ({getPagoLabel(pago.medioPago)}):</Text>
                                <Text>{formatCurrency(pago.monto)}</Text>
                            </View>
                        ))}
                    </View>

                    {venta.vuelto > 0 && (
                        <View style={styles.changeRow}>
                            <Text>VUELTO:</Text>
                            <Text>{formatCurrency(venta.vuelto)}</Text>
                        </View>
                    )}
                </View>
                
                <Text 
                    style={styles.footer} 
                    fixed 
                    render={() => `¡Gracias por su preferencia! · DecoAromas - ${currentYear}`}
                />
            </Page>
        </Document>
    );
};

SaleComprobantePDF.propTypes = {
    venta: PropTypes.shape({
        ventaId: PropTypes.number.isRequired,
        fecha: PropTypes.string.isRequired,
        usuarioNombre: PropTypes.string,
        clienteNombre: PropTypes.string,
        tipoDocumento: PropTypes.string.isRequired,
        numeroDocumento: PropTypes.string,
        
        // --- Campos de Totales ---
        totalBruto: PropTypes.number.isRequired,
        valorDescuentoGlobal: PropTypes.number,
        tipoDescuentoGlobal: PropTypes.string,
        montoDescuentoGlobalCalculado: PropTypes.number,
        totalDescuentosUnitarios: PropTypes.number,
        totalDescuentoTotal: PropTypes.number,
        totalNeto: PropTypes.number.isRequired,
        
        vuelto: PropTypes.number,
        pagos: PropTypes.arrayOf(PropTypes.shape({
            medioPago: PropTypes.string.isRequired,
            monto: PropTypes.number.isRequired,
        })).isRequired,
        detalles: PropTypes.arrayOf(PropTypes.shape({
            productoNombre: PropTypes.string.isRequired,
            codigoBarras: PropTypes.string.isRequired, 
            cantidad: PropTypes.number.isRequired,
            precioUnitario: PropTypes.number.isRequired,
            
            subtotalBruto: PropTypes.number,
            montoDescuentoUnitarioCalculado: PropTypes.number,
            valorDescuentoUnitario: PropTypes.number,
            tipoDescuentoUnitario: PropTypes.string,
            subtotal: PropTypes.number.isRequired,

        })).isRequired,
    }).isRequired
};

export default SaleComprobantePDF;