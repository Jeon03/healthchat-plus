# ==============================
# 1단계: Gradle 빌드
# ==============================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# 🔥 application.yml 강제 COPY
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test --build-cache

# ==============================
# 2단계: 실행 이미지
# ==============================
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
