#!/bin/sh

# ====== КОНФИГУРАЦИОННЫЕ ПЕРЕМЕННЫЕ ОКРУЖЕНИЯ ======
# Определяем имя узла etcd, по умолчанию "etcd1"
ETCD_NAME=${ETCD_NAME:-etcd1}

# Список всех узлов кластера etcd с указанием их peer URL
# Формат: <имя узла>=<url>,...
# Peer URL используются для коммуникации между узлами кластера
ETCD_INITIAL_CLUSTER=${ETCD_INITIAL_CLUSTER:-etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380}

# Уникальный токен кластера - все узлы с одинаковым токеном образуют один кластер
ETCD_INITIAL_CLUSTER_TOKEN=${ETCD_INITIAL_CLUSTER_TOKEN:-etcd-cluster}

# Состояние присоединения узла к кластеру:
# "new" - создаем новый кластер
# "existing" - присоединяемся к существующему кластеру
# мы здесь все время свежее готовим
ETCD_INITIAL_CLUSTER_STATE=${ETCD_INITIAL_CLUSTER_STATE:-new}

# URL, который этот узел объявляет другим узлам для peer-to-peer соединений
# Используется для обмена данными репликации между узлами
ETCD_INITIAL_ADVERTISE_PEER_URLS=${ETCD_INITIAL_ADVERTISE_PEER_URLS:-http://${ETCD_NAME}:2380}

# URL, который этот узел объявляет клиентам для подключения
# По этому адресу другие сервисы (например, Patroni) будут соединяться с etcd
ETCD_ADVERTISE_CLIENT_URLS=${ETCD_ADVERTISE_CLIENT_URLS:-http://${ETCD_NAME}:2379}

# На каких сетевых интерфейсах и портах слушать p2p соединения
# 0.0.0.0 мы открыты всем
ETCD_LISTEN_PEER_URLS=${ETCD_LISTEN_PEER_URLS:-http://0.0.0.0:2380}

# На каких сетевых интерфейсах и портах слушать клиентские соединения
ETCD_LISTEN_CLIENT_URLS=${ETCD_LISTEN_CLIENT_URLS:-http://0.0.0.0:2379}

# ====== ЗАПУСК ETCD ======
exec etcd \
  --name ${ETCD_NAME} \
  --initial-advertise-peer-urls ${ETCD_INITIAL_ADVERTISE_PEER_URLS} \
  --listen-peer-urls ${ETCD_LISTEN_PEER_URLS} \
  --listen-client-urls ${ETCD_LISTEN_CLIENT_URLS} \
  --advertise-client-urls ${ETCD_ADVERTISE_CLIENT_URLS} \
  --initial-cluster-token ${ETCD_INITIAL_CLUSTER_TOKEN} \
  --initial-cluster ${ETCD_INITIAL_CLUSTER} \
  --initial-cluster-state ${ETCD_INITIAL_CLUSTER_STATE} \
  --data-dir /var/lib/etcd