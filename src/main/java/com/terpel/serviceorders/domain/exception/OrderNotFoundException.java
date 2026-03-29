package com.terpel.serviceorders.domain.exception;

import java.util.UUID;

/**
 * Excepcion lanzada cuando no se encuentra una orden de servicio en la base de datos.
 * Pertenece al dominio puro — sin dependencias de frameworks.
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * Crea la excepcion indicando el UUID de la orden que no fue encontrada.
     *
     * @param id identificador de la orden buscada
     */
    public OrderNotFoundException(UUID id) {
        super("Orden de servicio no encontrada con id: " + id);
    }
}
