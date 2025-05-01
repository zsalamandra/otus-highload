package ru.otus.backend.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.otus.backend.config.KafkaConfig;
import ru.otus.backend.model.Post;
import ru.otus.backend.repository.FriendshipRepository;
import ru.otus.backend.service.FeedService;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedUpdateListener {

    private final FeedService feedService;
    private final FriendshipRepository friendshipRepository;
    private final ObjectMapper objectMapper;

    // Пороговое значение для определения "знаменитости"
    private static final int CELEBRITY_THRESHOLD = 1000;

    @KafkaListener(topics = KafkaConfig.FEED_UPDATE_TOPIC, groupId = "feed-group")
    public void processFeedUpdate(ConsumerRecord<String, Map<String, Object>> record) {
        try {
            Map<String, Object> message = record.value();
            String action = (String) message.get("action");
            log.info("Processing feed update message with action: {}", action);

            switch (action) {
                case "CREATE":
                    handlePostCreate(message);
                    break;
                case "UPDATE":
                    handlePostUpdate(message);
                    break;
                case "DELETE":
                    handlePostDelete(message);
                    break;
                case "ADD_FRIEND":
                    handleAddFriend(message);
                    break;
                case "DELETE_FRIEND":
                    handleDeleteFriend(message);
                    break;
                default:
                    log.warn("Unknown action: {}", action);
            }
        } catch (Exception e) {
            log.error("Error processing feed update message", e);
        }
    }

    private void handlePostCreate(Map<String, Object> message) {
        Post post = objectMapper.convertValue(message.get("post"), Post.class);

        // Получаем ID друзей автора поста
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(post.getUserId());

        // Проверяем, является ли автор "знаменитостью"
        boolean isCelebrity = friendIds.size() > CELEBRITY_THRESHOLD;

        if (isCelebrity) {
            log.info("Пользователь {} является знаменитостью с {} подписчиками. Не выполняем немедленное обновление лент.",
                    post.getUserId(), friendIds.size());

            // Для знаменитостей не делаем немедленное обновление лент
            // Задача материализации будет создана при запросе ленты
        } else {
            // Для не-знаменитостей продолжаем как обычно
            for (Long friendId : friendIds) {
                feedService.addPostToFeed(friendId, post);
            }
        }
    }

    private void handlePostUpdate(Map<String, Object> message) {
        Post post = objectMapper.convertValue(message.get("post"), Post.class);

        // инвалидируем кеши лент всех друзей
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(post.getUserId());

        for (Long friendId : friendIds) {
            feedService.invalidateFeed(friendId);
        }
    }

    private void handlePostDelete(Map<String, Object> message) {
        Long postId = Long.valueOf(message.get("postId").toString());
        Long userId = Long.valueOf(message.get("userId").toString());

        // Получаем ID друзей автора поста
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(userId);

        // Удаляем пост из лент всех друзей
        for (Long friendId : friendIds) {
            feedService.removePostFromFeed(friendId, postId);
        }
    }

    private void handleAddFriend(Map<String, Object> message) {
        Long userId = Long.valueOf(message.get("userId").toString());

        // Перестраиваем ленту, т.к. добавился новый друг
        feedService.rebuildFeedCache(userId);
    }

    private void handleDeleteFriend(Map<String, Object> message) {
        Long userId = Long.valueOf(message.get("userId").toString());

        // Перестраиваем ленту, т.к. удалился друг
        feedService.rebuildFeedCache(userId);
    }
}