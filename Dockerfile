# 1. ETAPA DE CONSTRUCCIÓN (BUILD)
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Copiamos archivos del wrapper y pom.xml primero para aprovechar caché
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Damos permisos de ejecución al wrapper
RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw

# Descargamos dependencias (esto se cacheará si no cambia el pom.xml)
RUN ./mvnw dependency:go-offline

# Copiamos el código fuente
COPY src ./src

# Empaquetamos la aplicación
RUN ./mvnw clean package -DskipTests

# 2. ETAPA DE EJECUCIÓN (PRODUCTION)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Exponemos el puerto
EXPOSE 8082

# Copiamos el JAR generado
COPY --from=build /app/target/*.jar app.jar

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]