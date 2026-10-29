# Контекст сборки — корень репозитория (см. deploy/docker-compose.yml).

# ---- Сборка фронтенда ----
FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# VITE_API_URL не задаём: запросы идут на тот же домен, а Caddy проксирует /auth и /v1 на бэкенд
RUN npm run build

# ---- Веб-сервер ----
FROM caddy:2-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv
