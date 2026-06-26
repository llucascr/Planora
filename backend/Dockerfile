# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia primeiro os arquivos de build para aproveitar o cache de dependencias.
# As dependencias so sao baixadas novamente quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

# Copia o codigo-fonte (inclui src/main/resources/app.pub e app.key) e empacota.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario nao-root para executar a aplicacao.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN chown spring:spring app.jar

USER spring

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
