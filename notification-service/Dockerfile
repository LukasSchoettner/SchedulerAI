FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl notification-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service-1.0-SNAPSHOT.jar notification-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
