
export const MEDIOS_PAGO_MAP = {
    MERCADO_PAGO: { value: 'MERCADO_PAGO', label: 'Mercado Pago' },
    BCI: { value: 'BCI', label: 'Bci' },
    TRANSFERENCIA: { value: 'TRANSFERENCIA', label: 'Transferencia' },
    EFECTIVO: { value: 'EFECTIVO', label: 'Efectivo' },
    BOTON_DE_PAGO: { value: 'BOTON_DE_PAGO', label: 'Botón de Pago' },
    POST: { value: 'POST', label: 'Post' },
};

// Transformamos el objeto en un array para usarlo fácilmente en .map() en los componentes
export const MEDIOS_PAGO = Object.values(MEDIOS_PAGO_MAP);