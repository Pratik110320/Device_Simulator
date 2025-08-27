# ============================
# 1. Build stage
# ============================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom + src
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ============================
# 2. Runtime stage
# ============================
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
