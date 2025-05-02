FROM python:3.9-slim

# Установка необходимых пакетов
RUN pip install --no-cache-dir websocket-client requests

# Рабочая директория внутри контейнера
WORKDIR /app

# Копирование скрипта демонстрации
COPY websocket_demo.py /app/

# Делаем скрипт исполняемым
RUN chmod +x /app/websocket_demo.py

# Команда по умолчанию
ENTRYPOINT ["python", "/app/websocket_demo.py"]