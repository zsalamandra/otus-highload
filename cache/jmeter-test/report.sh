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
  echo "=== Результаты тестирования кеширования ==="
  echo

  # Заголовок таблицы
  printf "%-25s %-12s %-12s %-12s %-15s %-12s\n" "Тип запроса           " "Запросов    " "Мин (мс)    " "Медиана (мс)" "Макс (мс)      " "Throughput  "
  printf "%-25s %-12s %-12s %-12s %-15s %-12s\n" "-------------------------" "------------" "------------" "------------" "---------------" "------------"

  # Получение данных для Feed Request - With Cache
  WITH_CACHE_COUNT=$(jq -r '."Feed Request - With Cache".sampleCount' $STATS_FILE)
  WITH_CACHE_MIN=$(jq -r '."Feed Request - With Cache".minResTime' $STATS_FILE)
  WITH_CACHE_MEDIAN=$(jq -r '."Feed Request - With Cache".medianResTime' $STATS_FILE)
  WITH_CACHE_MAX=$(jq -r '."Feed Request - With Cache".maxResTime' $STATS_FILE)
  WITH_CACHE_THROUGHPUT=$(jq -r '."Feed Request - With Cache".throughput' $STATS_FILE)

  # Получение данных для Feed Request - Without Cache
  WITHOUT_CACHE_COUNT=$(jq -r '."Feed Request - Without Cache".sampleCount' $STATS_FILE)
  WITHOUT_CACHE_MIN=$(jq -r '."Feed Request - Without Cache".minResTime' $STATS_FILE)
  WITHOUT_CACHE_MEDIAN=$(jq -r '."Feed Request - Without Cache".medianResTime' $STATS_FILE)
  WITHOUT_CACHE_MAX=$(jq -r '."Feed Request - Without Cache".maxResTime' $STATS_FILE)
  WITHOUT_CACHE_THROUGHPUT=$(jq -r '."Feed Request - Without Cache".throughput' $STATS_FILE)

  # Получение общих данных
  TOTAL_COUNT=$(jq -r '."Total".sampleCount' $STATS_FILE)
  TOTAL_MIN=$(jq -r '."Total".minResTime' $STATS_FILE)
  TOTAL_MEDIAN=$(jq -r '."Total".medianResTime' $STATS_FILE)
  TOTAL_MAX=$(jq -r '."Total".maxResTime' $STATS_FILE)
  TOTAL_THROUGHPUT=$(jq -r '."Total".throughput' $STATS_FILE)

  # Вывод данных в таблицу
  printf "%-30s %-12s %-12s %-12s %-15s %-12.2f\n" "Feed Request - С кешем" "$WITH_CACHE_COUNT" "$WITH_CACHE_MIN" "$WITH_CACHE_MEDIAN" "$WITH_CACHE_MAX" "$WITH_CACHE_THROUGHPUT"
  printf "%-30s %-12s %-12s %-12s %-15s %-12.2f\n" "Feed Request - Без кеша" "$WITHOUT_CACHE_COUNT" "$WITHOUT_CACHE_MIN" "$WITHOUT_CACHE_MEDIAN" "$WITHOUT_CACHE_MAX" "$WITHOUT_CACHE_THROUGHPUT"
  printf "%-30s %-12s %-12s %-12s %-15s %-12s\n" "-------------------------" "------------" "------------" "------------" "---------------" "------------"
  printf "%-30s %-12s %-12s %-12s %-15s %-12.2f\n" "ИТОГО" "$TOTAL_COUNT" "$TOTAL_MIN" "$TOTAL_MEDIAN" "$TOTAL_MAX" "$TOTAL_THROUGHPUT"

  # Расчет и вывод улучшения производительности
  if [ -n "$WITH_CACHE_MEDIAN" ] && [ -n "$WITHOUT_CACHE_MEDIAN" ] && [ "$WITHOUT_CACHE_MEDIAN" != "null" ] && [ "$WITH_CACHE_MEDIAN" != "null" ]; then
    IMPROVEMENT=$(echo "scale=2; (($WITHOUT_CACHE_MEDIAN - $WITH_CACHE_MEDIAN) / $WITHOUT_CACHE_MEDIAN) * 100" | bc)
    
    echo ""
    echo "=== Анализ производительности кеширования ==="
    echo ""
    echo "Улучшение медианного времени отклика: $IMPROVEMENT%"
    
    # Оценка эффективности кеширования
    if (( $(echo "$IMPROVEMENT > 50" | bc -l) )); then
      echo "Оценка: Очень высокая эффективность кеширования (>50%)"
    elif (( $(echo "$IMPROVEMENT > 30" | bc -l) )); then
      echo "Оценка: Высокая эффективность кеширования (30-50%)"
    elif (( $(echo "$IMPROVEMENT > 10" | bc -l) )); then
      echo "Оценка: Средняя эффективность кеширования (10-30%)"
    else
      echo "Оценка: Низкая эффективность кеширования (<10%)"
    fi
    
    # Пропускная способность
    THROUGHPUT_IMPROVEMENT=$(echo "scale=2; (($WITH_CACHE_THROUGHPUT - $WITHOUT_CACHE_THROUGHPUT) / $WITHOUT_CACHE_THROUGHPUT) * 100" | bc)
    echo "Увеличение пропускной способности: $THROUGHPUT_IMPROVEMENT%"
  fi
}

