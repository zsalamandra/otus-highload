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
import java.util.HashMap;
import java.util.Map;
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

        // Читаем переменную окружения - должна ли эта нода инициализировать данные
        String initializerNode = System.getenv("INITIALIZER_NODE");
        String instanceId = System.getenv("INSTANCE_ID");

        // Если эта нода не отмечена как инициализатор, пропускаем инициализацию
        if (initializerNode != null && instanceId != null && !initializerNode.equals(instanceId)) {
            log.info("This instance ({}) is not the initializer node. Skipping initialization.", instanceId);
            return;
        }

        // Проверяем, существует ли база данных
        if (!isDatabaseExists()) {
            log.info("Database '{}' does not exist. Creating it...", dbName);
            createDatabase();
        }

        createTable("users");
        loadDataFromSqlScript("sql/insert_users.sql");

        createTable("friendships");
        loadDataFromSqlScript("sql/insert_friendships.sql");

        createTable("posts");
        loadPostsFromTextFile("sql/posts.txt");

        createTable("feeds");
        loadDataFromSqlScript("sql/insert_feeds.sql");

        createTable("messages");
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


    private void createTable(String tableName) {

        log.info("Creating table: {}", tableName);
        try {
            String createTableScript = readResourceFile("sql/" + tableName + ".sql");
            jdbcTemplate.execute(createTableScript);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void loadDataFromSqlScript(String scriptName) {
        try {
            // Загружаем SQL-скрипт из ресурсов
            String sql = readResourceFile(scriptName);

            log.info("Executing SQL script to load data...");
            jdbcTemplate.execute(sql);
            log.info("Successfully loaded sample data.");
        } catch (IOException e) {
            log.error("Failed to load SQL script: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error executing SQL script: {}", e.getMessage(), e);
        }
    }

    private String readResourceFile(String scriptName) throws IOException {
        ClassPathResource resource = new ClassPathResource(scriptName);
        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        return FileCopyUtils.copyToString(reader);
    }

    private void loadPostsFromTextFile(String fileName) {
        try {
            // Загружаем файл из ресурсов
            ClassPathResource resource = new ClassPathResource(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            log.info("Loading posts from file: {}", fileName);

            String line;
            int postCount = 0;

            // Читаем файл построчно
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Пропускаем пустые строки
                    // Вставляем пост в таблицу posts
                    jdbcTemplate.update(
                            "INSERT INTO posts (user_id, content) VALUES (?, ?)",
                            getRandomUserId(), // Случайный user_id
                            line.trim()        // Текст поста
                    );
                    postCount++;
                }
            }

            log.info("Successfully loaded {} posts into the 'posts' table.", postCount);
        } catch (IOException e) {
            log.error("Failed to load posts from file: {}", e.getMessage(), e);
        }
    }

    private Integer getRandomUserId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users ORDER BY RANDOM() LIMIT 1",
                Integer.class
        );
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