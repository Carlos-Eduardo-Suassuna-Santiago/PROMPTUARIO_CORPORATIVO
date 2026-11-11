# =============================================================================
# Stage 1 (Build): compila o backup-service com Maven + Temurin 21 Alpine
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copia apenas o pom.xml primeiro para cachear dependências
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copia o código-fonte e compila
COPY src ./src
RUN mvn -q -B package -DskipTests

# =============================================================================
# Stage 2 (Runtime): JRE Temurin 21 Alpine + ferramentas nativas de dump
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Instala ferramentas nativas de dump e utilitários essenciais
RUN apk add --no-cache \
        postgresql15-client \
        mongodb-tools \
        bash \
        tzdata \
    && rm -rf /var/cache/apk/*

# Cria usuário não-root para execução (onde viável)
RUN addgroup -S backup 2>/dev/null || true \
    && adduser -S backup -G backup -s /bin/bash 2>/dev/null || true

WORKDIR /app

# Copia o JAR executável do stage de build
COPY --from=build /build/target/backup-service-*.jar app.jar

# Cria o ponto de montagem persistente para backups
RUN mkdir -p /var/backups \
    && chown -R backup:backup /var/backups /app 2>/dev/null || true

VOLUME ["/var/backups"]

# Usuário não-root (com fallback para root se não conseguir criar)
USER backup

# Entrypoint otimizado para contêiner
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
