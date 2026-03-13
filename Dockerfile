# Etapa 1: Compilación (Build)
# Utilizamos una imagen de Maven con Eclipse Temurin (JDK 17) para compilar el código.
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Establecemos el directorio de trabajo dentro del contenedor
WORKDIR /app

# Primero copiamos el pom.xml y descargamos las dependencias
# (Aprovechamos la caché de Docker para no tener que bajarlas de nuevo si el POM no ha cambiado)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el resto del código fuente
COPY src/ ./src/

# Compilamos el proyecto, saltándonos las pruebas para agilizar el proceso
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------

# Etapa 2: Ejecución (Run)
# Empleamos una imagen muchísimo más ligera que solo contiene el JRE (Runtime) y no las herramientas de desarrollo.
FROM eclipse-temurin:17-jre-alpine

# Creamos un usuario sin privilegios de root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copiamos de la "Etapa 1" únicamente el archivo JAR generado
COPY --from=builder /app/target/microservicio-notificaciones-app.jar app.jar

# Informamos el puerto sobre el que escucha la aplicación (Documental, pero buena práctica)
EXPOSE 8085

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]