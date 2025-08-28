plugins {
	java
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.project_bot"
version = "0.0.1-SNAPSHOT"
description = "Telegram bot example project"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("io.vavr:vavr:0.10.4")
	implementation("org.telegram:telegrambots-spring-boot-starter:6.8.0")

	implementation("jakarta.xml.ws:jakarta.xml.ws-api:3.0.0")
	implementation("jakarta.jws:jakarta.jws-api:3.0.0")

	implementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
	implementation("org.glassfish.jaxb:jaxb-runtime:3.0.2")
	implementation("org.ehcache:ehcache:3.10.8")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}