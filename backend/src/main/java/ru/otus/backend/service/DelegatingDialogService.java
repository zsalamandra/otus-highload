package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.backend.client.DialogFeignClient;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.model.DialogMessageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DelegatingDialogService {

    private final DialogFeignClient client;

    public void sendMessage(Long toUserId, DialogMessageRequest request) {

        try {
            client.sendMessage(toUserId, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<DialogMessage> getDialogMessages(Long otherUserId) {
        return client.getDialogMessages(otherUserId);
    }
}
