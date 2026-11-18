rootProject.name = "microservice-poster"

listOf(
    "shared-contracts",
    "security-common",
    "eureka-server",
    "config-server",
    "api-gateway",
    "user-service",
    "event-service",
    "order-service"
).forEach { name ->
    if (file(name).exists()) {
        include(name)
    }
}
