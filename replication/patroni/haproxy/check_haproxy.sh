#!/bin/bash

# Проверка, запущен ли HAProxy
if pidof haproxy > /dev/null ; then
    exit 0
else
    exit 1
fi