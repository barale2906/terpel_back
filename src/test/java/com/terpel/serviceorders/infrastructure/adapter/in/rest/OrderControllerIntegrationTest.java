package com.terpel.serviceorders.infrastructure.adapter.in.rest;

import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.OrderResponse;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.UpdateStatusRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integracion end-to-end (de punta a punta) con Testcontainers.
 *
 * A diferencia de los tests unitarios, estos tests levantan la aplicacion COMPLETA
 * con una base de datos PostgreSQL real (dentro de un contenedor Docker temporal).
 * Esto permite probar el flujo real tal como lo experimentaria un usuario:
 * peticion HTTP → controlador → facade → servicio → base de datos → respuesta HTTP.
 *
 * Los datos se crean usando la propia API (POST) para validar el flujo real
 * desde el principio.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("serviceorders_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<OrderResponse> createOrder(String stationId, OrderType type,
                                                       OrderStatus status, String description) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .stationId(stationId)
                .type(type)
                .status(status)
                .description(description)
                .build();
        return restTemplate.postForEntity("/service-orders", request, OrderResponse.class);
    }

    // ========== POST /service-orders ==========

    /*
     * Que se busca: Verificar que al enviar una peticion POST con todos los datos
     *               correctos, la API crea la orden y responde con codigo 201 (Creado).
     *               Tambien se verifica que la respuesta incluye todos los campos
     *               esperados: id generado, fechas automaticas, y los datos enviados.
     * Resultado esperado: Codigo HTTP 201, y el cuerpo de la respuesta contiene la
     *                     orden completa con id, estacion, tipo, estado y fechas.
     */
    @Test
    @Order(1)
    @DisplayName("POST /service-orders → 201 Created con body correcto")
    void post_validOrder_shouldReturn201() {
        ResponseEntity<OrderResponse> response = createOrder(
                "ST-INT-001", OrderType.INVOICE, OrderStatus.CREATED, "Factura de integracion");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        OrderResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getId());
        assertEquals("ST-INT-001", body.getStationId());
        assertEquals(OrderType.INVOICE, body.getType());
        assertEquals(OrderStatus.CREATED, body.getStatus());
        assertEquals("Factura de integracion", body.getDescription());
        assertNotNull(body.getCreatedAt());
        assertNotNull(body.getUpdatedAt());
    }

    /*
     * Que se busca: Verificar que si se intenta crear una orden sin los campos
     *               obligatorios (stationId, type, status), la API rechaza la
     *               peticion con codigo 400 (error de validacion).
     * Resultado esperado: Codigo HTTP 400 Bad Request, indicando que faltan datos.
     */
    @Test
    @Order(2)
    @DisplayName("POST /service-orders sin campos obligatorios → 400")
    void post_missingFields_shouldReturn400() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .description("Solo descripcion, sin campos obligatorios")
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/service-orders", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========== GET /service-orders/{id} ==========

    /*
     * Que se busca: Verificar que al consultar una orden que SI existe usando su ID,
     *               la API la devuelve correctamente con todos sus datos y codigo 200.
     *               Primero se crea la orden y luego se consulta por su ID.
     * Resultado esperado: Codigo HTTP 200 OK, con la orden completa y el mismo ID.
     */
    @Test
    @Order(3)
    @DisplayName("GET /service-orders/{id} existente → 200")
    void get_existingId_shouldReturn200() {
        ResponseEntity<OrderResponse> created = createOrder(
                "ST-INT-002", OrderType.SUPPORT, OrderStatus.CREATED, null);
        UUID id = created.getBody().getId();

        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                "/service-orders/" + id, OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
        assertEquals("ST-INT-002", response.getBody().getStationId());
    }

    /*
     * Que se busca: Verificar que al buscar una orden con un ID que NO existe en la
     *               base de datos, la API responde con codigo 404 y un mensaje claro
     *               de "Orden no encontrada" (formato Problem Details).
     * Resultado esperado: Codigo HTTP 404 Not Found, con el titulo "Orden no encontrada".
     */
    @Test
    @Order(4)
    @DisplayName("GET /service-orders/{id} inexistente → 404")
    void get_nonExistingId_shouldReturn404() {
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/service-orders/" + fakeId, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Orden no encontrada", response.getBody().get("title"));
    }

    // ========== GET /service-orders?stationId=&status= ==========

    /*
     * Que se busca: Verificar que se pueden buscar ordenes filtrando por estacion.
     *               Se crean varias ordenes de distintas estaciones y se filtra por
     *               una de ellas. Solo deben aparecer las de esa estacion.
     * Resultado esperado: Codigo HTTP 200 con resultados paginados, donde al menos
     *                     aparecen las 2 ordenes de la estacion "ST-FILTER-A".
     */
    @Test
    @Order(5)
    @DisplayName("GET /service-orders?stationId=X → resultados filtrados")
    void get_filterByStationId_shouldReturnFiltered() {
        createOrder("ST-FILTER-A", OrderType.INVOICE, OrderStatus.CREATED, null);
        createOrder("ST-FILTER-A", OrderType.SUPPORT, OrderStatus.CREATED, null);
        createOrder("ST-FILTER-B", OrderType.REDEMPTION, OrderStatus.CREATED, null);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/service-orders?stationId=ST-FILTER-A&page=0&size=10", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        int totalElements = ((Number) body.get("totalElements")).intValue();
        assertTrue(totalElements >= 2, "Debe haber al menos 2 ordenes de ST-FILTER-A");
    }

    /*
     * Que se busca: Verificar que se pueden combinar dos filtros a la vez: estacion
     *               y estado. Esto permite busquedas mas precisas, por ejemplo
     *               "todas las ordenes creadas de la estacion ST-DUAL".
     * Resultado esperado: Codigo HTTP 200 con al menos 1 resultado que cumple ambos
     *                     filtros simultaneamente.
     */
    @Test
    @Order(6)
    @DisplayName("GET /service-orders?stationId=X&status=Y → resultados con ambos filtros")
    void get_filterByStationIdAndStatus_shouldReturnFiltered() {
        createOrder("ST-DUAL", OrderType.INVOICE, OrderStatus.CREATED, null);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/service-orders?stationId=ST-DUAL&status=CREATED&page=0&size=10", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        int totalElements = ((Number) body.get("totalElements")).intValue();
        assertTrue(totalElements >= 1);
    }

    // ========== PATCH /service-orders/{id}/status ==========

    /*
     * Que se busca: Verificar que un cambio de estado valido funciona de extremo a
     *               extremo. Se crea una orden, y luego se cambia su estado de
     *               CREATED a IN_PROGRESS usando el endpoint PATCH.
     * Resultado esperado: Codigo HTTP 200 OK, y el estado de la orden en la respuesta
     *                     es IN_PROGRESS.
     */
    @Test
    @Order(7)
    @DisplayName("PATCH transicion valida CREATED → IN_PROGRESS → 200")
    void patch_validTransition_shouldReturn200() {
        ResponseEntity<OrderResponse> created = createOrder(
                "ST-PATCH-OK", OrderType.INVOICE, OrderStatus.CREATED, null);
        UUID id = created.getBody().getId();

        UpdateStatusRequest patchRequest = UpdateStatusRequest.builder()
                .status(OrderStatus.IN_PROGRESS)
                .build();

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/service-orders/" + id + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(patchRequest),
                OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.IN_PROGRESS, response.getBody().getStatus());
    }

    /*
     * Que se busca: Verificar la regla de negocio clave: una orden que ya esta
     *               terminada (DONE) NO puede volver a "en progreso" (IN_PROGRESS).
     *               Primero se lleva la orden a DONE, y luego se intenta el cambio
     *               prohibido. La API debe rechazarlo con codigo 409 (Conflicto).
     * Resultado esperado: Codigo HTTP 409 Conflict, con el titulo
     *                     "Transicion de estado no permitida".
     */
    @Test
    @Order(8)
    @DisplayName("PATCH transicion invalida DONE → IN_PROGRESS → 409 Conflict")
    void patch_invalidTransition_shouldReturn409() {
        ResponseEntity<OrderResponse> created = createOrder(
                "ST-PATCH-FAIL", OrderType.SUPPORT, OrderStatus.CREATED, null);
        UUID id = created.getBody().getId();

        UpdateStatusRequest toDone = UpdateStatusRequest.builder().status(OrderStatus.DONE).build();
        restTemplate.exchange("/service-orders/" + id + "/status",
                HttpMethod.PATCH, new HttpEntity<>(toDone), OrderResponse.class);

        UpdateStatusRequest toInProgress = UpdateStatusRequest.builder()
                .status(OrderStatus.IN_PROGRESS).build();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/service-orders/" + id + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(toInProgress),
                Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Transicion de estado no permitida", response.getBody().get("title"));
    }

    /*
     * Que se busca: Verificar la segunda regla de negocio clave: una orden CANCELADA
     *               no acepta ningun cambio posterior. CANCELLED es un estado final
     *               e irreversible. Al intentar cambiar el estado, la API debe
     *               rechazarlo con codigo 409 (Conflicto).
     * Resultado esperado: Codigo HTTP 409 Conflict, confirmando que la orden cancelada
     *                     no puede modificarse.
     */
    @Test
    @Order(9)
    @DisplayName("PATCH sobre orden CANCELLED → 409 Conflict")
    void patch_cancelledOrder_shouldReturn409() {
        ResponseEntity<OrderResponse> created = createOrder(
                "ST-PATCH-CANCEL", OrderType.REDEMPTION, OrderStatus.CREATED, null);
        UUID id = created.getBody().getId();

        UpdateStatusRequest toCancel = UpdateStatusRequest.builder()
                .status(OrderStatus.CANCELLED).build();
        restTemplate.exchange("/service-orders/" + id + "/status",
                HttpMethod.PATCH, new HttpEntity<>(toCancel), OrderResponse.class);

        UpdateStatusRequest toDone = UpdateStatusRequest.builder()
                .status(OrderStatus.DONE).build();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/service-orders/" + id + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(toDone),
                Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
