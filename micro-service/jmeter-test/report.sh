#!/bin/bash

# Путь к файлу statistics.json
STATS_FILE="/results/dashboard/statistics.json"

# Функция для ожидания создания файла
wait_for_file() {
  echo "Ожидание статистики..."
  while [ ! -f "$STATS_FILE" ]; do
    sleep 2
  done
}

# Функция для чтения данных из JSON и создания таблицы
show_stats_table() {
  echo "=== Результаты тестирования производительности диалогов ==="
  echo

  # Заголовок таблицы
  printf "%-22s %-9s %-9s %-9s %-9s %-9s\n" "Операция" "Запросов" "Мин(мс)" "Медиана" "Макс(мс)" "Throughput"
  printf "%-22s %-9s %-9s %-9s %-9s %-9s\n" "----------------------" "---------" "---------" "---------" "---------" "---------"

  # Получение данных для отправки сообщений
  SEND_MSG_COUNT=$(jq -r '."Send Message - Tarantool".sampleCount' $STATS_FILE)
  SEND_MSG_MIN=$(jq -r '."Send Message - Tarantool".minResTime' $STATS_FILE)
  SEND_MSG_MEDIAN=$(jq -r '."Send Message - Tarantool".medianResTime' $STATS_FILE)
  SEND_MSG_MAX=$(jq -r '."Send Message - Tarantool".maxResTime' $STATS_FILE)
  SEND_MSG_THROUGHPUT=$(jq -r '."Send Message - Tarantool".throughput' $STATS_FILE)

  # Получение данных для получения сообщений
  GET_MSG_COUNT=$(jq -r '."Get Dialog Messages - Tarantool".sampleCount' $STATS_FILE)
  GET_MSG_MIN=$(jq -r '."Get Dialog Messages - Tarantool".minResTime' $STATS_FILE)
  GET_MSG_MEDIAN=$(jq -r '."Get Dialog Messages - Tarantool".medianResTime' $STATS_FILE)
  GET_MSG_MAX=$(jq -r '."Get Dialog Messages - Tarantool".maxResTime' $STATS_FILE)
  GET_MSG_THROUGHPUT=$(jq -r '."Get Dialog Messages - Tarantool".throughput' $STATS_FILE)

  # Вывод данных в таблицу
  printf "%-22s %-9s %-9s %-9s %-9s %-9.2f\n" "Отправка сообщений" "$SEND_MSG_COUNT" "$SEND_MSG_MIN" "$SEND_MSG_MEDIAN" "$SEND_MSG_MAX" "$SEND_MSG_THROUGHPUT"
  printf "%-22s %-9s %-9s %-9s %-9s %-9.2f\n" "Получение сообщений" "$GET_MSG_COUNT" "$GET_MSG_MIN" "$GET_MSG_MEDIAN" "$GET_MSG_MAX" "$GET_MSG_THROUGHPUT"

  if [ -n "$STORAGE_TYPE" ]; then
    echo ""
    jq '{
      "Send Message": {
        "count": ."Send Message - Tarantool".sampleCount,
        "min": ."Send Message - Tarantool".minResTime,
        "median": ."Send Message - Tarantool".medianResTime,
        "max": ."Send Message - Tarantool".maxResTime,
        "throughput": ."Send Message - Tarantool".throughput
      },
      "Get Dialog Messages": {
        "count": ."Get Dialog Messages - Tarantool".sampleCount,
        "min": ."Get Dialog Messages - Tarantool".minResTime,
        "median": ."Get Dialog Messages - Tarantool".medianResTime,
        "max": ."Get Dialog Messages - Tarantool".maxResTime,
        "throughput": ."Get Dialog Messages - Tarantool".throughput
      }
    }' $STATS_FILE > "/results/results.json"
  fi
}

# Основная логика
echo "Анализ результатов тестирования..."
wait_for_file
show_stats_table

echo ""
echo "Анализ завершен. Полный отчет доступен в папке /results/dashboard/"