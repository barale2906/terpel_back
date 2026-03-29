.PHONY: up down start stop restart ps logs app db build test test-unit test-integration clean init show-urls

up:
	@echo "=> Levantando contenedores (build incluido)..."
	docker compose up -d --build
	@$(MAKE) show-urls

init:
	@echo "=> Inicializando proyecto por primera vez..."
	@if [ ! -f .env ]; then cp .env.example .env && echo "=> .env creado desde .env.example"; fi
	@$(MAKE) up
	@echo "=> Proyecto listo."
	@$(MAKE) show-urls

down:
	@echo "=> Deteniendo y eliminando contenedores/red..."
	docker compose down

stop:
	@echo "=> Deteniendo contenedores (sin borrar datos)..."
	docker compose stop

start:
	@echo "=> Iniciando contenedores existentes..."
	docker compose start
	@$(MAKE) show-urls

restart:
	@echo "=> Reiniciando contenedores..."
	docker compose restart
	@$(MAKE) show-urls

ps:
	@echo "=> Estado de servicios:"
	docker compose ps

logs:
	docker compose logs -f $(filter-out $@,$(MAKECMDGOALS))

app:
	docker compose exec app sh

db:
	docker compose exec db psql -U postgres -d serviceorders

build:
	@echo "=> Construyendo proyecto (sin tests)..."
	docker compose run --rm app sh -c "cd /app && mvn clean package -DskipTests -B"

test:
	@echo "=> Ejecutando todos los tests..."
	docker compose run --rm app sh -c "cd /app && mvn test -B"

test-unit:
	@echo "=> Ejecutando tests unitarios..."
	docker compose run --rm app sh -c "cd /app && mvn test -Dgroups=unit -B"

test-integration:
	@echo "=> Ejecutando tests de integracion..."
	docker compose run --rm app sh -c "cd /app && mvn test -Dgroups=integration -B"

clean:
	@echo "=> Limpiando artefactos de build..."
	docker compose run --rm app sh -c "cd /app && mvn clean -B"

show-urls:
	@echo ""
	@echo "=> Accesos:"
	@echo "   API:         http://localhost:8080"
	@echo "   Swagger UI:  http://localhost:8080/swagger-ui.html"
	@echo "   Actuator:    http://localhost:8080/actuator/health"
	@echo "   PostgreSQL:  localhost:5432 (postgres/postgres, db: serviceorders)"
	@echo ""

%:
	@:
