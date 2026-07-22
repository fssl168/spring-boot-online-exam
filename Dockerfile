# ==================== 多阶段构建：后端 ====================
# Stage 1: Maven 构建
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
# 先 copy pom.xml 利用 Docker 缓存加速依赖下载
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
# 再 copy 源码编译
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: 运行时
FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 9527
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
