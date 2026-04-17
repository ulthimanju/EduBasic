# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_EXAM_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_EXAM_API_BASE_URL=${VITE_EXAM_API_BASE_URL}

COPY package*.json ./
# Deterministic, lockfile-based install for CI/container builds
RUN npm ci

COPY . .
RUN npm run build

# ─── Stage 2: Runtime (Nginx) ──────────────────────────────────────────────────
FROM nginx:alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets only
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
