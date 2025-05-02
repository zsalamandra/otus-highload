package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.otus.backend.model.Post;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Метод для отправки сообщения о новом посте конкретному пользователю
     */
    public void sendPostNotification(Long userId, Post post) {
        String destination = "/post/feed/posted";
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, post);
    }

    /**
     * Метод для отправки сообщения всем подписанным на канал (для тестирования)
     */
    @MessageMapping("/broadcast")
    public void broadcastMessage(@Payload String message) {
        messagingTemplate.convertAndSend("/topic/public", message);
    }
}
