#!/bin/bash

echo "Мониторинг состояния кластера Patroni..."

while true; do
    echo -e "\n[$(date)] Состояние кластера:"

    echo -e "\nПроверка postgres1:"
    curl -s http://z-social-network-postgres1:8008/patroni | jq .

    echo -e "\nПроверка postgres2:"
    curl -s http://z-social-network-postgres2:8008/patroni | jq .

    echo -e "\nПроверка postgres3:"
    curl -s http://z-social-network-postgres3:8008/patroni | jq .

    sleep 5
done