FROM postgres:14

# Копируем пользовательские конфиги
COPY postgresql.conf /etc/postgresql/postgresql.conf
COPY pg_hba.conf /etc/postgresql/pg_hba.conf

# Добавляем скрипт для инициализации репликации
COPY init-replication.sh /docker-entrypoint-initdb.d/init-replication.sh
RUN chmod +x /docker-entrypoint-initdb.d/init-replication.sh

# Указываем PostgreSQL использовать пользовательский конфиг
 CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
