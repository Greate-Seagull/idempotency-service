plugins {
    java
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
}