package com.terpel.serviceorders.domain.service;

import com.terpel.serviceorders.domain.exception.OrderNotFoundException;
import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import com.terpel.serviceorders.domain.port.in.CreateOrderUseCase;
import com.terpel.serviceorders.domain.port.in.GetOrderUseCase;
import com.terpel.serviceorders.domain.port.in.UpdateStatusUseCase;
import com.terpel.serviceorders.domain.port.out.OrderRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Servicio de dominio que implementa los casos de uso de ordenes de servicio.
 * Contiene la logica de negocio pura y delega la persistencia al puerto de repositorio.
 *
 * <p>No es un bean de Spring por si mismo — se registra como bean en {@code AppConfig}
 * para mantener el dominio libre de anotaciones de framework.</p>
 */
public class OrderService implements CreateOrderUseCase, GetOrderUseCase, UpdateStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepositoryPort repositoryPort;

    /**
     * Constructor con inyeccion del puerto de persistencia.
     *
     * @param repositoryPort implementacion del puerto de repositorio
     */
    public OrderService(OrderRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    /**
     * {@inheritDoc}
     * Crea la orden usando el factory method de la entidad y la persiste.
     */
    @Override
    public EstacionOrder create(String stationId, OrderType type, String description) {
        log.info("Creando orden de servicio para estacion: {}, tipo: {}", stationId, type);
        EstacionOrder order = EstacionOrder.create(stationId, type, description);
        EstacionOrder saved = repositoryPort.save(order);
        log.info("Orden creada exitosamente con id: {}", saved.getId());
        return saved;
    }

    /**
     * {@inheritDoc}
     * Lanza {@link OrderNotFoundException} si el ID no existe en la base de datos.
     */
    @Override
    public EstacionOrder findById(UUID id) {
        log.debug("Buscando orden por id: {}", id);
        return repositoryPort.findById(id)
                .orElseThrow(() -> {
                    log.warn("Orden no encontrada con id: {}", id);
                    return new OrderNotFoundException(id);
                });
    }

    /**
     * {@inheritDoc}
     * Delega la busqueda con filtros opcionales al puerto de repositorio.
     */
    @Override
    public Page<EstacionOrder> search(String stationId, OrderStatus status, Pageable pageable) {
        log.debug("Buscando ordenes con filtros — stationId: {}, status: {}, page: {}",
                stationId, status, pageable.getPageNumber());
        return repositoryPort.findByFilters(stationId, status, pageable);
    }

    /**
     * {@inheritDoc}
     * Busca la orden, delega la validacion de transicion a la entidad de dominio,
     * y persiste el cambio. Si la transicion es invalida, la entidad lanza
     * {@link com.terpel.serviceorders.domain.exception.InvalidTransitionException}.
     */
    @Override
    public EstacionOrder updateStatus(UUID id, OrderStatus newStatus) {
        log.info("Actualizando estado de orden {} a {}", id, newStatus);
        EstacionOrder order = findById(id);
        order.changeStatus(newStatus);
        EstacionOrder saved = repositoryPort.save(order);
        log.info("Estado de orden {} actualizado a {}", id, newStatus);
        return saved;
    }
}
