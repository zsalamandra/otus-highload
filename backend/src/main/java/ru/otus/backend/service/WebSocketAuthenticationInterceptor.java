package ru.otus.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import ru.otus.backend.model.User;
import ru.otus.backend.repository.UserRepository;

@Slf4j
@Component
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthenticationInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                String username = jwtService.extractUsername(token);

                if (username != null) {
                    User user = userRepository.findByUsername(username);
                    // Устанавливаем userId в атрибуты сессии
                    log.debug("принудительно добавляем userId: {}", user.getId());
                    accessor.getSessionAttributes().put("userId", user.getId().toString());
                }
            }
        }
        return message;
    }
}
