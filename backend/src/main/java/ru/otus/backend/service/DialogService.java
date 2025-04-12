package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.repository.DialogRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogRepository dialogRepository;

    @Transactional
    public Long sendMessage(Long fromUserId, Long toUserId, String text) {
        // Генерируем dialog_id по хеш-функции от упорядоченной пары пользователей
        String dialogId = generateDialogId(fromUserId, toUserId);

        return dialogRepository.saveMessage(fromUserId, toUserId, text, dialogId);
    }

    @Transactional(readOnly = true)
    public List<DialogMessage> getDialogMessages(Long currentUserId, Long otherUserId) {
        // Генерируем dialog_id по той же хеш-функции
        String dialogId = generateDialogId(currentUserId, otherUserId);

        return dialogRepository.findMessagesByDialogId(dialogId);
    }

    /**
     * Генерирует уникальный ID диалога на основе ID участников
     * Используется min/max для гарантии одинакового ID независимо от порядка пользователей
     */
    private String generateDialogId(Long user1Id, Long user2Id) {
        Long minUserId = Math.min(user1Id, user2Id);
        Long maxUserId = Math.max(user1Id, user2Id);

        // Используем простую хеш-функцию для демонстрации
        // В реальном проекте можно использовать более сложную функцию
        return minUserId + "_" + maxUserId;
    }
}
