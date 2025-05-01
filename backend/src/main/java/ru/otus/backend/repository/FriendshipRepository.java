package ru.otus.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.model.Friendship;

import java.util.List;

@Repository
public class FriendshipRepository {

    private final JdbcTemplate jdbcTemplate;

    public FriendshipRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Подсчитывает количество друзей у пользователя
     * @param userId ID пользователя
     * @return количество друзей
     */
    @Transactional(readOnly = true)
    public int countFriendsByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count != null ? count : 0;
    }

    @Transactional
    public void save(Friendship friendship) {
        jdbcTemplate.update(
                "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                friendship.getUserId(), friendship.getFriendId()
        );
    }

    @Transactional
    public void delete(Long userId, Long friendId) {
        jdbcTemplate.update(
                "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?",
                userId, friendId
        );
    }

    @Transactional(readOnly = true)
    public List<Long> findFriendIdsByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT friend_id FROM friendships WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    @Transactional(readOnly = true)
    public boolean existsFriendship(Long userId, Long friendId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?",
                Integer.class,
                userId, friendId
        );
        return count != null && count > 0;
    }
}
