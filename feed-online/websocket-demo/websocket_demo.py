#!/usr/bin/env python3
import json
import time
import requests
import websocket
import threading
import sys
import os

# Цвета для вывода
GREEN = '\033[0;32m'
BLUE = '\033[0;34m'
RED = '\033[0;31m'
YELLOW = '\033[0;33m'
NC = '\033[0m'

# Флаг для отслеживания получения сообщений
message_received = False

def on_message(ws, message):
    global message_received
    # Если сообщение начинается с MESSAGE и содержит NEW_POST, считаем его уведомлением о посте
    if message.startswith("MESSAGE") and "NEW_POST" in message:
        print(f"{GREEN}Обнаружено сообщение о новом посте!{NC}")
        message_received = True

def on_error(ws, error):
    print(f"{RED}Ошибка: {error}{NC}")

def on_open(ws):
    print(f"{GREEN}WebSocket соединение установлено{NC}")
    # Отправляем STOMP подключение с добавлением userId в заголовках
    connect_frame = "CONNECT\naccept-version:1.0,1.1,2.0\nhost:localhost\n\n\0"
    ws.send(connect_frame)

    time.sleep(1)

    # Подписываемся на канал с уведомлениями о постах
    subscribe_frame = "SUBSCRIBE\nid:sub-1\ndestination:/user/2/post/feed/posted\n\n\0"
    ws.send(subscribe_frame)
    print(f"{GREEN}Подписка на канал /user/2/post/feed/posted выполнена{NC}")

def main():
    global friend_token
    print(f"{BLUE}=== Демонстрация WebSocket обновления ленты через Nginx ==={NC}")

    # Хост сервиса - используем Nginx вместо прямого подключения
    # По умолчанию используем 'nginx' как имя контейнера в сети Docker
    host = "z-social-network-nginx"
    port = "80"
    base_url = f"http://{host}:{port}"

    print(f"{GREEN}Используем балансировщик: {base_url}{NC}")

    post_creator_id = "1"  # ID того кто генерирует пост (zaur)
    friend_id = "2"      # Слушатель поста (должен быть другом пользователя 1)

    print(f"{YELLOW}===== Шаг 1: Аутентификация друга (ID: {friend_id}) =====")
    login_data_friend = {"username": "lenin", "password": "pwd"}
    try:
        login_response = requests.post(f"{base_url}/login", json=login_data_friend)

        if login_response.status_code != 200:
            print(f"{RED}Ошибка аутентификации друга: {login_response.text}{NC}")
            sys.exit(1)

        friend_token = login_response.json()["jwt"]
    except requests.exceptions.RequestException as e:
        print(f"{RED}Ошибка соединения с сервером: {e}{NC}")
        sys.exit(1)

    # Шаг 2: Устанавливаем WebSocket соединение для слушателя
    print(f"{YELLOW}===== Шаг 2: Открываем WebSocket друга (ID: {friend_id}) =====")

    # Настройка websocket с передачей userId в URL через Nginx
    websocket.enableTrace(True)
    ws_url = f"ws://{host}:{port}/ws/websocket"

    print(f"{GREEN}Подключаемся к WebSocket URL: {ws_url}{NC}")

    # Передаем токен в заголовках для аутентификации
    headers = {"Authorization": f"Bearer {friend_token}"}

    ws = websocket.WebSocketApp(ws_url,
                                header=headers,
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error)

    # Запускаем WebSocket соединение в отдельном потоке
    wst = threading.Thread(target=ws.run_forever)
    wst.daemon = True
    wst.start()

    # ждем пока соединится гарантированно
    time.sleep(3)

    # Шаг 3: Аутентификация пользователя-создателя поста
    print(f"{YELLOW}===== Шаг 3: Аутентификация создателя поста (ID: {post_creator_id}) =====")
    login_data_creator = {"username": "zaur", "password": "pwd"}
    try:
        login_response = requests.post(f"{base_url}/login", json=login_data_creator)

        if login_response.status_code != 200:
            print(f"{RED}Ошибка аутентификации создателя поста: {login_response.text}{NC}")
            sys.exit(1)

        creator_token = login_response.json()["jwt"]
    except requests.exceptions.RequestException as e:
        print(f"{RED}Ошибка соединения с сервером: {e}{NC}")
        sys.exit(1)

    # Шаг 4: Создаем дружескую связь между пользователями (вдруг они не друзья)
    print(f"{YELLOW}===== Шаг 4: Добавление пользователя в друзья =====")

    # Добавляем lenin (ID 2) в друзья к zaur (ID 1)
    try:
        add_friend_headers = {
            "Authorization": f"Bearer {creator_token}",
            "Content-Type": "application/json"
        }
        add_friend_response = requests.post(
            f"{base_url}/friend/add?friendId={friend_id}",
            headers=add_friend_headers
        )

        if add_friend_response.status_code == 200:
            print(f"{GREEN}Пользователь zaur (ID 1) успешно добавил пользователя lenin (ID 2) в друзья{NC}")
        else:
            print(f"{GREEN}Добавление в друзья вернуло статус {add_friend_response.status_code}. Возможно, они уже друзья.{NC}")
            print(f"{GREEN}Ответ: {add_friend_response.text}{NC}")
    except requests.exceptions.RequestException as e:
        print(f"{RED}Ошибка при добавлении в друзья: {e}{NC}")

    # Даем время на обработку запросов дружбы
    time.sleep(2)

    # Шаг 5: Создаем тестовый пост от имени друга с ID = 1
    print(f"{YELLOW}===== Шаг 5: Создание поста пользователем (ID: {post_creator_id}) =====")
    post_content = f"Демонстрационный пост через Nginx для проверки WebSocket {time.ctime()}"
    post_data = {"content": post_content}

    headers = {
        "Authorization": f"Bearer {creator_token}",
        "Content-Type": "application/json"
    }

    try:
        create_response = requests.post(f"{base_url}/post/create",
                                        json=post_data,
                                        headers=headers)

        if create_response.status_code == 201 or create_response.status_code == 200:
            post_id = create_response.json().get("id")
            print(f"{GREEN}Пост успешно создан с ID: {post_id}{NC}")
            print(f"{GREEN}Содержимое: {post_content}{NC}")
        else:
            print(f"{RED}Ошибка создания поста: {create_response.text} (Статус: {create_response.status_code}){NC}")
    except requests.exceptions.RequestException as e:
        print(f"{RED}Ошибка соединения с сервером: {e}{NC}")

    # Шаг 6: Ждем уведомления через WebSocket для слушателя
    print(f"{YELLOW}===== Шаг 6: Ожидание WebSocket сообщения слушателем (20 секунд максимум) =====")

    # Ждем получения сообщения или истечения таймаута
    timeout = 20
    while timeout > 0 and not message_received:
        time.sleep(1)
        timeout -= 1
        sys.stdout.write(".")
        sys.stdout.flush()

    print("")

    if not message_received:
        print(f"{RED}Не получено WebSocket сообщение в отведенное время{NC}")
    else:
        print(f"{GREEN}Успешно получено сообщение о новом посте через WebSocket!{NC}")

    # Закрываем WebSocket соединение
    ws.close()

    print(f"{BLUE}Демонстрация завершена{NC}")
    print(f"{BLUE}Результат: {'Успешно' if message_received else 'Не удалось получить сообщение'}{NC}")

if __name__ == "__main__":
    main()