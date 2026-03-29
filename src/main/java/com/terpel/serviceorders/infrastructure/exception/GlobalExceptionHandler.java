package com.terpel.serviceorders.infrastructure.exception;

import com.terpel.serviceorders.domain.exception.InvalidTransitionException;
import com.terpel.serviceorders.domain.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API REST.
 *
 * <p>Traduce excepciones de dominio e infraestructura a respuestas HTTP
 * con formato Problem Details (RFC 7807), garantizando errores consistentes.</p>
 *
 * <p>Mapeos:</p>
 * <ul>
 *   <li>{@link OrderNotFoundException} → 404 Not Found</li>
 *   <li>{@link InvalidTransitionException} → 409 Conflict</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request (validaciones)</li>
 *   <li>{@link Exception} → 500 Internal Server Error (fallback)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Orden no encontrada en la base de datos.
     *
     * @param ex excepcion con el id no encontrado
     * @return ProblemDetail con status 404
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Orden no encontrada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Orden no encontrada");
        problem.setType(URI.create("https://api.terpel.com/errors/order-not-found"));
        return problem;
    }

    /**
     * Transicion de estado no permitida por las reglas de negocio.
     *
     * @param ex excepcion con detalle de la transicion rechazada
     * @return ProblemDetail con status 409 Conflict
     */
    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException ex) {
        log.warn("Transicion de estado invalida: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Transicion de estado no permitida");
        problem.setType(URI.create("https://api.terpel.com/errors/invalid-transition"));
        return problem;
    }

    /**
     * Errores de validacion de campos (Jakarta Validation).
     * Agrupa todos los errores de campo en un solo mensaje.
     *
     * @param ex excepcion con los errores de validacion
     * @return ProblemDetail con status 400 y detalle de campos invalidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Error de validacion: {}", details);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, details);
        problem.setTitle("Error de validacion");
        problem.setType(URI.create("https://api.terpel.com/errors/validation"));
        return problem;
    }

    /**
     * Fallback para excepciones no controladas.
     * Registra el error completo en logs pero retorna un mensaje generico al cliente.
     *
     * @param ex excepcion no esperada
     * @return ProblemDetail con status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error interno no esperado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno. Contacte al administrador.");
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("https://api.terpel.com/errors/internal"));
        return problem;
    }
}
