FROM quay.io/coreos/etcd:v3.4.15

EXPOSE 2379 2380

# скрипт запуска
COPY start-etcd.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/start-etcd.sh

# Запуск etcd с помощью скрипта
CMD ["/usr/local/bin/start-etcd.sh"]