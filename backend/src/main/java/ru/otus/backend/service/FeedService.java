package ru.otus.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.model.Post;
import ru.otus.backend.repository.FriendshipRepository;
import ru.otus.backend.repository.PostRepository;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final ObjectMapper objectMapper;

    private static final String FEED_CACHE_PREFIX = "feed:";
    private static final int FEED_SIZE_LIMIT = 1000;
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // Пороговое значение для определения "знаменитости"
    private static final int CELEBRITY_THRESHOLD = 1000;
    // Флаг для включения/отключения обработки "знаменитостей"
    private static final boolean ENABLE_CELEBRITY_OPTIMIZATION = true;

    @Transactional(readOnly = true)
    public List<Post> getFeed(Long userId, int limit, int offset) {
        String cacheKey = FEED_CACHE_PREFIX + userId;

        // Проверяем есть ли лента в кеше
        Boolean hasKey = redisTemplate.hasKey(cacheKey);

        if (Boolean.FALSE.equals(hasKey)) {
            // Если кеша нет, перестраиваем его
            rebuildFeedCache(userId);
        }

        // Получаем диапазон постов из отсортированного множества Redis
        Set<ZSetOperations.TypedTuple<Object>> postsWithScores =
                redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, offset, offset + limit - 1);

        if (postsWithScores == null || postsWithScores.isEmpty()) {
            return Collections.emptyList();
        }

        // Получаем ID постов из кеша
        List<Long> postIds = postsWithScores.stream()
                .map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue()).toString()))
                .toList();

        // Получаем полные данные постов из БД в правильном порядке
        // (т.к. findAllById не гарантирует порядок)
        List<Post> postList = new ArrayList<>();
        for (Long postId : postIds) {
            Post post = postRepository.findById(postId);
            if (post != null) {
                postList.add(post);
            }
        }

        return postList;
    }

    public void rebuildFeedCache(Long userId) {
        log.info("Rebuilding feed cache for user ID: {}", userId);

        // Получаем список ID друзей
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(userId);

        if (friendIds.isEmpty()) {
            log.info("User {} has no friends, skipping feed rebuild", userId);
            return;
        }

        // Проверяем, есть ли среди друзей "знаменитости"
        boolean hasCelebrityFriends = false;
        if (ENABLE_CELEBRITY_OPTIMIZATION) {
            for (Long friendId : friendIds) {
                int friendCount = friendshipRepository.countFriendsByUserId(friendId);
                if (friendCount > CELEBRITY_THRESHOLD) {
                    hasCelebrityFriends = true;
                    log.info("Пользователь {} подписан на знаменитость {} с {} подписчиками",
                            userId, friendId, friendCount);
                }
            }
        }

        // Получаем посты друзей из БД
        List<Post> posts = postRepository.findByUserIdsOrderByCreatedAtDesc(friendIds, FEED_SIZE_LIMIT);

        // Очищаем старый кеш
        String cacheKey = FEED_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);

        // Сохраняем посты в Redis Sorted Set с метками времени в качестве score
        for (Post post : posts) {
            double score = post.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC);
            redisTemplate.opsForZSet().add(cacheKey, post.getId().toString(), score);
        }

        // Устанавливаем время жизни кеша
        redisTemplate.expire(cacheKey, CACHE_TTL);

        log.info("Successfully rebuilt feed cache for user ID: {} with {} posts", userId, posts.size());
    }

    public void addPostToFeed(Long feedOwnerId, Post post) {
        String cacheKey = FEED_CACHE_PREFIX + feedOwnerId;

        // Добавляем пост в Sorted Set с использованием времени создания как score
        double score = post.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC);
        redisTemplate.opsForZSet().add(cacheKey, post.getId().toString(), score);

        // Удаляем старые посты, если их больше FEED_SIZE_LIMIT
        Long size = redisTemplate.opsForZSet().size(cacheKey);
        if (size != null && size > FEED_SIZE_LIMIT) {
            redisTemplate.opsForZSet().removeRange(cacheKey, 0, size - FEED_SIZE_LIMIT - 1);
        }
    }

    public void removePostFromFeed(Long feedOwnerId, Long postId) {
        String cacheKey = FEED_CACHE_PREFIX + feedOwnerId;
        redisTemplate.opsForZSet().remove(cacheKey, postId.toString());
    }

    public void invalidateFeed(Long userId) {
        String cacheKey = FEED_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}