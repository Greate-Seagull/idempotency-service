plugins {
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(platform("org.junit:junit-bom:6.0.0"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}