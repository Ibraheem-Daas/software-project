package com.example.library.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @AfterEach
    void cleanup() {
        // Clear system properties after each test
        System.clearProperty("db.url");
        System.clearProperty("db.username");
        System.clearProperty("db.password");
    }

    @Test
    void testGetUrl() {
        String url = DatabaseConfig.getUrl();
        assertNotNull(url, "Database URL should not be null");
        assertTrue(url.contains("jdbc"), "URL should be a JDBC URL");
    }

    @Test
    void testGetUrl_withSystemPropertyOverride() {
        String testUrl = "jdbc:postgresql://override-host:5432/testdb";
        System.setProperty("db.url", testUrl);
        
        String url = DatabaseConfig.getUrl();
        assertEquals(testUrl, url, "Should use system property override");
    }

    @Test
    void testGetUrl_withEmptySystemProperty() {
        System.setProperty("db.url", "");
        
        String url = DatabaseConfig.getUrl();
        assertNotNull(url);
        assertTrue(url.contains("jdbc"), "Should use properties file when system property is empty");
    }

    @Test
    void testGetUsername() {
        String username = DatabaseConfig.getUsername();
        assertNotNull(username, "Database username should not be null");
        assertFalse(username.isEmpty(), "Database username should not be empty");
    }

    @Test
    void testGetUsername_withSystemPropertyOverride() {
        String testUsername = "override_user";
        System.setProperty("db.username", testUsername);
        
        String username = DatabaseConfig.getUsername();
        assertEquals(testUsername, username, "Should use system property override");
    }

    @Test
    void testGetUsername_withEmptySystemProperty() {
        System.setProperty("db.username", "");
        
        String username = DatabaseConfig.getUsername();
        assertNotNull(username);
        assertFalse(username.isEmpty(), "Should use properties file when system property is empty");
    }

    @Test
    void testGetPassword() {
        String password = DatabaseConfig.getPassword();
        assertNotNull(password, "Database password should not be null");
    }

    @Test
    void testGetPassword_withSystemPropertyOverride() {
        String testPassword = "override_password";
        System.setProperty("db.password", testPassword);
        
        String password = DatabaseConfig.getPassword();
        assertEquals(testPassword, password, "Should use system property override");
    }

    @Test
    void testGetPassword_withEmptySystemProperty() {
        System.setProperty("db.password", "");
        
        String password = DatabaseConfig.getPassword();
        assertNotNull(password, "Should use properties file when system property is empty");
    }

    @Test
    void testGetDriver() {
        String driver = DatabaseConfig.getDriver();
        assertNotNull(driver, "Database driver should not be null");
        assertTrue(driver.contains("postgresql"), "Driver should be PostgreSQL driver");
    }

    @Test
    void testConfigurationIsConsistent() {
        // Verify all required properties are present
        assertNotNull(DatabaseConfig.getUrl(), "URL must be configured");
        assertNotNull(DatabaseConfig.getUsername(), "Username must be configured");
        assertNotNull(DatabaseConfig.getPassword(), "Password must be configured");
        assertNotNull(DatabaseConfig.getDriver(), "Driver must be configured");
    }

    @Test
    void testGetUrl_ContainsDatabaseName() {
        String url = DatabaseConfig.getUrl();
        assertTrue(url.contains("library_db") || url.contains("library_test") || url.contains("database"), 
            "URL should contain database name");
    }

    @Test
    void testGetDriver_IsPostgresDriver() {
        String driver = DatabaseConfig.getDriver();
        assertEquals("org.postgresql.Driver", driver, 
            "Driver should be PostgreSQL JDBC driver");
    }

    @Test
    void testAllSystemPropertiesOverride() {
        // Test all overrides together
        System.setProperty("db.url", "jdbc:test:override");
        System.setProperty("db.username", "test_user");
        System.setProperty("db.password", "test_pass");
        
        assertEquals("jdbc:test:override", DatabaseConfig.getUrl());
        assertEquals("test_user", DatabaseConfig.getUsername());
        assertEquals("test_pass", DatabaseConfig.getPassword());
    }
}
