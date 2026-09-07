plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "app.dam"
version = "0.0.1-SNAPSHOT"
description = "dam_BE"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-mysql")
    /* 스프링 부트 스타터는 아직 부트 4 를 모른다(RestClientCustomizer 가 없어져
       기동에서 죽는다). 예외는 ApiExceptionHandler 가 전부 잡아 MVC 통합에 남는 것도
       없으므로, 애초에 우리가 쓰려던 경로인 로그백 appender 만 붙인다. */
    implementation("io.sentry:sentry-logback:8.16.0")
    implementation(platform("software.amazon.awssdk:bom:2.31.0"))
    implementation("software.amazon.awssdk:s3")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

/* 버전이 붙은 이름이면 배포 스크립트가 글롭으로 집어야 하는데, 그 글롭에
   -plain.jar 까지 걸린다. 서버의 /srv/dam/dam.jar 와 이름을 맞춰 둔다. */
tasks.bootJar {
    archiveFileName = "dam.jar"
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
