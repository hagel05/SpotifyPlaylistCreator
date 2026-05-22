plugins {
    id("java")
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.node-gradle.node") version "7.0.2"
}

group = "org.hagelbrand"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test:3.5.8")
}

tasks.test {
    useJUnitPlatform()
}

// ── Local dev: build the React frontend and serve it from Spring Boot ──────────
// The frontend build only runs when launching the app locally via bootRun.
// It is intentionally excluded from `test` and `build` so CI backend jobs don't
// need Node/npm installed.
node {
    nodeProjectDir = file("../frontend")
}

val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    group = "frontend"
    description = "Build the React frontend with Vite"
    args = listOf("run", "build")
}

// Copy the Vite output into a staging directory on the build classpath.
// This task depends on buildFrontend, but nothing else does — so it only
// runs when something explicitly depends on it (i.e. bootRun below).
val copyFrontendDist by tasks.registering(Copy::class) {
    group = "frontend"
    description = "Stage the built React assets so Spring Boot can serve them"
    dependsOn(buildFrontend)
    from("../frontend/dist")
    into(layout.buildDirectory.dir("tmp/frontend-static/static"))
}

// Add the staging directory to bootRun's classpath so Spring Boot picks up
// the React assets under /static/** — without touching processResources.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn(copyFrontendDist)
    classpath(layout.buildDirectory.dir("tmp/frontend-static"))
}