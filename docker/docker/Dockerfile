# Multi-stage build per ottimizzare dimensioni immagine
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copia file di configurazione Maven
COPY pom.xml .
COPY src ./src

# Build dell'applicazione
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -g 1001 -S etfpac && \
    adduser -u 1001 -S etfpac -G etfpac

WORKDIR /app

# Installa dipendenze di sistema necessarie
RUN apk add --no-cache curl

# Copia JAR dall'immagine builder
COPY --from=builder /app/target/etf-pac-simulator-backend-1.0.0.jar app.jar

# Crea directory per logs
RUN mkdir -p /app/logs && \
    chown -R etfpac:etfpac /app

USER etfpac

# Configurazione JVM
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]