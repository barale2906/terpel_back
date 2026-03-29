package com.terpel.serviceorders.domain.service;

import com.terpel.serviceorders.domain.exception.InvalidTransitionException;
import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de las reglas de transicion de estado en la entidad de dominio.
 * JUnit puro, sin framework — se ejecutan en milisegundos.
 *
 * Estos tests verifican las reglas de negocio mas importantes de la aplicacion:
 * como y cuando una orden puede cambiar de estado, y que pasa cuando se intenta
 * un cambio que no esta permitido.
 */
@Tag("unit")
class EstacionOrderTest {

    // ========== Tests de creacion ==========

    /*
     * Que se busca: Verificar que al crear una orden nueva, el sistema le asigna
     *               automaticamente un identificador unico (UUID), la fecha de creacion,
     *               la fecha de actualizacion, y el estado inicial "CREATED".
     * Resultado esperado: La orden se crea correctamente con todos los datos del usuario
     *                     (estacion, tipo, descripcion) y los campos automaticos rellenos.
     */
    @Test
    @DisplayName("create() genera UUID, timestamps y status CREATED")
    void create_shouldSetIdTimestampsAndStatusCreated() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, "Factura de prueba");

        assertNotNull(order.getId(), "El id debe generarse automaticamente");
        assertEquals("ST-001", order.getStationId());
        assertEquals(OrderType.INVOICE, order.getType());
        assertEquals("Factura de prueba", order.getDescription());
        assertEquals(OrderStatus.CREATED, order.getStatus(), "El estado inicial debe ser CREATED");
        assertNotNull(order.getCreatedAt(), "createdAt debe autocompletarse");
        assertNotNull(order.getUpdatedAt(), "updatedAt debe autocompletarse");
    }

    /*
     * Que se busca: Confirmar que la descripcion es un campo opcional. Una estacion
     *               puede crear una orden sin proporcionar descripcion y eso no debe
     *               causar ningun error.
     * Resultado esperado: La orden se crea sin problemas, con la descripcion en null.
     */
    @Test
    @DisplayName("create() sin descripcion (campo opcional) funciona correctamente")
    void create_withoutDescription_shouldWork() {
        EstacionOrder order = EstacionOrder.create("ST-002", OrderType.SUPPORT, null);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertNull(order.getDescription(), "La descripcion es opcional y puede ser null");
    }

    // ========== Tests de transiciones VALIDAS ==========

    /*
     * Que se busca: Verificar que una orden recien creada puede pasar a "en progreso".
     *               Este es el flujo normal: la estacion crea la orden y luego alguien
     *               empieza a trabajar en ella.
     * Resultado esperado: El estado cambia de CREATED a IN_PROGRESS sin errores.
     */
    @Test
    @DisplayName("CREATED -> IN_PROGRESS: transicion permitida")
    void changeStatus_fromCreatedToInProgress_shouldSucceed() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);

        order.changeStatus(OrderStatus.IN_PROGRESS);

        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
    }

    /*
     * Que se busca: Verificar que una orden recien creada puede marcarse directamente
     *               como terminada. Por ejemplo, cuando una tarea se resuelve de inmediato.
     * Resultado esperado: El estado cambia de CREATED a DONE sin errores.
     */
    @Test
    @DisplayName("CREATED -> DONE: transicion permitida")
    void changeStatus_fromCreatedToDone_shouldSucceed() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);

        order.changeStatus(OrderStatus.DONE);

        assertEquals(OrderStatus.DONE, order.getStatus());
    }

    /*
     * Que se busca: Verificar que una orden recien creada puede cancelarse. Esto ocurre
     *               cuando se crea por error o ya no se necesita.
     * Resultado esperado: El estado cambia de CREATED a CANCELLED sin errores.
     */
    @Test
    @DisplayName("CREATED -> CANCELLED: transicion permitida")
    void changeStatus_fromCreatedToCancelled_shouldSucceed() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);

        order.changeStatus(OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    /*
     * Que se busca: Verificar que una orden que esta "en progreso" puede marcarse como
     *               terminada. Este es el flujo normal de trabajo: se trabaja en ella
     *               y se completa.
     * Resultado esperado: El estado cambia de IN_PROGRESS a DONE sin errores.
     */
    @Test
    @DisplayName("IN_PROGRESS -> DONE: transicion permitida")
    void changeStatus_fromInProgressToDone_shouldSucceed() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.SUPPORT, null);
        order.changeStatus(OrderStatus.IN_PROGRESS);

        order.changeStatus(OrderStatus.DONE);

        assertEquals(OrderStatus.DONE, order.getStatus());
    }

    /*
     * Que se busca: Verificar que una orden "en progreso" puede cancelarse. Esto ocurre
     *               cuando se decide abandonar una tarea que ya se habia empezado.
     * Resultado esperado: El estado cambia de IN_PROGRESS a CANCELLED sin errores.
     */
    @Test
    @DisplayName("IN_PROGRESS -> CANCELLED: transicion permitida")
    void changeStatus_fromInProgressToCancelled_shouldSucceed() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.SUPPORT, null);
        order.changeStatus(OrderStatus.IN_PROGRESS);

        order.changeStatus(OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    // ========== Tests de transiciones INVALIDAS ==========

    /*
     * Que se busca: Verificar que una orden ya terminada (DONE) NO puede volver a
     *               "en progreso". Esta es una REGLA DE NEGOCIO clave de la prueba:
     *               una vez completada, no se puede reabrir.
     * Resultado esperado: El sistema lanza un error indicando que esa transicion
     *                     no esta permitida, y el mensaje menciona ambos estados.
     */
    @Test
    @DisplayName("DONE -> IN_PROGRESS: transicion NO permitida (regla de negocio)")
    void changeStatus_fromDoneToInProgress_shouldThrowException() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);
        order.changeStatus(OrderStatus.DONE);

        InvalidTransitionException ex = assertThrows(
                InvalidTransitionException.class,
                () -> order.changeStatus(OrderStatus.IN_PROGRESS)
        );

        assertTrue(ex.getMessage().contains("DONE"));
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
    }

    /*
     * Que se busca: Verificar que una orden terminada (DONE) NO puede volver al estado
     *               inicial "CREATED". No tiene sentido "reiniciar" una orden ya hecha.
     * Resultado esperado: El sistema lanza un error de transicion no permitida.
     */
    @Test
    @DisplayName("DONE -> CREATED: transicion NO permitida")
    void changeStatus_fromDoneToCreated_shouldThrowException() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);
        order.changeStatus(OrderStatus.DONE);

        assertThrows(
                InvalidTransitionException.class,
                () -> order.changeStatus(OrderStatus.CREATED)
        );
    }

    /*
     * Que se busca: Verificar que una orden cancelada NO puede reactivarse. CANCELLED
     *               es un estado final — la decision de cancelar es definitiva.
     * Resultado esperado: El sistema lanza un error de transicion no permitida.
     */
    @Test
    @DisplayName("CANCELLED -> IN_PROGRESS: transicion NO permitida (estado final)")
    void changeStatus_fromCancelledToInProgress_shouldThrowException() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.REDEMPTION, null);
        order.changeStatus(OrderStatus.CANCELLED);

        assertThrows(
                InvalidTransitionException.class,
                () -> order.changeStatus(OrderStatus.IN_PROGRESS)
        );
    }

    /*
     * Que se busca: Verificar que una orden cancelada NO puede volver al inicio.
     *               Refuerza la regla de que CANCELLED es un estado sin retorno.
     * Resultado esperado: El sistema lanza un error de transicion no permitida.
     */
    @Test
    @DisplayName("CANCELLED -> CREATED: transicion NO permitida (estado final)")
    void changeStatus_fromCancelledToCreated_shouldThrowException() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.REDEMPTION, null);
        order.changeStatus(OrderStatus.CANCELLED);

        assertThrows(
                InvalidTransitionException.class,
                () -> order.changeStatus(OrderStatus.CREATED)
        );
    }

    /*
     * Que se busca: Verificar que una orden cancelada NO puede marcarse como terminada.
     *               Si se cancelo, ya no tiene sentido completarla.
     * Resultado esperado: El sistema lanza un error de transicion no permitida.
     */
    @Test
    @DisplayName("CANCELLED -> DONE: transicion NO permitida (estado final)")
    void changeStatus_fromCancelledToDone_shouldThrowException() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.REDEMPTION, null);
        order.changeStatus(OrderStatus.CANCELLED);

        assertThrows(
                InvalidTransitionException.class,
                () -> order.changeStatus(OrderStatus.DONE)
        );
    }

    // ========== Tests de idempotencia y updatedAt ==========

    /*
     * Que se busca: Verificar que si alguien intenta cambiar una orden al mismo estado
     *               en el que ya esta, el sistema no lanza error. Esto evita problemas
     *               cuando se reciben peticiones duplicadas (por ejemplo, por reintentos).
     * Resultado esperado: No ocurre ningun error, la orden mantiene su estado actual.
     */
    @Test
    @DisplayName("Mismo estado -> mismo estado: no lanza excepcion (idempotente)")
    void changeStatus_toSameStatus_shouldBeIdempotent() {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);

        assertDoesNotThrow(() -> order.changeStatus(OrderStatus.CREATED));
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    /*
     * Que se busca: Verificar que cada vez que cambia el estado de una orden, la fecha
     *               de "ultima actualizacion" se actualiza automaticamente. Esto es
     *               importante para saber cuando fue la ultima modificacion.
     * Resultado esperado: La fecha updatedAt despues del cambio es posterior a la original.
     */
    @Test
    @DisplayName("changeStatus actualiza updatedAt al cambiar estado")
    void changeStatus_shouldUpdateUpdatedAt() throws InterruptedException {
        EstacionOrder order = EstacionOrder.create("ST-001", OrderType.INVOICE, null);
        var originalUpdatedAt = order.getUpdatedAt();

        Thread.sleep(10);
        order.changeStatus(OrderStatus.IN_PROGRESS);

        assertTrue(order.getUpdatedAt().isAfter(originalUpdatedAt),
                "updatedAt debe actualizarse al cambiar de estado");
    }
}
