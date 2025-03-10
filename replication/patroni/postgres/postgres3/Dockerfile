FROM postgres:14

# Установка необходимых системных пакетов
RUN apt-get update && apt-get install -y \
    python3-dev \
    python3-venv \
    python3-full \
    net-tools \
    curl \
    jq \
    gosu

# Создание виртуальной среды Python и установка Patroni
RUN python3 -m venv /opt/patroni_venv
RUN /opt/patroni_venv/bin/pip install --upgrade pip
RUN /opt/patroni_venv/bin/pip install 'patroni[etcd3]' psycopg2-binary

# Создание директорий для конфигурации
RUN mkdir -p /config
RUN chown postgres:postgres /config

# Копирование файла конфигурации Patroni
COPY patroni.yml /config/patroni.yml
RUN chown postgres:postgres /config/patroni.yml

# Копирование скрипта запуска
COPY start-patroni.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/start-patroni.sh

# Порты для PostgreSQL и Patroni API
EXPOSE 5432 8008

# Запуск Patroni
CMD ["/usr/local/bin/start-patroni.sh"]