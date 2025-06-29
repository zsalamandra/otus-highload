package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.client.CounterFeignClient;
import ru.otus.backend.messages.CounterEventPublisher;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.repository.DialogRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogRepository dialogRepository;
    private final CounterFeignClient counterFeignClient;
    private final CounterEventPublisher counterEventPublisher;

    @Transactional
    public Long sendMessage(Long fromUserId, Long toUserId, String text) {
        // Генерируем dialog_id по хеш-функции от упорядоченной пары пользователей
        String dialogId = generateDialogId(fromUserId, toUserId);

        Long result = 0L;
        result = dialogRepository.saveMessage(fromUserId, toUserId, text, dialogId);
        counterEventPublisher.publishMessageSent(toUserId, result);

        return result;
    }

    @Transactional(readOnly = true)
    public List<DialogMessage> getDialogMessages(Long currentUserId, Long otherUserId) {
        // Генерируем dialog_id по той же хеш-функции
        String dialogId = generateDialogId(currentUserId, otherUserId);

        List<DialogMessage> messagesByDialogId = dialogRepository.findMessagesByDialogId(dialogId);

        List<Long> messagesIds = messagesByDialogId.stream()
                .map(DialogMessage::getId)
                .collect(Collectors.toList());
        counterEventPublisher.publishMessageRead(otherUserId, messagesIds);

        return messagesByDialogId;
    }

    public long getUnreadCounter(Long currentUserId) {

        return counterFeignClient.getUnread(currentUserId);
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
