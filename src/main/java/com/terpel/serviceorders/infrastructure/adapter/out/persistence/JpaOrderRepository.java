package com.terpel.serviceorders.infrastructure.adapter.out.persistence;

import com.terpel.serviceorders.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link OrderJpaEntity}.
 *
 * <p>Usa derived queries de Spring Data para generar las consultas
 * automaticamente a partir del nombre del metodo. El indice compuesto
 * {@code idx_estacion_orders_station_status} optimiza estas consultas.</p>
 */
public interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    /**
     * Busca ordenes por identificador de estacion con paginacion.
     *
     * @param stationId identificador de la estacion
     * @param pageable  configuracion de paginacion
     * @return pagina de entidades JPA que coinciden
     */
    Page<OrderJpaEntity> findByStationId(String stationId, Pageable pageable);

    /**
     * Busca ordenes por estado con paginacion.
     *
     * @param status estado de la orden
     * @param pageable configuracion de paginacion
     * @return pagina de entidades JPA que coinciden
     */
    Page<OrderJpaEntity> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Busca ordenes por identificador de estacion y estado con paginacion.
     * Aprovecha el indice compuesto {@code (station_id, status)}.
     *
     * @param stationId identificador de la estacion
     * @param status    estado de la orden
     * @param pageable  configuracion de paginacion
     * @return pagina de entidades JPA que coinciden con ambos filtros
     */
    Page<OrderJpaEntity> findByStationIdAndStatus(String stationId, OrderStatus status, Pageable pageable);
}
