# Stage 1: Build JAR with Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Stage 2: Run application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]