import org.gradle.api.tasks.Exec

plugins {
    java
    // Upgrade Spring Boot; we’ll still force Hibernate 7 explicitly below
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "io.monosense.synergyflow"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    testCompileOnly {
        extendsFrom(configurations.testAnnotationProcessor.get())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Force Hibernate ORM 7.x for UUIDv7 native support
    implementation("org.hibernate.orm:hibernate-core:7.0.0.Final")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.nimbusds:nimbus-jose-jwt")
    implementation("org.redisson:redisson-spring-boot-starter:3.35.0")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.h2database:h2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:1.2.4")
        mavenBom("org.testcontainers:testcontainers-bom:1.20.4")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        val include = System.getProperty("includeTags")
        if (!include.isNullOrBlank()) {
            includeTags(*include.split(',').map { it.trim() }.toTypedArray())
        } else {
            excludeTags("load")
        }
    }

    // Tame memory usage and avoid agent/OOM issues during CI
    minHeapSize = System.getProperty("test.minHeap", "256m")
    maxHeapSize = System.getProperty("test.maxHeap", "1024m")
    jvmArgs(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Dio.netty.leakDetection.level=disabled"
    )

    // Keep tests sequential to reduce resource pressure
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    // Ensure Redis auth works across all tests
    systemProperty("spring.redis.password", System.getProperty("spring.redis.password") ?: System.getenv("REDIS_PASSWORD") ?: "integration-secret")
}

val infrastructureDir = projectDir.resolve("../infrastructure").normalize()

val dockerComposeUp by tasks.registering(Exec::class) {
    workingDir = infrastructureDir
    commandLine("docker", "compose", "up", "-d", "redis")
    environment("REDIS_PASSWORD", "integration-secret")
}

val dockerComposeDown by tasks.registering(Exec::class) {
    workingDir = infrastructureDir
    commandLine("docker", "compose", "down")
    environment("REDIS_PASSWORD", "integration-secret")
}

tasks.withType<Test>().configureEach {
    dependsOn(dockerComposeUp)
    finalizedBy(dockerComposeDown)
    environment("REDIS_PASSWORD", "integration-secret")
}
