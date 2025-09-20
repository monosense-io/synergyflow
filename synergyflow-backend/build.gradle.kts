import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    java
    jacoco
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(platform("org.springframework.modulith:spring-modulith-bom:${libs.versions.spring.modulith.get()}"))
    implementation(platform("com.baomidou:mybatis-plus-bom:${libs.versions.mybatis.plus.get()}"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // MyBatis-Plus
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter")
    implementation("com.baomidou:mybatis-plus-jsqlparser")

    // Lombok
    compileOnly("org.projectlombok:lombok:${libs.versions.lombok.get()}")
    annotationProcessor("org.projectlombok:lombok:${libs.versions.lombok.get()}")
    testCompileOnly("org.projectlombok:lombok:${libs.versions.lombok.get()}")
    testAnnotationProcessor("org.projectlombok:lombok:${libs.versions.lombok.get()}")

    // MapStruct
    implementation("org.mapstruct:mapstruct:${libs.versions.mapstruct.get()}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${libs.versions.mapstruct.get()}")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Testcontainers (DBs, Kafka)
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:kafka")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Additional logical test tasks (filters) to align with CI gates
val architectureTest by tasks.registering(Test::class) {
    description = "Runs architecture verification tests"
    useJUnitPlatform()
    include("**/*Architecture*.*")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration-level tests"
    useJUnitPlatform()
    // heuristic include; adjust when separate sourceSet added
    include("**/*IT.*", "**/*Integration*.*")
    exclude("**/*Unit*.*")
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    mustRunAfter(tasks.test)
}

tasks.check { dependsOn(architectureTest, integrationTest, tasks.jacocoTestReport) }

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // overall ≥ 80%
            }
        }
    }
}

tasks.check { dependsOn("jacocoCoverageVerification") }
