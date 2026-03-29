package com.terpel.serviceorders.application;

import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.service.OrderService;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.OrderResponse;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.UpdateStatusRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Facade que orquesta los casos de uso del dominio (patron Facade — GoF).
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Recibir DTOs desde el controller</li>
 *   <li>Extraer datos y delegar la logica al {@link OrderService}</li>
 *   <li>Convertir entidades de dominio a DTOs de respuesta via {@link OrderMapper}</li>
 * </ul>
 *
 * <p>El controller solo habla con esta clase, nunca directamente con el servicio.
 * Esto centraliza la orquestacion y permite reutilizar el Facade desde otros
 * adaptadores (Kafka consumers, batch jobs, etc.).</p>
 */
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /**
     * Crea una nueva orden de servicio a partir del DTO de entrada.
     * Extrae los campos del request y delega la creacion al servicio de dominio.
     *
     * @param request DTO con stationId, type, status y description (opcional)
     * @return DTO de respuesta con la orden creada (incluye id y timestamps)
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Facade: creando orden para estacion {}", request.getStationId());
        EstacionOrder order = orderService.create(
                request.getStationId(),
                request.getType(),
                request.getDescription()
        );
        return orderMapper.toResponse(order);
    }

    /**
     * Consulta una orden por su identificador unico.
     *
     * @param id UUID de la orden
     * @return DTO de respuesta con los datos de la orden
     * @throws com.terpel.serviceorders.domain.exception.OrderNotFoundException si no existe
     */
    public OrderResponse getById(UUID id) {
        log.debug("Facade: consultando orden por id {}", id);
        EstacionOrder order = orderService.findById(id);
        return orderMapper.toResponse(order);
    }

    /**
     * Actualiza el estado de una orden existente.
     *
     * @param id      UUID de la orden a actualizar
     * @param request DTO con el nuevo estado
     * @return DTO de respuesta con la orden actualizada
     * @throws com.terpel.serviceorders.domain.exception.OrderNotFoundException      si la orden no existe
     * @throws com.terpel.serviceorders.domain.exception.InvalidTransitionException  si la transicion no es valida
     */
    public OrderResponse updateStatus(UUID id, UpdateStatusRequest request) {
        log.info("Facade: actualizando estado de orden {} a {}", id, request.getStatus());
        EstacionOrder order = orderService.updateStatus(id, request.getStatus());
        return orderMapper.toResponse(order);
    }

    /**
     * Busca ordenes con filtros opcionales y paginacion.
     * Si ambos filtros son null, retorna todas las ordenes paginadas.
     *
     * @param stationId filtro por identificador de estacion (puede ser null)
     * @param status    filtro por estado de la orden (puede ser null)
     * @param pageable  configuracion de paginacion y ordenamiento
     * @return pagina de DTOs de respuesta
     */
    public Page<OrderResponse> search(String stationId, OrderStatus status, Pageable pageable) {
        log.debug("Facade: buscando ordenes — stationId: {}, status: {}", stationId, status);
        Page<EstacionOrder> domainPage = orderService.search(stationId, status, pageable);
        return domainPage.map(orderMapper::toResponse);
    }
}
