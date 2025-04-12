package ru.otus.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogMessage {
    private Long id;
    private Long from;  // ID отправителя
    private Long to;    // ID получателя
    private String text;
    private LocalDateTime createdAt;
}
