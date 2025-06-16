package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.backend.model.User;
import ru.otus.backend.repository.UserRepository;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class SlowReadController {

    private final UserRepository userRepository;

    @GetMapping("/slow-select")
    @Transactional(readOnly = true)
    public User slowSelect() throws InterruptedException {
        // че то там думаем
        Thread.sleep(2000);

        return userRepository.findByUsername("zaur");
    }
}
