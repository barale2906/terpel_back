# Manual de Usuario — Service Orders API

## 1. Introduccion

La **Service Orders API** permite gestionar ordenes de servicio para estaciones Terpel. Con esta API puedes:

- **Crear** nuevas ordenes de servicio
- **Consultar** una orden especifica por su ID
- **Buscar** ordenes filtrando por estacion y/o estado, con paginacion
- **Cambiar el estado** de una orden, respetando las reglas de negocio

---

## 2. Acceso a la API

| Recurso | URL |
|---|---|
| Base de la API | `http://localhost:8080` |
| Swagger UI (interfaz visual) | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON (para Postman) | `http://localhost:8080/v3/api-docs` |

### Swagger UI

Swagger UI es una interfaz web interactiva donde puedes ver todos los endpoints, sus parametros, y probarlos directamente desde el navegador sin necesidad de herramientas externas.

### Importar en Postman

1. Descargar `openapi.json` (incluido en el proyecto) o acceder a `http://localhost:8080/v3/api-docs`
2. En Postman: **Import** → seleccionar el archivo JSON
3. Se crea una coleccion con los 4 endpoints configurados

---

## 3. Entidad: Orden de Servicio (EstacionOrder)

Cada orden de servicio tiene los siguientes campos:

| Campo | Tipo | Obligatorio | Descripcion |
|---|---|---|---|
| `id` | UUID | Autogenerado | Identificador unico de la orden |
| `stationId` | String | Si | Identificador de la estacion (ej: "ST-001") |
| `type` | Enum | Si | Tipo de orden |
| `description` | String | No | Descripcion libre de la orden |
| `status` | Enum | Si | Estado actual de la orden |
| `createdAt` | Fecha/hora | Autogenerado | Cuando se creo la orden |
| `updatedAt` | Fecha/hora | Autogenerado | Ultima vez que se modifico |

### Tipos de orden (`type`)

| Valor | Significado |
|---|---|
| `INVOICE` | Facturacion |
| `SUPPORT` | Soporte tecnico |
| `REDEMPTION` | Redencion de puntos |

### Estados posibles (`status`)

| Valor | Significado |
|---|---|
| `CREATED` | Orden recien creada |
| `IN_PROGRESS` | Orden en proceso de atencion |
| `DONE` | Orden completada |
| `CANCELLED` | Orden cancelada (irreversible) |

---

## 4. Reglas de Negocio para Cambio de Estado

No todos los cambios de estado estan permitidos. Las reglas son:

### Transiciones permitidas

```
CREATED ──────→ IN_PROGRESS
CREATED ──────→ DONE
CREATED ──────→ CANCELLED
IN_PROGRESS ──→ DONE
IN_PROGRESS ──→ CANCELLED
```

### Transiciones prohibidas

| Desde | Hacia | Razon |
|---|---|---|
| `DONE` | `IN_PROGRESS` | Una orden terminada no puede volver a "en progreso" |
| `DONE` | `CREATED` | Una orden terminada no puede reiniciarse |
| `CANCELLED` | Cualquiera | Una orden cancelada no acepta ningun cambio |

### Diagrama visual de estados

```
                    ┌──────────────┐
                    │   CREATED    │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ↓            ↓            ↓
     ┌────────────┐  ┌──────────┐  ┌───────────┐
     │IN_PROGRESS │  │   DONE   │  │ CANCELLED │
     └─────┬──────┘  └──────────┘  └───────────┘
           │              ↑
           ├──────────────┘
           │
           ↓
     ┌───────────┐
     │ CANCELLED │
     └───────────┘
```

---

## 5. Endpoints de la API

### 5.1 Crear una Orden de Servicio

**`POST /service-orders`**

Crea una nueva orden. Los campos `id`, `createdAt` y `updatedAt` se generan automaticamente.

**Cuerpo de la peticion (JSON):**

```json
{
  "stationId": "ST-001",
  "type": "INVOICE",
  "status": "CREATED",
  "description": "Factura de combustible diesel"
}
```

| Campo | Obligatorio | Valores validos |
|---|---|---|
| `stationId` | Si | Cualquier texto (ej: "ST-001") |
| `type` | Si | `INVOICE`, `SUPPORT`, `REDEMPTION` |
| `status` | Si | `CREATED`, `IN_PROGRESS`, `DONE`, `CANCELLED` |
| `description` | No | Texto libre o se omite |

