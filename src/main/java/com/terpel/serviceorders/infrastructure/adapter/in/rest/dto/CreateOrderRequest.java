package com.terpel.serviceorders.infrastructure.adapter.in.rest.dto;

import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para crear una orden de servicio.
 *
 * <p>Campos obligatorios: {@code stationId}, {@code type}, {@code status}.
 * Campo opcional: {@code description}.</p>
 *
 * <p>Los campos {@code id}, {@code createdAt} y {@code updatedAt} se autogeneran
 * en el dominio y no se reciben del cliente.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear una nueva orden de servicio")
public class CreateOrderRequest {

    /** Identificador de la estacion. Obligatorio. */
    @NotBlank(message = "El stationId es obligatorio")
    @Schema(description = "Identificador de la estacion", example = "ST-001")
    private String stationId;

    /** Tipo de orden: INVOICE, SUPPORT o REDEMPTION. Obligatorio. */
    @NotNull(message = "El tipo de orden es obligatorio")
    @Schema(description = "Tipo de orden", example = "INVOICE")
    private OrderType type;

    /** Estado inicial de la orden. Obligatorio. */
    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Estado inicial de la orden", example = "CREATED")
    private OrderStatus status;

    /** Descripcion de la orden. Opcional. */
    @Schema(description = "Descripcion opcional de la orden", example = "Factura de combustible")
    private String description;
}
