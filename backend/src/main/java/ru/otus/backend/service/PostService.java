package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.config.KafkaConfig;
import ru.otus.backend.model.FeedUpdateTask;
import ru.otus.backend.model.Post;
import ru.otus.backend.model.PostCreatedEvent;
import ru.otus.backend.repository.FriendshipRepository;
import ru.otus.backend.repository.PostRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FriendshipRepository friendshipRepository;

    // Пороговое значение для определения "знаменитости"
    private static final int CELEBRITY_THRESHOLD = 1000;

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

        // Получаем список друзей автора поста
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(savedPost.getUserId());

        // Создаем и отправляем событие о новом посте в отдельный топик для WebSocket уведомлений
        PostCreatedEvent event = new PostCreatedEvent(
                savedPost.getId(),
                savedPost.getUserId(),
                savedPost.getContent(),
                savedPost.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
        );
        kafkaTemplate.send(PostCreatedEvent.TOPIC, savedPost.getUserId().toString(), event);

        // Проверяем, является ли автор "знаменитостью"
        boolean isCelebrity = friendIds.size() > CELEBRITY_THRESHOLD;

        // Создаем задачу на материализацию ленты
        if (isCelebrity) {
            // Для знаменитостей - не материализуем ленту сразу, а только отправляем уведомления
            log.info("Пользователь {} является знаменитостью с {} подписчиками. Материализация ленты отложена.",
                    savedPost.getUserId(), friendIds.size());
        } else {
            // Для обычных пользователей - отправляем задачу на материализацию ленты
            FeedUpdateTask feedTask = new FeedUpdateTask(
                    savedPost.getId(),
                    savedPost.getUserId(),
                    friendIds,
                    FeedUpdateTask.TaskType.ADD_POST
            );

            kafkaTemplate.send(FeedUpdateTask.TOPIC, savedPost.getUserId().toString(), feedTask);
            log.info("Отправлена задача на материализацию ленты для поста {} пользователя {} для {} друзей",
                    savedPost.getId(), savedPost.getUserId(), friendIds.size());
        }

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