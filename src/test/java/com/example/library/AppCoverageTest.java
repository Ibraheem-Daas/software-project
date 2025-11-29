package com.example.library;

import com.example.library.DatabaseConnection;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppCoverageTest {

    private String oldUrl;
    private String oldUser;
    private String oldPass;

    @BeforeEach
    void backupProps() {
        oldUrl  = System.getProperty("db.url");
        oldUser = System.getProperty("db.username");
        oldPass = System.getProperty("db.password");
    }

    @AfterEach
    void restoreProps() {
        restore("db.url", oldUrl);
        restore("db.username", oldUser);
        restore("db.password", oldPass);
    }

    private void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void main_shouldRunSuccessfully_whenDatabaseIsConfigured() {
        assertDoesNotThrow(() -> App.main(new String[0]));
    }

    @Test
    void main_shouldHandleConnectionFailure_gracefully() {
        System.setProperty("db.url", "jdbc:postgresql://invalid-host:6543/invaliddb");
        System.setProperty("db.username", "invalid-user");
        System.setProperty("db.password", "invalid-pass");

        DatabaseConnection.closeConnection();

        assertDoesNotThrow(() -> App.main(new String[0]));
    }
}