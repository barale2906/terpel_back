package com.terpel.serviceorders.infrastructure.adapter.out.persistence;

import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code estacion_orders} en la base de datos.
 *
 * <p>Separada de la entidad de dominio ({@link com.terpel.serviceorders.domain.model.EstacionOrder})
 * para mantener la independencia del dominio respecto a JPA/Hibernate.</p>
 */
@Entity
@Table(name = "estacion_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderJpaEntity {

    /** Identificador unico de la orden (UUID generado en el dominio). */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Identificador de la estacion asociada a la orden. */
    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    /** Tipo de orden: INVOICE, SUPPORT o REDEMPTION. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type;

    /** Descripcion opcional de la orden. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Estado actual de la orden: CREATED, IN_PROGRESS, DONE o CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Fecha y hora de creacion de la orden (autogenerada). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Fecha y hora de la ultima actualizacion de la orden (autogenerada). */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
