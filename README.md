# Cash Tracker

![CI](https://github.com/finezhulidov-create/cash-tracker/actions/workflows/ci.yml/badge.svg)

Backend личного трекера финансов на Spring Boot. Пользователь ведёт **категории**, добавляет **траты** (одну трату можно разбить на несколько категорий: чек из супермаркета → «Еда» + «Быт»), задаёт **месячные бюджеты** и видит **остаток** и **аналитику** для дашборда.

## Возможности

- Регистрация и вход по email и паролю, JWT. Регистрация сразу возвращает токен.
- Категории пользователя, список кэшируется в Redis.
- Траты с разбивкой на части по категориям; сумма частей обязана равняться сумме траты. Дату можно указать вручную (внести трату задним числом).
- Бюджеты на месяц: **по категории** или **общий** (без категории, считает все траты месяца). Остаток всегда считается из трат при запросе, а не хранится.
- Аналитика: траты по категориям за период, динамика по месяцам года, сводка месяца (все траты, общий бюджет и бюджеты категорий с остатками).
- Баланс пользователя: поле, которое пользователь задаёт сам; от трат не меняется.
- Данные каждого пользователя изолированы: чужой ресурс отвечает 404, как будто его нет.

## Стек

| Область | Технологии |
|---|---|
| Язык, фреймворк | Java 17, Spring Boot 3.5 |
| Безопасность | Spring Security, JWT (jjwt), BCrypt |
| Данные | PostgreSQL 16, Spring Data JPA, Liquibase |
| Кэш и лимиты | Redis |
| Маппинг | MapStruct, Lombok |
| Документация | springdoc-openapi (Swagger UI) |
| Эксплуатация | Spring Boot Actuator, Docker, Docker Compose |
| Тесты | JUnit 5, Mockito, Testcontainers, JaCoCo |
| CI | GitHub Actions |

## Архитектура

```
cash-tracker/
├── cash-tracker-common/         # JWT, обработка ошибок, исключения, утилиты
├── cash-tracker-transactions/   # домен: категории, траты со сплитами, бюджеты, аналитика
└── cash-tracker-app/            # запускаемое приложение: пользователи, авторизация, безопасность,
                                 # конфигурация Redis/CORS/Swagger, миграции Liquibase
```

`app` зависит от `transactions` и `common`, `transactions` от `common`; обратных зависимостей нет. Домен трат знает пользователя только по `userId`.

**Запрос.** `RateLimitFilter` (только `/auth/**`) → `JwtFilter` → контроллер → сервис → репозиторий. Идентификатор пользователя берётся только из токена, а не из тела запроса. Владение ресурсом проверяется в сервисе (`SecurityUtils.assertOwner`) на каждом обращении, включая вложенные ссылки (например, категория внутри части траты).

**Кэш.** Список категорий и части трат по категории лежат в Redis (ключи `categories::{userId}` и `splits::{userId}:{categoryId}`, TTL 15 минут), значения хранятся строго типизированно. Кэш сбрасывается при изменении категорий и трат.

**Модель данных** (все связи с пользователем каскадно удаляются вместе с ним):

```
users ──< categories ──< transaction_split >── transaction >── users
users ──< budget            (category_id необязателен: пусто = общий бюджет месяца)
```

Схему ведёт только Liquibase (`ddl-auto=validate`). Применённые changeset'ы не редактируются, изменения идут новыми файлами в `cash-tracker-app/src/main/resources/db/changelog`.

## Быстрый старт

Нужны Docker и Docker Compose.

```bash
docker compose up --build
```

| Что | Адрес |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Состояние | http://localhost:8080/actuator/health |

Локальная разработка (нужны запущенные PostgreSQL и Redis, например `docker compose up db redis`):

```bash
mvn spring-boot:run -pl cash-tracker-app -am -Dspring-boot.run.profiles=dev
```

## Как работать с API

Все запросы, кроме `/auth/**`, Swagger и Actuator, требуют заголовок `Authorization: Bearer <токен>`. Токен приходит в ответах `POST /auth/register` и `POST /auth/login`:

```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 86400 }
```

`expiresIn` — секунды. Токен действует 24 часа, обновления токена нет: по истечении срока пользователь входит заново.

### Эндпоинты

| Область | Эндпоинт | Назначение |
|---|---|---|
| Auth | `POST /auth/register` | Регистрация, сразу возвращает токен |
| | `POST /auth/login` | Вход |
| Профиль | `GET /v1/users/me` | Имя, email, баланс |
| | `PUT /v1/users/me/balance` | Задать баланс, тело `{"balance": 1500.50}` |
| | `DELETE /v1/users/delete` | Удалить аккаунт со всеми данными |
| Категории | `POST/GET/PUT/DELETE /v1/categories[/{id}]`, `GET /v1/categories/mine` | CRUD, список. Удаление категории с тратами: 409 |
| | `GET /v1/categories/{id}/splits` | Части трат в категории |
| Траты | `POST /v1/transactions` | Создать трату с разбивкой |
| | `GET /v1/transactions` | Список: фильтры `categoryId`, `minAmount`, `maxAmount`, `from`, `to`, пагинация, сортировка |
| | `GET/PUT/DELETE /v1/transactions/{id}` | Одна трата |
| | `GET /v1/transactions/total?from&to` | Сумма за период |
| Бюджеты | `POST /v1/budgets` | Создать (с `categoryId` или общий) |
| | `GET /v1/budgets?yearMonth=2026-10` | Бюджеты месяца |
| | `GET/PUT/DELETE /v1/budgets/{id}` | Один бюджет |
| | `GET /v1/budgets/{id}/status` | `assigned`, `spent`, `available` |
| Аналитика | `GET /v1/analytics/by-category?from&to` | Траты по категориям (даты `yyyy-MM-dd`, включительно) |
| | `GET /v1/analytics/monthly?year` | 12 месяцев с суммами |
| | `GET /v1/analytics/summary?yearMonth` | Сводка месяца |

Полное описание со схемами и кодами ответов: Swagger UI.

### Создание траты

```json
{
  "description": "Супермаркет",
  "amount": 1000.00,
  "dateTime": "2026-10-28T14:30:00",
  "splits": [
    { "categoryId": 10, "amount": 700.00 },
    { "categoryId": 11, "amount": 300.00 }
  ]
}
```

`dateTime` необязателен: без него берётся текущее время сервера (UTC). Время хранится и возвращается **без часового пояса, как его ввёл пользователь**, месяц бюджета определяется по этой дате. Сумма частей должна равняться `amount`, иначе 400 `NOT_VALID_TRANSACTION_SPLIT_SUM`.

### Списки и пагинация

Списки возвращаются в формате `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`. Параметры: `page` (с нуля), `size`, `sort=поле,asc|desc`. Список трат по умолчанию идёт от новых к старым.

### Формат ошибок

Все ошибки приходят в одном виде, `errors` есть только у ошибок валидации (поле → сообщение):

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-10-28T14:30:00",
  "errorCode": "VALIDATION_ERROR",
  "errors": { "splits[0].amount": "must be greater than 0" }
}
```

| Статус | `errorCode` | Когда |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `MISSING_PARAMETER`, `INVALID_PARAMETER`, `INVALID_REQUEST`, `INVALID_SORT`, `NOT_VALID_TRANSACTION_SPLIT_SUM` | Некорректный запрос |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` | Нет или невалиден токен; неверный email или пароль |
| 403 | `FORBIDDEN` | Нет прав |
| 404 | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, `NOT_FOUND` | Не найдено, **либо ресурс чужой** |
| 409 | `EMAIL_ALREADY_EXISTS`, `RESOURCE_ALREADY_EXISTS`, `CATEGORY_IN_USE`, `DATA_INTEGRITY_VIOLATION` | Конфликт |
| 429 | `RATE_LIMIT_EXCEEDED` | Превышен лимит запросов к `/auth/**` |
| 500 | `INTERNAL_ERROR` | Сбой сервера (без внутренних деталей, они только в логе) |

