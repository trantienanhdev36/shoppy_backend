# Stage 1: Build file JAR bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng bằng Java 17 JRE nhỏ gọn
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render sẽ tự động cấp phát cổng ngẫu nhiên qua biến $PORT
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]