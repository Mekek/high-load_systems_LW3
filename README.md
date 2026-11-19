# Лабораторная работа №3. JWT-авторизация и ролевая модель

Это отдельное продолжение лабораторной №2. Исходная папка `lab2` не изменяется.

## Архитектура

- `api-gateway` — единственная внешняя точка входа;
- `user-service` — пользователи, BCrypt-хэши паролей, вход и выдача JWT;
- `event-service` — события, места, регистрация, лист ожидания и резервирование;
- `order-service` — заказы и билеты;
- `security-common` — общая проверка JWT и модель текущего пользователя;
- `eureka-server` — регистрация сервисов;
- `config-server` и `config-repo` — общая конфигурация;
- один контейнер PostgreSQL с тремя отдельными логическими БД: `user_db`, `event_db`, `order_db`.

Все прикладные сервисы сами проверяют подпись JWT. Gateway передаёт заголовок
`Authorization` дальше без изменений. Внутренние Feign-методы `/internal/**`
доступны без пользовательского JWT, но порты прикладных сервисов не опубликованы
на хосте: они доступны только внутри сети Docker Compose. Связь `order-service` с
`event-service` сделана через Reactive Feign, а с блокирующим `user-service` — через
обычный OpenFeign с переносом вызова на `boundedElastic`.

## Роли

| Роль | Разрешения |
| --- | --- |
| `USER` | Свои заказы, билеты и уведомления; отмена своего неподтверждённого заказа |
| `ADMIN` | Создание и изменение событий, тегов и типов билетов (`canManageCatalog`) |
| `CENSOR` | Модерация события: `ACTIVE` или `BLOCKED` с причиной (`canModerate`) |
| `SUPERVISOR` | Единственная роль, которая создаёт пользователей; полный доступ к пользователям, заказам и билетам; служебная очистка |

Соответствие заданию Lab 3: Spring Security + JWT в `Authorization`, ролевая модель,
логика методов зависит от текущего пользователя, пароль не отдаётся в DTO, в БД только
BCrypt-хэш, пользователей создаёт только `SUPERVISOR`. Kafka в этой лабораторной не используется.

Обычный пользователь не может подставить чужой `userId`: `order-service` проверяет
его по подписанному токену. Пользователь также не может повысить себе роль через
изменение профиля. Вход (`POST /api/auth/login`) идёт через `UserDetailsService` +
`PasswordEncoder` (как в лекции по Security), а JWT выдаёт `user-service`.

## Пароли и JWT

- В БД есть только `password_hash` с BCrypt-хэшем.
- Пароль принимается только при создании пользователя и входе.
- В `UserResponse`, межсервисном `UserDto` и JWT пароля нет.
- JWT передаётся как `Authorization: Bearer <token>`.
- Срок действия по умолчанию — 3600 секунд.
- Секрет берётся из `JWT_SECRET`; в рабочем окружении его нужно заменить.

Миграция `005-add-authentication.yaml` добавляет хэш старым тестовым пользователям.
Их начальный пароль: `Poster123!`. Пользователь `admin` получает роль `SUPERVISOR`.

## Запуск

Нужны JDK 21 и запущенный Docker Desktop.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Сборка `bootJar` идёт внутри Docker (multi-stage Dockerfile + Gradle wrapper).

Прикладной API опубликован только через Gateway: `http://localhost:8080`.
Для проверки регистрации сервисов также доступна Eureka: `http://localhost:8761`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- health: `http://localhost:8080/actuator/health`
- Eureka Dashboard: `http://localhost:8761`

Если остались volumes от раннего варианта лабораторной №3:

```powershell
docker compose down -v
docker compose up --build
```

## Проверка вручную

Получить токен supervisor:

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' `
  -Body '{"userName":"admin","password":"Poster123!"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

Создать пользователя (это может сделать только `SUPERVISOR`):

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/users `
  -Headers $headers `
  -ContentType 'application/json' `
  -Body '{"userName":"student01","email":"student@example.com","password":"StrongPass1!","role":"USER"}'
```

В Swagger сначала выполните `/api/auth/login`, скопируйте `accessToken`, нажмите
`Authorize` и вставьте токен. Префикс `Bearer` Swagger добавит сам.

## Тесты

```powershell
.\gradlew.bat test jacocoTestReport jacocoTestCoverageVerification
```

Интеграционные тесты используют Testcontainers, поэтому без Docker они не запустятся.
Есть отдельные тесты входа, BCrypt-хэша, запрета создания пользователей обычной
ролью и ограничения заказов текущим пользователем.
