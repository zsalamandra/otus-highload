package ru.otus.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.backend.mapper.UserRowMapper;
import ru.otus.backend.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Repository
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserRepository(NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(User user) {

        String sql = "INSERT INTO users (first_name, last_name, birth_date, gender, interests, city, username, password) " +
                "VALUES (:firstName, :lastName, :birthDate, :gender, :interests, :city, :username, :password)";

        Map<String, Object> params = new HashMap<>();
        params.put("firstName", user.getFirstName());
        params.put("lastName", user.getLastName());
        params.put("birthDate", user.getBirthDate());
        params.put("gender", user.getGender());
        params.put("interests", user.getInterests());
        params.put("city", user.getCity());
        params.put("username", user.getUsername());
        params.put("password", user.getPassword());

        jdbcTemplate.update(sql, params);
    }

    @Transactional(readOnly = true) // Операция чтения - идет на slave
    public User findById(Long id) {

        String sql = "SELECT * FROM users WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);

        return jdbcTemplate.queryForObject(sql, params, new UserRowMapper());
    }

    @Transactional(readOnly = true) // Операция чтения - идет на slave
    public User findByUsername(String username) {

        String sql = "SELECT * FROM users WHERE username = :username";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username);

        return jdbcTemplate.queryForObject(sql, params, new UserRowMapper());
    }

    @Transactional(readOnly = true) // Операция чтения - идет на slave
    public List<User> findByFirstNameContainingAndLastNameContaining(String firstName, String lastName) {
        String sql = "SELECT * FROM users WHERE first_name LIKE :first_name AND last_name LIKE :last_name";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("first_name", firstName + "%")
                .addValue("last_name", lastName + "%");

        return jdbcTemplate.query(sql, params, new UserRowMapper());
    }
}
