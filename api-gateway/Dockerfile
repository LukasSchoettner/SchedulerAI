FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-1.0-SNAPSHOT.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
