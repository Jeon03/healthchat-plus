# ==============================
# 1단계: Gradle 빌드
# ==============================
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src
RUN chmod +x ./gradlew

RUN ./gradlew clean bootJar -x test --build-cache

# ==============================
# 2단계: 실행 이미지
# ==============================
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
