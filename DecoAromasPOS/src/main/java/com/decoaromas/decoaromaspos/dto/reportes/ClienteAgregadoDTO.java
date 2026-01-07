package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para manejar resultados de consultas de agregación de clientes.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAgregadoDTO {
    // Usado para agrupar (ej: TipoCliente, o Nombre/Apellido del cliente)
    private String nombre;
    // Usado para la métrica (ej: Cantidad de clientes, Total gastado, Recencia)
    private Double valor;
}