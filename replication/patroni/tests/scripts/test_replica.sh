#!/bin/bash

export PGPASSWORD=postgres
counter=0

echo "Тестирование подключения к репликам (порт 5001)..."

while true; do
    counter=$((counter+1))
    result=$(PGPASSWORD=postgres psql -h 192.168.200.100 -p 5001 -U postgres -c "SELECT inet_server_addr(), now(), pg_is_in_recovery();" 2>&1)
    if [ $? -eq 0 ]; then
        echo "[$counter] ✅ Соединение с РЕПЛИКОЙ УСПЕШНО: $result"
    else
        echo "[$counter] ❌ Соединение с РЕПЛИКОЙ НЕУДАЧНО: $result"
    fi
    sleep 1
done