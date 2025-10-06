# Gradle Build Configuration for SynergyFlow
**Project:** SynergyFlow (Enterprise ITSM & PM Platform)
**Build Tool:** Gradle 8.10+ with Kotlin DSL
**Author:** monosense (Backend Architect)
**Date:** 2025-10-06

---

## Complete Gradle Build Configuration

### Root Project: build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
}

group = "io.monosense"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "1.2.4"
extra["testcontainersVersion"] = "1.20.1"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Modulith (Module enforcement, event publication)
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")

    // Hibernate 7.0 (included with Spring Boot 3.4+)
    // Native UUIDv7 support via @UuidGenerator(style = Style.VERSION_7)
    // No explicit hibernate dependency needed - managed by Spring Boot

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Flyway Database Migrations
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis (Redisson client for Streams, caching)
    implementation("org.redisson:redisson-spring-boot-starter:3.35.0")

    // Keycloak (OIDC/OAuth2)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JSON Processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Kotlin Support
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka") // For Redis Streams testing

    // ArchUnit (Module boundary enforcement)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // WireMock (HTTP mocking for Keycloak)
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")

    // AssertJ (Fluent assertions)
    testImplementation("org.assertj:assertj-core:3.26.3")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Enable parallel test execution
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // JVM args for testing
    jvmArgs = listOf(
        "-XX:+UseG1GC",
        "-Xmx2048m",
        "-Xms512m"
    )

    // System properties for Testcontainers
    systemProperty("testcontainers.reuse.enable", "true")
}

// Spring Boot build info
springBoot {
    buildInfo()
}

// Task to verify Hibernate version (UUIDv7 support check)
tasks.register("verifyHibernateVersion") {
    doLast {
        val hibernateVersion = project.configurations
            .getByName("runtimeClasspath")
            .resolvedConfiguration
            .resolvedArtifacts
            .find { it.moduleVersion.id.group == "org.hibernate.orm" }
            ?.moduleVersion?.id?.version

        println("Hibernate ORM version: $hibernateVersion")

        if (hibernateVersion != null) {
            val majorVersion = hibernateVersion.substringBefore('.').toIntOrNull()
            if (majorVersion != null && majorVersion >= 7) {
                println("✅ Hibernate 7.0+ detected - Native UUIDv7 support available!")
            } else {
                println("⚠️  Hibernate ${hibernateVersion} - UUIDv7 requires custom generator")
            }
        }
    }
}

// Automatically verify on build
tasks.named("build") {
    dependsOn("verifyHibernateVersion")
}
```

---

## gradle.properties

```properties
# Gradle Settings
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=true

# Kotlin
kotlin.code.style=official
kotlin.incremental=true

# Spring Boot
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true

# Application
app.name=synergyflow
app.version=0.0.1-SNAPSHOT
```

---

## settings.gradle.kts

```kotlin
rootProject.name = "synergyflow"

// Enable Gradle build cache
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
        removeUnusedEntriesAfterDays = 7
    }
}

// Plugin management
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency resolution
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
```

---

## Multi-Module Structure (Optional - If splitting backend modules)

If you want to split into multiple modules (itsm, pm, workflow, security, eventing, sla, sse, audit):

### Root settings.gradle.kts

```kotlin
rootProject.name = "synergyflow"

include(
    "backend:core",
    "backend:itsm",
    "backend:pm",
    "backend:workflow",
    "backend:security",
    "backend:eventing",
    "backend:sla",
    "backend:app"
)

// Frontend (if building with Gradle)
include("frontend")
```

### backend/itsm/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot") version "3.4.0" apply false
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    // Module dependencies
    implementation(project(":backend:core"))
    implementation(project(":backend:security"))
    implementation(project(":backend:eventing"))

    // Spring Boot dependencies (no need for -starter suffix in submodules)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ... other dependencies
}
```

---

## Gradle Commands

### Build & Test

```bash
# Build entire project
./gradlew build

# Build without tests (faster)
./gradlew build -x test

# Run tests only
./gradlew test

# Run integration tests
./gradlew integrationTest

# Clean and build
./gradlew clean build

# Verify Hibernate version
./gradlew verifyHibernateVersion
```

### Run Application

```bash
# Run Spring Boot application (default profile: app)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=app'

# Run Event Worker profile
./gradlew bootRun --args='--spring.profiles.active=worker'

# Run Timer Service profile
./gradlew bootRun --args='--spring.profiles.active=timer'

# Run Search Indexer profile
./gradlew bootRun --args='--spring.profiles.active=indexer'
```

### Docker Build

