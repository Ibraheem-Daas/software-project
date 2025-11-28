package com.example.library.testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Singleton Testcontainers PostgreSQL setup for all repository integration tests.
 * 
 * This class:
 * - Starts a single PostgreSQL container shared across all tests (faster execution)
 * - Loads schema.sql automatically when container starts
 * - Configures system properties so DatabaseConfig uses the test container
 * - Keeps production code unchanged - only test configuration is affected
 * 
 * Usage in test classes:
 * 
 * @BeforeAll
 * static void setupTestContainer() {
 *     TestDatabaseContainer.start();
 * }
 */
public class TestDatabaseContainer {
    
    private static final String POSTGRES_IMAGE = "postgres:17-alpine";
    private static final String DATABASE_NAME = "library_test";
    private static final String USERNAME = "test_user";
    private static final String PASSWORD = "test_password";
    
    private static PostgreSQLContainer<?> container;
    private static boolean initialized = false;
    
    /**
     * Starts the PostgreSQL Testcontainer and loads the schema.
     * This method is idempotent - calling it multiple times is safe.
     */
    @SuppressWarnings("resource") // Container is intentionally kept open and closed via shutdown hook
    public static synchronized void start() {
        if (initialized) {
            return;
        }
        
        // Create and start PostgreSQL container
        container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName(DATABASE_NAME)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withReuse(false); // Fresh container for each test run
        
        container.start();
        
        // Override system properties so DatabaseConfig uses test container
        System.setProperty("db.url", container.getJdbcUrl());
        System.setProperty("db.username", container.getUsername());
        System.setProperty("db.password", container.getPassword());
        
        // Load schema.sql
        loadSchema();
        
        initialized = true;
        
        // Shutdown hook to stop container when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (container != null && container.isRunning()) {
                container.stop();
            }
        }));
    }
    
    /**
     * Loads the schema.sql file into the test database.
     */
    private static void loadSchema() {
        try (InputStream inputStream = TestDatabaseContainer.class
                .getClassLoader()
                .getResourceAsStream("schema.sql")) {
            
            if (inputStream == null) {
                throw new RuntimeException("Could not find schema.sql in test resources");
            }
            
            String schemaSql = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            try (Connection connection = DriverManager.getConnection(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword());
                 Statement statement = connection.createStatement()) {
                
                statement.execute(schemaSql);
                System.out.println("✅ Test database schema loaded successfully");
                
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema.sql", e);
        }
    }
    
    /**
     * Cleans all data from tables (keeps schema intact).
     * Useful to reset state between test classes.
     */
    public static void cleanDatabase() {
        if (!initialized) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        
        try (Connection connection = DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword());
             Statement statement = connection.createStatement()) {
            
            // Disable foreign key checks temporarily
            statement.execute("SET session_replication_role = 'replica'");
            
            // Truncate all tables
            statement.execute("TRUNCATE TABLE fine, loan, media_item, app_user RESTART IDENTITY CASCADE");
            
            // Re-enable foreign key checks
            statement.execute("SET session_replication_role = 'origin'");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean database", e);
        }
    }
    
    /**
     * Gets the JDBC URL for the test container.
     */
    public static String getJdbcUrl() {
        if (!initialized) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        return container.getJdbcUrl();
    }
    
    /**
     * Gets the username for the test container.
     */
    public static String getUsername() {
        if (!initialized) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        return container.getUsername();
    }
    
    /**
     * Gets the password for the test container.
     */
    public static String getPassword() {
        if (!initialized) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        return container.getPassword();
    }
    
    /**
     * Gets the underlying container instance.
     */
    public static PostgreSQLContainer<?> getContainer() {
        if (!initialized) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        return container;
    }
}
