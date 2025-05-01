package ru.otus.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ru.otus.backend.service.WebSocketAuthenticationInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@EnableRedisHttpSession
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthenticationInterceptor webSocketAuthenticationInterceptor;

    public WebSocketConfig(WebSocketAuthenticationInterceptor interceptor) {
        this.webSocketAuthenticationInterceptor = interceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Префикс для адресов назначения, которые будут обрабатываться брокером сообщений
        // Настраиваем брокер сообщений для использования Redis
        // Это позволит масштабировать WebSocket сервер горизонтально
        config.enableSimpleBroker("/topic", "/user")
                .setTaskScheduler(new ConcurrentTaskScheduler()) // Для периодического heartbeat
                .setHeartbeatValue(new long[] {10000, 10000}); // Настройка heartbeat

        // Префикс для адресов назначения, которые будут обрабатываться контроллерами аннотированными с @MessageMapping
        config.setApplicationDestinationPrefixes("/app");

        // Установка префикса для личных сообщений
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthenticationInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрация конечной точки для WebSocket с поддержкой SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(true); // Включаем куки для sticky sessions
    }
}
