package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.otus.backend.model.User;
import ru.otus.backend.service.FriendshipService;
import ru.otus.backend.service.UserService;

@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @PostMapping("/add")
    public ResponseEntity<Void> addFriend(@RequestParam Long friendId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());

        // Проверяем, что пользователь с friendId существует
        User friend = userService.findById(friendId);
        if (friend == null) {
            return ResponseEntity.notFound().build();
        }

        // Добавляем друга
        friendshipService.addFriend(user.getId(), friendId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteFriend(@RequestParam Long friendId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());

        friendshipService.deleteFriend(user.getId(), friendId);

        return ResponseEntity.ok().build();
    }
}