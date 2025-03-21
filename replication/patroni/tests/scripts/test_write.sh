#!/bin/bash

export PGPASSWORD=postgres
counter=0
total_successful=0

# Создаем таблицу для тестов, если она не существует
PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -c "
CREATE TABLE IF NOT EXISTS write_test (
    id SERIAL PRIMARY KEY,
    message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);" 2>&1

echo "Тестирование записи в кластер через порт мастера (5000)..."

while true; do
    counter=$((counter+1))
    test_message="Test message $counter at $(date)"

    result=$(PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -c "
    INSERT INTO write_test (message) VALUES ('$test_message') RETURNING id;" 2>&1)

    if [ $? -eq 0 ]; then
        total_successful=$((total_successful+1))
        echo "[$counter] ✅ Запись УСПЕШНА: $result (Всего успешных: $total_successful)"
    else
        echo "[$counter] ❌ Запись НЕУДАЧНА: $result (Всего успешных: $total_successful)"
    fi

    sleep 1
done