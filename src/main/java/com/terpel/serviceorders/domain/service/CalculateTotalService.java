package com.terpel.serviceorders.domain.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Servicio utilitario que calcula el total de una lista de valores BigDecimal.
 *
 * <p>Esta clase corrige el bug clasico de BigDecimal donde se usa
 * {@code total.add(item)} sin reasignar el resultado. BigDecimal es
 * <strong>inmutable</strong>: {@code add()} retorna un nuevo objeto
 * y no modifica el original.</p>
 *
 * <h3>Codigo con bug (original):</h3>
 * <pre>{@code
 * public BigDecimal calculateTotal(List<BigDecimal> items) {
 *     BigDecimal total = BigDecimal.ZERO;
 *     for (BigDecimal item : items) {
 *         total.add(item);  // BUG: no reasigna, total siempre es ZERO
 *     }
 *     return total;
 * }
 * }</pre>
 *
 * <h3>Correccion:</h3>
 * <p>Usar {@code Stream.reduce} que acumula correctamente, con filtro
 * de nulos para robustez.</p>
 */
public class CalculateTotalService {

    /**
     * Calcula la suma de una lista de valores BigDecimal.
     * Maneja lista nula, lista vacia y elementos nulos dentro de la lista.
     *
     * @param items lista de valores a sumar (puede ser null o contener nulos)
     * @return la suma total, o {@link BigDecimal#ZERO} si la lista es nula o vacia
     */
    public BigDecimal calculateTotal(List<BigDecimal> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
