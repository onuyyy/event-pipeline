FROM gradle:9.4.1-jdk21 AS builder
WORKDIR /app

COPY gradlew gradlew.bat ./
RUN chmod +x gradlew

COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon -q

COPY src/ src/

RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