### Для фронтенда

- **Вход.** Токен лучше держать в памяти приложения или `sessionStorage`; он передаётся заголовком, cookie и CSRF не используются. При 401 направляйте пользователя на экран входа.
- **CORS.** Разрешённые адреса задаются в `app.cors.allowed-origins` (по умолчанию `http://localhost:3000` и `http://localhost:5173`).
- **Лимит.** При 429 ответ содержит заголовок `Retry-After` (секунды); он доступен браузеру (`Access-Control-Expose-Headers`).
- **Деньги** приходят десятичными числами, из базы до 4 знаков после запятой (`1000.0000`). Не используйте float для расчётов, форматируйте сумму при отображении.
- **Генерация клиента.** Спецификация: `GET /v3/api-docs` (можно подать в openapi-generator).

## Настройка

Основные свойства (`application.properties`, переопределяются переменными окружения):

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `spring.datasource.url/username/password` | профиль `dev`: localhost | PostgreSQL |
| `spring.data.redis.host/port` | профиль `dev`: localhost:6379 | Redis |
| `jwt.secret` (`JWT_SECRET`) | в `dev` есть значение по умолчанию | Секрет подписи: **base64-строка от 32 байт**, например `openssl rand -base64 48` |
| `jwt.expiration` | `86400000` | Срок жизни токена, мс |
| `app.cors.allowed-origins` | `http://localhost:3000,http://localhost:5173` | Адреса фронтенда через запятую |
| `app.rate-limit.auth.requests` / `window-seconds` | `30` / `60` | Не больше 30 запросов в минуту с одного IP к `/auth/**` |
| `app.rate-limit.auth.enabled` | `true` | Выключатель лимита |

