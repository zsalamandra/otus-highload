package ru.otus.backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.otus.backend.model.DialogMessage;
import ru.otus.backend.model.DialogMessageRequest;

import java.util.List;

@FeignClient(name = "dialogServiceClient", url = "${dialog.service.url}")
public interface DialogFeignClient {

    @PostMapping("/dialog/{user_id}/send")
    void sendMessage(@PathVariable("user_id") Long toUserId,
                     @RequestBody DialogMessageRequest request);

    @GetMapping("/dialog/{user_id}/list")
    List<DialogMessage> getDialogMessages(@PathVariable("user_id") Long otherUserId);
}

