FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/
COPY order-service/pom.xml order-service/
COPY inventory-service/pom.xml inventory-service/
RUN mvn dependency:go-offline -pl common,inventory-service -am -q
COPY common/src common/src
COPY inventory-service/src inventory-service/src
RUN mvn package -pl common,inventory-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
