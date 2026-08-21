FROM m.daocloud.io/docker.io/library/gradle:8.10.2-jdk21 AS builder
WORKDIR /workspace
COPY . .
RUN gradle -p Code/backend -Dorg.gradle.java.home="$JAVA_HOME" bootJar --no-daemon

FROM m.daocloud.io/docker.io/library/eclipse-temurin:21-jre
WORKDIR /app
# The Gradle build is intentionally redirected to the repository's generated
# output area; copy the same canonical artifact used by the local build gate.
COPY --from=builder /workspace/tmp/build/gradle-output/backend/libs/*.jar /app/app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
