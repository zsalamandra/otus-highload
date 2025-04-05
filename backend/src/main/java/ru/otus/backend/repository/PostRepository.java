package ru.otus.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.model.Post;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class PostRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Post> postRowMapper;

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.postRowMapper = (rs, rowNum) -> {
            Post post = new Post();
            post.setId(rs.getLong("id"));
            post.setUserId(rs.getLong("user_id"));
            post.setContent(rs.getString("content"));
            post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return post;
        };
    }

    @Transactional
    public Post save(Post post) {
        if (post.getId() == null) {
            // Создание нового поста
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO posts (user_id, content, created_at) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, post.getUserId());
                ps.setString(2, post.getContent());
                ps.setTimestamp(3, post.getCreatedAt() != null ?
                        Timestamp.valueOf(post.getCreatedAt()) :
                        Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, keyHolder);

            post.setId((Long) Objects.requireNonNull(keyHolder.getKeys()).get("id"));
        } else {
            // Обновление существующего поста
            jdbcTemplate.update(
                    "UPDATE posts SET content = ? WHERE id = ? AND user_id = ?",
                    post.getContent(), post.getId(), post.getUserId());
        }
        return post;
    }

    @Transactional(readOnly = true)
    public Post findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM posts WHERE id = ?",
                new Object[]{id},
                postRowMapper
        );
    }

    @Transactional
    public void deleteById(Long id, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM posts WHERE id = ? AND user_id = ?",
                id, userId
        );
    }

    @Transactional(readOnly = true)
    public List<Post> findByUserIdOrderByCreatedAtDesc(Long userId, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
                new Object[]{userId, limit},
                postRowMapper
        );
    }

    @Transactional(readOnly = true)
    public List<Post> findByUserIdsOrderByCreatedAtDesc(List<Long> userIds, int limit) {
        // Конвертируем список ID в строку для SQL запроса
        String inClause = String.join(",", userIds.stream().map(String::valueOf).toArray(String[]::new));

        return jdbcTemplate.query(
                "SELECT * FROM posts WHERE user_id IN (" + inClause + ") ORDER BY created_at DESC LIMIT ?",
                new Object[]{limit},
                postRowMapper
        );
    }
}