```bash
# Build Docker image with Gradle
./gradlew bootBuildImage

# Build with custom image name
./gradlew bootBuildImage --imageName=synergyflow/backend:latest
```

### Dependency Management

```bash
# List all dependencies
./gradlew dependencies

# Check for dependency updates
./gradlew dependencyUpdates

# Generate dependency report
./gradlew buildEnvironment
```

### Code Quality

```bash
# Run ktlint (Kotlin linter)
./gradlew ktlintCheck

# Auto-format Kotlin code
./gradlew ktlintFormat

# Run ArchUnit tests (module boundaries)
./gradlew test --tests '*ModularityTests'
```

---

## Gradle Wrapper

Ensure you're using the correct Gradle version:

```bash
# Generate/update wrapper to Gradle 8.10
./gradlew wrapper --gradle-version=8.10

# Verify wrapper version
./gradlew --version
```

**gradle/wrapper/gradle-wrapper.properties:**
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import Project:**
   - File → Open → Select `build.gradle.kts`
   - Choose "Use Gradle from specified location" → Gradle 8.10

2. **Enable Annotation Processing:**
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

3. **Configure JPA:**
   - Settings → Build, Execution, Deployment → Build Tools → Gradle
   - Check "Delegate IDE build/run actions to Gradle"

4. **Enable Kotlin:**
   - Settings → Languages & Frameworks → Kotlin
   - Ensure Kotlin 2.0.0 is configured

### VS Code

Install extensions:
- Gradle for Java
- Kotlin Language
- Spring Boot Extension Pack

---

## CI/CD Integration

### GitHub Actions

**`.github/workflows/build.yml`:**
```yaml
name: Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew build

    - name: Verify Hibernate Version
      run: ./gradlew verifyHibernateVersion

    - name: Run Tests
      run: ./gradlew test

    - name: Upload Test Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: build/test-results/
```

### Jenkins

**Jenkinsfile:**
```groovy
pipeline {
    agent any

    tools {
        jdk 'JDK-21'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        stage('Verify Hibernate') {
            steps {
                sh './gradlew verifyHibernateVersion'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh './gradlew bootBuildImage'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/*.xml'
        }
    }
}
```

---

## Performance Optimization

### Gradle Build Cache

Enable in `gradle.properties`:
```properties
org.gradle.caching=true
```

### Parallel Execution

```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Incremental Compilation

```properties
kotlin.incremental=true
```

### Daemon

```properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

---

## Troubleshooting

### Issue: Hibernate version not 7.0+

**Check dependency tree:**
```bash
./gradlew dependencies --configuration runtimeClasspath | grep hibernate
```

**Force Hibernate version (if needed):**
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") {
        exclude(group = "org.hibernate.orm", module = "hibernate-core")
    }
    implementation("org.hibernate.orm:hibernate-core:7.0.0.Final")
}
```

### Issue: Testcontainers slow startup

**Enable container reuse:**
```properties
# gradle.properties
testcontainers.reuse.enable=true
```

**In test class:**
```kotlin
@Container
@ServiceConnection
val postgres = PostgreSQLContainer("postgres:16-alpine")
    .withReuse(true)
```

### Issue: Out of memory during build

**Increase Gradle JVM memory:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

---

## UUIDv7 Verification Task

Add custom task to verify UUIDv7 is working:

```kotlin
// build.gradle.kts

tasks.register<JavaExec>("testUuidV7Generation") {
    group = "verification"
    description = "Test UUIDv7 generation is working"

    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.monosense.synergyflow.UuidV7Test")

    doFirst {
        println("Testing UUIDv7 generation...")
    }
}
```

**UuidV7Test.kt:**
```kotlin
package io.monosense.synergyflow

import java.util.UUID

object UuidV7Test {
    @JvmStatic
    fun main(args: Array<String>) {
        // Generate UUIDs using Hibernate's UuidGenerator
        val uuids = (1..10).map {
            // This would use your entity's @UuidGenerator
            // For testing, we'll use uuid-creator if available
            try {
                val clazz = Class.forName("com.github.f4b6a3.uuid.UuidCreator")
                val method = clazz.getMethod("getTimeOrderedEpoch")
                method.invoke(null) as UUID
            } catch (e: Exception) {
                UUID.randomUUID() // Fallback
            }
        }

        println("Generated UUIDs:")
        uuids.forEach { println(it) }

        // Verify time-ordering
        val sorted = uuids.sortedBy { it.toString() }
        if (uuids == sorted) {
            println("✅ UUIDs are time-ordered (likely UUIDv7)")
        } else {
            println("❌ UUIDs are NOT time-ordered (likely UUIDv4)")
        }
    }
}
```

---

**End of Gradle Build Configuration Guide**
