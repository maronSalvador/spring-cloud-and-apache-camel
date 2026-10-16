FROM gradle:8.2-jdk17 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY src/ src/
# sh gradlew: no exec bit on Windows-copied files; tests run separately
RUN sh gradlew bootJar -x test --no-daemon -q

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/build/libs/camel-system-api-0.0.1-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
