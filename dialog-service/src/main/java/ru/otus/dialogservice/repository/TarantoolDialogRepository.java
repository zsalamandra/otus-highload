package ru.otus.dialogservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.tarantool.TarantoolClient;
import ru.otus.dialogservice.dto.DialogMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TarantoolDialogRepository {

    private final TarantoolClient tarantoolClient;

    public Long saveMessage(Long fromUserId, Long toUserId, String text, String dialogId) {
        log.info("Отправка сообщения через Tarantool: от {} к {}, текст: {}", fromUserId, toUserId, text);

        try {
            List<?> eval = tarantoolClient.syncOps().eval(
                    "return box.func.send_message:call(...)",
                    Arrays.asList(fromUserId, toUserId, text)
            );

            if (eval != null && !eval.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) eval.get(0);
                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    return ((Number) resultMap.get("id")).longValue();
                } else {
                    throw new RuntimeException("Ошибка: " + resultMap.get("error"));
                }
            }
            throw new RuntimeException("Пустой результат от Tarantool");
        } catch (Exception e) {
            log.error("Ошибка при вызове Tarantool", e);
            throw new RuntimeException(e);
        }
    }

    public List<DialogMessage> findMessagesByDialogId(String dialogId) {
        String[] ids = dialogId.split("_");
        Long user1Id = Long.parseLong(ids[0]);
        Long user2Id = Long.parseLong(ids[1]);

        try {
            List<?> result = tarantoolClient.syncOps().eval(
                    "return box.func.get_dialog_messages:call(...)",
                    Arrays.asList(user1Id, user2Id, 100, 0)
            );

            if (result != null && !result.isEmpty()) {
                Map<String, Object> resultMap = (Map<String, Object>) result.get(0);
                if (Boolean.TRUE.equals(resultMap.get("success"))) {
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) resultMap.get("messages");
                    return convertMessages(messages);
                } else {
                    log.error("Ошибка Tarantool: {}", resultMap.get("error"));
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка при вызове функции Tarantool", e);
            throw new RuntimeException(e);
        }
    }

    private List<DialogMessage> convertMessages(List<Map<String, Object>> messages) {
        List<DialogMessage> result = new ArrayList<>();
        for (Map<String, Object> data : messages) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(((Number) data.get("created_at")).longValue()),
                    ZoneId.systemDefault()
            );
            result.add(new DialogMessage(
                    ((Number) data.get("id")).longValue(),
                    ((Number) data.get("from_user_id")).longValue(),
                    ((Number) data.get("to_user_id")).longValue(),
                    (String) data.get("content"),
                    createdAt
            ));
        }
        return result;
    }
}
