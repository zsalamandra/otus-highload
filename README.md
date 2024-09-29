# Сервис zSocialNetwork

## Домашняя работа №1

### Нефункциональные требования:
- Любой язык программирования
- В качестве базы данных использовать PostgreSQL (при желании и необходимости любую другую SQL БД)
- Не использовать ORM
- Программа должна представлять собой монолитное приложение.
- Не рекомендуется использовать следующие технологии:
- Репликация
- Шардирование
- Индексы
- Кэширование

### Функциональные требования
Реализовать следующие эндпойнты:
- /login
- /user/register
- /user/get/{id}

### Критерии оценки:
Оценка происходит по принципу зачет/незачет.
Требования:
- Есть возможность авторизации, регистрации, получение анкет по ID.
- Отсутствуют SQL-инъекции.
- Пароль хранится безопасно.

### Запуск/Проверки

Для запуска приложения необходимо запустить docker-compose файл в корне проекта командой
```bash
docker-compose up -d
```

Проверить, что проект запустился и нет ошибок можно командой 
```bash
docker logs z-social-network-service
```

Запуск Postman скриптов в newman:
```bash
newman run otus-highload.postman_collection.json
```
_Процедура получения токена по url'у /login реализовано автоматически с помощью Pre-request скрипта_


#### Проверка cURL'ом:

##### Регистрация
```bash
curl --location 'http://localhost:8085/user/register' \
--header 'Content-Type: application/json' \
--data '{
    "username": "zaur",
    "firstName": "John",
    "lastName": "Doe",
    "birthDate": "1990-01-01",
    "gender": "male",
    "interests": "programming",
    "city": "Berlin",
    "password": "zaur"
}'
```

##### Авторизация (получение токена)
```bash
curl --location 'http://localhost:8085/login' \
--header 'Content-Type: application/json' \
--data '{
    "username": "zaur",
    "password": "zaur"
}'
```

##### Получение анкеты _(токен необходимо приложить, полученный на запрос /login)_
```bash
curl --location 'http://localhost:8085/user/get/2' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ6YXVyIiwiaWF0IjoxNzI3NjA4OTA1LCJleHAiOjE3Mjc2NDQ5MDV9.qfHpPBUQfdEcWen0_j_vfbB5fMGo1OEvcEJ3t5NDKl8'
```
