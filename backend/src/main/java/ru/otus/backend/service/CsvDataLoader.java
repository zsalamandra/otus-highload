package ru.otus.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.Objects;

@Slf4j
@Service
public class CsvDataLoader {

    private final DataSource dataSource;

    public CsvDataLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public void loadData() {

        log.info("Loading data from database...");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users (first_name, last_name, birth_date, city, username, password) VALUES (?, ?, ?, ?, ?, ?)");
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("init/people.csv"))))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != 6) {
                    throw new IllegalArgumentException("Invalid CSV format: " + line);
                }

                statement.setString(1, values[0]); // first_name
                statement.setString(2, values[1]); // last_name
                statement.setDate(3, Date.valueOf(values[2])); // birth_date
                statement.setString(4, values[3]); // city
                statement.setString(5, values[4]); // username
                statement.setString(6, values[5]); // password
                statement.addBatch();
            }
            statement.executeBatch();

            log.info("Data successfully loaded from CSV!");

        } catch (Exception e) {
            throw new RuntimeException("Error loading data from CSV file", e);
        }
    }
}