package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.model.DialogMessageRequest;
import ru.otus.backend.model.User;
import ru.otus.backend.service.DialogService;
import ru.otus.backend.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/dialog")
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;
    private final UserService userService;

    @PostMapping("/{user_id}/send")
    public ResponseEntity<Void> sendMessage(
            @PathVariable("user_id") Long userId,
            @RequestBody DialogMessageRequest request,
            Authentication auth) {

        User currentUser = userService.findByUsername(auth.getName());

        log.info("Sending a message to user {}", userId);
        dialogService.sendMessage(currentUser.getId(), userId, request.getText());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{user_id}/list")
    public ResponseEntity<List<DialogMessage>> getDialogMessages(
            @PathVariable("user_id") Long userId,
            Authentication auth) {

        User currentUser = userService.findByUsername(auth.getName());

        List<DialogMessage> messages = dialogService.getDialogMessages(currentUser.getId(), userId);

        return ResponseEntity.ok(messages);
    }
}
