-- Схема данных для модуля диалогов
local log = require('log')

-- Создание схемы данных
local function init_schema()
    log.info('Инициализация схемы данных для диалогов...')

    -- Создаем спейс для сообщений
    if not box.space.messages then
        local space = box.schema.space.create('messages', {
            if_not_exists = true,
            format = {
                {name = 'id', type = 'unsigned'},            -- ID сообщения
                {name = 'dialog_id', type = 'string'},       -- ID диалога
                {name = 'from_user_id', type = 'unsigned'},  -- ID отправителя
                {name = 'to_user_id', type = 'unsigned'},    -- ID получателя
                {name = 'content', type = 'string'},         -- Содержимое сообщения
                {name = 'created_at', type = 'unsigned'},    -- Время создания (Unix timestamp)
                {name = 'is_read', type = 'boolean'}         -- Прочитано ли сообщение
            }
        })

        if not box.sequence.message_id then
            box.schema.sequence.create('message_id', {if_not_exists = true})
        end

        -- Создаем индексы для быстрого поиска
        -- Первичный ключ по ID сообщения
        space:create_index('primary', {
            type = 'HASH',
            parts = {'id'},
            if_not_exists = true
        })

        -- Индекс для поиска всех сообщений в конкретном диалоге
        space:create_index('dialog_id', {
            type = 'TREE',
            parts = {'dialog_id', 'created_at'},
            if_not_exists = true,
            unique = false
        })

        -- Индекс для поиска непрочитанных сообщений конкретного пользователя
        space:create_index('unread', {
            type = 'TREE',
            parts = {'to_user_id', 'is_read', 'dialog_id', 'created_at'},
            if_not_exists = true,
            unique = false  -- Индекс неуникальный, разрешаем дубликаты
        })

        log.info('Спейс messages создан с индексами')
    end

    -- Создаем спейс для счетчика ID сообщений
    if not box.space.message_counters then
        local counter_space = box.schema.space.create('message_counters', {
            if_not_exists = true,
            format = {
                {name = 'name', type = 'string'},     -- Имя счетчика
                {name = 'value', type = 'unsigned'}   -- Значение счетчика
            }
        })

        counter_space:create_index('primary', {
            type = 'HASH',
            parts = {'name'},
            if_not_exists = true
        })

        -- Инициализируем счетчик ID сообщений, если он не существует
        if not counter_space:get{'message_id'} then
            counter_space:insert{'message_id', 1}
        end

        log.info('Спейс message_counters создан')
    end

    log.info('Инициализация схемы данных завершена')
end

-- Запускаем инициализацию схемы
init_schema()

return {
    init_schema = init_schema
}