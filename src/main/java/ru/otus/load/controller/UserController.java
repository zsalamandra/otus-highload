package ru.otus.load.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.otus.load.model.User;
import ru.otus.load.repository.UserRepository;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {

        // Хэшируем пароль перед сохранением
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {

        User user = userRepository.findById(id);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(user);
    }
}
