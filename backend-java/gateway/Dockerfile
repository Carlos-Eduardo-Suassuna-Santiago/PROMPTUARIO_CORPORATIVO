FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl tzdata \
    && addgroup -S gatewaygroup \
    && adduser -S -G gatewaygroup gatewayuser

WORKDIR /app
COPY --from=builder /workspace/target/gateway-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown gatewayuser:gatewaygroup /app/app.jar

USER gatewayuser

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dio.netty.allocator.type=pooled"

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD curl -f http://localhost:8000/healthz || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]