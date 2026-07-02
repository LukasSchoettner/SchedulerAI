FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl customer-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/customer-service/target/customer-service-1.0-SNAPSHOT.jar customer-service.jar
EXPOSE 8082
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "customer-service.jar"]
