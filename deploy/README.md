# Развёртывание на своём сервере

Всё запускается одной командой через Docker Compose: PostgreSQL, Redis, бэкенд (профиль `prod`) и веб-сервер Caddy, который раздаёт фронтенд и сам получает HTTPS-сертификат.

```
Интернет ──443──► Caddy ──► статика фронтенда (/)
                    └──────► бэкенд :8080 (/auth/*, /v1/*) ──► PostgreSQL, Redis
```

Наружу открыты только порты 80 и 443. База, Redis и бэкенд доступны только внутри Docker-сети. Swagger и `/actuator` Caddy не проксирует, снаружи их нет.

## Что понадобится

- Linux-сервер (инструкция для Ubuntu 22.04/24.04), **от 2 ГБ оперативной памяти** (на 1 ГБ добавьте swap, см. ниже) и 5–10 ГБ диска.
- **Домен**, например `cash.example.com`, с A-записью на IP сервера. Без домена HTTPS не получить: пароль и токен пойдут по сети открытым текстом.
- Свободные порты 80 и 443 (для сертификата Let's Encrypt нужны оба).

## 1. Подготовка сервера

```bash
sudo apt update && sudo apt upgrade -y

# Docker и Compose (официальный установочный скрипт)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER      # затем выйдите из SSH и зайдите снова
docker compose version             # должна быть версия 2.x

# Файрвол: только SSH, HTTP и HTTPS
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Если у сервера 1 ГБ памяти, сборка Java-проекта может упасть из-за нехватки памяти. Добавьте swap:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 2. DNS

В панели вашего регистратора домена создайте A-запись `cash.example.com` → IP сервера. Проверьте (может занять до часа):

```bash
dig +short cash.example.com        # должен показать IP сервера
```

## 3. Код на сервере

```bash
git clone <адрес-вашего-репозитория> cash-tracker
cd cash-tracker/deploy
```

Репозиторий приватный: создайте на сервере deploy-ключ (`ssh-keygen`) и добавьте публичную часть в GitHub (Settings → Deploy keys) либо скопируйте проект с компьютера: `rsync -av --exclude node_modules --exclude target ./ user@сервер:cash-tracker/`.

## 4. Настройка

```bash
cp .env.example .env
nano .env                          # впишите DOMAIN
```

Пароль базы и секрет JWT сгенерируйте командами (они сами впишутся в `.env`):

```bash
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -hex 24)|"    .env
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|"   .env
chmod 600 .env
```

| Переменная | Что это |
|---|---|
| `DOMAIN` | Ваш домен без `https://`. Caddy получит для него сертификат |
| `DB_PASSWORD` | Пароль PostgreSQL |
| `JWT_SECRET` | Секрет подписи токенов, base64 от 32 байт. Если его узнают, любой сможет войти под любым пользователем |

Необязательные: `HTTP_PORT` и `HTTPS_PORT` (если 80/443 заняты другим сервисом) и `CORS_ORIGINS` — полные адреса сайта через запятую. Если сайт открывается на нестандартном порту, обязательно задайте `CORS_ORIGINS=https://домен:порт`, иначе вход и регистрация будут отвечать 403. На обычных 80/443 это не нужно.

`.env` лежит только на сервере и не попадает в git. Сохраните его копию в менеджере паролей: без `JWT_SECRET` все пользователи просто войдут заново, а вот пароль базы нужен, чтобы подключиться к существующим данным.

## 5. Запуск

```bash
docker compose up -d --build
```

Первая сборка занимает 5–10 минут (собираются бэкенд и фронтенд). Затем:

```bash
docker compose ps                  # все четыре сервиса Up, db/redis/app — healthy
docker compose logs -f web         # Caddy: «certificate obtained successfully» (около минуты)
```

Откройте `https://cash.example.com`, нажмите «Регистрация» и создайте аккаунт. Это и есть главная проверка: если регистрация проходит и вы попадаете на «Обзор», то база, Redis, бэкенд и секрет JWT работают.

## 6. Проверка

```bash
curl -sI https://cash.example.com | head -3        # HTTP/2 200
docker compose exec app wget -qO- http://localhost:8080/actuator/health   # {"status":"UP",...}
```

Состояние в любой момент: `docker compose ps`. Если `app` не `healthy`, смотрите `docker compose logs app`.

## 7. Обновление на новую версию

```bash
cd ~/cash-tracker && git pull
cd deploy && docker compose up -d --build
```

Миграции базы выполняются сами при старте бэкенда. Данные хранятся в томе `cash-tracker-prod_db-data` и при обновлении не трогаются. Контейнеры пересоздаются, поэтому на 10–30 секунд сайт может быть недоступен.

