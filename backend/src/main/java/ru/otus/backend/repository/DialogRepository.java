package ru.otus.backend.repository;

import ru.otus.backend.model.DialogMessage;

import java.util.List;

public interface DialogRepository {

    /**
     * Сохраняет новое сообщение в базе данных
     */
    Long saveMessage(Long fromUserId, Long toUserId, String text, String dialogId);

    /**
     * Находит все сообщения в диалоге, упорядоченные по времени
     */
    List<DialogMessage> findMessagesByDialogId(String dialogId);

//    List<DialogMessage> findDialogsBetween(Long fromUserId, Long toUserId);
}
