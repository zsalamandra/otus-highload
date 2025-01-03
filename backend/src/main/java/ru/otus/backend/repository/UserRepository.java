package ru.otus.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.otus.backend.mapper.UserRowMapper;
import ru.otus.backend.model.User;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void save(User user) {
        String sql = "INSERT INTO users (first_name, last_name, birth_date, gender, interests, city, username, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getFirstName(), user.getLastName(), user.getBirthDate(), user.getGender(), user.getInterests(), user.getCity(), user.getUsername(), user.getPassword());
    }

    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{id}, new UserRowMapper());
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, new UserRowMapper());
    }

    public List<User> findByFirstNameContainingAndLastNameContaining(String firstName, String lastName) {
        String sql = "SELECT * FROM users WHERE first_name LIKE :first_name AND last_name LIKE :last_name";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("first_name", "%" + firstName + "%")
                .addValue("last_name", "%" + lastName + "%");

        return namedParameterJdbcTemplate.query(sql, params, new UserRowMapper());
    }
}
