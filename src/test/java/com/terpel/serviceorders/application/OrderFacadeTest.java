package com.terpel.serviceorders.application;

import com.terpel.serviceorders.domain.model.EstacionOrder;
import com.terpel.serviceorders.domain.model.OrderStatus;
import com.terpel.serviceorders.domain.model.OrderType;
import com.terpel.serviceorders.domain.service.OrderService;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.OrderResponse;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.dto.UpdateStatusRequest;
import com.terpel.serviceorders.infrastructure.adapter.in.rest.mapper.OrderMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del Facade (patron Fachada).
 *
 * El Facade es la capa intermedia entre el controlador REST y el servicio de dominio.
 * Estos tests verifican que el Facade hace bien su trabajo: recibir los datos del
 * controlador, pasarlos al servicio, y convertir la respuesta del dominio al formato
 * que espera el cliente (JSON).
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderFacade facade;

    private EstacionOrder sampleOrder;
    private OrderResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleOrder = EstacionOrder.create("ST-001", OrderType.INVOICE, "Factura test");

        sampleResponse = OrderResponse.builder()
                .id(sampleOrder.getId())
                .stationId("ST-001")
                .type(OrderType.INVOICE)
                .description("Factura test")
                .status(OrderStatus.CREATED)
                .createdAt(sampleOrder.getCreatedAt())
                .updatedAt(sampleOrder.getUpdatedAt())
                .build();
    }

    // ========== Tests de createOrder ==========

    /*
     * Que se busca: Verificar que cuando llega una peticion para crear una orden,
     *               el Facade extrae los datos del formulario (DTO), los pasa al
     *               servicio de dominio para crear la orden, y luego convierte el
     *               resultado al formato de respuesta JSON.
     * Resultado esperado: Se retorna un OrderResponse con los datos correctos, y se
     *                     confirma que el servicio y el mapper fueron llamados.
     */
    @Test
    @DisplayName("createOrder: convierte DTO, llama al service y retorna OrderResponse")
    void createOrder_shouldDelegateToServiceAndMapResponse() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .stationId("ST-001")
                .type(OrderType.INVOICE)
                .status(OrderStatus.CREATED)
                .description("Factura test")
                .build();

        when(orderService.create("ST-001", OrderType.INVOICE, "Factura test"))
                .thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        OrderResponse result = facade.createOrder(request);

        assertNotNull(result);
        assertEquals("ST-001", result.getStationId());
        assertEquals(OrderType.INVOICE, result.getType());
        verify(orderService).create("ST-001", OrderType.INVOICE, "Factura test");
        verify(orderMapper).toResponse(sampleOrder);
    }

    // ========== Tests de getById ==========

    /*
     * Que se busca: Verificar que al consultar una orden por su ID, el Facade le pide
     *               al servicio que la busque y luego convierte el resultado a formato
     *               de respuesta. El Facade no debe hacer logica de negocio, solo
     *               coordinar.
     * Resultado esperado: Se obtiene el OrderResponse con el ID correcto, y tanto el
     *                     servicio como el mapper fueron invocados.
     */
    @Test
    @DisplayName("getById: delega al service y mapea a response")
    void getById_shouldDelegateToServiceAndMapResponse() {
        UUID id = sampleOrder.getId();
        when(orderService.findById(id)).thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        OrderResponse result = facade.getById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(orderService).findById(id);
        verify(orderMapper).toResponse(sampleOrder);
    }

    // ========== Tests de updateStatus ==========

    /*
     * Que se busca: Verificar que al recibir una peticion de cambio de estado, el
     *               Facade extrae el nuevo estado del DTO y lo pasa al servicio junto
     *               con el ID de la orden. Luego convierte el resultado.
     * Resultado esperado: Se retorna un OrderResponse valido, y el servicio recibio
     *                     el ID y el nuevo estado correctamente.
     */
    @Test
    @DisplayName("updateStatus: delega al service con id y nuevo status")
    void updateStatus_shouldDelegateToService() {
        UUID id = sampleOrder.getId();
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status(OrderStatus.IN_PROGRESS)
                .build();

        when(orderService.updateStatus(id, OrderStatus.IN_PROGRESS)).thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        OrderResponse result = facade.updateStatus(id, request);

        assertNotNull(result);
        verify(orderService).updateStatus(id, OrderStatus.IN_PROGRESS);
        verify(orderMapper).toResponse(sampleOrder);
    }

    // ========== Tests de search ==========

    /*
     * Que se busca: Verificar que al buscar ordenes con filtros (estacion y estado),
     *               el Facade pasa los filtros al servicio y convierte cada resultado
     *               de la pagina al formato de respuesta JSON.
     * Resultado esperado: Se obtiene una pagina con los resultados mapeados, y el
     *                     servicio recibio los filtros correctos.
     */
    @Test
    @DisplayName("search: delega al service con filtros y paginacion, mapea resultados")
    void search_shouldDelegateToServiceAndMapPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EstacionOrder> domainPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);

        when(orderService.search("ST-001", OrderStatus.CREATED, pageable))
                .thenReturn(domainPage);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        Page<OrderResponse> result = facade.search("ST-001", OrderStatus.CREATED, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(sampleResponse, result.getContent().get(0));
        verify(orderService).search("ST-001", OrderStatus.CREATED, pageable);
        verify(orderMapper).toResponse(sampleOrder);
    }

    /*
     * Que se busca: Verificar que cuando no se proporcionan filtros (ni estacion ni
     *               estado), el Facade pasa null como filtros al servicio, lo que
     *               significa "traer todo". Es el comportamiento por defecto.
     * Resultado esperado: La busqueda retorna resultados y el servicio fue llamado
     *                     con filtros null.
     */
    @Test
    @DisplayName("search: sin filtros delega correctamente")
    void search_withoutFilters_shouldDelegate() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<EstacionOrder> domainPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);

        when(orderService.search(null, null, pageable)).thenReturn(domainPage);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleResponse);

        Page<OrderResponse> result = facade.search(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(orderService).search(null, null, pageable);
    }
}
