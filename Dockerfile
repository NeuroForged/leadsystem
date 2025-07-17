#Stage 0: Environment Variables
ENV SPRING_PROFILES_ACTIVE=prod

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment port (optional but helpful for Render)
ENV PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
