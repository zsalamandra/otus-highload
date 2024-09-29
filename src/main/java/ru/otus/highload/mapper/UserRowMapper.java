package ru.otus.highload.mapper;

import org.springframework.jdbc.core.RowMapper;
import ru.otus.highload.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {

        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setBirthDate(rs.getDate("birth_date").toLocalDate());
        user.setGender(rs.getString("gender"));
        user.setInterests(rs.getString("interests"));
        user.setCity(rs.getString("city"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));

        return user;
    }
}
