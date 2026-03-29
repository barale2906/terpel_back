package com.terpel.serviceorders.domain.port.in;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;

import java.util.UUID;

/**
 * Puerto de entrada (driving port) para el caso de uso de actualizacion de estado.
 *
 * <p>Aplica las reglas de transicion de estado definidas en la entidad de dominio:</p>
 * <ul>
 *   <li>No permite DONE -> IN_PROGRESS</li>
 *   <li>No permite cambios si esta CANCELLED</li>
 * </ul>
 */
public interface UpdateStatusUseCase {

    /**
     * Actualiza el estado de una orden existente.
     *
     * @param id        UUID de la orden a actualizar
     * @param newStatus nuevo estado deseado
     * @return la orden con el estado actualizado
     * @throws com.terpel.serviceorders.domain.exception.OrderNotFoundException   si la orden no existe
     * @throws com.terpel.serviceorders.domain.exception.InvalidTransitionException si la transicion no esta permitida
     */
    EstacionOrder updateStatus(UUID id, OrderStatus newStatus);
}
