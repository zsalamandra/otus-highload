#!/bin/bash

MAX_ID=40
TARGET_CONTAINER=${1:-z-social-network-hw9-app-2}
THRESHOLD=15
NGINX_URL="http://localhost:8080/user/get"
LOGIN_URL="http://localhost:8080/login"
USERNAME="zaur"
PASSWORD="pwd"

login_response=$(curl -s -X POST "$LOGIN_URL" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\", \"password\":\"$PASSWORD\"}")

# Извлекаем JWT из поля "jwt"
token=$(echo "$login_response" | grep -o '"jwt":"[^"]*"' | sed 's/"jwt":"\(.*\)"/\1/')

for (( i=1; i<=MAX_ID; i++ ))
do
  echo "GET /get/$i"

  response=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $token" \
    "$NGINX_URL/$i")

  echo "Ответ: HTTP $response"

  if [ "$i" -eq "$THRESHOLD" ]; then
    echo "останавливаем контейнер $TARGET_CONTAINER"
    docker stop $TARGET_CONTAINER
  fi

  sleep 0.2
done

echo "Проверка завершён."
