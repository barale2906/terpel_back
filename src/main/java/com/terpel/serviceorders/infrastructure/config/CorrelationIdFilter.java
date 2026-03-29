package com.terpel.serviceorders.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP que genera un correlationId unico (UUID) para cada request entrante.
 *
 * <p>Funcionalidad:</p>
 * <ul>
 *   <li>Si el request trae el header {@code X-Correlation-Id}, lo reutiliza</li>
 *   <li>Si no, genera un UUID nuevo</li>
 *   <li>Lo registra en el MDC de SLF4J para que aparezca en todos los logs</li>
 *   <li>Lo agrega como header en el response para trazabilidad del cliente</li>
 *   <li>Limpia el MDC al finalizar para evitar leaks entre requests</li>
 * </ul>
 *
 * <p>El patron de log en {@code application.yml} incluye {@code [%X{correlationId}]}
 * para que el ID aparezca automaticamente en cada linea de log.</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Nombre del header HTTP y clave MDC para el correlation ID. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
