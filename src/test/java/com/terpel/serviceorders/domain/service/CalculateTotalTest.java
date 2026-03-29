package com.terpel.serviceorders.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests para el bug de BigDecimal (Seccion 3 de la prueba tecnica).
 *
 * Estos tests comprueban que la suma de valores monetarios funciona correctamente.
 * El bug original era que BigDecimal es "inmutable" (no cambia al sumar), asi que
 * el resultado siempre daba cero. La correccion usa Stream.reduce para acumular
 * la suma correctamente.
 */
@Tag("unit")
class CalculateTotalTest {

    private final CalculateTotalService service = new CalculateTotalService();

    /*
     * Que se busca: Verificar el caso mas comun — sumar una lista de valores normales.
     *               Si esto falla, significa que el bug original sigue presente.
     * Resultado esperado: La suma de 10.50 + 20.30 + 5.20 debe dar exactamente 36.00.
     */
    @Test
    @DisplayName("Lista con valores normales [10.50, 20.30, 5.20] → suma 36.00")
    void calculateTotal_withNormalValues_shouldReturnCorrectSum() {
        List<BigDecimal> items = List.of(
                new BigDecimal("10.50"),
                new BigDecimal("20.30"),
                new BigDecimal("5.20")
        );

        BigDecimal result = service.calculateTotal(items);

        assertEquals(new BigDecimal("36.00"), result);
    }

    /*
     * Que se busca: Verificar que si la lista de valores esta vacia (no hay nada que
     *               sumar), el resultado es cero y no se produce un error.
     * Resultado esperado: Se retorna BigDecimal.ZERO (cero).
     */
    @Test
    @DisplayName("Lista vacia → retorna BigDecimal.ZERO")
    void calculateTotal_withEmptyList_shouldReturnZero() {
        BigDecimal result = service.calculateTotal(Collections.emptyList());

        assertEquals(BigDecimal.ZERO, result);
    }

    /*
     * Que se busca: Verificar que si la lista es null (no se proporciono), el metodo
     *               no falla con un error, sino que retorna cero de forma segura.
     * Resultado esperado: Se retorna BigDecimal.ZERO sin lanzar NullPointerException.
     */
    @Test
    @DisplayName("Lista nula → retorna BigDecimal.ZERO")
    void calculateTotal_withNullList_shouldReturnZero() {
        BigDecimal result = service.calculateTotal(null);

        assertEquals(BigDecimal.ZERO, result);
    }

    /*
     * Que se busca: Verificar que si la lista contiene valores null mezclados con
     *               numeros validos, los null se ignoran y solo se suman los numeros
     *               reales. Esto previene errores en datos inconsistentes.
     * Resultado esperado: La suma de 10 + 20 = 30 (los dos null se ignoran).
     */
    @Test
    @DisplayName("Lista con nulos mezclados [10, null, 20, null] → ignora nulos, retorna 30")
    void calculateTotal_withMixedNulls_shouldIgnoreNullsAndSum() {
        List<BigDecimal> items = Arrays.asList(
                new BigDecimal("10"),
                null,
                new BigDecimal("20"),
                null
        );

        BigDecimal result = service.calculateTotal(items);

        assertEquals(new BigDecimal("30"), result);
    }

    /*
     * Que se busca: Verificar que si la lista tiene un solo elemento, el resultado
     *               es ese mismo valor. Es un caso limite simple pero importante.
     * Resultado esperado: Se retorna 42.50 (el unico elemento de la lista).
     */
    @Test
    @DisplayName("Lista con un solo elemento [42.50] → retorna 42.50")
    void calculateTotal_withSingleElement_shouldReturnThatElement() {
        List<BigDecimal> items = List.of(new BigDecimal("42.50"));

        BigDecimal result = service.calculateTotal(items);

        assertEquals(new BigDecimal("42.50"), result);
    }
}
