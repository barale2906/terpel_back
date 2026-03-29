package com.terpel.serviceorders.domain.service;

import com.terpel.serviceorders.domain.exception.InvalidTransitionException;
import com.terpel.serviceorders.domain.exception.OrderNotFoundException;
import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import com.terpel.serviceorders.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del servicio de dominio OrderService.
 * Se ejecutan SIN levantar la aplicacion — usan Mockito para simular la base de datos.
 *
 * Estos tests validan que el servicio orquesta correctamente las operaciones:
 * crear ordenes, buscarlas, y cambiar su estado aplicando las reglas de negocio.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepositoryPort repositoryPort;

    @InjectMocks
    private OrderService orderService;

    private EstacionOrder sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = EstacionOrder.create("ST-100", OrderType.INVOICE, "Orden de prueba");
    }

    // ========== Tests de creacion ==========

    /*
     * Que se busca: Verificar que cuando el servicio recibe una peticion para crear
     *               una orden, la construye correctamente (con UUID, fechas, estado
     *               inicial) y la guarda en la base de datos.
     * Resultado esperado: La orden se crea con todos los datos correctos y se llama
     *                     exactamente una vez al repositorio para guardarla.
     */
    @Test
    @DisplayName("create: genera orden con UUID, timestamps y la guarda via repositorio")
    void create_shouldGenerateOrderAndSave() {
        when(repositoryPort.save(any(EstacionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        EstacionOrder result = orderService.create("ST-100", OrderType.INVOICE, "Factura");

        assertNotNull(result.getId());
        assertEquals("ST-100", result.getStationId());
        assertEquals(OrderType.INVOICE, result.getType());
        assertEquals("Factura", result.getDescription());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(repositoryPort, times(1)).save(any(EstacionOrder.class));
    }

    /*
     * Que se busca: Confirmar que se puede crear una orden sin descripcion, ya que
     *               es un campo opcional segun los requisitos de la prueba.
     * Resultado esperado: La orden se crea y se guarda correctamente con descripcion null.
     */
    @Test
    @DisplayName("create: descripcion null es valida (campo opcional)")
    void create_withNullDescription_shouldSucceed() {
        when(repositoryPort.save(any(EstacionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        EstacionOrder result = orderService.create("ST-200", OrderType.SUPPORT, null);

        assertNull(result.getDescription());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(repositoryPort).save(any(EstacionOrder.class));
    }

    // ========== Tests de consulta por ID ==========

    /*
     * Que se busca: Verificar que al buscar una orden que SI existe en la base de datos,
     *               el servicio la retorna correctamente con todos sus datos.
     * Resultado esperado: Se obtiene la orden con el mismo ID y datos de la estacion.
     */
    @Test
    @DisplayName("findById: retorna orden cuando existe")
    void findById_whenExists_shouldReturnOrder() {
        UUID id = sampleOrder.getId();
        when(repositoryPort.findById(id)).thenReturn(Optional.of(sampleOrder));

        EstacionOrder result = orderService.findById(id);

        assertEquals(id, result.getId());
        assertEquals("ST-100", result.getStationId());
        verify(repositoryPort).findById(id);
    }

    /*
     * Que se busca: Verificar que al buscar una orden con un ID que NO existe en la
     *               base de datos, el servicio lanza un error claro indicando que no
     *               se encontro, en lugar de retornar null o un dato vacio.
     * Resultado esperado: Se lanza OrderNotFoundException con un mensaje que incluye
     *                     el ID buscado para facilitar la depuracion.
     */
    @Test
    @DisplayName("findById: lanza OrderNotFoundException cuando no existe")
    void findById_whenNotExists_shouldThrowException() {
        UUID id = UUID.randomUUID();
        when(repositoryPort.findById(id)).thenReturn(Optional.empty());

        OrderNotFoundException ex = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.findById(id)
        );

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(repositoryPort).findById(id);
    }

    // ========== Tests de actualizacion de estado ==========

    /*
     * Que se busca: Verificar que cuando se actualiza el estado de una orden con una
     *               transicion valida (ej: CREATED a IN_PROGRESS), el servicio cambia
     *               el estado y guarda la orden actualizada en la base de datos.
     * Resultado esperado: El estado cambia correctamente y se guarda en la BD.
     */
    @Test
    @DisplayName("updateStatus: transicion valida CREATED -> IN_PROGRESS actualiza y guarda")
    void updateStatus_validTransition_shouldUpdateAndSave() {
        UUID id = sampleOrder.getId();
        when(repositoryPort.findById(id)).thenReturn(Optional.of(sampleOrder));
        when(repositoryPort.save(any(EstacionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        EstacionOrder result = orderService.updateStatus(id, OrderStatus.IN_PROGRESS);

        assertEquals(OrderStatus.IN_PROGRESS, result.getStatus());
        verify(repositoryPort).findById(id);
        verify(repositoryPort).save(sampleOrder);
    }

    /*
     * Que se busca: Verificar que cuando se intenta una transicion de estado prohibida
     *               (DONE a IN_PROGRESS), el servicio lanza un error y NO guarda nada
     *               en la base de datos. Es crucial que un cambio invalido no persista.
     * Resultado esperado: Se lanza InvalidTransitionException y el repositorio NO recibe
     *                     ninguna llamada a save.
     */
    @Test
    @DisplayName("updateStatus: transicion invalida DONE -> IN_PROGRESS lanza InvalidTransitionException")
    void updateStatus_invalidTransition_shouldThrowException() {
        sampleOrder.changeStatus(OrderStatus.DONE);
        UUID id = sampleOrder.getId();
        when(repositoryPort.findById(id)).thenReturn(Optional.of(sampleOrder));

        assertThrows(
                InvalidTransitionException.class,
                () -> orderService.updateStatus(id, OrderStatus.IN_PROGRESS)
        );

        verify(repositoryPort).findById(id);
        verify(repositoryPort, never()).save(any());
    }

    /*
     * Que se busca: Verificar que una orden cancelada no acepta ningun cambio de estado.
     *               CANCELLED es un estado final e irreversible.
     * Resultado esperado: Se lanza InvalidTransitionException y NO se guarda nada.
     */
    @Test
    @DisplayName("updateStatus: orden CANCELLED no permite cambios")
    void updateStatus_cancelledOrder_shouldThrowException() {
        sampleOrder.changeStatus(OrderStatus.CANCELLED);
        UUID id = sampleOrder.getId();
        when(repositoryPort.findById(id)).thenReturn(Optional.of(sampleOrder));

        assertThrows(
                InvalidTransitionException.class,
                () -> orderService.updateStatus(id, OrderStatus.IN_PROGRESS)
        );

        verify(repositoryPort, never()).save(any());
    }

    /*
     * Que se busca: Verificar que si se intenta actualizar el estado de una orden que
     *               no existe, el servicio lanza un error claro en lugar de fallar
     *               silenciosamente o causar un error generico.
     * Resultado esperado: Se lanza OrderNotFoundException antes de intentar el cambio.
     */
    @Test
    @DisplayName("updateStatus: orden no encontrada lanza OrderNotFoundException")
    void updateStatus_orderNotFound_shouldThrowException() {
        UUID id = UUID.randomUUID();
        when(repositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateStatus(id, OrderStatus.IN_PROGRESS)
        );
    }

    // ========== Tests de busqueda con filtros ==========

    /*
     * Que se busca: Verificar que al buscar ordenes con filtros (por estacion y estado),
     *               el servicio pasa correctamente esos filtros al repositorio y retorna
     *               los resultados paginados.
     * Resultado esperado: Se obtiene una pagina con las ordenes que coinciden, y el
     *                     repositorio recibe exactamente los filtros proporcionados.
     */
    @Test
    @DisplayName("search: delega al repositorio con filtros y paginacion")
    void search_shouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EstacionOrder> expectedPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);
        when(repositoryPort.findByFilters("ST-100", OrderStatus.CREATED, pageable))
                .thenReturn(expectedPage);

        Page<EstacionOrder> result = orderService.search("ST-100", OrderStatus.CREATED, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(sampleOrder, result.getContent().get(0));
        verify(repositoryPort).findByFilters("ST-100", OrderStatus.CREATED, pageable);
    }

    /*
     * Que se busca: Verificar que al buscar sin ningun filtro (ni estacion ni estado),
     *               el servicio retorna todas las ordenes de forma paginada.
     *               Esto es el comportamiento por defecto del endpoint GET.
     * Resultado esperado: Se llama al repositorio con filtros null y se obtienen
     *                     todas las ordenes.
     */
    @Test
    @DisplayName("search: sin filtros retorna todas las ordenes paginadas")
    void search_withoutFilters_shouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EstacionOrder> expectedPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);
        when(repositoryPort.findByFilters(null, null, pageable)).thenReturn(expectedPage);

        Page<EstacionOrder> result = orderService.search(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(repositoryPort).findByFilters(null, null, pageable);
    }
}
