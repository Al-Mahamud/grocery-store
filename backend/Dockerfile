# The API as a container: built from source, run as an unprivileged user with the prod profile.
# Tests are not run here; CI runs them before anything is deployed.

FROM eclipse-temurin:24-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -Dmaven.test.skip=true package \
    && cp target/store-backend-*.jar app.jar

FROM eclipse-temurin:24-jre
RUN useradd --system --uid 10001 --home-dir /app haat \
    && mkdir -p /var/lib/haat/storage \
    && chown haat /var/lib/haat/storage
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER haat
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
# Product pictures uploaded in the admin live here; mount a volume (deploy/compose.yaml).
VOLUME /var/lib/haat/storage
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
