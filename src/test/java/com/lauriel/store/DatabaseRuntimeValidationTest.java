package com.lauriel.store;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Explicit opt-in: uses configured MySQL and ddl-auto=validate only.
// No test database is created, no SQL dump is imported, and no data is written.
@EnabledIfEnvironmentVariable(named = "LAURIEL_DB_VALIDATION", matches = "true")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "app.roles.initialize=false"
)
class DatabaseRuntimeValidationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void startsApplicationAndValidatesExistingMysqlSchema() {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(
            18
        );
    }
}