**Respuesta exitosa (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "stationId": "ST-001",
  "type": "INVOICE",
  "description": "Factura de combustible diesel",
  "status": "CREATED",
  "createdAt": "2026-03-29T14:30:00",
  "updatedAt": "2026-03-29T14:30:00"
}
```

**Errores posibles:**

| Codigo | Cuando ocurre | Ejemplo |
|---|---|---|
| 400 | Faltan campos obligatorios | Enviar sin `stationId` |
| 400 | Tipo o estado invalido | Enviar `type: "OTRO"` |

**Ejemplo con curl:**

```bash
curl -X POST http://localhost:8080/service-orders \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "ST-001",
    "type": "INVOICE",
    "status": "CREATED",
    "description": "Factura de combustible"
  }'
```

---

### 5.2 Consultar una Orden por ID

**`GET /service-orders/{id}`**

Busca una orden por su identificador unico (UUID).

**Parametro de ruta:**

| Parametro | Tipo | Ejemplo |
|---|---|---|
| `id` | UUID | `550e8400-e29b-41d4-a716-446655440000` |

**Respuesta exitosa (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "stationId": "ST-001",
  "type": "INVOICE",
  "description": "Factura de combustible",
  "status": "CREATED",
  "createdAt": "2026-03-29T14:30:00",
  "updatedAt": "2026-03-29T14:30:00"
}
```

**Errores posibles:**

| Codigo | Cuando ocurre |
|---|---|
| 404 | El ID no existe en la base de datos |

**Ejemplo con curl:**

```bash
curl http://localhost:8080/service-orders/550e8400-e29b-41d4-a716-446655440000
```

---

### 5.3 Buscar Ordenes con Filtros

**`GET /service-orders`**

Busca ordenes usando filtros opcionales y paginacion. Si no se proporcionan filtros, retorna todas las ordenes.

**Parametros de consulta (query params):**

| Parametro | Obligatorio | Tipo | Descripcion | Ejemplo |
|---|---|---|---|---|
| `stationId` | No | String | Filtrar por estacion | `ST-001` |
| `status` | No | Enum | Filtrar por estado | `CREATED` |
| `page` | No | Numero | Pagina (empieza en 0) | `0` |
| `size` | No | Numero | Registros por pagina | `10` |

**Combinaciones de filtros:**

| Filtros usados | Resultado |
|---|---|
| Ninguno | Todas las ordenes |
| Solo `stationId` | Ordenes de esa estacion |
| Solo `status` | Ordenes en ese estado |
| `stationId` + `status` | Ordenes de esa estacion en ese estado |

**Respuesta exitosa (200 OK):**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "stationId": "ST-001",
      "type": "INVOICE",
      "description": "Factura de combustible",
      "status": "CREATED",
      "createdAt": "2026-03-29T14:30:00",
      "updatedAt": "2026-03-29T14:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true
}
```

| Campo de paginacion | Significado |
|---|---|
| `content` | Lista de ordenes en esta pagina |
| `totalElements` | Total de ordenes que coinciden |
| `totalPages` | Total de paginas disponibles |
| `size` | Tamano de pagina solicitado |
| `number` | Numero de pagina actual (0-indexed) |
| `first` / `last` | Si es la primera / ultima pagina |

**Ejemplos con curl:**

```bash
# Todas las ordenes (pagina 0, 10 por pagina)
curl "http://localhost:8080/service-orders?page=0&size=10"

# Filtrar por estacion
curl "http://localhost:8080/service-orders?stationId=ST-001"

# Filtrar por estado
curl "http://localhost:8080/service-orders?status=CREATED"

# Filtrar por estacion Y estado
curl "http://localhost:8080/service-orders?stationId=ST-001&status=IN_PROGRESS"

# Segunda pagina, 5 resultados por pagina
curl "http://localhost:8080/service-orders?page=1&size=5"
```

---

### 5.4 Actualizar Estado de una Orden

**`PATCH /service-orders/{id}/status`**

Cambia el estado de una orden existente. Se aplican las reglas de transicion de negocio (ver seccion 4).

**Parametro de ruta:**

| Parametro | Tipo | Ejemplo |
|---|---|---|
| `id` | UUID | `550e8400-e29b-41d4-a716-446655440000` |

**Cuerpo de la peticion (JSON):**

```json
{
  "status": "IN_PROGRESS"
}
```

**Respuesta exitosa (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "stationId": "ST-001",
  "type": "INVOICE",
  "description": "Factura de combustible",
  "status": "IN_PROGRESS",
  "createdAt": "2026-03-29T14:30:00",
  "updatedAt": "2026-03-29T14:35:00"
}
```

Nota: `updatedAt` se actualiza automaticamente al cambiar el estado.

**Errores posibles:**

| Codigo | Cuando ocurre | Mensaje |
|---|---|---|
| 404 | El ID no existe | "Orden no encontrada" |
| 409 | Transicion de estado no permitida | "Transicion de estado no permitida: DONE -> IN_PROGRESS" |

**Ejemplos con curl:**

```bash
# Cambio valido: CREATED → IN_PROGRESS
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# Cambio valido: IN_PROGRESS → DONE
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'

# Cambio INVALIDO: DONE → IN_PROGRESS (retorna 409)
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

---

## 6. Codigos de Respuesta HTTP

| Codigo | Nombre | Cuando se usa |
|---|---|---|
| 200 | OK | Consulta o actualizacion exitosa |
| 201 | Created | Orden creada exitosamente |
| 400 | Bad Request | Error de validacion (campos faltantes o invalidos) |
| 404 | Not Found | La orden no existe en la base de datos |
| 409 | Conflict | Transicion de estado no permitida por las reglas de negocio |
| 500 | Internal Server Error | Error inesperado del servidor |

### Formato de errores (Problem Details - RFC 7807)

Todos los errores siguen el estandar RFC 7807:

```json
{
  "type": "https://api.terpel.com/errors/order-not-found",
  "title": "Orden no encontrada",
  "status": 404,
  "detail": "Orden de servicio no encontrada con id: 550e8400-..."
}
```

---

## 7. Correlation ID (Trazabilidad)

Cada peticion recibe un identificador unico llamado **Correlation ID** que permite rastrear la operacion en los logs del servidor.

- Se genera automaticamente si no se envia
- Se puede enviar uno propio con el header `X-Correlation-Id`
- Siempre se retorna en la respuesta como header `X-Correlation-Id`

**Ejemplo:**

```bash
# Enviar un Correlation ID propio
curl -H "X-Correlation-Id: mi-trace-123" http://localhost:8080/service-orders

# El response incluira:
# X-Correlation-Id: mi-trace-123
```

Esto es util para depurar problemas: si algo falla, el Correlation ID permite buscar en los logs del servidor exactamente que paso con esa peticion.

---

## 8. Monitoreo (Actuator)

La aplicacion expone endpoints de monitoreo:

### Health (salud del sistema)

```bash
curl http://localhost:8080/actuator/health
```

Muestra si la aplicacion y la base de datos estan funcionando correctamente.

### Info (informacion de la aplicacion)

```bash
curl http://localhost:8080/actuator/info
```

Muestra nombre, descripcion y version de la API.

---

## 9. Flujo de Uso Tipico

A continuacion un ejemplo completo del ciclo de vida de una orden:

### Paso 1: Crear una orden

```bash
curl -X POST http://localhost:8080/service-orders \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "ST-BOGOTA-01",
    "type": "SUPPORT",
    "status": "CREATED",
    "description": "Revision del dispensador #3"
  }'
```

Respuesta: `201 Created` con el `id` de la orden (guardar este ID).

### Paso 2: Consultar la orden

```bash
curl http://localhost:8080/service-orders/{id}
```

Respuesta: `200 OK` con todos los datos de la orden.

### Paso 3: Pasar a "En Progreso"

```bash
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

Respuesta: `200 OK`, estado actualizado a `IN_PROGRESS`.

### Paso 4: Completar la orden

```bash
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'
```

Respuesta: `200 OK`, estado actualizado a `DONE`.

### Paso 5: Verificar que no se puede reabrir

```bash
curl -X PATCH http://localhost:8080/service-orders/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

Respuesta: `409 Conflict` — "Transicion de estado no permitida: DONE -> IN_PROGRESS".

---

## 10. Busquedas Avanzadas

### Ver todas las ordenes de una estacion

```bash
curl "http://localhost:8080/service-orders?stationId=ST-BOGOTA-01"
```

### Ver ordenes pendientes de todas las estaciones

```bash
curl "http://localhost:8080/service-orders?status=CREATED"
```

### Paginacion: navegar por resultados grandes

```bash
# Pagina 1 (primeros 5 resultados)
curl "http://localhost:8080/service-orders?page=0&size=5"

# Pagina 2 (siguientes 5 resultados)
curl "http://localhost:8080/service-orders?page=1&size=5"
```

Los resultados se ordenan por fecha de creacion (mas recientes primero).
