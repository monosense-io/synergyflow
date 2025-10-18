plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.6"
    id("java")
}

group = "io.monosense.synergyflow"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.modulith:spring-modulith-bom:1.4.2"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Modulith core + JDBC event publication registry
    implementation("org.springframework.modulith:spring-modulith-starter-jdbc")
    runtimeOnly("org.springframework.modulith:spring-modulith-events-jdbc")
    runtimeOnly("org.springframework.modulith:spring-modulith-events-jackson")

    // MyBatis-Plus (Spring Boot 3 starter)
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.9")

    // OpenTelemetry API for correlation filter
    implementation("io.opentelemetry:opentelemetry-api:1.39.0")

    runtimeOnly("org.postgresql:postgresql:42.7.4")
    testRuntimeOnly("com.h2database:h2:2.2.224")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.test {
    useJUnitPlatform()
}
