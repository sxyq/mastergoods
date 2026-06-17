# 154 Server Deployment

This directory contains the Docker Compose deployment for the backend on `154.217.241.207`.

## Layout

- `docker-compose.yml`: PostgreSQL, Redis, and Spring Boot backend
- `.env.example`: environment variable template

## Server-side usage

Copy the project subset to the server, then run:

```bash
cd /opt/zhihuiji-backend/deploy/154
cp .env.example .env
docker compose up --build -d
```

Health check:

```bash
curl http://127.0.0.1:18080/v1/sync/health
```

Public endpoint:

```text
http://154.217.241.207:18080
```
