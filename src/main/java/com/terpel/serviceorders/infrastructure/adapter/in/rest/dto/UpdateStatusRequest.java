package com.terpel.serviceorders.infrastructure.adapter.in.rest.dto;

import com.terpel.serviceorders.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para actualizar el estado de una orden de servicio.
 *
 * <p>Solo contiene el nuevo estado deseado. Las reglas de transicion
 * se validan en la entidad de dominio.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para actualizar el estado de una orden")
public class UpdateStatusRequest {

    /** Nuevo estado deseado para la orden. Obligatorio. */
    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Nuevo estado de la orden", example = "IN_PROGRESS")
    private OrderStatus status;
}
