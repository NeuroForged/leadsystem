# ----------- Stage 1: Build -----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom files first to leverage Docker layer caching

RUN chmod +x ./mvnw
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

# Copy the entire source
COPY src ./src

# Package the application
RUN ./mvnw clean package -DskipTests

# ----------- Stage 2: Runtime -----------
FROM eclipse-temurin:17-jdk-alpine

# Set a non-root user (optional for security)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy only the jar file from the builder stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (match your application.yml config if needed)
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
