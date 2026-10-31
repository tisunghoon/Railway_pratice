FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
