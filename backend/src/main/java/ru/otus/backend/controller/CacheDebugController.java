package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Контроллер для отладки и проверки состояния кеша Redis.
 * Предоставляет API-конечные точки для проверки содержимого кеша лент пользователей.
 */
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class CacheDebugController {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String FEED_CACHE_PREFIX = "feed:";

    /**
     * Получает информацию о кеше ленты для конкретного пользователя.
     *
     * @param userId ID пользователя
     * @return Информация о кеше ленты пользователя
     */
    @GetMapping("/cache/feed/{userId}")
    public ResponseEntity<Map<String, Object>> getFeedCache(@PathVariable(name = "userId") Long userId) {
        String cacheKey = FEED_CACHE_PREFIX + userId;
        Map<String, Object> result = new HashMap<>();

        // Проверяем существование кеша
        Boolean exists = redisTemplate.hasKey(cacheKey);
        result.put("exists", exists);

        // Если кеш не существует, возвращаем только информацию о его отсутствии
        if (!exists) {
            return ResponseEntity.ok(result);
        }

        // Получаем и добавляем информацию о TTL кеша
        Long ttl = redisTemplate.getExpire(cacheKey);
        result.put("ttl", ttl);

        // Получаем размер Sorted Set (количество постов в ленте)
        Long size = redisTemplate.opsForZSet().size(cacheKey);
        result.put("size", size);

        // Получаем все элементы Sorted Set (ID постов и их score)
        Set<ZSetOperations.TypedTuple<Object>> postsWithScores =
                redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, 0, -1);

        // Формируем список постов с их score (метка времени) для ответа
        List<Map<String, Object>> posts = new ArrayList<>();
        if (postsWithScores != null) {
            for (ZSetOperations.TypedTuple<Object> post : postsWithScores) {
                Map<String, Object> postInfo = new HashMap<>();
                postInfo.put("id", post.getValue());
                postInfo.put("score", post.getScore());
                posts.add(postInfo);
            }
        }

        result.put("posts", posts);

        return ResponseEntity.ok(result);
    }

    /**
     * Получает общую информацию о всех кешах лент в Redis.
     *
     * @return Общая информация о кешах лент
     */
    @GetMapping("/cache/feeds")
    public ResponseEntity<Map<String, Object>> getAllFeedCaches() {
        Map<String, Object> result = new HashMap<>();

        // Получаем все ключи, начинающиеся с feed:
        Set<String> feedKeys = redisTemplate.keys(FEED_CACHE_PREFIX + "*");
        result.put("totalCaches", feedKeys != null ? feedKeys.size() : 0);

        // Собираем базовую информацию по каждому кешу (без постов)
        Map<String, Object> feeds = new HashMap<>();
        if (feedKeys != null) {
            for (String key : feedKeys) {
                Map<String, Object> feedInfo = new HashMap<>();

                Long size = redisTemplate.opsForZSet().size(key);
                feedInfo.put("size", size);

                Long ttl = redisTemplate.getExpire(key);
                feedInfo.put("ttl", ttl);

                feeds.put(key, feedInfo);
            }
        }

        result.put("feeds", feeds);

        return ResponseEntity.ok(result);
    }

    /**
     * Проверяет существование поста в кеше ленты конкретного пользователя.
     *
     * @param userId ID пользователя
     * @param postId ID поста
     * @return Информация о наличии поста в кеше ленты
     */
    @GetMapping("/cache/feed/{userId}/post/{postId}")
    public ResponseEntity<Map<String, Object>> checkPostInFeed(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "postId") Long postId) {

        String cacheKey = FEED_CACHE_PREFIX + userId;
        Map<String, Object> result = new HashMap<>();

        // Проверяем существование кеша
        Boolean exists = redisTemplate.hasKey(cacheKey);
        result.put("cacheExists", exists != null && exists);

        if (exists) {
            // Проверяем наличие поста в кеше (его score)
            Double score = redisTemplate.opsForZSet().score(cacheKey, postId.toString());
            result.put("postExists", score != null);

            if (score != null) {
                result.put("score", score);
            }
        } else {
            result.put("postExists", false);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Принудительно очищает кеш ленты пользователя (для тестирования).
     *
     * @param userId ID пользователя
     * @return Информация об операции
     */
    @GetMapping("/cache/feed/{userId}/clear")
    public ResponseEntity<Map<String, Object>> clearFeedCache(@PathVariable(name = "userId") Long userId) {
        String cacheKey = FEED_CACHE_PREFIX + userId;
        Map<String, Object> result = new HashMap<>();

        Boolean deleted = redisTemplate.delete(cacheKey);
        result.put("deleted", deleted != null && deleted);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/cache/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllFeedCaches() {
        Map<String, Object> result = new HashMap<>();

        Set<String> feedKeys = redisTemplate.keys(FEED_CACHE_PREFIX + "*");
        if (feedKeys != null && !feedKeys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(feedKeys);
            result.put("deletedCachesCount", deletedCount);
        } else {
            result.put("deletedCachesCount", 0);
        }

        return ResponseEntity.ok(result);
    }
}