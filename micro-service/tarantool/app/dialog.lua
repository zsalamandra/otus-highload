-- Модуль для работы с диалогами
local log = require('log')
local json = require('json')
local clock = require('clock')

local dialog = {}

-- Генерирует уникальный ID диалога из двух ID пользователей
function dialog.generate_dialog_id(user1_id, user2_id)
     local min_id = math.min(user1_id, user2_id)
     local max_id = math.max(user1_id, user2_id)
     return min_id .. "_" .. max_id
end

-- Получает следующий ID сообщения
function dialog.next_message_id()
    return box.sequence.message_id:next()
end

-- Отправка сообщения
function dialog.send_message(from_user_id, to_user_id, content)

    -- Проверяем входные данные
    if not from_user_id or not to_user_id or not content then
        log.error('Неверные входные данные для отправки сообщения')
        return {success = false, error = 'Invalid input data'}
    end

    -- Генерируем ID диалога
    local dialog_id = dialog.generate_dialog_id(from_user_id, to_user_id)

    -- Получаем новый ID сообщения
    local message_id = dialog.next_message_id()

    -- Добавляем сообщение в спейс
    local created_at = os.time()
    local tuple = box.space.messages:insert{
        message_id,
        dialog_id,
        from_user_id,
        to_user_id,
        content,
        created_at,
        false -- is_read = false (непрочитанное)
    }

    if tuple then
        log.info('Сообщение успешно отправлено, id=%s', message_id)
        return {success = true, id = message_id, created_at = created_at}
    else
        log.error('Ошибка при отправке сообщения')
        return {success = false, error = 'Failed to insert message'}
    end
end

-- Получение сообщений диалога
function dialog.get_dialog_messages(dialog_id, limit, offset)
    limit = limit or 100   -- По умолчанию до 100 сообщений
    offset = offset or 0   -- По умолчанию с начала

    local messages = {}
    local result = box.space.messages.index.dialog_id:select(
        {dialog_id},
        {iterator = 'EQ', limit = limit, offset = offset}
    )

    for _, tuple in ipairs(result) do
        table.insert(messages, {
            id = tuple[1],
            dialog_id = tuple[2],
            from_user_id = tuple[3],
            to_user_id = tuple[4],
            content = tuple[5],
            created_at = tuple[6],
            is_read = tuple[7]
        })
    end

    log.info('Найдено %d сообщений', #messages)
    return {success = true, messages = messages}
end

-- Получение сообщений между двумя пользователями
function dialog.get_messages_between_users(user1_id, user2_id)
    local dialog_id = dialog.generate_dialog_id(user1_id, user2_id)
    return dialog.get_dialog_messages(dialog_id, 100, 0)
end

-- Отметка сообщения как прочитанного
function dialog.mark_message_as_read(message_id)
    log.info('Отметка сообщения %s как прочитанного', message_id)

    local message = box.space.messages:get{message_id}
    if message then
        box.space.messages:update({message_id}, {{'=', 7, true}})
        return {success = true}
    else
        return {success = false, error = 'Message not found'}
    end
end

-- Подсчет непрочитанных сообщений для пользователя
function dialog.count_unread_messages(user_id)
    log.info('Подсчет непрочитанных сообщений для пользователя %s', user_id)

    local count = 0
    local result = box.space.messages.index.unread:select(
        {user_id, false},  -- to_user_id = user_id, is_read = false
        {iterator = 'EQ'}
    )

    count = #result
    log.info('Пользователь %s имеет %d непрочитанных сообщений', user_id, count)
    return {success = true, count = count}
end

return dialog