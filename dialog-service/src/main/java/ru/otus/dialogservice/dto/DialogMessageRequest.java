package ru.otus.dialogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DialogMessageRequest {
    private String text;
}
