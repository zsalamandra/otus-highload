FROM haproxy:2.4

USER root

# Установка Keepalived и необходимых инструментов
RUN apt-get update && apt-get install -y \
    keepalived \
    iproute2 \
    curl \
    jq \
    procps \
    socat

COPY haproxy-master/haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg
COPY haproxy-master/keepalived.master.conf /etc/keepalived/keepalived.conf
COPY check_haproxy.sh /usr/local/bin/check_haproxy.sh

RUN chmod +x /usr/local/bin/check_haproxy.sh

RUN chmod 644 /etc/keepalived/keepalived.conf

# Копирование скрипта запуска
COPY start.sh /usr/local/bin/start.sh
RUN chmod +x /usr/local/bin/start.sh

EXPOSE 5000 5001 7000

CMD ["/usr/local/bin/start.sh"]