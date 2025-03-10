#!/bin/bash

# Путь к файлу statistics.json
STATS_FILE="/results/dashboard/statistics.json"

# Функция для ожидания создания файла
wait_for_file() {
  echo "Ожидайте статистику..."
  while [ ! -f "$STATS_FILE" ]; do
    sleep 2
  done
}

# Функция для чтения данных из JSON и создания таблицы
show_stats_table() {
  echo "=== Результаты тестирования ==="
  echo

  # Заголовок таблицы
  printf "%-15s %-12s %-12s %-12s %-15s %-12s\n" "Транзакция     " "Запросов    " "Мин (мс)    " "Медиана (мс)" "Макс (мс)      " "Throughput  "
  printf "%-15s %-12s %-12s %-12s %-15s %-12s\n" "---------------" "------------" "------------" "------------" "---------------" "------------"

  # Получение данных для get-user
  GET_USER_COUNT=$(jq -r '.["get-user"].sampleCount' $STATS_FILE)
  GET_USER_MIN=$(jq -r '.["get-user"].minResTime' $STATS_FILE)
  GET_USER_MEDIAN=$(jq -r '.["get-user"].medianResTime' $STATS_FILE)
  GET_USER_MAX=$(jq -r '.["get-user"].maxResTime' $STATS_FILE)
  GET_USER_THROUGHPUT=$(jq -r '.["get-user"].throughput' $STATS_FILE)

  # Получение данных для profile
  PROFILE_COUNT=$(jq -r '.["profile"].sampleCount' $STATS_FILE)
  PROFILE_MIN=$(jq -r '.["profile"].minResTime' $STATS_FILE)
  PROFILE_MEDIAN=$(jq -r '.["profile"].medianResTime' $STATS_FILE)
  PROFILE_MAX=$(jq -r '.["profile"].maxResTime' $STATS_FILE)
  PROFILE_THROUGHPUT=$(jq -r '.["profile"].throughput' $STATS_FILE)

  # Получение общих данных
  TOTAL_COUNT=$(jq -r '.["Total"].sampleCount' $STATS_FILE)
  TOTAL_MIN=$(jq -r '.["Total"].minResTime' $STATS_FILE)
  TOTAL_MEDIAN=$(jq -r '.["Total"].medianResTime' $STATS_FILE)
  TOTAL_MAX=$(jq -r '.["Total"].maxResTime' $STATS_FILE)
  TOTAL_THROUGHPUT=$(jq -r '.["Total"].throughput' $STATS_FILE)

  # Вывод данных в таблицу
  printf "%-15s %-12s %-12s %-12s %-15s %-12.2f\n" "get-user" "$GET_USER_COUNT" "$GET_USER_MIN" "$GET_USER_MEDIAN" "$GET_USER_MAX" "$GET_USER_THROUGHPUT"
  printf "%-15s %-12s %-12s %-12s %-15s %-12.2f\n" "profile" "$PROFILE_COUNT" "$PROFILE_MIN" "$PROFILE_MEDIAN" "$PROFILE_MAX" "$PROFILE_THROUGHPUT"
  printf "%-15s %-12s %-12s %-12s %-15s %-12s\n" "---------------" "------------" "------------" "------------" "---------------" "------------"
  printf "%-15s %-12s %-12s %-12s %-15s %-12.2f\n" "TOTAL" "$TOTAL_COUNT" "$TOTAL_MIN" "$TOTAL_MEDIAN" "$TOTAL_MAX" "$TOTAL_THROUGHPUT"

}

wait_for_file
show_stats_table