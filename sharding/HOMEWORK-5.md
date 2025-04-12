# Шардирование подсистемы диалогов

## Аналитика и архитектурные решения

### Выбор стратегии шардирования
Для шардирования сообщений диалогов выбрана стратегия: **шардирование по хешу от упорядоченной пары ID пользователей**.

Ключ шардирования: `hash(min(user1_id, user2_id), max(user1_id, user2_id))`

плюсы подхода:
1. Сообщения одного диалога всегда находятся на одном шарде
2. Нагрузка от "звезд" распределяется по разным шардам (диалоги с разными пользователями попадают на разные шарды)
3. Запрос истории диалога требует обращения только к одному шарду

### Схема данных
Таблица сообщений:
- id: первичный ключ
- from_user_id: ID отправителя
- to_user_id: ID получателя
- content: текст сообщения
- dialog_id: ключ шардирования (хеш от min/max пары ID)
- created_at: время создания сообщения

## Инструкция по запуску

### 1. Билдим проект и готовим образ
```bash
mvn -f ../backend/pom.xml clean package -Dmaven.test.skip=true && \
docker build --no-cache --build-arg CONFIG_FILE=application-sharding.yaml -t zsalamandra/z-social-network-hw5 -f ../backend/Dockerfile ../backend  
```

### 2. Запуск проекта 
```shell
docker-compose up -d
```
#### 2.1 Настройка кластера Citus
Для организации шардинга производим след шаги:
 - Добавление двух рабочих узла в кластер
 - Создаем распределенную таблицу messages по ключу шардирования dialog_id
 - Создаем индексы для оптимизации запросов
```shell
docker exec -it z-social-network-citus-coordinator bash -c "
PGPASSWORD=citus psql -U citus -d z-social-network -c \"SELECT * FROM citus_add_node('citus-worker-1', 5432);\" &&
PGPASSWORD=citus psql -U citus -d z-social-network -c \"SELECT * FROM citus_add_node('citus-worker-2', 5432);\" &&
PGPASSWORD=citus psql -U citus -d z-social-network -c \"SELECT create_distributed_table('messages', 'dialog_id');\" &&
PGPASSWORD=citus psql -U citus -d z-social-network -c \"CREATE INDEX IF NOT EXISTS idx_messages_dialog_id ON messages(dialog_id);\" &&
PGPASSWORD=citus psql -U citus -d z-social-network -c \"CREATE INDEX IF NOT EXISTS idx_messages_from_user_id ON messages(from_user_id);\" &&
PGPASSWORD=citus psql -U citus -d z-social-network -c \"CREATE INDEX IF NOT EXISTS idx_messages_to_user_id ON messages(to_user_id);\"
"
```

### 3. Тестирование шардирования

### 3.1 Проверка настройки распределения таблицы
Проверка статуса распределения таблицы, и что таблица использует метод h (hash)
```shell
docker exec -it z-social-network-citus-coordinator psql -U citus -d z-social-network -c "
SELECT logicalrelid, partmethod, partkey FROM pg_dist_partition WHERE logicalrelid = 'messages'::regclass;
"
```
Ответ:
```
 logicalrelid | partmethod |                                                                    partkey                                                                     
--------------+------------+------------------------------------------------------------------------------------------------------------------------------------------------
 messages     | h          | {VAR :varno 1 :varattno 5 :vartype 25 :vartypmod -1 :varcollid 100 :varnullingrels (b) :varlevelsup 0 :varnosyn 1 :varattnosyn 5 :location -1}
(1 row)
```
Видим, что таблица использует метод h (hash) для распределения, что соответствует нашей стратегии.

