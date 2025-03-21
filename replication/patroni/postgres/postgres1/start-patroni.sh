#!/bin/bash

# Проверяем существование директории для данных
if [ ! -d "/data" ]; then
    mkdir -p /data
fi

# Создаем подкаталог pgdata внутри смонтированного тома
mkdir -p /var/lib/postgresql/data/pgdata
chown postgres:postgres /var/lib/postgresql/data/pgdata
chmod 700 /var/lib/postgresql/data/pgdata

# Назначаем владельца директории данных
chown -R postgres:postgres /data
chmod 700 /data

# Запускаем Patroni от имени пользователя postgres
exec gosu postgres /opt/patroni_venv/bin/patroni /config/patroni.yml