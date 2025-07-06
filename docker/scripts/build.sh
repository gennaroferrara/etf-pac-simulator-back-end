#!/bin/bash

echo "Costruzione immagine Docker per Spring Boot..."
#./mvnw clean package -DskipTests
#docker compose down -v --remove-orphans
#docker compose up --build --force-recreate
docker-compose build