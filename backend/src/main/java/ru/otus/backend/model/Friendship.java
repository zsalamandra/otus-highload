package ru.otus.backend.model;

import lombok.Data;

@Data
public class Friendship {
    private Long userId;
    private Long friendId;
}
