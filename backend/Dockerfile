FROM m.daocloud.io/docker.io/library/gradle:8.10.2-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN gradle -Dorg.gradle.java.home="$JAVA_HOME" bootJar --no-daemon

FROM m.daocloud.io/docker.io/library/eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
