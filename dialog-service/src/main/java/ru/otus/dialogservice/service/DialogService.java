package ru.otus.dialogservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.dialogservice.dto.DialogMessage;
import ru.otus.dialogservice.repository.TarantoolDialogRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final TarantoolDialogRepository dialogRepository;

    public Long sendMessage(Long fromUserId, Long toUserId, String text) {
        String dialogId = generateDialogId(fromUserId, toUserId);
        return dialogRepository.saveMessage(fromUserId, toUserId, text, dialogId);
    }

    public List<DialogMessage> getDialogMessages(Long currentUserId, Long otherUserId) {
        String dialogId = generateDialogId(currentUserId, otherUserId);
        return dialogRepository.findMessagesByDialogId(dialogId);
    }

    private String generateDialogId(Long user1Id, Long user2Id) {
        Long minUserId = Math.min(user1Id, user2Id);
        Long maxUserId = Math.max(user1Id, user2Id);
        return minUserId + "_" + maxUserId;
    }
}
