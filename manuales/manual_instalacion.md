# Manual de Instalacion — Service Orders API

## 1. Descripcion General

**Service Orders API** es una API REST desarrollada en Java 21 con Spring Boot 3.4.4 para la gestion de ordenes de servicio de estaciones Terpel. Permite crear, consultar, filtrar y actualizar el estado de ordenes de servicio.

**Tecnologias principales:**

| Componente | Tecnologia | Version |
|---|---|---|
| Lenguaje | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.4 |
| Base de datos | PostgreSQL | 16 |
| Migraciones BD | Flyway | Incluida en Spring Boot |
| Documentacion API | Springdoc OpenAPI (Swagger UI) | 2.8.6 |
| Contenedores | Docker + Docker Compose | 29.x / v5.x |
| Tests | JUnit 5 + Mockito + Testcontainers | Incluidos |
| Cobertura | JaCoCo | 0.8.12 |

---

## 2. Prerequisitos

Antes de instalar, asegurate de tener instalado:

| Software | Version minima | Como verificar |
|---|---|---|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | v2.0+ | `docker compose version` |
| Git | 2.x | `git --version` |
| Make (opcional) | 3.x | `make --version` |

> **Nota:** NO es necesario instalar Java ni Maven. Todo se ejecuta dentro de contenedores Docker.

### Verificacion rapida de prerequisitos

```bash
docker --version
docker compose version
git --version
```

Los tres comandos deben responder con sus respectivas versiones sin errores.

---

## 3. Instalacion Paso a Paso

### Paso 1 — Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd terpel_back
```

### Paso 2 — Configurar variables de entorno

Copiar el archivo de ejemplo y ajustar si es necesario:

```bash
cp .env.example .env
```

**Contenido del archivo `.env`:**

| Variable | Descripcion | Valor por defecto |
|---|---|---|
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contrasena de la base de datos | `postgres` |
| `DB_PORT` | Puerto externo de PostgreSQL | `5432` |
| `APP_LOG_LEVEL` | Nivel de log (INFO, DEBUG, WARN) | `INFO` |
| `APP_PAGINATION_SIZE` | Tamano de pagina por defecto | `10` |

> **Seguridad:** En entornos productivos, cambia `DB_USERNAME` y `DB_PASSWORD` por credenciales seguras. Estas variables simulan el uso de **Secrets** de Kubernetes.

### Paso 3 — Levantar la aplicacion

**Opcion A: Con Make (recomendado)**

```bash
make init
```

Este comando:
1. Crea el `.env` si no existe
2. Construye la imagen Docker de la aplicacion
3. Levanta PostgreSQL y espera a que este saludable
4. Levanta la aplicacion Spring Boot
5. Muestra las URLs de acceso

**Opcion B: Con Docker Compose directamente**

```bash
docker compose up -d --build
```

### Paso 4 — Verificar que todo funciona

**a) Verificar que los contenedores estan corriendo:**

```bash
docker compose ps
```

Resultado esperado: dos servicios (`app` y `db`) con estado "running".

**b) Verificar la salud de la aplicacion:**

```bash
curl http://localhost:8080/actuator/health
```

Resultado esperado:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**c) Verificar que Swagger UI carga:**

Abrir en el navegador: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Debe mostrarse la interfaz de Swagger con los 4 endpoints documentados.

**d) Verificar la base de datos:**

```bash
docker compose exec db psql -U postgres -d serviceorders -c "\dt"
```

Debe mostrar la tabla `estacion_orders` y la tabla de Flyway.

---

## 4. URLs de Acceso

| Recurso | URL |
|---|---|
| API REST | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator Health | http://localhost:8080/actuator/health |
| Actuator Info | http://localhost:8080/actuator/info |
| PostgreSQL | localhost:5432 (usuario y contrasena segun `.env`) |

---

## 5. Comandos Utiles

| Comando | Descripcion |
|---|---|
| `make init` | Primera vez: configura y levanta todo |
| `make up` | Reconstruye y levanta los contenedores |
| `make stop` | Detiene los contenedores (conserva datos) |
| `make start` | Reinicia contenedores existentes |
| `make down` | Detiene y elimina contenedores y red |
| `make logs app` | Ver logs de la aplicacion en tiempo real |
| `make db` | Abrir consola de PostgreSQL |
| `make test` | Ejecutar todos los tests (43 tests) |
| `make test-unit` | Solo tests unitarios (34 tests) |
| `make test-integration` | Solo tests de integracion (9 tests) |
| `make ps` | Ver estado de los servicios |
| `make clean` | Limpiar artefactos de compilacion |

---

## 6. Estructura del Proyecto

```
terpel_back/
├── docker/
│   └── java/
│       └── Dockerfile              ← Imagen Docker multi-stage (build + runtime)
├── src/
│   ├── main/
│   │   ├── java/com/terpel/serviceorders/
│   │   │   ├── domain/             ← Logica de negocio (sin frameworks)
│   │   │   │   ├── model/          ← Entidad, enums
│   │   │   │   ├── exception/      ← Excepciones de dominio
│   │   │   │   ├── port/           ← Interfaces (puertos)
│   │   │   │   └── service/        ← Servicio de dominio + CalculateTotal
│   │   │   ├── application/        ← Facade (orquestacion)
│   │   │   └── infrastructure/     ← Adaptadores (REST, JPA, config)
│   │   └── resources/
│   │       ├── application.yml     ← Configuracion principal
│   │       ├── application-docker.yml
│   │       └── db/migration/       ← Scripts Flyway
│   └── test/                       ← Tests unitarios e integracion
├── docker-compose.yml              ← Orquestacion de servicios
├── Makefile                        ← Atajos de comandos
├── pom.xml                         ← Dependencias Maven
├── .env.example                    ← Plantilla de variables de entorno
└── .env                            ← Variables de entorno (no se sube al repo)
```

---

## 7. Arquitectura

La aplicacion usa **Arquitectura Hexagonal** (Ports & Adapters) con el **Patron Facade**:

```
Peticion HTTP
    ↓
