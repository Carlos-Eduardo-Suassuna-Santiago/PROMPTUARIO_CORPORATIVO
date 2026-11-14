.PHONY: help up down logs build restart test lint clean seed backup backup-once restore-db restore-mongo status \
	dev-ai test-ai dev-patient-clinical test-patient-clinical

# ─── Config ───────────────────────────────────────────────────────────────────
COMPOSE = docker compose
SERVICES = gateway iam-service patient-clinical-service ai-service reporting-service reporting-worker
TEST_JWT_SECRET_KEY ?= test-only-secret-key-with-at-least-32-characters

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ─── Infrastructure ───────────────────────────────────────────────────────────

infra-up: ## Start only infrastructure (DBs, RabbitMQ, Redis, MinIO)
	$(COMPOSE) up -d db-iam db-patient-clinical db-reporting db-ai \
		redis rabbitmq minio
	@echo "⏳ Waiting for services to be healthy..."
	@sleep 8

infra-down: ## Stop infrastructure
	$(COMPOSE) stop db-iam db-patient-clinical db-reporting db-ai \
		redis rabbitmq minio

# ─── Application ──────────────────────────────────────────────────────────────

up: ## Start all services
	cp -n .env.example .env 2>/dev/null || true
	$(COMPOSE) up -d
	@echo ""
	@echo "✅ PROMPTUARIO Backend running:"
	@echo "   Gateway:   http://localhost:8000"
	@echo "   Docs:      http://localhost:8000/docs"
	@echo "   IAM:       http://localhost:8001/docs"
	@echo "   Patient-Clinical: http://localhost:8002/docs (pacientes, consultas, prontuarios, prescricoes)"
	@echo "   AI:        http://localhost:8004/docs"
	@echo "   Reporting: http://localhost:8005/docs"
	@echo "   RabbitMQ:  http://localhost:15672 (promptuario/promptuario_pass)"
	@echo "   MinIO:     http://localhost:9001 (promptuario/promptuario_pass)"

down: ## Stop all services
	$(COMPOSE) down

restart: ## Restart all app services (not infra)
	$(COMPOSE) restart $(SERVICES)

build: ## Build all service images
	$(COMPOSE) build --parallel

build-service: ## Build a specific service: make build-service SVC=iam-service
	$(COMPOSE) build $(SVC)

logs: ## Tail logs of all services
	$(COMPOSE) logs -f --tail=100

logs-svc: ## Tail logs of a specific service: make logs-svc SVC=iam-service
	$(COMPOSE) logs -f --tail=100 $(SVC)

# ─── Development ──────────────────────────────────────────────────────────────

dev-iam: ## Run IAM service locally (no Docker)
	cd iam-service && PYTHONPATH=/home/$(USER)/promptuario-backend \
		uvicorn app.main:app --reload --port 8001

dev-ai: ## Build and run the Python AI service with its Compose dependencies
	$(COMPOSE) up --build ai-service

dev-patient-clinical: ## Build and run the Java service with its internal Spring AMQP worker
	$(COMPOSE) up --build patient-clinical-service

dev-gateway: ## Run Gateway locally (no Docker)
	cd gateway && PYTHONPATH=/home/$(USER)/promptuario-backend \
		uvicorn app.main:app --reload --port 8000

# ─── Testing ──────────────────────────────────────────────────────────────────

test: ## Run Java IAM, Java Patient-Clinical, and Python AI tests
	cd iam-service && mvn clean test
	$(MAKE) test-patient-clinical
	$(MAKE) test-ai

test-svc: ## Test a specific Java service: make test-svc SVC=iam-service
	cd $(SVC) && mvn clean test

test-ai: ## Run the Python AI service tests
	cd ai-service && JWT_SECRET_KEY=$(TEST_JWT_SECRET_KEY) PYTHONPATH=.. \
		python -m pytest tests/ -m "not integration" -o asyncio_mode=auto -v --tb=short

test-patient-clinical: ## Run the Java Patient-Clinical tests (includes Spring AMQP worker context)
	cd patient-clinical-service && mvn clean test

smoke-auth: ## Run the terminal smoke test for auth flow
	python scripts/insomnia_auth_smoke.py

smoke-fastapi: ## Run full FastAPI interface smoke test across all services
	python scripts/fastapi_services_smoke.py

# ─── Database ─────────────────────────────────────────────────────────────────

db-shell-iam: ## psql into IAM DB
	$(COMPOSE) exec db-iam psql -U iam -d iam_db

db-shell-patient-clinical: ## psql into Patient-Clinical DB
	$(COMPOSE) exec db-patient-clinical psql -U clinical -d patient_clinical_db

db-shell-reporting: ## psql into Reporting DB
	$(COMPOSE) exec db-reporting psql -U reporting -d reporting_db

# ─── Utilities ────────────────────────────────────────────────────────────────

health: ## Check health of all services
	@echo "Gateway:  " && curl -sf http://localhost:8000/healthz | python3 -m json.tool
	@echo "IAM:      " && curl -sf http://localhost:8001/healthz | python3 -m json.tool
	@echo "Patient-Clinical: " && curl -sf http://localhost:8002/healthz | python3 -m json.tool
	@echo "AI:       " && curl -sf http://localhost:8004/healthz | python3 -m json.tool
	@echo "Reporting:" && curl -sf http://localhost:8005/healthz | python3 -m json.tool

login: ## Quick login test (returns JWT)
	@curl -s -X POST http://localhost:8000/api/v1/auth/login \
		-H "Content-Type: application/json" \
		-d '{"email":"admin@promptuario.health","password":"Admin@12345"}' \
		| python3 -m json.tool

seed: ## Populate database with realistic simulated data (500 users)
	python scripts/seed_db.py

backup: ## Start the backup service (scheduled mode)
	$(COMPOSE) up -d backup-service

backup-once: ## Run a single backup cycle immediately
	$(COMPOSE) run --rm backup-service python backup_runner.py --once

restore-db: ## Restore PostgreSQL from a local backup file. Usage: make restore-db FILE=path/to/file.sql.gz TARGET=iam_db
	$(COMPOSE) run --rm backup-service python backup_runner.py --restore --kind postgres --file $(FILE) --target $(TARGET)

restore-mongo: ## Restore MongoDB from a local backup archive. Usage: make restore-mongo FILE=path/to/file.archive.gz TARGET=ai_db
	$(COMPOSE) run --rm backup-service python backup_runner.py --restore --kind mongo --file $(FILE) --target $(TARGET)

status: ## Show the latest backup status report
	@$(COMPOSE) exec -T backup-service sh -lc 'cat /var/backups/status.json 2>/dev/null || echo "Nenhum status disponível"'

clean: ## Remove containers, volumes, and built images
	$(COMPOSE) down -v --rmi local
	@echo "🧹 Cleaned up"

ps: ## Show running containers
	$(COMPOSE) ps
