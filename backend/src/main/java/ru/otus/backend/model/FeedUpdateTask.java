package ru.otus.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedUpdateTask {
    private Long postId;
    private Long authorId;
    private List<Long> targetUserIds; // ID пользователей, которым нужно обновить ленту
    private TaskType taskType;

    // Тип задачи
    public enum TaskType {
        ADD_POST,      // Добавить пост в ленту
        UPDATE_POST,   // Обновить существующий пост
        REMOVE_POST    // Удалить пост из ленты
    }

    // Константа для имени топика Kafka
    public static final String TOPIC = "feed-materialization";
}
