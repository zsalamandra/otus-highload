package ru.otus.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "counter-client", url = "${counter-service.url}")
public interface CounterFeignClient {

    @GetMapping("/counter/unread/{userId}")
    long getUnread(@PathVariable("userId") Long userId);
}
