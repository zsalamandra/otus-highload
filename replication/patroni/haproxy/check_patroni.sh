#!/bin/bash
set -e

ROLE=$1
HOST=$2
PORT=$3

# Проверка роли ноды через API Patroni
RESPONSE=$(curl -s "http://${HOST}:${PORT}/patroni")
CURRENT_ROLE=$(echo $RESPONSE | jq -r '.role')

if [[ "$ROLE" == "master" && "$CURRENT_ROLE" == "master" ]]; then
    exit 0
elif [[ "$ROLE" == "replica" && "$CURRENT_ROLE" == "replica" ]]; then
    exit 0
else
    exit 1
fi