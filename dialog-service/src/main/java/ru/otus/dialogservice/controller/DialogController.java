package ru.otus.dialogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.otus.dialogservice.auth.CurrentUserService;
import ru.otus.dialogservice.dto.DialogMessage;
import ru.otus.dialogservice.dto.DialogMessageRequest;
import ru.otus.dialogservice.service.DialogService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dialog")
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;
    private final CurrentUserService currentUserService;

    @PostMapping("/{user_id}/send")
    public ResponseEntity<Void> sendMessage(
            @PathVariable("user_id") Long toUserId,
            @RequestBody DialogMessageRequest request,
            @RequestHeader Map<String, String> headers,
            Authentication authentication) {

        log.info("Request headers: {}", headers);

        Long fromUserId = currentUserService.getCurrentUserId(authentication);

        log.info("Sending message from {} to {}", fromUserId, toUserId);
        dialogService.sendMessage(fromUserId, toUserId, request.getText());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{user_id}/list")
    public ResponseEntity<List<DialogMessage>> getDialogMessages(
            @PathVariable("user_id") Long otherUserId,
            Authentication authentication) {

        Long currentUserId = currentUserService.getCurrentUserId(authentication);

        List<DialogMessage> messages = dialogService.getDialogMessages(currentUserId, otherUserId);
        return ResponseEntity.ok(messages);
    }
}

