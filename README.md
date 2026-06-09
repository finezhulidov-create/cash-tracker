# 💰 Cash Tracker

Веб-приложение для учёта личных расходов с JWT-аутентификацией, built on Spring Boot 3.x.

## 📋 Описание

Cash Tracker — это REST API бэкенд с простым HTML/JS фронтендом.  
Позволяет регистрироваться, входить в систему и вести учёт расходов по категориям.

## 🛠 Стек технологий

| Технология | Версия |
|---|---|
| Java | 17 |
| Spring Boot | 3.x |
| Spring Security | 6.x |
| Spring Data JPA | 3.x |
| Hibernate | 6.x |
| PostgreSQL | 15+ |
| Maven | 3.x |
| JWT (jjwt) | 0.11+ |
| BCrypt | — |
| HTML / JS | — |

## ⚡ Функциональность

- 🔐 Регистрация и вход пользователя (JWT + BCrypt)
- 📂 CRUD категорий расходов
- 💸 CRUD расходов с привязкой к категории
- 🛡 Глобальная обработка ошибок (`@RestControllerAdvice`)
- 🌐 Простой фронтенд на HTML/JS

## 🗂 Структура проекта
src/
├── main/
│   ├── java/
│   │   └── dev/zhulidov/cashtracker/
│   │       ├── config/        # Spring Security, JWT конфигурация
│   │       ├── controller/    # REST контроллеры
│   │       ├── dto/           # Java Records (DTO)
│   │       ├── entity/        # JPA сущности (User, Category, Expense)
│   │       ├── exception/     # Глобальная обработка ошибок
│   │       ├── filter/        # JWT фильтр
│   │       ├── repository/    # Spring Data репозитории
│   │       └── service/       # Бизнес-логика
│   └── resources/
│       ├── application.properties
│       └── static/            # HTML/JS фронтенд
## 🚀 Запуск локально

### Требования
- Java 17+
- PostgreSQL 15+
- Maven 3.x

### Шаги

**1. Клонируй репозиторий**
```bash
git clone https://github.com/finezhulidov-create/cash-tracker.git
cd cash-tracker
```

**2. Создай базу данных**
```sql
CREATE DATABASE cash_tracker;
```

**3. Настрой `application.properties`**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cash_tracker
spring.datasource.username=твой_юзер
spring.datasource.password=твой_пароль
jwt.secret=твой_секретный_ключ
```

**4. Запусти приложение**
```bash
mvn spring-boot:run
```

**5. Открой в браузере**
http://localhost:8080
## 🔗 API Endpoints

| Метод | URL | Описание |
|---|---|---|
| POST | `/api/auth/register` | Регистрация |
| POST | `/api/auth/login` | Вход, получение JWT |
| GET | `/api/categories` | Список категорий |
| POST | `/api/categories` | Создать категорию |
| GET | `/api/expenses` | Список расходов |
| POST | `/api/expenses` | Добавить расход |
| PUT | `/api/expenses/{id}` | Обновить расход |
| DELETE | `/api/expenses/{id}` | Удалить расход |

## 👤 Автор

**Zhulidov Aleksandr**  
[![GitHub](https://img.shields.io/badge/GitHub-finezhulidov--create-black)](https://github.com/finezhulidov-create)  
[![Telegram](https://img.shields.io/badge/Telegram-@daiwa1998-blue)](https://t.me/daiwa1998)