## 8. Резервные копии

Все данные приложения лежат в PostgreSQL. Ежедневная копия в 03:00 (`crontab -e`):

```cron
0 3 * * * cd /home/ПОЛЬЗОВАТЕЛЬ/cash-tracker/deploy && docker compose exec -T db pg_dump -U cashtracker expense_tracker | gzip > /var/backups/cash-tracker-$(date +\%F).sql.gz && find /var/backups -name 'cash-tracker-*.sql.gz' -mtime +14 -delete
```

Заранее создайте папку: `sudo mkdir -p /var/backups && sudo chown $USER /var/backups`. Копии лучше периодически забирать и за пределы сервера (`scp`, `rclone`).

Восстановление в **пустую** базу (бэкенд остановите):

```bash
docker compose stop app
gunzip -c /var/backups/cash-tracker-2026-10-29.sql.gz | docker compose exec -T db psql -U cashtracker expense_tracker
docker compose start app
```

## 9. Повседневное

| Задача | Команда (из папки `deploy`) |
|---|---|
| Логи бэкенда | `docker compose logs -f --tail=100 app` |
| Перезапуск | `docker compose restart app` |
| Остановить всё, данные сохранятся | `docker compose down` |
| Свободное место | `docker system df`; чистка старых образов: `docker image prune -f` |

**Никогда не запускайте `docker compose down -v`**: флаг `-v` удаляет тома, то есть всю базу данных.

## 10. Безопасность

- **Регистрация открыта всем, кто знает адрес.** Если приложение личное, создайте свой аккаунт и закройте регистрацию: в `Caddyfile` внутрь блока сайта, до `handle @api`, добавьте
  ```
  @register path /auth/register
  handle @register {
  	respond 403
  }
  ```
  и примените: `docker compose up -d --build web`. Открыть обратно — убрать эти строки.
- Токен хранится в `localStorage` браузера на 24 часа. Поэтому в `Caddyfile` включена строгая политика CSP: скрипты разрешены только с вашего домена. Не добавляйте на сайт сторонние скрипты (аналитику, виджеты) без необходимости.
- Лимит попыток входа и регистрации: 30 в минуту с одного IP. Он работает по реальному адресу клиента (Caddy передаёт его бэкенду).
- Подтверждения email и сброса пароля нет: забытый пароль восстановить нельзя, нужна ручная правка в базе.
- Входите на сервер по SSH-ключу, а парольный вход отключите (`PasswordAuthentication no` в `/etc/ssh/sshd_config`), включите автоматические обновления безопасности: `sudo apt install unattended-upgrades`.

## Если что-то не работает

| Симптом | Причина и что делать |
|---|---|
| Браузер ругается на сертификат или сайт не открывается по HTTPS | DNS ещё не обновился или закрыты порты 80/443. Смотрите `docker compose logs web`: там написано, на каком шаге Let's Encrypt споткнулся. Проверьте `dig +short ваш-домен` и файрвол провайдера (в облаке часто есть свой, отдельный от `ufw`) |
| Приложение `healthy`, но регистрация и вход отвечают ошибкой 500 | `JWT_SECRET` не base64 или короче 32 байт: бэкенд проверяет ключ только при выдаче первого токена, поэтому стартует нормально. В `docker compose logs app` будет `WeakKeyException` или `DecodingException`. Сгенерируйте секрет заново командой из шага 4 и выполните `docker compose up -d` |
| В логах `app`: `password authentication failed` | Пароль базы запоминается при **первом** создании тома. Если вы потом поменяли `DB_PASSWORD` в `.env`, база о нём не знает. Верните прежнее значение либо задайте новое внутри базы: `docker compose exec db psql -U cashtracker -c "ALTER USER cashtracker PASSWORD 'новый'"` |
| Вход и регистрация отвечают «Forbidden» (403), хотя бэкенд здоров | Адрес сайта в браузере не совпал с `CORS_ORIGINS` (нестандартный порт, другой домен, `www.`). Задайте точный адрес в `.env` и выполните `docker compose up -d` |
| Сайт открывается, но вход пишет «Нет связи с сервером» | `app` не запущен или перезапускается. `docker compose ps`, затем логи `app` |
| 502 от Caddy сразу после запуска | Бэкенд ещё стартует (до минуты). Подождите |
| Сборка падает с `Killed` или `OOM` | Мало памяти: добавьте swap (шаг 1) |

## Проверка без домена (только для теста)

В `.env` задайте `DOMAIN=:80` — сайт заработает по `http://IP-сервера` без HTTPS. Пароль и токен при этом идут открытым текстом, в таком виде оставлять нельзя.
