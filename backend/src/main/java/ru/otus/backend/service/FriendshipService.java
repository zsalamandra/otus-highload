package ru.otus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.config.KafkaConfig;
import ru.otus.backend.model.Friendship;
import ru.otus.backend.repository.FriendshipRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot add yourself as a friend");
        }

        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);

        Map<String, Object> message = new HashMap<>();
        message.put("action", "ADD_FRIEND");
        message.put("userId", userId);
        message.put("friendId", friendId);

        kafkaTemplate.send(KafkaConfig.FEED_UPDATE_TOPIC, userId.toString(), message);

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {

        friendshipRepository.delete(userId, friendId);

        // Отправляем событие об удалении друга для обновления ленты
        Map<String, Object> message = new HashMap<>();
        message.put("action", "DELETE_FRIEND");
        message.put("userId", userId);
        message.put("friendId", friendId);

        kafkaTemplate.send(KafkaConfig.FEED_UPDATE_TOPIC, userId.toString(), message);
    }

    @Transactional(readOnly = true)
    public List<Long> getFriendIds(Long userId) {
        return friendshipRepository.findFriendIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFriend(Long userId, Long friendId) {
        return friendshipRepository.existsFriendship(userId, friendId);
    }
}