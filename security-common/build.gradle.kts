plugins {
    `java-library`
}

dependencies {
    api(project(":shared-contracts"))
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("io.projectreactor:reactor-core")
}