### 3.2 Проверка созданных шардов
Проверяем, какие шарды созданы и как они распределены между рабочими узлами:
```shell
docker exec -it z-social-network-citus-coordinator psql -U citus -d z-social-network -c "
SELECT s.shardid, s.logicalrelid, p.groupid, n.nodename 
FROM pg_dist_shard s
JOIN pg_dist_placement p ON s.shardid = p.shardid
JOIN pg_dist_node n ON p.groupid = n.groupid
WHERE s.logicalrelid = 'messages'::regclass
ORDER BY s.shardid;
"
```
Ответ:
```
 shardid | logicalrelid | groupid |    nodename    
---------+--------------+---------+----------------
  102008 | messages     |       1 | citus-worker-1
  102009 | messages     |       2 | citus-worker-2
  102010 | messages     |       1 | citus-worker-1
  102011 | messages     |       2 | citus-worker-2
  102012 | messages     |       1 | citus-worker-1
  102013 | messages     |       2 | citus-worker-2
  102014 | messages     |       1 | citus-worker-1
  102015 | messages     |       2 | citus-worker-2
  102016 | messages     |       1 | citus-worker-1
  102017 | messages     |       2 | citus-worker-2
  102018 | messages     |       1 | citus-worker-1
  102019 | messages     |       2 | citus-worker-2
  102020 | messages     |       1 | citus-worker-1
  102021 | messages     |       2 | citus-worker-2
  102022 | messages     |       1 | citus-worker-1
  102023 | messages     |       2 | citus-worker-2
  102024 | messages     |       1 | citus-worker-1
  102025 | messages     |       2 | citus-worker-2
  102026 | messages     |       1 | citus-worker-1
  102027 | messages     |       2 | citus-worker-2
  102028 | messages     |       1 | citus-worker-1
  102029 | messages     |       2 | citus-worker-2
  102030 | messages     |       1 | citus-worker-1
  102031 | messages     |       2 | citus-worker-2
  102032 | messages     |       1 | citus-worker-1
  102033 | messages     |       2 | citus-worker-2
  102034 | messages     |       1 | citus-worker-1
  102035 | messages     |       2 | citus-worker-2
  102036 | messages     |       1 | citus-worker-1
  102037 | messages     |       2 | citus-worker-2
  102038 | messages     |       1 | citus-worker-1
  102039 | messages     |       2 | citus-worker-2
```
Ответ показывает, что таблица messages разделена на несколько шардов, 
которые равномерно распределены между рабочими узлами citus-worker-1 и citus-worker-2.

### 3.3 Анализ плана запроса
Проверяем, что запросы эффективно направляются только на нужный шард:
```shell
docker exec -it z-social-network-citus-coordinator psql -U citus -d z-social-network -c "
EXPLAIN ANALYZE SELECT * FROM messages WHERE dialog_id = '1_2';
"
```
Результат:
```
                                                                    QUERY PLAN                                                                    
--------------------------------------------------------------------------------------------------------------------------------------------------
 Custom Scan (Citus Adaptive)  (cost=0.00..0.00 rows=0 width=0) (actual time=13.456..13.456 rows=0 loops=1)
   Task Count: 1
   Tuple data received from nodes: 0 bytes
   Tasks Shown: All
   ->  Task
         Tuple data received from node: 0 bytes
         Node: host=citus-worker-1 port=5432 dbname=z-social-network
         ->  Bitmap Heap Scan on messages_102014 messages  (cost=4.17..11.28 rows=3 width=96) (actual time=0.005..0.005 rows=0 loops=1)
               Recheck Cond: (dialog_id = '1_2'::text)
               ->  Bitmap Index Scan on idx_messages_dialog_id_102014  (cost=0.00..4.17 rows=3 width=0) (actual time=0.002..0.002 rows=0 loops=1)
                     Index Cond: (dialog_id = '1_2'::text)
             Planning Time: 0.250 ms
             Execution Time: 0.014 ms
 Planning Time: 1.269 ms
 Execution Time: 13.482 ms
```
Анализ плана запроса показывает, что:
Task Count: 1 - запрос выполняется только на одном шарде, что подтверждает эффективность выбранной стратегии шардирования
Запрос направляется на конкретный узел (citus-worker-1) и конкретный шард (messages_102014)
Используется индекс по dialog_id для быстрого поиска

