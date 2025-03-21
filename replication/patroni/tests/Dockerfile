FROM alpine:latest

# Установка необходимых инструментов
RUN apk add --no-cache \
    postgresql-client \
    bash \
    curl \
    jq

WORKDIR /app

# Копирование внешних скриптов
COPY scripts/test_master.sh /app/test_master.sh
COPY scripts/test_replica.sh /app/test_replica.sh
COPY scripts/monitor_cluster.sh /app/monitor_cluster.sh
COPY scripts/test_write.sh /app/test_write.sh
COPY scripts/test_failover.sh /app/test_failover.sh
COPY scripts/run.sh /app/run.sh

RUN chmod +x /app/*.sh

CMD ["/bin/bash"]