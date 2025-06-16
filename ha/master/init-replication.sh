#!/bin/bash
set -e

echo "Создаем пользователя для репликации..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replica_password';
EOSQL
echo "Пользователь replicator создан."
