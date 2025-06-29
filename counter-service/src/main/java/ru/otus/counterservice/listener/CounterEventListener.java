package ru.otus.counterservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.otus.counterservice.service.CounterService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounterEventListener {

    private final CounterService counterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "dialog-events", groupId = "counter-service-group")
    public void handle(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String type = node.get("type").asText();
            Long userId = node.get("userId").asLong();

            switch (type) {
                case "MESSAGE_SENT" -> {
                    log.info("Incrementing counter for user {}", userId);
                    counterService.increment(userId);
                }
                case "MESSAGE_READ" -> {
                    log.info("Decrementing counter for user {}", userId);
                    counterService.decrement(userId);
                }
                default -> log.warn("Unknown event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", message, e);
        }
    }
}

