package com.terpel.serviceorders.domain.exception;

import com.terpel.serviceorders.domain.model.OrderStatus;

/**
 * Excepcion lanzada cuando se intenta una transicion de estado no permitida.
 * Reglas de negocio:
 * <ul>
 *   <li>No se permite DONE -> IN_PROGRESS</li>
 *   <li>No se permite cambiar estado si la orden esta CANCELLED</li>
 * </ul>
 * Pertenece al dominio puro — sin dependencias de frameworks.
 */
public class InvalidTransitionException extends RuntimeException {

    /**
     * Crea la excepcion indicando la transicion de estado que fue rechazada.
     *
     * @param from estado actual de la orden
     * @param to   estado destino solicitado
     */
    public InvalidTransitionException(OrderStatus from, OrderStatus to) {
        super("Transicion de estado no permitida: " + from + " -> " + to);
    }
}
