package com.terpel.serviceorders.infrastructure.adapter.out.persistence;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import org.mapstruct.Mapper;

/**
 * Mapper entre la entidad de dominio {@link EstacionOrder} y la entidad JPA {@link OrderJpaEntity}.
 *
 * <p>Usa MapStruct con {@code componentModel = "spring"} para que se inyecte
 * como bean de Spring. La implementacion se genera automaticamente en compilacion.</p>
 *
 * <p>Los nombres de campos coinciden entre dominio y JPA, por lo que MapStruct
 * genera el mapeo sin configuracion adicional.</p>
 */
@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {

    /**
     * Convierte una entidad JPA a entidad de dominio.
     * Usado al leer datos de la base de datos.
     *
     * @param entity entidad JPA leida de la BD
     * @return entidad de dominio con los mismos datos
     */
    EstacionOrder toDomain(OrderJpaEntity entity);

    /**
     * Convierte una entidad de dominio a entidad JPA.
     * Usado al persistir datos en la base de datos.
     *
     * @param domain entidad de dominio a persistir
     * @return entidad JPA lista para guardar
     */
    OrderJpaEntity toJpaEntity(EstacionOrder domain);
}
