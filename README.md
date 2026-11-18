# Лабораторная работа №3. JWT-авторизация и ролевая модель

Это отдельное продолжение лабораторной №2. Исходная папка `lab2` не изменяется.

## Архитектура

- `api-gateway` — единственная внешняя точка входа;
- `user-service` — пользователи, BCrypt-хэши паролей, вход и выдача JWT;
- `event-service` — события, регистрация, лист ожидания и резервирование;
- `order-service` — заказы и билеты;
- `security-common` — общая проверка JWT и модель текущего пользователя;
- `eureka-server` — регистрация сервисов;
- `config-server` и `config-repo` — общая конфигурация;
- три отдельные PostgreSQL: для пользователей, событий и заказов.

Все прикладные сервисы сами проверяют подпись JWT. Gateway передаёт заголовок
`Authorization` дальше без изменений. Внутренние Feign-методы `/internal/**`
доступны без пользовательского JWT, но порты сервисов не опубликованы на хосте:
они доступны только внутри сети Docker Compose.

## Роли

| Роль | Разрешения |
| --- | --- |
| `USER` | Свои заказы и уведомления, отмена своего неподтверждённого заказа |
| `SUPERUSER` | Создание и изменение событий, тегов и типов билетов |
| `CENSOR` | Модерация события: `ACTIVE` или `BLOCKED` с причиной |
| `SUPERVISOR` | Создание пользователей, полный доступ к пользователям и заказам, служебная очистка данных |
| `ADMIN` | Старая роль из лабораторной №2; специальных прав в лабораторной №3 не даёт |

Обычный пользователь не может подставить чужой `userId`: `order-service` проверяет
его по подписанному токену. Пользователь также не может повысить себе роль через
изменение профиля.

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

Снаружи опубликован только Gateway: `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- health: `http://localhost:8080/actuator/health`

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
