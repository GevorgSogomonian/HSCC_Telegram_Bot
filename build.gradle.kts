plugins {
    id("java")
    id("org.springframework.boot") version "3.1.4"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "org.example"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Database
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.liquibase:liquibase-core")
    implementation("io.minio:minio:8.4.3")

    // Telegram Bot API
    implementation("org.telegram:telegrambots")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.5.0")

    // Lombook
    implementation("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
}