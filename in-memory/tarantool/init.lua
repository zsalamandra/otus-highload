#!/usr/bin/env tarantool

-- Конфигурация
local box_cfg = {
    listen = 3301,
    memtx_memory = 256 * 1024 * 1024, -- 128MB
    background = false,
    log_level = 3, -- verbose logging
    wal_dir = '/var/lib/tarantool',
    memtx_dir = '/var/lib/tarantool',
    vinyl_dir = '/var/lib/tarantool'
}

-- Загрузка необходимых модулей
local log = require('log')
local json = require('json')
local clock = require('clock')

-- Инициализация базы данных
local function init()
    log.info('Инициализация Tarantool...')

    -- Получаем учетные данные из переменных окружения
    local user = os.getenv('TARANTOOL_USER_NAME') or 'admin'
    local password = os.getenv('TARANTOOL_USER_PASSWORD') or 'pass'

    -- Применяем конфигурацию
    box.cfg(box_cfg)

    -- Создаем пользователя, если его еще нет
    if box.space._user.index.name:select({user})[1] == nil then
        log.info('Создание пользователя: %s', user)
        box.schema.user.create(user, {password = password, if_not_exists = true})
        box.schema.user.grant(user, 'read,write,execute,create,drop', 'universe', nil, {if_not_exists = true})
    end

    -- Загружаем схему и другие скрипты из папки app
    dofile('/opt/tarantool/app/schema.lua')
    dofile('/opt/tarantool/app/dialog.lua')
    dofile('/opt/tarantool/app/api.lua')

    log.info('Инициализация завершена.')
end

-- Запуск инициализации
init()

log.info('Tarantool запущен и готов к работе на порту %d', box_cfg.listen)