OrderController         ← Solo maneja HTTP (codigos, headers)
    ↓
OrderFacade             ← Orquesta y convierte DTOs ↔ Dominio
    ↓
OrderService            ← Reglas de negocio puras
    ↓
OrderRepositoryPort     ← Interfaz (contrato)
    ↓
OrderRepositoryAdapter  ← Implementacion JPA
    ↓
PostgreSQL
```

**Beneficios:**
- El dominio no depende de ningun framework
- Tests ultrarapidos (milisegundos) sin levantar Spring
- Facil de extender (agregar Kafka, jobs, etc.)
- Principio SOLID aplicado en todo el codigo

---

## 8. Configuracion por Entornos

La aplicacion sigue el principio **"build once, deploy everywhere"** usando variables de entorno:

| Concepto Kubernetes | Variable en el proyecto | Ejemplo |
|---|---|---|
| **Secret** | `DB_USERNAME`, `DB_PASSWORD` | Credenciales de BD |
| **ConfigMap** | `APP_LOG_LEVEL`, `APP_PAGINATION_SIZE` | Configuracion no sensible |

Para cambiar la configuracion no hace falta recompilar, solo modificar el `.env` y reiniciar:

```bash
# Editar .env
make restart
```

---

## 9. Importar API en Postman

1. Con la aplicacion corriendo, descargar el JSON:
   ```bash
   curl http://localhost:8080/v3/api-docs -o docs/openapi.json
   ```
   (Ya esta incluido en `docs/openapi.json`)

2. Abrir Postman → **Import** → seleccionar `docs/openapi.json`

3. Se crea automaticamente una coleccion con los 4 endpoints listos para usar.

---

## 10. Ejecucion de Tests

### Todos los tests (43 tests)
```bash
make test
```

### Solo tests unitarios (34 tests, rapidos)
```bash
make test-unit
```

### Solo tests de integracion (9 tests, necesitan Docker)
```bash
make test-integration
```

### Reporte de cobertura (JaCoCo)
Despues de ejecutar los tests, el reporte se genera en:
```
target/site/jacoco/index.html
```

**Cobertura alcanzada:**

| Metrica | Porcentaje |
|---|---|
| Instrucciones | 94.2% |
| Lineas | 93.1% |
| Metodos | 94.7% |
| Clases | 100% |

---

## 11. Solucion de Problemas Comunes

| Problema | Causa | Solucion |
|---|---|---|
| Puerto 8080 ocupado | Otra aplicacion usa el puerto | Detener la otra app o cambiar `APP_PORT` en `.env` |
| Puerto 5432 ocupado | Otro PostgreSQL corriendo | Cambiar `DB_PORT` en `.env` |
| Docker compose falla | Docker no esta corriendo | Iniciar Docker Desktop o el servicio Docker |
| La app no arranca | BD no esta lista | Esperar a que `db` este "healthy": `docker compose ps` |
| Flyway falla | Schema inconsistente | `docker compose down -v` y levantar de nuevo |
| Tests de integracion fallan | Docker no disponible | Verificar que Docker esta corriendo |

---

## 12. Requisitos de Hardware

| Recurso | Minimo | Recomendado |
|---|---|---|
| RAM | 4 GB | 8 GB |
| Disco | 2 GB libres | 5 GB libres |
| CPU | 2 nucleos | 4 nucleos |
| SO | Linux, macOS, Windows (con Docker Desktop) | Linux |
