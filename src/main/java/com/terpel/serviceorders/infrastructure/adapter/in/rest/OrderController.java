package com.terpel.serviceorders.infrastructure.adapter.in.rest;

import com.terpel.serviceorders.application.OrderFacade;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.OrderResponse;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para ordenes de servicio de estaciones.
 *
 * <p>Delega toda la logica al {@link OrderFacade} (patron Facade).
 * Solo se ocupa de HTTP: status codes, headers, request/response binding.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST   /service-orders — Crear orden</li>
 *   <li>GET    /service-orders/{id} — Consultar por ID</li>
 *   <li>GET    /service-orders?stationId=&amp;status= — Filtrar con paginacion</li>
 *   <li>PATCH  /service-orders/{id}/status — Actualizar estado</li>
 * </ul>
 */
@RestController
@RequestMapping("/service-orders")
@RequiredArgsConstructor
@Tag(name = "Service Orders", description = "API de ordenes de servicio para estaciones Terpel")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderFacade orderFacade;

    /**
     * Registra una nueva orden de servicio.
     * Los campos id, createdAt y updatedAt se autogeneran.
     *
     * @param request DTO con stationId, type (obligatorios) y description (opcional)
     * @return 201 Created con la orden creada
     */
    @PostMapping
    @Operation(summary = "Crear orden de servicio",
            description = "Registra una nueva orden con estado CREATED. Los campos id, createdAt y updatedAt se autogeneran.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden creada exitosamente",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Error de validacion",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /service-orders — stationId: {}", request.getStationId());
        OrderResponse response = orderFacade.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta una orden por su identificador unico.
     *
     * @param id UUID de la orden
     * @return 200 OK con la orden, o 404 si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar orden por ID",
            description = "Retorna la orden si existe, o 404 Not Found si no se encuentra.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orden encontrada",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> getById(
            @Parameter(description = "ID de la orden (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        log.info("GET /service-orders/{}", id);
        OrderResponse response = orderFacade.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca ordenes con filtros opcionales y paginacion.
     * Si no se proporcionan filtros, retorna todas las ordenes paginadas.
     *
     * @param stationId filtro por identificador de estacion (opcional)
     * @param status    filtro por estado (opcional)
     * @param page      numero de pagina (0-indexed, default 0)
     * @param size      tamano de pagina (default 10)
     * @return 200 OK con pagina de ordenes
     */
    @GetMapping
    @Operation(summary = "Buscar ordenes con filtros",
            description = "Busca ordenes por stationId y/o status con paginacion. Sin filtros retorna todas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados paginados")
    })
    public ResponseEntity<Page<OrderResponse>> search(
            @Parameter(description = "Filtro por ID de estacion", example = "ST-001")
            @RequestParam(required = false) String stationId,

            @Parameter(description = "Filtro por estado", example = "CREATED")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Numero de pagina (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamano de pagina", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /service-orders — stationId: {}, status: {}, page: {}, size: {}",
                stationId, status, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderResponse> result = orderFacade.search(stationId, status, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Actualiza el estado de una orden existente.
     * Aplica las reglas de transicion de negocio:
     * <ul>
     *   <li>No permite DONE → IN_PROGRESS</li>
     *   <li>No permite cambios si esta CANCELLED</li>
     * </ul>
     *
     * @param id      UUID de la orden a actualizar
     * @param request DTO con el nuevo estado
     * @return 200 OK con la orden actualizada, 404 si no existe, 409 si transicion invalida
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de orden",
            description = "Cambia el estado aplicando reglas de negocio. DONE->IN_PROGRESS y cambios en CANCELLED no estan permitidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transicion de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "ID de la orden (UUID)")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("PATCH /service-orders/{}/status — nuevoEstado: {}", id, request.getStatus());
        OrderResponse response = orderFacade.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
