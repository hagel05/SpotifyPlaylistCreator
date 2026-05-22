plugins {
    id("java")
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
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
val isWindows = System.getProperty("os.name").lowercase().contains("windows")

val buildFrontend by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Build the React frontend with Vite"
    workingDir = file("../frontend")
    if (isWindows) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
    // No inputs/outputs declared: task always runs, never cached as UP-TO-DATE
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(buildFrontend)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("../frontend/dist") {
        into("static")
    }
}