# ----------- Stage 1: Build -----------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy build config first for better caching
COPY pom.xml mvnw ./
RUN chmod +x ./mvnw
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

# Copy source files
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# ----------- Stage 2: Runtime -----------
FROM eclipse-temurin:21-jdk-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy only the built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
