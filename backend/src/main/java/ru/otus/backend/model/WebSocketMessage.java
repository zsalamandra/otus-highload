package ru.otus.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type;    // Тип сообщения, например "NEW_POST"
    private Object payload; // Содержимое сообщения
    private LocalDateTime timestamp = LocalDateTime.now();
}
