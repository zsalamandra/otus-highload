// PostController.java
package ru.otus.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.otus.backend.model.Post;
import ru.otus.backend.model.User;
import ru.otus.backend.service.FeedService;
import ru.otus.backend.service.PostService;
import ru.otus.backend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final FeedService feedService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Post> createPost(@RequestBody Post post, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        post.setUserId(user.getId());

        Post createdPost = postService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PostMapping("/update")
    public ResponseEntity<Post> updatePost(@RequestBody Post post, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        post.setUserId(user.getId());

        Post updatedPost = postService.updatePost(post);
        if (updatedPost == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(updatedPost);
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deletePost(@RequestParam Long postId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());

        postService.deletePost(postId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/get")
    public ResponseEntity<Post> getPost(@RequestParam Long postId) {
        Post post = postService.getPost(postId);
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(post);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            Authentication auth) {

        User user = userService.findByUsername(auth.getName());
        List<Post> feed = feedService.getFeed(user.getId(), limit, offset);

        return ResponseEntity.ok(feed);
    }
}