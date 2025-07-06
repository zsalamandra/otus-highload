package.path = package.path .. ';/opt/tarantool/app/?.lua'

-- API функции для доступа к модулю диалогов
local log = require('log')
local json = require('json')
local dialog = require('dialog')

-- Функция для отправки сообщения
function send_message(from_user_id, to_user_id, content)
    return dialog.send_message(from_user_id, to_user_id, content)
end

-- Функция для получения сообщений между пользователями
function get_dialog_messages(user1_id, user2_id, limit, offset)
    return dialog.get_messages_between_users(user1_id, user2_id, limit, offset)
end

-- Функция для получения сообщений по ID диалога
function get_messages_by_dialog_id(dialog_id, limit, offset)
    return dialog.get_dialog_messages(dialog_id, limit, offset)
end

-- Функция для отметки сообщения как прочитанного
function mark_as_read(message_id)
    return dialog.mark_message_as_read(message_id)
end

-- Функция для подсчета непрочитанных сообщений
function count_unread(user_id)
    return dialog.count_unread_messages(user_id)
end

-- Регистрация API-функций для внешнего доступа
box.schema.func.create('send_message', {if_not_exists = true})
box.schema.func.create('get_dialog_messages', {if_not_exists = true})
box.schema.func.create('get_messages_by_dialog_id', {if_not_exists = true})
box.schema.func.create('mark_as_read', {if_not_exists = true})
box.schema.func.create('count_unread', {if_not_exists = true})

-- Предоставление прав на выполнение функций
local admin_user = os.getenv('TARANTOOL_USER_NAME') or 'admin'
box.schema.user.grant(admin_user, 'execute', 'function', 'send_message', {if_not_exists = true})
box.schema.user.grant(admin_user, 'execute', 'function', 'get_dialog_messages', {if_not_exists = true})
box.schema.user.grant(admin_user, 'execute', 'function', 'get_messages_by_dialog_id', {if_not_exists = true})
box.schema.user.grant(admin_user, 'execute', 'function', 'mark_as_read', {if_not_exists = true})
box.schema.user.grant(admin_user, 'execute', 'function', 'count_unread', {if_not_exists = true})

log.info('API функции зарегистрированы и доступны для использования')