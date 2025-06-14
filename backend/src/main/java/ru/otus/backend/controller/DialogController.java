package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.model.DialogMessageRequest;
import ru.otus.backend.service.DelegatingDialogService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/dialog")
@RequiredArgsConstructor
public class DialogController {

    private final DelegatingDialogService dialogService;

    @PostMapping("/{user_id}/send")
    public ResponseEntity<Void> sendMessage(
            @PathVariable("user_id") Long toUserId,
            @RequestBody DialogMessageRequest request) {

        dialogService.sendMessage(toUserId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{user_id}/list")
    public ResponseEntity<List<DialogMessage>> getDialogMessages(
            @PathVariable("user_id") Long otherUserId) {

        return ResponseEntity.ok(dialogService.getDialogMessages(otherUserId));
    }
}

