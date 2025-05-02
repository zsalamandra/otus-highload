package ru.otus.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private Long postId;
    private Long userId;
    private String content;
    private Long timestamp;

    // Константа для имени топика Kafka
    public static final String TOPIC = "post-events";
}
