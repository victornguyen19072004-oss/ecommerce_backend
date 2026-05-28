# Stage 1: Build file .jar bằng Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Đóng gói ứng dụng, bỏ qua test để deploy nhanh hơn
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng bằng JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy file jar từ Stage 1 sang
COPY --from=build /app/target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
# Mở cổng 8080
EXPOSE 8080
# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]