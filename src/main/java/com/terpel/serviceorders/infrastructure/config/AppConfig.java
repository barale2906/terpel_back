package com.terpel.serviceorders.infrastructure.config;

import com.terpel.serviceorders.domain.port.out.OrderRepositoryPort;
import com.terpel.serviceorders.domain.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion general de la aplicacion.
 *
 * <p>Registra como beans de Spring las clases del dominio que NO tienen
 * anotaciones de framework, manteniendo el dominio limpio.</p>
 *
 * <p>Tambien expone propiedades personalizadas (configurables via ConfigMap/Secret)
 * para que otros componentes las inyecten.</p>
 */
@Configuration
public class AppConfig {

    /**
     * Registra el servicio de dominio como bean de Spring.
     * El dominio no conoce Spring — esta clase hace el puente.
     *
     * @param repositoryPort implementacion del puerto de persistencia (inyectada por Spring)
     * @return instancia de OrderService lista para usar
     */
    @Bean
    public OrderService orderService(OrderRepositoryPort repositoryPort) {
        return new OrderService(repositoryPort);
    }

    /**
     * Tamano de pagina por defecto para las consultas paginadas.
     * Configurable via variable de entorno {@code APP_PAGINATION_SIZE} o ConfigMap.
     */
    @Value("${app.pagination.default-size:10}")
    private int defaultPageSize;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }
}
