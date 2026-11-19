# Собирает bootJar локальным Gradle (кэш на хосте), затем поднимает Docker Compose.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}

$gradle = Join-Path $env:USERPROFILE ".gradle\wrapper\dists\gradle-9.8.2-bin\8wepis63pzuhrlfaiy04bagwu\gradle-9.8.2\bin\gradle.bat"
if (-not (Test-Path $gradle)) {
    $gradle = ".\gradlew.bat"
}

Write-Host "Building bootJars with local Gradle..."
& $gradle :eureka-server:bootJar :config-server:bootJar :api-gateway:bootJar `
    :user-service:bootJar :event-service:bootJar :order-service:bootJar --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting docker compose..."
docker compose up --build @args
