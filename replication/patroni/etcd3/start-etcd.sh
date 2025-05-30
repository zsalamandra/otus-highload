#!/bin/sh

ETCD_NAME=${ETCD_NAME:-etcd3}

ETCD_INITIAL_CLUSTER=${ETCD_INITIAL_CLUSTER:-etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380}

ETCD_INITIAL_CLUSTER_TOKEN=${ETCD_INITIAL_CLUSTER_TOKEN:-etcd-cluster}

ETCD_INITIAL_CLUSTER_STATE=${ETCD_INITIAL_CLUSTER_STATE:-new}

ETCD_INITIAL_ADVERTISE_PEER_URLS=${ETCD_INITIAL_ADVERTISE_PEER_URLS:-http://${ETCD_NAME}:2380}

ETCD_ADVERTISE_CLIENT_URLS=${ETCD_ADVERTISE_CLIENT_URLS:-http://${ETCD_NAME}:2379}

ETCD_LISTEN_PEER_URLS=${ETCD_LISTEN_PEER_URLS:-http://0.0.0.0:2380}

ETCD_LISTEN_CLIENT_URLS=${ETCD_LISTEN_CLIENT_URLS:-http://0.0.0.0:2379}

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