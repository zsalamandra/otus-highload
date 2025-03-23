package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.config.KafkaConfig;
import ru.otus.backend.model.Post;
import ru.otus.backend.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Post createPost(Post post) {
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
        }

        Post savedPost = postRepository.save(post);

        // Отправляем сообщение в очередь для асинхронного обновления лент
        Map<String, Object> message = new HashMap<>();
        message.put("action", "CREATE");
        message.put("post", savedPost);

        kafkaTemplate.send(KafkaConfig.FEED_UPDATE_TOPIC, savedPost.getUserId().toString(), message);

        return savedPost;
    }

    @Transactional
    public Post updatePost(Post post) {
        Post existingPost = postRepository.findById(post.getId());

        if (existingPost != null && existingPost.getUserId().equals(post.getUserId())) {
            existingPost.setContent(post.getContent());

            Post updatedPost = postRepository.save(existingPost);

            Map<String, Object> message = new HashMap<>();
            message.put("action", "UPDATE");
            message.put("post", updatedPost);

            kafkaTemplate.send(KafkaConfig.FEED_UPDATE_TOPIC, updatedPost.getUserId().toString(), message);

            return updatedPost;
        }

        return null; // Пост не найден или не принадлежит пользователю
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post existingPost = postRepository.findById(postId);

        if (existingPost != null && existingPost.getUserId().equals(userId)) {
            postRepository.deleteById(postId, userId);

            Map<String, Object> message = new HashMap<>();
            message.put("action", "DELETE");
            message.put("postId", postId);
            message.put("userId", userId);

            kafkaTemplate.send(KafkaConfig.FEED_UPDATE_TOPIC, userId.toString(), message);
        }
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findById(postId);
    }

    @Transactional(readOnly = true)
    public List<Post> getUserPosts(Long userId, int limit) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, limit);
    }
}