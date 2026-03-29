package com.terpel.serviceorders.domain.port.in;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Puerto de entrada (driving port) para los casos de uso de consulta de ordenes.
 *
 * <p>Incluye busqueda por ID unico y busqueda con filtros y paginacion.</p>
 */
public interface GetOrderUseCase {

    /**
     * Busca una orden por su identificador unico.
     *
     * @param id UUID de la orden
     * @return la orden encontrada
     * @throws com.terpel.serviceorders.domain.exception.OrderNotFoundException si no existe
     */
    EstacionOrder findById(UUID id);

    /**
     * Busca ordenes con filtros opcionales y paginacion.
     *
     * @param stationId filtro por identificador de estacion (puede ser null)
     * @param status    filtro por estado (puede ser null)
     * @param pageable  configuracion de paginacion
     * @return pagina de ordenes que cumplen los filtros
     */
    Page<EstacionOrder> search(String stationId, OrderStatus status, Pageable pageable);
}
