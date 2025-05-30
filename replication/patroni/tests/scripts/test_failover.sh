#!/bin/bash
#✅❌
# Устанавливаем пароль для PostgreSQL и инициализируем счетчики для отслеживания
# выполненных транзакций
export PGPASSWORD=postgres
counter=0
total_successful=0
total_transactions=0

# Переменная для хранения имени остановленного мастера
original_master=""

# Флаг для отслеживания статуса теста
# 0 - начальная фаза, мастер ещё не остановлен
# 1 - мастер остановлен, ждем переключения и стабилизации
# 2 - мастер восстановлен, можно проверять репликацию
test_phase=0

# Выводим заголовок тестирования
echo "================================================================="
echo "          ТЕСТ ОТКАЗОУСТОЙЧИВОСТИ POSTGRESQL КЛАСТЕРА            "
echo "================================================================="
echo
echo "Этот тест проверяет поведение кластера при отказе мастер-узла."
echo "Скрипт автоматически определит текущий мастер и подготовит команду"
echo "для его остановки, которую вы выполните на следующем шаге."
echo

# Создаю функцию для определения текущего мастера в кластере PostgreSQL.
# Так как нам нужно знать, какой узел является мастером,
# чтобы его "убить" и проверить, как кластер справится с переключением на новый мастер.
get_current_master() {
    local found_master="Unknown"
    local current_role=""

    # Перебираем все узлы нашего кластера Patroni
    # В моем случае я развернул три узла
    for node in "z-social-network-postgres1" "z-social-network-postgres2" "z-social-network-postgres3"; do

        # Используем API Patroni для получения роли узла.
        # Он поддерживает API на порту 8008, который позволяет узнать статус узла
        current_role=$(curl -s http://$node:8008/patroni | jq -r '.role' 2>/dev/null)

        # Проверяем, является ли узел мастером (primary)
        # В Patroni мастер обозначается как "primary" (где то читал вроде master было)
        if [[ "$current_role" == "primary" ]]; then
            found_master=$node
            break
        fi
    done

    # Возвращаем имя мастера
    printf "%s" "$found_master"
}

# Функция для проверки статуса контейнера (запущен или нет)
# Это нужно, чтобы определить, когда бывший мастер снова поднимется
# Не удалось определить через docker - нет клиента внутри контейнера
# Не удалось через HaProxy - он выдает CSV и JSON билиберду, которую хер распарсишь
# А если патрони спрашивать - он как сам поднлся так сразу "running"
check_container_status() {
   local container_name="$1"
     local postgres_port="${2:-5432}"
     local postgres_user="${3:-postgres}"
     local postgres_password="${4:-postgres}"

     export PGPASSWORD="$postgres_password"

     # Попытка подключения к PostgreSQL с малым таймаутом
     local response
     response=$(PGCONNECT_TIMEOUT=2 psql -h "$container_name" -p "$postgres_port" -U "$postgres_user" -c "SELECT 1 as is_running;" -t 2>/dev/null)

     # Удаляем пробелы из ответа, хотя навряд ли они там есть
     response=$(echo "$response" | tr -d '[:space:]')

     if [[ "$response" == "1" ]]; then
         echo "running"
     else
         echo "stopped"
     fi
}

# Функция для подсчета строк в таблице, в которую пишем
# Это критически важно для проверки консистентности данных при отказе мастера
# Синхронная репликация должна гарантировать, что данные не потеряются
count_rows() {
    # Подключаемся к HAProxy на порт 5000, который направляет на текущий мастер
    # Выполняем SQL-запрос и получаем количество строк
    count=$(PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -t -c "SELECT COUNT(*) FROM failover_test;" 2>/dev/null)
    echo $count
}

# Функция для проверки восстановленного узла
# Вызывается, когда мы обнаруживаем, что остановленный мастер снова запущен
check_restored_node() {
    local restored_node="$1"

    echo
    echo "=== ПРОВЕРКА ВОССТАНОВЛЕННОГО УЗЛА ==="
    echo "Проверяем, что бывший мастер - (${restored_node}) корректно подключился к кластеру"
    echo "и восстановил пропущенные данные через репликацию"

    # Даем время на восстановление и подключение к кластеру
    echo "Ожидаем 30 секунд для завершения первоначальной синхронизации..."
    sleep 30

    # Проверяем роль восстановленного узла
    echo "Проверка роли восстановленного узла..."
    local node_role
    node_role=$(curl -s "http://${restored_node}:8008/patroni" | jq -r '.role' 2>/dev/null)

    if [[ "${node_role}" == "replica" ]]; then
        echo "✅ Узел ${restored_node} успешно подключился к кластеру в роли реплики"
    else
        echo "❌ Узел ${restored_node} имеет неожиданную роль: ${node_role} (ожидалась 'replica')"
        echo "Возможно, необходимо проверить настройки Patroni или копаться дальше"
    fi

    # Получаем количество строк в основной таблице (через мастер)
    local master_count
    master_count=$(PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -t -c "SELECT COUNT(*) FROM failover_test;" 2>/dev/null)
    master_count=${master_count:-0}  # Если получили пустую строку, считаем 0

    # Получаем количество строк в восстановленной реплике
    # Здесь используем прямое подключение к контейнеру, так как HAProxy подключится к мастеру
    local replica_count
    replica_count=$(PGPASSWORD=postgres psql -h "${restored_node}" -p 5432 -U postgres -t -c "SELECT COUNT(*) FROM failover_test;" 2>/dev/null)
    replica_count=${replica_count:-0}  # Если получили пустую строку, считаем 0

    echo
    echo "--- ПРОВЕРКА КОНСИСТЕНТНОСТИ ДАННЫХ НА ВОССТАНОВЛЕННОЙ РЕПЛИКЕ ---"
    echo "Количество строк в таблице на мастере: ${master_count}"
    echo "Количество строк в таблице на восстановленной реплике: ${replica_count}"

    if [ "${master_count}" -eq "${replica_count}" ]; then
        echo "✅ ДАННЫЕ КОНСИСТЕНТНЫ - Восстановленная реплика получила все данные"
        echo "Тест успешно завершен! Отказоустойчивость и репликация работают корректно."
    else
        local diff
        diff=$((master_count - replica_count))
        echo "❌ ОБНАРУЖЕНО РАСХОЖДЕНИЕ ДАННЫХ - На реплике отсутствует ${diff} строк!"
        echo "Возможно, необходимо больше времени для полной синхронизации."
        echo "Можно изменить время ожидания для полной синхронизации, и попробовать еще."
    fi

    # Тест завершен, выходим
    exit 0
}

# Определяем текущий мастер
echo "Определяем текущий мастера..."

current_master=$(get_current_master)
echo "Мастер: $current_master"

# Сохраняем имя исходного мастера для последующих проверок
original_master=$current_master

# Проверяем, удалось ли определить мастер
# Если не удалось - значит что-то не так с кластером, и тестить не получится
if [[ "$current_master" == "Unknown" ]]; then
    echo "❌ Не удалось определить мастера. Видать какая то лажа с кластером. Приходите завтра"
    exit 1
fi

# Выводим инструкции для пользователя о том, что нужно будет сделать
# Подготавливаем команду для "убийства" мастера,
# что позволит протестировать отказоустойчивость
echo
echo "****************** ИНФОРМАЦИЯ **************************************"
echo "Для выполнения теста потребуется:"
echo "1. Сначала остановить текущий мастер командой:"
echo "   docker stop $current_master"
echo "2. ПОСЛЕ стабилизации кластера снова запустить остановленный контейнер:"
echo "   docker start $current_master"
echo "---------------------------------------------------------------------"
echo "Скрипт автоматически обнаружит восстановление контейнера и проверит,"
echo "корректно ли работает репликация на восстановленный узел."
echo "---------------------------------------------------------------------"
echo

# Ждем, пока пользователь неторопливо подготовит второй терминал и будет готов!!!!
read -p "Если готовы начать тест, нажмите [Enter]: "

# Создаем тестовую таблицу для записи данных
# Используем IF NOT EXISTS, чтобы избежать лажи, если таблица уже есть
# (такое тоже может быть - повторный запуск хехе)
echo "Создание тестовой таблицы..."
echo "Если таблица была (повторный запуск теста) это учитываетс в тесте"
PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -c "
CREATE TABLE IF NOT EXISTS failover_test (
    id SERIAL PRIMARY KEY,
    message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);" 2>&1

# Выводим информацию о начале теста
echo
echo "=== ТЕСТ ОТКАЗОУСТОЙЧИВОСТИ КЛАСТЕРА =========================="
echo "Запись данных и мониторинг при отказе мастера"
echo "Проверка синхронной репликации - проверим, что данные не теряются"
echo

# Напоминаем о необходимости остановить мастер
echo "ВНИМАНИЕ: Текущий мастер: $current_master"
echo "Выполните команду остановки мастера через 10-15 секунд после начала записи данных"
echo

# Получаем начальное количество строк в таблице
# Это нужно, если мы запускаем тест повторно на существующей таблице
initial_count=$(count_rows)
initial_count=${initial_count:-0}  # Если получили пустую строку, считаем это за 0
echo "Начальное количество строк в таблице: $initial_count"

# Последний шанс даю подготовиться перед началом непрерывной записи
read -p "Нажмите [Enter] для начала непрерывной записи данных... "

# Явно информируем о начале теста и напоминаем команду остановки
echo "НАЧИНАЕМ ЗАПИСЬ ДАННЫХ..."
echo "ТЕПЕРЬ ВЫПОЛНИТЕ КОМАНДУ ОСТАНОВКИ МАСТЕРА В ДРУГОМ ТЕРМИНАЛЕ!"
echo "docker stop $current_master"
echo

# Основной цикл теста, в котором будем непрерывно записываться данные
# Цикл будет работать до тех пор, пока не обнаружим, что остановленный мастер снова поднят
while true; do
    # Увеличиваем счетчик итераций для уникальности сообщений
    counter=$((counter+1))
    test_message="Failover test $counter at $(date)"

    # Определяем текущий мастер перед каждой записью
    # Это нужно для наблюдения за процессом переключения - мы увидим,
    # когда операции начнут выполняться через новый мастер после отказа
    current_master=$(get_current_master)

    # Пытаемся записать данные в БД
    # Подключаемся через HAProxy (192.168.200.100:5000), который автоматически (надеюсь)
    # направит запрос на текущий мастер
    result=$(PGPASSWORD=postgres psql -h 192.168.200.100 -p 5000 -U postgres -c "
    INSERT INTO failover_test (message) VALUES ('$test_message') RETURNING id;" 2>&1)

    # Проверяем, успешна ли была запись (вставка втбалицу) одной чудо командой
    if [ $? -eq 0 ]; then
        # Если запись успешна, увеличиваем счетчики и выводим ID
        # Это нужна для отслеживания процесса и проверки консистентности
        # Сюда будем попадать если есть мастер, пока его нет, обработка за(после) else
        total_successful=$((total_successful+1))
        total_transactions=$((total_transactions+1))
        inserted_id=$(echo $result | grep -o '[0-9]\+' | head -1)
        echo "[$counter] ✅ Запись УСПЕШНА: ID=$inserted_id через мастер $current_master (Успешно: $total_successful/$total_transactions)"

        # Если текущий мастер отличается от оригинального, значит произошла переключение
        # Устанавливаем фазу теста = 1 (мастер остановлен)
        if [[ "$current_master" != "$original_master" && $test_phase -eq 0 ]]; then
            test_phase=1
            echo
            echo "ОБНАРУЖЕНО ПЕРЕКЛЮЧЕНИЕ МАСТЕРА: $original_master -> $current_master"
            echo "Кластер успешно выбрал новый мастер-узел! Продолжаем запись..."
            echo
            echo "Теперь можно запустить остановленный контейнер командой:"
            echo "docker start $original_master"
            echo
        fi
    else
        # Если запись не удалась, выводим ошибку и увеличиваем счетчик транзакций
        # В момент отказа мастера и переключения ролей здесь будут появляться ошибки,
        # не боимся, держим кулачки, мы выйдем из этой ямы (не 100%)
        total_transactions=$((total_transactions+1))
        echo "[$counter] ❌ Запись НЕУДАЧНА: $result (Успешно: $total_successful из $total_transactions)"
        echo "      Текущий мастер: $current_master"

        # При ошибке ждем дольше, чтобы дать время кластеру стабилизироваться/успокоиться/принять действительность
        # После отказа мастера Patroni нужно время для повышения реплики до мастера
        sleep 5
    fi

    # Каждые 10 итераций выполняем дополнительные проверки
    if [ $((counter % 10)) -eq 0 ]; then
        # 1. Проверяем консистентность данных
        current_count=$(count_rows)
        current_count=${current_count:-0}  # Если получили пустую строку, считаем 0
        expected_count=$((initial_count + total_successful))

        # Выводим статистику консистентности
        echo
        echo "--- ПРОВЕРКА КОНСИСТЕНТНОСТИ ДАННЫХ ---"
        echo "Ожидаемое количество строк: $expected_count"
        echo "Фактическое количество строк: $current_count"

        # Сравниваем ожидаемое и фактическое количество строк
        # При правильно настроенной синхронной репликации ожидания самый радужные -
        # мы не должны потерять данные, даже при отказе мастера!!!
        if [ "$current_count" -eq "$expected_count" ]; then
            echo "✅ ДАННЫЕ КОНСИСТЕНТНЫ - Все транзакции сохранены"
        else
            difference=$((expected_count - current_count))
            echo "❌ ОБНАРУЖЕНА ПОТЕРЯ ДАННЫХ - Отсутствует $difference строк!"
        fi
        echo

        # 2. Проверяем, не восстановлен ли исходный мастер (только после обнаружения переключения)
        if [ $test_phase -eq 1 ]; then
            container_status=$(check_container_status "$original_master")
            echo "Состояние ранее остановленного контейнера - $original_master: $container_status"
            if [[ "$container_status" == "running" ]]; then
                echo "ОБНАРУЖЕН ЗАПУСК ОСТАНОВЛЕННОГО МАСТЕРА: $original_master"
                echo "Переходим к проверке репликации на восстановленный узел..."
                # Устанавливаем фазу теста = 2 (контейнер восстановлен)
                test_phase=2

                # Вызываем функцию проверки восстановленного узла
                # Эта функция также завершит скрипт после выполнения проверок
                check_restored_node "$original_master"
            else
                echo "⏳ Ожидаем восстановления контейнера $original_master..."
            fi
        fi
    fi

    # Небольшая пауза перед следующей итерацией для более равномерной нагрузки
    sleep 1
done