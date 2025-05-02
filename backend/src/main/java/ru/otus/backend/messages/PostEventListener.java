package ru.otus.backend.messages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.otus.backend.model.PostCreatedEvent;
import ru.otus.backend.repository.FriendshipRepository;
import ru.otus.backend.model.WebSocketMessage;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final FriendshipRepository friendshipRepository;
    private final WebSocketEventListener webSocketEventListener;

    @KafkaListener(topics = PostCreatedEvent.TOPIC, groupId = "post-events-group")
    public void processPostEvent(ConsumerRecord<String, PostCreatedEvent> record) {
        try {
            PostCreatedEvent event = record.value();
            log.info("Получено событие о новом посте: {}", event);

            // Создаем экземпляр сообщения для WebSocket
            WebSocketMessage wsMessage = new WebSocketMessage(
                    "NEW_POST",
                    event,
                    null // timestamp будет установлен автоматически
            );

            // Получаем список друзей автора поста (их ID)
            List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(event.getUserId());

            log.debug("Друзья пользователя {} -> {}", event.getUserId(), friendIds);

            // Отправляем уведомление только активным друзьям
            int activeUsersNotified = 0;
            for (Long friendId : friendIds) {
                // Проверяем, подключен ли пользователь сейчас
                if (webSocketEventListener.isUserConnected(friendId.toString())) {
                    messagingTemplate.convertAndSendToUser(
                            friendId.toString(),
                            "/post/feed/posted",
                            wsMessage
                    );
                    activeUsersNotified++;
                }
            }

            log.info("Отправлено {} уведомлений о новом посте из {} друзей пользователя {}",
                    activeUsersNotified, friendIds.size(), event.getUserId());

        } catch (Exception e) {
            log.error("Ошибка обработки события о новом посте", e);
        }
    }
}
