// Root build configuration for Concert Playlist Builder monorepo

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.wrapper {
    gradleVersion = "8.5"
}
