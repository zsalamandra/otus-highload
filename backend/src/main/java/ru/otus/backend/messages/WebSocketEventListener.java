package ru.otus.backend.messages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.otus.backend.model.User;
import ru.otus.backend.repository.UserRepository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisTemplate<String, Object> webSocketRedisTemplate;
    private final UserRepository userRepository;

    private static final String WS_SESSIONS_KEY = "ws:sessions";

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {

        log.debug("handleWebSocketConnectListener: {}", event);

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = Objects.requireNonNullElse(headerAccessor.getSessionId(), "mainSessionId");

        String userId = "anonymous";

        if (headerAccessor.getUser() != null) {
            // Извлекаем имя пользователя из аутентификации
            String username = headerAccessor.getUser().getName();
            log.debug("Found authenticated user: {}", username);

            // Находим пользователя по имени и получаем его ID
            try {
                User user = userRepository.findByUsername(username);
                if (user != null) {
                    userId = user.getId().toString();
                    log.debug("Found user ID from database: {}", userId);
                }
            } catch (Exception e) {
                log.error("Error finding user by username: {}", e.getMessage());
            }
        }

        log.info("Received a new web socket connection, session: {}, user: {}", sessionId, userId);

        // Сохраняем в Redis для глобального доступа
        webSocketRedisTemplate.opsForHash().put(WS_SESSIONS_KEY, sessionId, userId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        webSocketRedisTemplate.opsForHash().delete(WS_SESSIONS_KEY, sessionId);
    }

    /**
     * Проверяет, подключен ли пользователь в настоящий момент через WebSocket
     */
    public boolean isUserConnected(String userId) {
        // Проверяем в Redis
        Map<Object, Object> allSessions = webSocketRedisTemplate.opsForHash().entries(WS_SESSIONS_KEY);

        log.debug("Search connected user on Redis. Sessions = {}", allSessions);

        return allSessions.containsValue(userId);
    }
}
