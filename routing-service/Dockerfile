FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl routing-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/routing-service/target/routing-service-1.0-SNAPSHOT.jar routing-service.jar
EXPOSE 8084
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "routing-service.jar"]
