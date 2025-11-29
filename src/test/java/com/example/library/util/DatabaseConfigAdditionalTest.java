package com.example.library.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigAdditionalTest {

    @AfterEach
    void clearProps() {
        System.clearProperty("db.url");
        System.clearProperty("db.username");
        System.clearProperty("db.password");
    }

    @Test
    void getUrl_usesSystemPropertyWhenNotEmpty() {
        System.setProperty("db.url", "jdbc:postgresql://test-host:5432/testdb");
        String url = DatabaseConfig.getUrl();
        assertEquals("jdbc:postgresql://test-host:5432/testdb", url);
    }

    @Test
    void getUrl_ignoresEmptySystemPropertyAndFallsBackToFile() {
        System.setProperty("db.url", "");
        String url = DatabaseConfig.getUrl();
        assertNotNull(url);
        assertFalse(url.isEmpty());
    }

    @Test
    void getUsername_usesSystemPropertyWhenNotEmpty() {
        System.setProperty("db.username", "test-user");
        assertEquals("test-user", DatabaseConfig.getUsername());
    }

    @Test
    void getUsername_ignoresEmptySystemPropertyAndFallsBackToFile() {
        System.setProperty("db.username", "");
        String username = DatabaseConfig.getUsername();
        assertNotNull(username);
        assertFalse(username.isEmpty());
    }

    @Test
    void getPassword_usesSystemPropertyWhenNotEmpty() {
        System.setProperty("db.password", "secret");
        assertEquals("secret", DatabaseConfig.getPassword());
    }

    @Test
    void getPassword_ignoresEmptySystemPropertyAndFallsBackToFile() {
        System.setProperty("db.password", "");
        String password = DatabaseConfig.getPassword();
        assertNotNull(password);
        assertFalse(password.isEmpty());
    }
}