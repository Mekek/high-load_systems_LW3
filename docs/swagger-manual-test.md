# Ручной прогон через Swagger (Lab 3)

Предусловия:

1. `Copy-Item .env.example .env`
2. `docker compose up --build`
3. Открыть [Swagger UI](http://localhost:8080/swagger-ui.html)
4. В выпадающем списке выбрать сервис (`user-service` / `event-service` / `order-service`)

После миграций в БД уже есть:

| Данные | Зачем |
| --- | --- |
| ~30 пользователей (`admin` + `demo_user_01…25`), пароль `Poster123!` | offset-пагинация `GET /api/users` |
| 25 уведомлений у `admin` (id=1) | `X-Total-Count` на `GET /api/notifications` |
| 30 `Demo Event #01…#30`, теги, VIP-места на `#01` | infinite scroll + заказ |

---

## 0. Авторизация (обязательно для Lab 3)

1. Выбрать **user-service**.
2. `POST /api/auth/login`:

```json
{
  "userName": "admin",
  "password": "Poster123!"
}
```

Ожидание: **200**, в теле `accessToken`, `tokenType: Bearer`.

3. Нажать **Authorize** в Swagger UI, вставить `accessToken` (префикс `Bearer` добавится сам).
4. Без токена защищённые методы дают **401**; с токеном обычного пользователя (`demo_user_01`) `POST /api/users` даёт **403**.

---

## 1. Пагинация (offset) — `user-service`

1. С токеном `admin` (`SUPERVISOR`): `GET /api/users` → `Try it out`:
   - `page=0`, `size=5` → Execute  
   - в Response headers найти **`X-Total-Count`** (ожидается ≥ 30)  
   - в body ровно 5 пользователей, **без поля password**
3. Повторить с `page=1`, `size=5` — следующий набор id/username.
4. Аналогично `GET /api/notifications?userId=1&page=0&size=5` — снова смотреть **`X-Total-Count: 25`**.

---

## 2. Infinite scroll (cursor) — `event-service`

1. Выбрать **event-service** (тот же Bearer-токен).
2. `GET /api/events`:
   - `afterId=0`, `limit=5` → первые 5 событий, запомнить **последний `id`** в ответе (например `5`)
   - `afterId=<тот id>`, `limit=5` → следующая «страница» без `X-Total-Count` (так и задумано)
3. Опционально `status=ACTIVE` — только активные.

---

## 3. CRUD пользователя — `user-service`

Создавать пользователей может только **`SUPERVISOR`**.

### Create

`POST /api/users`

```json
{
  "userName": "swagger_demo",
  "email": "swagger@poster.ru",
  "password": "StrongPass1!",
  "role": "USER"
}
```

Ожидание: **201**, в теле есть `id`, **пароля в ответе нет** — сохранить `id`.

### Read / Update / Delete

- `GET /api/users/{id}`
- `PUT /api/users/{id}`:

```json
{
  "userName": "swagger_demo2",
  "email": "swagger2@poster.ru",
  "role": "USER"
}
```

- `DELETE /api/users/{id}` → **204**, повторный GET → **404**

---

## 4. CRUD события — `event-service`

Нужна роль `ADMIN` или `SUPERVISOR` (у `admin` есть `SUPERVISOR`).

### Create

`POST /api/events`

```json
{
  "eventName": "Swagger Live",
  "description": "Created from Swagger UI",
  "location": "Demo Hall",
  "dateTime": "2026-12-01T19:00:00",
  "status": "ACTIVE",
  "tagNames": ["music", "live"]
}
```

### Update / Delete

- `PUT /api/events/{id}` — сменить `eventName` / `status`
- `DELETE /api/events/{id}` → **204**

Теги: `GET/POST/PUT/DELETE /api/tags` — тот же сценарий create → list → delete.

Модерация (`CENSOR` / `SUPERVISOR`): `PUT /api/events/{id}/moderation`.

---

## 5. Места события (каталог перед заказом) — `event-service`

1. Найти id события (`GET /api/events?afterId=0&limit=5`).
2. `GET /api/events/{eventId}/seats?page=0&size=20` — все места, смотреть **`X-Total-Count`** и поле `available`.
3. Опционально:
   - `ticketTypeId=1` — только выбранный тип;
   - `availableOnly=true` — только свободные (без `reservationId`).

---

## 6. Заказ (сквозной) — `order-service`

Нужны: пользователь (например id **1** = `admin`), событие **`Demo Event #01`** (ACTIVE + seats VIP), тип билета **VIP** (`GET /api/ticket-types`, обычно id **1**).

Обычный `USER` может создать заказ только на **свой** `userId` из JWT. Supervisor — на любого.

1. В **event-service**: `GET /api/events?afterId=0&limit=5` — найти id `Demo Event #01`.  
   Перед заказом можно глянуть `GET /api/events/{id}/seats?availableOnly=true`.
2. В **order-service**: `POST /api/orders`

```json
{
  "userId": 1,
  "items": [
    {
      "eventId": <id Demo Event #01>,
      "ticketTypeId": 1,
      "quantity": 2
    }
  ]
}
```

Ожидание: **201**, `status: PENDING`, в items есть `reservationId` и tickets.

3. `GET /api/orders?userId=1&page=0&size=10` — список + **`X-Total-Count`**.
4. `PUT /api/orders/{id}/status`:

```json
{ "status": "CONFIRMED" }
```

Подтверждение — только `SUPERVISOR`.  
`{ "status": "CANCELLED" }` — владелец или supervisor (для PENDING снимет резерв в event-service).

5. `GET /api/tickets` — supervisor видит все; обычный пользователь — только билеты своих заказов.

---

## Что показать на защите (Lab 3)

1. **Login** → JWT в `Authorization: Bearer`.
2. **BCrypt** — в ответах нет пароля; в БД только `password_hash`.
3. **Роли** — supervisor создаёт пользователей; USER получает 403 на `POST /api/users`; ADMIN/CENSOR отличаются на событиях.
4. **Логика от текущего пользователя** — чужой `userId` в заказе/билетах запрещён.
5. Offset / cursor / Feign-заказ — как в Lab 2, но уже под JWT.
