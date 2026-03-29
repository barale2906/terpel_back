package com.terpel.serviceorders.domain.port.out;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (driven port) que define las operaciones de persistencia
 * requeridas por el dominio.
 *
 * <p>El dominio programa contra esta interfaz; la implementacion concreta
 * (JPA, MongoDB, etc.) vive en la capa de infraestructura.</p>
 */
public interface OrderRepositoryPort {

    /**
     * Persiste una orden de servicio (crear o actualizar).
     *
     * @param order entidad de dominio a persistir
     * @return la entidad persistida con datos actualizados
     */
    EstacionOrder save(EstacionOrder order);

    /**
     * Busca una orden por su identificador unico.
     *
     * @param id UUID de la orden
     * @return Optional con la orden si existe, vacio si no
     */
    Optional<EstacionOrder> findById(UUID id);

    /**
     * Busca ordenes aplicando filtros opcionales con paginacion.
     * Si ambos filtros son null, retorna todas las ordenes paginadas.
     *
     * @param stationId filtro por identificador de estacion (puede ser null)
     * @param status    filtro por estado de la orden (puede ser null)
     * @param pageable  configuracion de paginacion y ordenamiento
     * @return pagina de ordenes que cumplen los filtros
     */
    Page<EstacionOrder> findByFilters(String stationId, OrderStatus status, Pageable pageable);
}
