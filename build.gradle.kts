plugins {
    java
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "Greate-Seagull_idempotency-service")
        property("sonar.organization", "greate-seagull")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

subprojects {
    plugins.apply("java")
    plugins.apply("java-library")
    group = "com.hungvers.idempotency"
    version = "0.0.1"

    repositories { mavenCentral() }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}