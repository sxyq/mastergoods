plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.2.6"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.zhihuiji"
version = "0.1.0"

// Keep backend build output outside the source tree. The backend project lives
// at Code/backend, so the repository root is two levels above this project.
val repositoryRoot = rootProject.projectDir.parentFile.parentFile
layout.buildDirectory.set(repositoryRoot.resolve("tmp/build/gradle-output/backend"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.flywaydb:flyway-core:10.17.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.2")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("com.github.librepdf:openpdf:3.0.5")
    runtimeOnly("com.h2database:h2:2.2.224")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2:2.2.224")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("LOCAL_H2_PATH", repositoryRoot.resolve("tmp/build/gradle-cache/backend-local/zhihuiji_local").absolutePath)
    systemProperty("MEDIA_STORAGE_PATH", repositoryRoot.resolve("data/media").absolutePath)
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment("LOCAL_H2_PATH", repositoryRoot.resolve("tmp/build/gradle-cache/backend-local/zhihuiji_local").absolutePath)
    environment("MEDIA_STORAGE_PATH", repositoryRoot.resolve("data/media").absolutePath)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
