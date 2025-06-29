package ru.otus.counterservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.otus.counterservice.client.TarantoolUnreadCounterClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final StringRedisTemplate redisTemplate;
    private final TarantoolUnreadCounterClient tarantoolClient;

    private String key(Long userId) {
        return "unread_counter:" + userId;
    }

    public long getUnread(Long userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null) {
            return invalidate(userId);
        }
        return Long.parseLong(value);
    }

    public long invalidate(Long userId) {
        long actual = tarantoolClient.countUnreadMessages(userId);
        redisTemplate.opsForValue().set(
                key(userId),
                String.valueOf(actual),
                Duration.ofMinutes(30)
        );
        return actual;
    }

    public long increment(Long userId) {
        Long updated = redisTemplate.opsForValue().increment(key(userId));
        if (updated != null) {
            redisTemplate.expire(key(userId), Duration.ofMinutes(30));
            return updated;
        }
        return invalidate(userId);
    }

    public long decrement(Long userId) {
        Long updated = redisTemplate.opsForValue().decrement(key(userId));
        if (updated != null) {
            redisTemplate.expire(key(userId), Duration.ofMinutes(30));
            return updated;
        }
        return invalidate(userId);
    }
}


