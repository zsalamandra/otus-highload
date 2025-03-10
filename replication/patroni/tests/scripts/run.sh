#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Обработчик для перехвата Ctrl+C
trap handle_interrupt INT

handle_interrupt() {
    echo
    echo -e "${YELLOW}Тест был прерван.${NC}"
    echo -e "${CYAN}Нажмите Enter, чтобы вернуться в меню...${NC}"
    read -r
    clear
    show_header
    return 1
}

clear

show_header() {
    echo -e "${BLUE}===============================================================${NC}"
    echo -e "${CYAN}               ТЕСТИРОВАНИЕ POSTGRESQL КЛАСТЕРА                ${NC}"
    echo -e "${BLUE}===============================================================${NC}"
    echo
}

show_menu() {
    echo -e "${GREEN}Доступные тесты:${NC}"
    echo -e "${YELLOW}1.${NC} Тестирование подключения к мастеру${NC} (порт 5000)"
    echo -e "${YELLOW}2.${NC} Тестирование подключения к репликам${NC} (порт 5001)"
    echo -e "${YELLOW}3.${NC} Мониторинг состояния кластера Patroni${NC}"
    echo -e "${YELLOW}4.${NC} Тестирование записи в кластер${NC}"
    echo -e "${YELLOW}5.${NC} Тестирование отказоустойчивости и синхронной репликации${NC}"
    echo -e "${YELLOW}0.${NC} ВЫХОД${NC}"
    echo
    echo -e "${PURPLE}Примечание:${NC} Для завершения любого теста нажмите ${RED}Ctrl+C${NC}"
    echo
}

run_test() {
    local test_number=$1
    local scripts_dir="/app"

    case $test_number in
        1)
            echo -e "${GREEN}Запуск теста подключения к мастеру...${NC}"
            "$scripts_dir/test_master.sh"
            ;;
        2)
            echo -e "${GREEN}Запуск теста подключения к репликам...${NC}"
            "$scripts_dir/test_replica.sh"
            ;;
        3)
            echo -e "${GREEN}Запуск мониторинга кластера Patroni...${NC}"
            "$scripts_dir/monitor_cluster.sh"
            ;;
        4)
            echo -e "${GREEN}Запуск теста записи в кластер...${NC}"
            "$scripts_dir/test_write.sh"
            ;;
        5)
            echo -e "${GREEN}Запуск теста отказоустойчивости...${NC}"
            "$scripts_dir/test_failover.sh"
            ;;
        0)
            echo -e "${YELLOW}Выход из программы.${NC}"
            # Сбрасываем обработчик перед выходом
            trap - INT
            exit 0
            ;;
        *)
            echo -e "${RED}Неверный выбор. Пожалуйста, выберите снова.${NC}"
            ;;
    esac
}

# Главная функция
main() {
    show_header

    while true; do
        show_menu
        echo -e "${CYAN}Выберите тест для запуска [0-5]:${NC} "
        read -r choice

        if [[ "$choice" =~ ^[0-5]$ ]]; then
            run_test "$choice"
        else
            echo -e "${RED}Неверный выбор. Пожалуйста, введите число от 0 до 5.${NC}"
            sleep 2
        fi
    done
}

# Запускаем главную функцию
main