### 3.4 Тестирование с реальными данными
Вставляем тестовые сообщения для разных диалогов:
```shell
docker exec -i z-social-network-citus-coordinator psql -U citus -d z-social-network << EOF

-- Диалог между пользователями 1 и 2
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (1, 2, 'Привет, как дела?', '1_2');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (2, 1, 'У меня всё отлично, спасибо!', '1_2');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (1, 2, 'Чем занимаешься?', '1_2');

-- Диалог между пользователями 1 и 3
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (1, 3, 'Привет, пользователь 3!', '1_3');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (3, 1, 'Привет, пользователь 1!', '1_3');

-- Диалог между пользователями 2 и 3
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (2, 3, 'Привет, я пользователь 2', '2_3');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (3, 2, 'Рад знакомству!', '2_3');

-- Диалог между пользователями 4 и 5
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (4, 5, 'Здравствуйте, я пользователь 4', '4_5');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (5, 4, 'Здравствуйте! Как ваши дела?', '4_5');

-- Диалог между пользователями 10 и 20
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (10, 20, 'Это сообщение от пользователя 10', '10_20');
INSERT INTO messages (from_user_id, to_user_id, content, dialog_id) 
VALUES (20, 10, 'Это ответ от пользователя 20', '10_20');
EOF
```
Проверка распределения диалогов по шардам:
```shell
docker exec -it z-social-network-citus-coordinator psql -U citus -d z-social-network -c "
SELECT dialog_id, COUNT(*) as msg_count FROM messages GROUP BY dialog_id ORDER BY dialog_id;
"
```
Результат:
```
 dialog_id | msg_count 
-----------+-----------
 10_20     |         2
 1_2       |         3
 1_3       |         2
 2_3       |         2
 4_5       |         2
```
Видим, что сообщения распределяются по шардам равномерно

## 4 Процесс решардинга без даунтайма
Для решардинга данных в Citus без даунтайма можно использовать следующий шаги:

### 4.1. Создание новой таблицы с нужной структурой шардирования
```sql
CREATE TABLE messages_new (
id BIGSERIAL,
from_user_id BIGINT NOT NULL,
to_user_id BIGINT NOT NULL,
content TEXT NOT NULL,
dialog_id TEXT NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT NOW(),
PRIMARY KEY (id, dialog_id)
);
SELECT create_distributed_table('messages_new', 'dialog_id');
```
### 4.2. Перенос данных из старой таблицы в новую:
```sql
INSERT INTO messages_new (from_user_id, to_user_id, content, dialog_id, created_at)
SELECT from_user_id, to_user_id, content, dialog_id, created_at
FROM messages;
```
### 4.3. Обновление приложения для записи в обе таблицы - на этом этапе новые сообщения записываются как в старую, так и в новую таблицу, а чтение происходит из старой.
### 4.4. Синхронизация данных - переносим все новые записи, которые могли появиться во время миграции.
### 4.5. Переключение чтения на новую таблицу - после полной синхронизации данных приложение переключает чтение на новую таблицу.
### 4.6. Переименование таблиц:
```sql
ALTER TABLE messages RENAME TO messages_old;
ALTER TABLE messages_new RENAME TO messages;
```
### 4.7. Обновление приложения для использования только новой таблицы - приложение перестает писать в старую таблицу и использует только новую.
### 4.8. Удаление старой таблицы после завершения перехода:
```sql
DROP TABLE messages_old;
```
Этот процесс обеспечивает бесшовную миграцию без прерывания работы приложения.


### 5. Очистки
```bash
docker ps -q --filter "name=z-social-network*" | xargs -r docker stop && \
docker ps -aq --filter "name=z-social-network*" | xargs -r docker rm && \
docker images | grep "^zsalamandra/" | awk '{print $3}' | xargs -r docker rmi && \
docker network ls --filter "name=social-network$" -q | xargs -r docker network rm && \
docker volume ls -qf dangling=true | xargs -r docker volume rm
```