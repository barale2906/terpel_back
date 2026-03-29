package com.terpel.serviceorders.infrastructure.adapter.in.rest.mapper;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.OrderResponse;
import org.mapstruct.Mapper;

/**
 * Mapper entre la entidad de dominio {@link EstacionOrder} y los DTOs REST.
 *
 * <p>Usa MapStruct con {@code componentModel = "spring"} para inyeccion
 * automatica como bean. La implementacion se genera en compilacion.</p>
 *
 * <p>Nota: la conversion de {@code CreateOrderRequest} a entidad de dominio
 * no se hace via mapper porque el dominio usa un factory method
 * ({@link EstacionOrder#create}) que autogenera id, timestamps y status.</p>
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Convierte una entidad de dominio a DTO de respuesta REST.
     *
     * @param domain entidad de dominio
     * @return DTO con todos los campos de la orden
     */
    OrderResponse toResponse(EstacionOrder domain);
}
