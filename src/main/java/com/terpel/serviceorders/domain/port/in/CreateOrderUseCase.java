package com.terpel.serviceorders.domain.port.in;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderType;

/**
 * Puerto de entrada (driving port) para el caso de uso de creacion de ordenes.
 *
 * <p>Responsabilidad: recibir los datos necesarios, generar UUID y timestamps,
 * y persistir la nueva orden con estado CREATED.</p>
 */
public interface CreateOrderUseCase {

    /**
     * Crea una nueva orden de servicio para una estacion.
     *
     * @param stationId   identificador de la estacion (obligatorio)
     * @param type        tipo de orden: INVOICE, SUPPORT o REDEMPTION (obligatorio)
     * @param description descripcion de la orden (opcional)
     * @return la orden creada con id, timestamps y status asignados
     */
    EstacionOrder create(String stationId, OrderType type, String description);
}
