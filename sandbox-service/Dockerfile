# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# 1. Copy POM first for dependency caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 2. Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B && \
    rm -f target/*-sources.jar target/*-javadoc.jar

# ─── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install tini for proper signal handling and su-exec for dropping privileges
RUN apk add --no-cache tini su-exec
WORKDIR /app

# Non-root user for security (entrypoint will drop to this)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/app.jar app.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

# Container-friendly JVM defaults (override at runtime as needed)
ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["/sbin/tini", "--", "./entrypoint.sh"]

