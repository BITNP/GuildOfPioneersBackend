FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN mkdir -p /root/.m2/repository/com/potato
COPY --from=veil_m2 Veil/1.0-SNAPSHOT \
    /root/.m2/repository/com/potato/Veil/1.0-SNAPSHOT

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /app/build/libs/GuildOfPioneers-0.0.1-SNAPSHOT.jar app.jar
COPY default_avatar.jpg .

RUN mkdir -p /app/uploads && chown -R app:app /app

USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_UPLOAD_DIR=/app/uploads \
    APP_DEFAULT_AVATAR=/app/default_avatar.jpg
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
