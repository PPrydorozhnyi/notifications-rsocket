import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedReader

plugins {
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.google.cloud.tools.jib") version "3.3.1"

    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
}

var commitHash: String by extra
commitHash = Runtime
    .getRuntime()
    .exec("git rev-parse --short HEAD")
    .let { process ->
        process.waitFor()
        val output = process.inputStream.use {
            it.bufferedReader().use(BufferedReader::readText)
        }
        process.destroy()
        output.trim()
    }
group = "com.petro"
version = "0.0.1-SNAPSHOT-$commitHash"
java.sourceCompatibility = JavaVersion.VERSION_17
val buildNumber by extra("0")

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.security:spring-security-rsocket")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-blackbird")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    from {
        image = "eclipse-temurin:17-jre-alpine"
    }
    to {
        image = "petro-fake.us-east-1.amazonaws.com/notification-rsocket"
        tags = setOf("dev", "$version", "$version.${extra["buildNumber"]}", commitHash)
        credHelper {
            helper = "ecr-login"
            environment = mapOf(
                "AWS_PROFILE" to "role-dev-cloud"
            )
        }
    }
    container {
        jvmFlags = listOf(
            "-server"
        )
    }
}
