# EduBasic Management Makefile

.PHONY: help up down restart logs ps build clean rebuild shell-auth shell-exam shell-db

# Default target: show help
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  up         Start all services in background"
	@echo "  down       Stop and remove containers"
	@echo "  restart    Restart all services"
	@echo "  logs       Tail all service logs"
	@echo "  ps         List running containers and health status"
	@echo "  build      Build or rebuild services"
	@echo "  rebuild    Force rebuild and start services"
	@echo "  clean      Stop containers and WIPE all volumes (fresh start)"
	@echo "  shell-auth Open a shell in the auth service"
	@echo "  shell-exam Open a shell in the exam service"

up:
	docker compose up -d

down:
	docker compose down

restart:
	docker compose restart

logs:
	docker compose logs -f

ps:
	docker compose ps

build:
	docker compose build

rebuild:
	docker compose up -d --build

clean:
	docker compose down -v --remove-orphans

# Shorthand for debugging service containers
shell-auth:
	docker compose exec auth sh

shell-exam:
	docker compose exec exam sh

shell-db:
	docker compose exec postgres psql -U postgres -d exam_db
