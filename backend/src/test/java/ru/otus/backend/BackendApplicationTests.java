package ru.otus.backend;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private SpringLiquibase liquibase;

    @Test
    void contextLoads() {
    }

    @Test
    public void testLiquibase() throws Exception {
        liquibase.afterPropertiesSet();
    }
}
