package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUserRepositoryExceptionTest {
    
    private JdbcUserRepository userRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        userRepository = new JdbcUserRepository();
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'exception_test_%'")) {
                stmt.executeUpdate();
            }
        }
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'exception_test_%'")) {
                stmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testUpdate_NonExistentUser_ThrowsException() {
        User user = new User();
        user.setUserId(99999); // Non-existent ID
        user.setUsername("exception_test_nonexistent");
        user.setPassword("password");
        user.setEmail("nonexistent@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        
        assertThrows(DataAccessException.class, () -> {
            userRepository.update(user);
        });
    }
    
    @Test
    void testSave_DuplicateUsername_ThrowsException() throws SQLException {
        User user1 = new User();
        user1.setUsername("exception_test_duplicate");
        user1.setPassword("password");
        user1.setEmail("unique1@example.com");
        user1.setRole("USER");
        user1.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setUsername("exception_test_duplicate"); // Duplicate username
        user2.setPassword("password");
        user2.setEmail("unique2@example.com");
        user2.setRole("USER");
        user2.setCreatedAt(LocalDateTime.now());
        
        assertThrows(DataAccessException.class, () -> {
            userRepository.save(user2);
        });
    }
    
    @Test
    void testSave_DuplicateEmail_ThrowsException() throws SQLException {
        User user1 = new User();
        user1.setUsername("exception_test_user1");
        user1.setPassword("password");
        user1.setEmail("exception_test_duplicate@example.com");
        user1.setRole("USER");
        user1.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(user1);
        
        User user2 = new User();
        user2.setUsername("exception_test_user2");
        user2.setPassword("password");
        user2.setEmail("exception_test_duplicate@example.com"); // Duplicate email
        user2.setRole("USER");
        user2.setCreatedAt(LocalDateTime.now());
        
        assertThrows(DataAccessException.class, () -> {
            userRepository.save(user2);
        });
    }
    
    @Test
    void testSave_WithNullUsername_ThrowsException() {
        User user = new User();
        user.setUsername(null); // Null username
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        
        assertThrows(DataAccessException.class, () -> {
            userRepository.save(user);
        });
    }
    
    @Test
    void testUpdate_WithNullUsername_ThrowsException() throws SQLException {
        User user = new User();
        user.setUsername("exception_test_update_null");
        user.setPassword("password");
        user.setEmail("updatenull@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        
        User saved = userRepository.save(user);
        
        saved.setUsername(null); // Set username to null
        
        assertThrows(DataAccessException.class, () -> {
            userRepository.update(saved);
        });
    }
    
    @Test
    void testDeleteById_NonExistentUser_DoesNotThrow() {
        assertDoesNotThrow(() -> {
            userRepository.deleteById(99999);
        });
    }
    
    @Test
    void testDeleteById_ValidId_DoesNotThrow() throws SQLException {
        User user = new User();
        user.setUsername("exception_test_delete");
        user.setPassword("password");
        user.setEmail("delete@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        
        User saved = userRepository.save(user);
        
        assertDoesNotThrow(() -> {
            userRepository.deleteById(saved.getUserId());
        });
    }
    
    @Test
    void testFindById_InvalidId_ReturnsEmpty() {
        var result = userRepository.findById(-1);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByUsername_NullUsername_ReturnsEmpty() {
        var result = userRepository.findByUsername(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByEmail_NullEmail_ReturnsEmpty() {
        var result = userRepository.findByEmail(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testExistsByUsername_NullUsername_ReturnsFalse() {
        boolean result = userRepository.existsByUsername(null);
        assertFalse(result);
    }
    
    @Test
    void testExistsByEmail_NullEmail_ReturnsFalse() {
        boolean result = userRepository.existsByEmail(null);
        assertFalse(result);
    }
    
    @Test
    void testFindByRole_NullRole_ReturnsEmpty() {
        var result = userRepository.findByRole(null);
        assertTrue(result.isEmpty());
    }
}
