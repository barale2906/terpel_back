-- ============================================================================
-- V1: Tabla principal de ordenes de servicio de estaciones
-- Almacena las ordenes con su estado, tipo y timestamps de auditoria.
-- ============================================================================

CREATE TABLE estacion_orders (
    id          UUID            PRIMARY KEY,
    station_id  VARCHAR(50)     NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    description TEXT,
    status      VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL
);

-- Indice compuesto para optimizar las consultas con filtros por stationId y status
CREATE INDEX idx_estacion_orders_station_status
    ON estacion_orders (station_id, status);
