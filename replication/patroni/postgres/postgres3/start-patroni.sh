#!/bin/bash

# Проверяем существование директории для данных
if [ ! -d "/data" ]; then
    mkdir -p /data
fi

# Назначаем владельца директории данных
chown -R postgres:postgres /data
chmod 700 /data

# Запускаем Patroni от имени пользователя postgres
exec gosu postgres /opt/patroni_venv/bin/patroni /config/patroni.yml