# Дополнительная функция для анализа результатов в файле jtl
analyze_jtl_file() {
  JTL_FILE="/results/results.jtl"
  
  if [ -f "$JTL_FILE" ]; then
    echo ""
    echo "=== Дополнительный анализ данных ==="
    echo ""
    
    # Расчет 90%, 95% и 99% перцентилей для запросов с кешем
    echo "Перцентили времени отклика (С кешем):"
    WITH_CACHE_90=$(grep "Feed Request - With Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.9)]}')
    WITH_CACHE_95=$(grep "Feed Request - With Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.95)]}')
    WITH_CACHE_99=$(grep "Feed Request - With Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.99)]}')
    
    echo "  90%: $WITH_CACHE_90 мс"
    echo "  95%: $WITH_CACHE_95 мс"
    echo "  99%: $WITH_CACHE_99 мс"
    
    # Расчет 90%, 95% и 99% перцентилей для запросов без кеша
    echo ""
    echo "Перцентили времени отклика (Без кеша):"
    WITHOUT_CACHE_90=$(grep "Feed Request - Without Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.9)]}')
    WITHOUT_CACHE_95=$(grep "Feed Request - Without Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.95)]}')
    WITHOUT_CACHE_99=$(grep "Feed Request - Without Cache" "$JTL_FILE" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN {c=0} {a[c++]=$1} END {print a[int(c*0.99)]}')
    
    echo "  90%: $WITHOUT_CACHE_90 мс"
    echo "  95%: $WITHOUT_CACHE_95 мс"
    echo "  99%: $WITHOUT_CACHE_99 мс"
    
    # Анализ ошибок
    ERROR_COUNT=$(grep -c ",false," "$JTL_FILE")
    TOTAL_LINES=$(wc -l < "$JTL_FILE")
    ERROR_PERCENTAGE=$(echo "scale=2; ($ERROR_COUNT / $TOTAL_LINES) * 100" | bc)
    
    echo ""
    echo "Анализ ошибок:"
    echo "  Всего запросов: $TOTAL_LINES"
    echo "  Ошибок: $ERROR_COUNT ($ERROR_PERCENTAGE%)"
  else
    echo "Файл $JTL_FILE не найден. Дополнительный анализ невозможен."
  fi
}

# Функция для проверки состояния кеша конкретного пользователя
check_user_cache() {
  USER_ID=$1
  echo ""
  echo "=== Проверка кеша ленты пользователя $USER_ID ==="
  echo ""

  CACHE_DATA=$(curl -s http://z-social-network-hw4:8085/debug/cache/feed/$USER_ID)

  # Проверяем, существует ли кеш
  EXISTS=$(echo $CACHE_DATA | jq -r '.exists')

  if [ "$EXISTS" = "true" ]; then
    TTL=$(echo $CACHE_DATA | jq -r '.ttl')
    SIZE=$(echo $CACHE_DATA | jq -r '.size')

    echo "Статус кеша: АКТИВЕН"
    echo "TTL: $TTL секунд"
    echo "Количество постов в кеше: $SIZE"

    # Выводим первые 5 постов из кеша для наглядности
    echo ""
    echo "Первые 5 постов в кеше:"
    echo "-----------------------------"
    printf "%-10s %-20s\n" "Post ID" "Timestamp (score)"
    printf "%-10s %-20s\n" "-------" "----------------"

    echo $CACHE_DATA | jq -r '.posts[:5] | .[] | [.id, .score] | @tsv' |
      while IFS=$'\t' read -r id score; do
        printf "%-10s %-20s\n" "$id" "$score"
      done
  else
    echo "Статус кеша: НЕ СУЩЕСТВУЕТ"
    echo "Для пользователя $USER_ID кеш ленты не создан или был инвалидирован."
  fi
}

# Основная логика скрипта
echo "Анализ результатов тестирования кеширования..."
wait_for_file
show_stats_table
analyze_jtl_file

echo ""
echo "Анализ завершен. Полный отчет доступен в папке /results/dashboard/"

echo ""
echo "Ожидание 3 секунды перед проверкой состояния кеша..."
sleep 3

# Проверяем кеш для пользователя с ID 1
check_user_cache 1