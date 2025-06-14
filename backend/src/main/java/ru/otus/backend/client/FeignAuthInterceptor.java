package ru.otus.backend.client;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignAuthInterceptor {

    @Bean
    public RequestInterceptor requestInterceptor(JwtTokenProvider tokenProvider) {
        return requestTemplate -> {
            String token = tokenProvider.getToken();
            if (token != null) {
                log.info("Will authenticate token: {}", token);
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }
}
