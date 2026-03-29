package com.terpel.serviceorders.domain.model;

import com.terpel.serviceorders.domain.exception.InvalidTransitionException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad de dominio que representa una orden de servicio de estacion.
 * Contiene las reglas de negocio de transicion de estado.
 *
 * <p>Reglas de transicion:</p>
 * <ul>
 *   <li>DONE es estado terminal: solo permite transicion a CANCELLED</li>
 *   <li>CANCELLED es estado final: no permite ninguna transicion</li>
 *   <li>Transiciones al mismo estado son idempotentes (no lanzan excepcion)</li>
 * </ul>
 *
 * <p>Pertenece al dominio puro — CERO dependencias de frameworks (Spring, JPA, etc.).</p>
 */
public class EstacionOrder {

    private UUID id;
    private String stationId;
    private OrderType type;
    private String description;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Transiciones prohibidas desde cada estado.
     * DONE solo puede ir a CANCELLED; CANCELLED no puede ir a ningun lado.
     */
    private static final Set<OrderStatus> DONE_BLOCKED = Set.of(
            OrderStatus.CREATED, OrderStatus.IN_PROGRESS
    );

    /**
     * Constructor completo para reconstruccion desde persistencia.
     * No valida reglas porque los datos ya fueron validados al crearse.
     */
    public EstacionOrder(UUID id, String stationId, OrderType type,
                         String description, OrderStatus status,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.stationId = stationId;
        this.type = type;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Metodo factory para crear una nueva orden de servicio.
     * Autogenera id (UUID), createdAt y updatedAt con la fecha actual.
     * El estado inicial siempre es CREATED.
     *
     * @param stationId   identificador de la estacion (obligatorio)
     * @param type        tipo de orden: INVOICE, SUPPORT o REDEMPTION (obligatorio)
     * @param description descripcion de la orden (opcional, puede ser null)
     * @return nueva instancia de EstacionOrder con estado CREATED
     */
    public static EstacionOrder create(String stationId, OrderType type, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new EstacionOrder(
                UUID.randomUUID(),
                stationId,
                type,
                description,
                OrderStatus.CREATED,
                now,
                now
        );
    }

    /**
     * Cambia el estado de la orden aplicando las reglas de negocio.
     *
     * <p>Reglas:</p>
     * <ul>
     *   <li>Si el estado actual es CANCELLED, no se permite ningun cambio</li>
     *   <li>Si el estado actual es DONE, solo se permite ir a CANCELLED</li>
     *   <li>Transicion al mismo estado es idempotente (no-op)</li>
     * </ul>
     *
     * @param newStatus nuevo estado deseado
     * @throws InvalidTransitionException si la transicion viola las reglas de negocio
     */
    public void changeStatus(OrderStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidTransitionException(this.status, newStatus);
        }

        if (this.status == OrderStatus.DONE && DONE_BLOCKED.contains(newStatus)) {
            throw new InvalidTransitionException(this.status, newStatus);
        }

        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Getters ==========

    public UUID getId() { return id; }

    public String getStationId() { return stationId; }

    public OrderType getType() { return type; }

    public String getDescription() { return description; }

    public OrderStatus getStatus() { return status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
