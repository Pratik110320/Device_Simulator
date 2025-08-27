# --- build stage ---
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# --- runtime stage ---
FROM eclipse-temurin:17-jre-alpine
ARG JAR_FILE=/workspace/target/*.jar
COPY --from=build ${JAR_FILE} /app/app.jar
USER 1000
# worker doesn't need public port, but keep 8080 if you want to test locally
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
