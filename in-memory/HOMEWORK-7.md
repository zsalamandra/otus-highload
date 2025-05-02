# Применение In-Memory СУБД для хранения диалогов

В рамках данного домашнего задания было реализовано перенесение модуля диалогов из PostgreSQL в In-Memory СУБД Tarantool. 
Для этого была использована логика хранения и обработки сообщений на стороне Tarantool в виде хранимых функций (UDF).

# Архитектура решения
В проекте реализована гибкая архитектура для переключения между разными типами хранилищ диалогов:

**Модуль диалогов в PostgreSQL:**

* Хранение сообщений в таблице messages
* Использование SQL-запросов для операций CRUD
* Реализация через репозиторий DialogRepositoryImpl


**Модуль диалогов в Tarantool:**

* Реализация пространства данных (space) messages в Tarantool
* Создание Lua-скриптов для обработки операций с сообщениями (диалагоами)
* Реализация через репозиторий TarantoolDialogRepository
* Взаимодействие с Tarantool сделано посредством через вызов Lua-функций

**Компоненты системы Tarantool:**

* Схема данных (schema.lua)
* Модуль диалогов (dialog.lua)
* API интерфейс (api.lua)
* Инициализация (init.lua)


# Инструкция по запуску

============================ ПРОВЕРКА (Диалоги на PostgreSQL) ======================================
## 1. Сборка проекта
```shell
mvn -f ../backend/pom.xml clean package -Dmaven.test.skip=true && \
docker build --no-cache --build-arg CONFIG_FILE=application-in-memory.yaml -t zsalamandra/z-social-network-hw7 -f ../backend/Dockerfile ../backend
```

## 2. Запуск проекта и тестирование

### 2.1 Запускаем все сервисы кроме самого приложения и тестового контейнера jmeter
```shell
docker-compose up -d postgres tarantool zookeeper kafka redis
```
### 2.2 Запуск сервиса с опицией dialog.storage = postgres (нужно подождать 15 сек мин поднятия сервиса)
```shell
docker-compose run -d --name z-social-network-hw7-app -p 8085:8085 -e DIALOG_STORAGE=postgres z-social-network-hw7
```
### 2.3 Прогон теста с помощью jmeter
```bash
docker-compose run -e STORAGE_TYPE=postgres --name z-social-network-jmeter -v \results:/results jmeter
```
Результаты:
```
Операция       Запросов Мин(мс) Медиана Макс(мс) Throughput
---------------------- --------- --------- --------- --------- ---------
Отправка сообщений 1000      7         33        1096      205.34
Получение сообщений 1000      5         13        1099      201.21
```
### 2.4 очистки
```bash
docker ps -q --filter "name=z-social-network*" | xargs -r docker stop && \
docker ps -aq --filter "name=z-social-network*" | xargs -r docker rm && \
docker images | grep "^zsalamandra/" | awk '{print $3}' | xargs -r docker rmi && \
docker network ls --filter "name=social-network$" -q | xargs -r docker network rm && \
docker volume ls -qf dangling=true | xargs -r docker volume rm
```



============================ ПРОВЕРКА (Диалоги на Tarantool) ======================================
## 1. Сборка проекта
```shell
mvn -f ../backend/pom.xml clean package -Dmaven.test.skip=true && \
docker build --no-cache --build-arg CONFIG_FILE=application-in-memory.yaml -t zsalamandra/z-social-network-hw7 -f ../backend/Dockerfile ../backend
```

## 2. Запуск проекта и тестирование

### 2.1 Запускаем все сервисы кроме самого приложения и тестового контейнера jmeter
```shell
docker-compose up -d postgres tarantool zookeeper kafka redis
```
### 2.2 Запуск сервиса с опицией dialog.storage = tarantool (нужно подождать 15 сек мин поднятия сервиса)
```shell
docker-compose run -d --name z-social-network-hw7-app -p 8085:8085 -e DIALOG_STORAGE=tarantool z-social-network-hw7
```
### 2.3 Прогон теста с помощью jmeter
```bash
docker-compose run -e STORAGE_TYPE=tarantool --name z-social-network-jmeter -v \results:/results jmeter
```
Результаты:
```
Операция       Запросов Мин(мс) Медиана Макс(мс) Throughput
---------------------- --------- --------- --------- --------- ---------
Отправка сообщений 1000      4         22        679       207.60
Получение сообщений 1000      3         7         373       201.33
```
### 2.4 очистки
```bash
docker ps -q --filter "name=z-social-network*" | xargs -r docker stop && \
docker ps -aq --filter "name=z-social-network*" | xargs -r docker rm && \
docker images | grep "^zsalamandra/" | awk '{print $3}' | xargs -r docker rmi && \
docker network ls --filter "name=social-network$" -q | xargs -r docker network rm && \
docker volume ls -qf dangling=true | xargs -r docker volume rm
```


Анализ результатов данного тестирования
Сравнение производительности с PostgreSQL и Tarantool:

**Отправка сообщений:**

PostgreSQL: Медиана - 33 мс, Макс - 1096 мс
Tarantool: Медиана - 22 мс, Макс - 679 мс
Улучшение: ~33% сокращение среднего времени отклика, ~38% сокращение максимального времени


**Получение сообщений:**

PostgreSQL: Медиана - 13 мс, Макс - 1099 мс
Tarantool: Медиана - 7 мс, Макс - 373 мс
Улучшение: ~46% сокращение среднего времени отклика, ~66% сокращение максимального времени


**Пропускная способность (Throughput):**

Практически идентична для обоих хранилищ, что говорит о том, что система обрабатывает одинаковае количество запросов в секунду
Небольшое увеличение пропускной способности на Tarantool (~1%)


Интерпритация результатов:

**Преимущества Tarantool:**
Значительная лучшая производительность операций чтения
Более стабильная производительность с меньшими максимальными задержками
Улучшение производительности операций записи 


**Объяснение улучшения производительности:**
Хранение данных в оперативной памяти обеспечивает более быстрый доступ
Оптимизированные структуры данных и индексы в Tarantool (можно было лучше, но время поджимает)
Отсутствие накладных расходов на SQL-парсинг и планирование запросов, на чтение с диска (не дай бог 5400rpm)
Выполнение логики непосредственно на сервере базы данных (UDF Lua)


**Что можно было сделать лучше:**
Настройка параметров памяти Tarantool для улучшения стабильности
Оптимизация Lua-скриптов для уменьшения накладных расходов
возможно  и кэширования запросов (не уверен)


Результаты тестирования подтверждают эффективность использования in-memory решений для высаканагруженных модулей, 
особенно для сценариев с частыми операциями чтения и необходимостью низкой латентности ответа