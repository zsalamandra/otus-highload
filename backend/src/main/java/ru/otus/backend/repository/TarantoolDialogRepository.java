package ru.otus.backend.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.tarantool.TarantoolClient;
import ru.otus.backend.model.DialogMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dialog.storage", havingValue = "tarantool")
public class TarantoolDialogRepository implements DialogRepository {

    private final TarantoolClient tarantoolClient;

    @Override
    public Long saveMessage(Long fromUserId, Long toUserId, String text, String dialogId) {
        log.info("Отправка сообщения через Tarantool: от {} к {}, текст: {}", fromUserId, toUserId, text);

        try {
            // Вызываем функцию send_message на Tarantool сервере через syncOps
            List<?> eval = tarantoolClient.syncOps().eval(
                    "return box.func.send_message:call(...)",
                    Arrays.asList(fromUserId, toUserId, text)
            );

            if (eval != null && !eval.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) eval.get(0);

                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    Long messageId = ((Number) resultMap.get("id")).longValue();
                    log.debug("Сообщение успешно отправлено с ID: {}", messageId);
                    return messageId;
                } else {
                    log.error("Ошибка при отправке сообщения: {}", resultMap.get("error"));
                    throw new RuntimeException("Ошибка при отправке сообщения: " + resultMap.get("error"));
                }
            } else {
                log.error("Получен пустой результат от Tarantool");
                throw new RuntimeException("Получен пустой результат от Tarantool");
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException("Ошибка при вызове функции Tarantool", e);
        }
    }

    @Override
    public List<DialogMessage> findMessagesByDialogId(String dialogId) {
        log.debug("Получение сообщений диалога через Tarantool: {}", dialogId);

        try {
            // Парсим dialogId для получения ID пользователей
            String[] userIds = dialogId.split("_");
            if (userIds.length != 2) {
                log.error("Неверный формат dialogId: {}", dialogId);
                return Collections.emptyList();
            }

            Long user1Id = Long.parseLong(userIds[0]);
            Long user2Id = Long.parseLong(userIds[1]);

            // Вызываем функцию get_dialog_messages на Tarantool сервере
            List<?> result = tarantoolClient.syncOps().eval(
                    "return box.func.get_dialog_messages:call(...)",
                    Arrays.asList(user1Id, user2Id, 100, 0)
            );

            if (result != null && !result.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) result.get(0);

                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) resultMap.get("messages");
                    List<DialogMessage> dialogMessages = new ArrayList<>();

                    for (Map<String, Object> messageData : messages) {
                        // Преобразуем timestamp в LocalDateTime
                        Long timestamp = ((Number) messageData.get("created_at")).longValue();
                        LocalDateTime createdAt = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                ZoneId.systemDefault()
                        );

                        DialogMessage message = new DialogMessage(
                                ((Number) messageData.get("id")).longValue(),
                                ((Number) messageData.get("from_user_id")).longValue(),
                                ((Number) messageData.get("to_user_id")).longValue(),
                                (String) messageData.get("content"),
                                createdAt
                        );
                        dialogMessages.add(message);
                    }

                    log.debug("Получено {} сообщений для диалога {}", dialogMessages.size(), dialogId);
                    return dialogMessages;
                } else {
                    log.error("Ошибка при получении сообщений: {}", resultMap.get("error"));
                    return Collections.emptyList();
                }
            } else {
                log.error("Получен пустой результат от Tarantool");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException("Ошибка при вызове функции Tarantool", e);
        }
    }

//    @Override
    public List<DialogMessage> findDialogsBetween(Long fromUserId, Long toUserId) {

        log.debug("Получение сообщений диалога между пользователями: {} и {}", fromUserId, toUserId);

        try {

            // Вызываем функцию get_dialog_messages на Tarantool сервере
            List<?> result = tarantoolClient.syncOps().eval(
                    "return box.func.get_messages_between_users:call(...)",
                    Arrays.asList(fromUserId, toUserId)
            );

            if (result != null && !result.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) result.get(0);

                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) resultMap.get("messages");
                    List<DialogMessage> dialogMessages = new ArrayList<>();

                    for (Map<String, Object> messageData : messages) {
                        // Преобразуем timestamp в LocalDateTime
                        Long timestamp = ((Number) messageData.get("created_at")).longValue();
                        LocalDateTime createdAt = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                ZoneId.systemDefault()
                        );

                        DialogMessage message = new DialogMessage(
                                ((Number) messageData.get("id")).longValue(),
                                ((Number) messageData.get("from_user_id")).longValue(),
                                ((Number) messageData.get("to_user_id")).longValue(),
                                (String) messageData.get("content"),
                                createdAt
                        );
                        dialogMessages.add(message);
                    }

                    log.debug("Получено {} сообщений", dialogMessages.size());
                    return dialogMessages;
                } else {
                    log.error("Ошибка при получении сообщений: {}", resultMap.get("error"));
                    return Collections.emptyList();
                }
            } else {
                log.error("Получен пустой результат от Tarantool");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException("Ошибка при вызове функции Tarantool", e);
        }
    }

    public int countUnreadMessages(Long userId) {
        log.debug("Подсчет непрочитанных сообщений через Tarantool для пользователя: {}", userId);

        try {
            // Вызываем функцию count_unread на Tarantool сервере
            List<?> result = tarantoolClient.syncOps().eval(
                    "return box.func.count_unread:call(...)",
                    Collections.singletonList(userId)
            );

            if (result != null && !result.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) result.get(0);

                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    int count = ((Number) resultMap.get("count")).intValue();
                    log.debug("Пользователь {} имеет {} непрочитанных сообщений", userId, count);
                    return count;
                } else {
                    log.error("Ошибка при подсчете непрочитанных сообщений: {}", resultMap.get("error"));
                    return 0;
                }
            } else {
                log.error("Получен пустой результат от Tarantool");
                return 0;
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException("Ошибка при вызове функции Tarantool", e);
        }
    }

    public boolean markMessageAsRead(Long messageId) {
        log.debug("Отметка сообщения как прочитанного через Tarantool: {}", messageId);

        try {
            // Вызываем функцию mark_as_read на Tarantool сервере
            List<?> result = tarantoolClient.syncOps().eval(
                    "return box.func.mark_as_read:call(...)",
                    Collections.singletonList(messageId)
            );

            if (result != null && !result.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) result.get(0);

                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    log.debug("Сообщение {} успешно отмечено как прочитанное", messageId);
                    return true;
                } else {
                    log.error("Ошибка при отметке сообщения как прочитанного: {}", resultMap.get("error"));
                    return false;
                }
            } else {
                log.error("Получен пустой результат от Tarantool");
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException("Ошибка при вызове функции Tarantool", e);
        }
    }
}