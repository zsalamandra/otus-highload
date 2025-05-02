package ru.otus.backend.messages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.otus.backend.model.FeedUpdateTask;
import ru.otus.backend.model.Post;
import ru.otus.backend.service.FeedService;
import ru.otus.backend.service.PostService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedMaterializationListener {

    private final FeedService feedService;
    private final PostService postService;

    // Создаем пул потоков для параллельной обработки задач
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @KafkaListener(topics = FeedUpdateTask.TOPIC, groupId = "feed-materialization-group")
    public void processFeedMaterializationTask(ConsumerRecord<String, FeedUpdateTask> record) {
        try {
            FeedUpdateTask task = record.value();
            log.info("Получена задача материализации ленты: {}", task);

            if (task.getTargetUserIds() == null || task.getTargetUserIds().isEmpty()) {
                log.warn("Задача не содержит целевых пользователей, пропускаем");
                return;
            }

            switch (task.getTaskType()) {
                case ADD_POST:
                    processFeedAddition(task);
                    break;
                case UPDATE_POST:
                    processFeedUpdate(task);
                    break;
                case REMOVE_POST:
                    processFeedRemoval(task);
                    break;
                default:
                    log.warn("Неизвестный тип задачи: {}", task.getTaskType());
            }
        } catch (Exception e) {
            log.error("Ошибка обработки задачи материализации ленты", e);
        }
    }

    private void processFeedAddition(FeedUpdateTask task) {
        // Получаем пост из базы данных
        Post post = postService.getPost(task.getPostId());
        if (post == null) {
            log.error("Не удалось найти пост с ID {} для материализации ленты", task.getPostId());
            return;
        }

        // Обрабатываем каждого пользователя в отдельном потоке
        task.getTargetUserIds().forEach(userId ->
                executorService.submit(() -> {
                    try {
                        // Добавляем пост в ленту пользователя
                        feedService.addPostToFeed(userId, post);
                        log.debug("Пост {} успешно добавлен в ленту пользователя {}", post.getId(), userId);
                    } catch (Exception e) {
                        log.error("Ошибка при добавлении поста {} в ленту пользователя {}", post.getId(), userId, e);
                    }
                })
        );
    }

    private void processFeedUpdate(FeedUpdateTask task) {
        // Инвалидируем кеш для всех целевых пользователей
        task.getTargetUserIds().forEach(userId ->
                executorService.submit(() -> {
                    try {
                        // Инвалидируем кеш ленты пользователя
                        feedService.invalidateFeed(userId);
                        log.debug("Кеш ленты пользователя {} успешно инвалидирован", userId);
                    } catch (Exception e) {
                        log.error("Ошибка при инвалидации кеша ленты пользователя {}", userId, e);
                    }
                })
        );
    }

    private void processFeedRemoval(FeedUpdateTask task) {
        // Удаляем пост из лент всех целевых пользователей
        task.getTargetUserIds().forEach(userId ->
                executorService.submit(() -> {
                    try {
                        // Удаляем пост из ленты пользователя
                        feedService.removePostFromFeed(userId, task.getPostId());
                        log.debug("Пост {} успешно удален из ленты пользователя {}", task.getPostId(), userId);
                    } catch (Exception e) {
                        log.error("Ошибка при удалении поста {} из ленты пользователя {}", task.getPostId(), userId, e);
                    }
                })
        );
    }
}
