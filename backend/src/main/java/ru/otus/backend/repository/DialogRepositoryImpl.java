package ru.otus.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.otus.backend.model.DialogMessage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class DialogRepositoryImpl implements DialogRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DialogMessage> dialogMessageRowMapper = (rs, rowNum) -> {
        DialogMessage message = new DialogMessage();
        message.setId(rs.getLong("id"));
        message.setFrom(rs.getLong("from_user_id"));
        message.setTo(rs.getLong("to_user_id"));
        message.setText(rs.getString("content"));
        message.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return message;
    };

    @Override
    public Long saveMessage(Long fromUserId, Long toUserId, String text, String dialogId) {
        String sql = "INSERT INTO messages (from_user_id, to_user_id, content, dialog_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);
            ps.setString(3, text);
            ps.setString(4, dialogId);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return (Long) Objects.requireNonNull(keyHolder.getKeys()).get("id");
    }

    @Override
    public List<DialogMessage> findMessagesByDialogId(String dialogId) {
        String sql = "SELECT id, from_user_id, to_user_id, content, created_at " +
                "FROM messages " +
                "WHERE dialog_id = ? " +
                "ORDER BY created_at ASC";

        return jdbcTemplate.query(sql, dialogMessageRowMapper, dialogId);
    }

    // Дополнительный метод для получения сообщений с пагинацией
    public List<DialogMessage> findMessagesByDialogId(String dialogId, int limit, int offset) {
        String sql = "SELECT id, from_user_id, to_user_id, content, created_at " +
                "FROM messages " +
                "WHERE dialog_id = ? " +
                "ORDER BY created_at ASC " +
                "LIMIT ? OFFSET ?";

        return jdbcTemplate.query(sql, dialogMessageRowMapper, dialogId, limit, offset);
    }
}
