# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies > /dev/null
COPY src src
# bootJar depends on processAot: Spring AOT generates the bean definitions at build time,
# so the context does not need to evaluate configuration classes on startup.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar -x test \
    && java -Djarmode=tools -jar build/libs/*-SNAPSHOT.jar extract --destination /application \
    && mv /application/*-SNAPSHOT.jar /application/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /application
COPY --from=build /application/ ./

# Flags that shape the JVM AOT cache below: it is only used when they match at runtime, so they
# are pinned here, regardless of the CPU/memory available to the build and to the container.
# Compact object headers (JEP 519) shrink every object header from 12 to 8 bytes, reducing the heap.
# Memory sizing does not affect the cache and is set per deployment through JDK_JAVA_OPTIONS.
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseCompactObjectHeaders"

# Training run for the JVM AOT cache (JEP 483/514/515): starts the context and exits on
# refresh, recording the loaded and linked classes into app.aot. There is no database during
# the build, so it runs on the regular (non Spring AOT) path, where Flyway can be switched off
# and Hibernate skips JDBC metadata; conditions are frozen under Spring AOT. The cache still
# covers the JDK, Spring, Hibernate and application classes used at runtime.
RUN java $JAVA_OPTS -XX:AOTCacheOutput=app.aot \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.datasource.url=jdbc:postgresql://localhost:1/training \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -XX:AOTCache=app.aot -Dspring.aot.enabled=true -jar app.jar"]
