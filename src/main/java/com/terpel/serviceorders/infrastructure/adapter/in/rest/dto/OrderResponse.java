package com.terpel.serviceorders.infrastructure.adapter.in.rest.dto;

import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida que representa una orden de servicio completa.
 *
 * <p>Incluye todos los campos de la entidad, incluyendo los autogenerados
 * ({@code id}, {@code createdAt}, {@code updatedAt}).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representacion completa de una orden de servicio")
public class OrderResponse {

    /** Identificador unico de la orden (UUID autogenerado). */
    @Schema(description = "ID unico de la orden", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /** Identificador de la estacion. */
    @Schema(description = "Identificador de la estacion", example = "ST-001")
    private String stationId;

    /** Tipo de orden. */
    @Schema(description = "Tipo de orden", example = "INVOICE")
    private OrderType type;

    /** Descripcion de la orden (puede ser null). */
    @Schema(description = "Descripcion de la orden", example = "Factura de combustible")
    private String description;

    /** Estado actual de la orden. */
    @Schema(description = "Estado actual de la orden", example = "CREATED")
    private OrderStatus status;

    /** Fecha y hora de creacion. */
    @Schema(description = "Fecha de creacion", example = "2026-03-29T12:00:00")
    private LocalDateTime createdAt;

    /** Fecha y hora de la ultima actualizacion. */
    @Schema(description = "Fecha de ultima actualizacion", example = "2026-03-29T12:30:00")
    private LocalDateTime updatedAt;
}
