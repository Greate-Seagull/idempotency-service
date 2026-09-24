plugins {
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
}

configurations {
    testCompileOnly.get().extendsFrom(compileOnly.get())
    testAnnotationProcessor.get().extendsFrom(annotationProcessor.get())
}

dependencies {
    // Module dependencies
    api(project(":idempotency-api"))

    // Compile-time only (applies to both main and test via extendsFrom above)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Runtime implementation
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(libs.spring.retry)

    // Test
    testImplementation(testFixtures(project(":idempotency-api")))
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("com.redis:testcontainers-redis")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Greate-Seagull/idempotency-service")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}