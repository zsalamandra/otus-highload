package ru.otus.backend.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.otus.backend.model.MessageReadEvent;
import ru.otus.backend.model.MessageSentEvent;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CounterEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "dialog-events";

    public void publishMessageSent(Long toUserId, Long messageId) {
        MessageSentEvent event = new MessageSentEvent("MESSAGE_SENT", toUserId, messageId);
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishMessageRead(Long userId, List<Long> messageIds) {
        MessageReadEvent event = new MessageReadEvent("MESSAGE_READ", userId, messageIds);
        kafkaTemplate.send(TOPIC, event);
    }
}
