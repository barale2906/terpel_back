package com.terpel.serviceorders.infrastructure.adapter.out.persistence;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador que implementa el puerto de salida {@link OrderRepositoryPort}.
 *
 * <p>Conecta el dominio con JPA sin que el dominio conozca JPA.
 * Traduce entre entidades de dominio y entidades JPA usando
 * {@link OrderPersistenceMapper}.</p>
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(OrderRepositoryAdapter.class);

    private final JpaOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     * Convierte la entidad de dominio a JPA, la persiste y retorna la version de dominio.
     */
    @Override
    public EstacionOrder save(EstacionOrder order) {
        log.debug("Persistiendo orden con id: {}", order.getId());
        OrderJpaEntity jpaEntity = mapper.toJpaEntity(order);
        OrderJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    /**
     * {@inheritDoc}
     * Busca por UUID en JPA y convierte el resultado a entidad de dominio.
     */
    @Override
    public Optional<EstacionOrder> findById(UUID id) {
        log.debug("Buscando orden por id en BD: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    /**
     * {@inheritDoc}
     * Aplica la combinacion de filtros segun los parametros recibidos:
     * <ul>
     *   <li>Ambos filtros presentes: busca por stationId Y status</li>
     *   <li>Solo stationId: busca por stationId</li>
     *   <li>Solo status: busca por status</li>
     *   <li>Ningun filtro: retorna todos paginados</li>
     * </ul>
     */
    @Override
    public Page<EstacionOrder> findByFilters(String stationId, OrderStatus status, Pageable pageable) {
        log.debug("Buscando ordenes con filtros — stationId: {}, status: {}", stationId, status);

        Page<OrderJpaEntity> page;

        if (stationId != null && status != null) {
            page = jpaRepository.findByStationIdAndStatus(stationId, status, pageable);
        } else if (stationId != null) {
            page = jpaRepository.findByStationId(stationId, pageable);
        } else if (status != null) {
            page = jpaRepository.findByStatus(status, pageable);
        } else {
            page = jpaRepository.findAll(pageable);
        }

        return page.map(mapper::toDomain);
    }
}
