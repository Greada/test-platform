FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# pom.xml 不变 → 缓存这层 → 依赖只下载一次
COPY pom.xml .
COPY backend/pom.xml backend/
RUN mvn -B dependency:go-offline -pl backend

# 源码改了，才重新编译
COPY backend/src backend/src
RUN mvn -B package -DskipTests -pl backend

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
