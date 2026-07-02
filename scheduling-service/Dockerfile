FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl scheduling-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/scheduling-service/target/scheduling-service-1.0-SNAPSHOT.jar scheduling-service.jar
EXPOSE 8085
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "scheduling-service.jar"]
