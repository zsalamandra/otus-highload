package ru.otus.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

@Slf4j
@Service
public class DbInitService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.master.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.master.username}")
    private String dbUsername;

    @Value("${spring.datasource.master.password}")
    private String dbPassword;

    @Value("${db.name:z-social-network}")
    private String dbName;

    public DbInitService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initData() {
        log.info("Checking if database initialization is needed...");

        // Проверяем, существует ли база данных
        if (!isDatabaseExists()) {
            log.info("Database '{}' does not exist. Creating it...", dbName);
            createDatabase();
        }

        // Проверяем, существуют ли уже данные в таблице users
        if (isTableEmpty("users")) {
            log.info("Users table is empty, initializing with sample data...");
            loadDataFromSqlScript();
        } else {
            log.info("Users table already contains data, skipping initialization.");
        }
    }

    private boolean isDatabaseExists() {
        // Получаем URL для подключения к PostgreSQL (без указания БД)
        String baseUrl = masterDbUrl.substring(0, masterDbUrl.lastIndexOf('/'));
        String postgresUrl = baseUrl + "/postgres"; // Подключаемся к системной БД postgres

        try (Connection conn = DriverManager.getConnection(postgresUrl, dbUsername, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Проверяем, существует ли наша БД
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            return rs.next();
        } catch (SQLException e) {
            log.error("Error checking if database exists: {}", e.getMessage(), e);
            return false;
        }
    }

    private void createDatabase() {
        // Получаем URL для подключения к PostgreSQL (без указания БД)
        String baseUrl = masterDbUrl.substring(0, masterDbUrl.lastIndexOf('/'));
        String postgresUrl = baseUrl + "/postgres"; // Подключаемся к системной БД postgres

        try (Connection conn = DriverManager.getConnection(postgresUrl, dbUsername, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Создаем новую базу данных
            String sql = "CREATE DATABASE \"" + dbName + "\"";
            stmt.executeUpdate(sql);
            log.info("Database '{}' successfully created", dbName);

            // Ждем немного, чтобы БД стала доступна
            Thread.sleep(2000);
        } catch (SQLException e) {
            log.error("Error creating database: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for database creation");
        }
    }

    private boolean isTableEmpty(String tableName) {
        try {
            // Проверяем, существует ли таблица
            if (!isTableExists(tableName)) {
                log.info("Table '{}' does not exist. Creating it...", tableName);
                createUsersTable();
                return true;
            }

            // Проверяем, есть ли записи в таблице
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
            return count == null || count == 0;
        } catch (Exception e) {
            log.error("Error checking if table is empty: {}", e.getMessage(), e);
            return true; // В случае ошибки считаем, что таблица пуста
        }
    }

    private boolean isTableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(
                    null, "public", tableName.toLowerCase(), new String[]{"TABLE"});
            return tables.next();
        } catch (SQLException e) {
            log.error("Error checking if table exists: {}", e.getMessage(), e);
            return false;
        }
    }

    private void createUsersTable() {
        log.info("Creating users table...");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                first_name VARCHAR(100),
                last_name VARCHAR(100),
                birth_date DATE,
                gender VARCHAR(10),
                interests TEXT,
                city VARCHAR(100),
                username VARCHAR(100) UNIQUE,
                password VARCHAR(255)
            )
        """);
    }

    private void loadDataFromSqlScript() {
        try {
            // Загружаем SQL-скрипт из ресурсов
            ClassPathResource resource = new ClassPathResource("insert_user.sql");
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            String sql = FileCopyUtils.copyToString(reader);

            log.info("Executing SQL script to load users...");
            jdbcTemplate.execute(sql);
            log.info("Successfully loaded sample users data.");
        } catch (IOException e) {
            log.error("Failed to load SQL script: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error executing SQL script: {}", e.getMessage(), e);
        }
    }

    // Старый метод загрузки из CSV файла - можно оставить как альтернативу или удалить
    public void loadDataFromCsv() {
        log.info("Loading data from CSV file...");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO users (first_name, last_name, birth_date, city, username, password) VALUES (?, ?, ?, ?, ?, ?)");
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("people.v2.csv"))))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != 6) {
                    throw new IllegalArgumentException("Invalid CSV format: " + line);
                }

                statement.setString(1, values[0]); // first_name
                statement.setString(2, values[1]); // last_name
                statement.setDate(3, java.sql.Date.valueOf(values[2])); // birth_date
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

    // Проверка, является ли узел мастером (primary)
    private boolean isPrimaryNode() {
        try {
            // Проверка, является ли узел мастером в Patroni кластере
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT NOT pg_is_in_recovery()", Boolean.class));
        } catch (Exception e) {
            log.error("Error checking if node is primary: {}", e.getMessage(), e);
            return false;
        }
    }
}