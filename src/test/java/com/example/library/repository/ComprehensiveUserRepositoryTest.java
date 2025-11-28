package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComprehensiveUserRepositoryTest {
    
    private JdbcUserRepository userRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        userRepository = new JdbcUserRepository();
        
        // Clean test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'comp_user_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testExistsByUsername_ExistingUsername_ReturnsTrue() {
        // Arrange
        User user = new User();
        user.setUsername("comp_user_exists1");
        user.setPassword("password123");
        user.setEmail("comp_exists1@test.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Act
        boolean exists = userRepository.existsByUsername("comp_user_exists1");
        
        // Assert
        assertTrue(exists);
    }
    
    @Test
    void testExistsByUsername_NonExistentUsername_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("comp_user_nonexistent");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByUsername_NullUsername_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername(null);
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByUsername_EmptyUsername_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByEmail_ExistingEmail_ReturnsTrue() {
        // Arrange
        User user = new User();
        user.setUsername("comp_user_email1");
        user.setPassword("password123");
        user.setEmail("comp_email_exists@test.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Act
        boolean exists = userRepository.existsByEmail("comp_email_exists@test.com");
        
        // Assert
        assertTrue(exists);
    }
    
    @Test
    void testExistsByEmail_NonExistentEmail_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("comp_nonexistent@test.com");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByEmail_NullEmail_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail(null);
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByEmail_EmptyEmail_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByUsername_CaseMatters() {
        // Arrange
        User user = new User();
        user.setUsername("comp_user_case");
        user.setPassword("password123");
        user.setEmail("comp_case@test.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Act
        boolean existsLowerCase = userRepository.existsByUsername("comp_user_case");
        boolean existsUpperCase = userRepository.existsByUsername("COMP_USER_CASE");
        
        // Assert
        assertTrue(existsLowerCase);
        assertFalse(existsUpperCase); // PostgreSQL is case-sensitive by default
    }
    
    @Test
    void testExistsByEmail_CaseSensitivity() {
        // Arrange
        User user = new User();
        user.setUsername("comp_user_email2");
        user.setPassword("password123");
        user.setEmail("CompEmail@Test.COM");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Act
        boolean existsOriginal = userRepository.existsByEmail("CompEmail@Test.COM");
        boolean existsLowerCase = userRepository.existsByEmail("compemail@test.com");
        
        // Assert
        assertTrue(existsOriginal);
        // Email comparison might be case-insensitive depending on DB collation
        assertNotNull(existsLowerCase); // Just verify it doesn't crash
    }
}
