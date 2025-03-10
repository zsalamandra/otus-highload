#!/bin/bash

# Очистка всех процессов keepalived при старте (без этого повторное восстановление не получалось)
killall -9 keepalived || true

# Удаление всех потенциальных PID-файлов
rm -f /run/vrrp.pid
rm -f /var/run/keepalived.pid
rm -f /run/keepalived.pid

# Небольшая пауза для уверенности, что все процессы завершились
sleep 1

# Запуск HAProxy
haproxy -f /usr/local/etc/haproxy/haproxy.cfg -D

# Запуск Keepalived с выводом в консоль
exec keepalived --dont-fork --log-console