Рейт-лимит считается по `getRemoteAddr()`. За обратным прокси там будет адрес прокси, и лимит станет общим на всех клиентов: передавайте реальный IP (`server.forward-headers-strategy=native` и настройки Tomcat remote IP). Если Redis недоступен, лимит не применяется, а вход продолжает работать.

## Эксплуатация

- `GET /actuator/health` — общее состояние (БД и Redis), `/actuator/health/liveness` и `/readiness` — для оркестраторов. Детали компонентов наружу не отдаются. Открыты только `health` и `info`.
- Healthcheck приложения настроен в `docker-compose.yml`.
- Swagger UI и `/v3/api-docs` открыты без токена. При публичном развёртывании их стоит закрыть или выключить.

## Тесты

```bash
mvn verify                              # всё: юнит- и интеграционные тесты, отчёты покрытия
mvn test -DexcludedGroups=integration   # только быстрые тесты, Docker не нужен
```

- **Юнит-тесты** (Mockito, `@WebMvcTest`): сервисы, контроллеры, валидация, формат ошибок, фильтры безопасности, конфигурация безопасности и CORS.
- **Интеграционные тесты** (`@Tag("integration")`, Testcontainers): настоящие PostgreSQL и Redis, полная цепочка безопасности, миграции Liquibase, сквозные сценарии API, кэш, рейт-лимит, Actuator, OpenAPI. Требуют запущенного Docker.
- **Покрытие (JaCoCo).** После `mvn verify`: `cash-tracker-app/target/site/jacoco-aggregate/index.html` (модули `common` и `transactions`) и `cash-tracker-app/target/site/jacoco/index.html` (классы `app`). Сгенерированные MapStruct-мапперы в отчёт не входят.

## CI

GitHub Actions (`.github/workflows/ci.yml`) на каждый push и pull request: `mvn verify` со всеми интеграционными тестами, публикация отчёта покрытия, отдельная проверка сборки Docker-образа.

## Известные ограничения

- Токен живёт 24 часа, обновления токена и выхода из системы на сервере нет.
- Подтверждения email и сброса пароля нет.
- Баланс пользователя задаётся вручную и не связан с тратами.
- Время хранится без часового пояса (так, как его ввёл пользователь).
- Конфигурация для продакшена (отдельные профили, управление секретами, HTTPS-прокси) не подготовлена: проект запускается в профиле `dev`.
