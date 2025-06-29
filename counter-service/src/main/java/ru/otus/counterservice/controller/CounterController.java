package ru.otus.counterservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.otus.counterservice.service.CounterService;

@RestController
@RequestMapping("/counter")
@RequiredArgsConstructor
public class CounterController {

    private final CounterService counterService;

    @GetMapping("/unread/{userId}")
    public long getUnread(@PathVariable Long userId) {
        return counterService.getUnread(userId);
    }

    @PostMapping("/unread/{userId}/increment")
    public long increment(@PathVariable Long userId) {
        return counterService.increment(userId);
    }

    @PostMapping("/unread/{userId}/decrement")
    public long decrement(@PathVariable Long userId) {
        return counterService.decrement(userId);
    }
}

