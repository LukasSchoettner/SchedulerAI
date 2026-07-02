FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl task-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/task-service/target/task-service-1.0-SNAPSHOT.jar task-service.jar
EXPOSE 8081
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "task-service.jar"]
