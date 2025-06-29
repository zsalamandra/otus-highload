package ru.otus.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageSentEvent {
    private String type = "MESSAGE_SENT";
    private Long userId;
    private Long messageId;
}
