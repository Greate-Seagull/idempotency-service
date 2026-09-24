plugins {
    alias(libs.plugins.spring.dependency.management)
    id("maven-publish")
    id("jacoco")
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.boot.dependencies.get().toString())
    }
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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    // Yêu cầu task test phải chạy trước khi tạo report
    dependsOn(tasks.test)

    reports {
        xml.required.set(true) // BẮT BUỘC: SonarCloud đọc coverage qua file XML
        html.required.set(true) // Giúp bạn mở trang HTML ở local xem chi tiết dòng nào chưa test
        csv.required.set(false)
    }

    // Loại bỏ các class không cần thiết khỏi báo cáo test (Ví dụ: DTO, Configuration, Lombok generated code)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/entity/**",
                    "**/dto/**",
                    "**/*Application.*"
                )
            }
        })
    )
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