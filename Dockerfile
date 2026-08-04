# ETAPA 1: Compilación (Maven + Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el pom.xml para descargar y cachear las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y compilamos sin ejecutar pruebas
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Imagen ligera con JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el archivo JAR compilado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto por defecto de Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]