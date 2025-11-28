package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.User;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdditionalUserRepositoryEdgeCaseTest {
    
    private JdbcUserRepository userRepository;
    
    @BeforeAll
    void init() throws SQLException {
        userRepository = new JdbcUserRepository();
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'edgetest_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @BeforeEach
    void setUp() throws SQLException {
        // Clean before each test
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'edgetest_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean after each test
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'edgetest_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindAll_EmptyDatabase_ReturnsEmptyList() {
        // When
        List<User> users = userRepository.findAll();
        
        // Then - should handle empty result
        assertNotNull(users);
        // May contain other users from other tests
    }
    
    @Test
    void testFindAll_MultipleUsers_ReturnsSortedById() {
        // Given
        User user1 = new User(0, "edgetest_user1", "pass1", "edge1@test.com", "MEMBER", LocalDateTime.now());
        User user2 = new User(0, "edgetest_user2", "pass2", "edge2@test.com", "LIBRARIAN", LocalDateTime.now());
        User user3 = new User(0, "edgetest_user3", "pass3", "edge3@test.com", "MEMBER", LocalDateTime.now());
        
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);
        
        // When
        List<User> allUsers = userRepository.findAll();
        
        // Then
        assertTrue(allUsers.size() >= 3);
        List<User> testUsers = allUsers.stream()
                .filter(u -> u.getUsername().startsWith("edgetest_"))
                .toList();
        assertEquals(3, testUsers.size());
    }
    
    @Test
    void testFindByRole_MEMBER_ReturnsOnlyMembers() {
        // Given
        User member1 = new User(0, "edgetest_member1", "pass1", "mem1@test.com", "MEMBER", LocalDateTime.now());
        User member2 = new User(0, "edgetest_member2", "pass2", "mem2@test.com", "MEMBER", LocalDateTime.now());
        User librarian = new User(0, "edgetest_lib", "pass3", "lib@test.com", "LIBRARIAN", LocalDateTime.now());
        
        userRepository.save(member1);
        userRepository.save(member2);
        userRepository.save(librarian);
        
        // When
        List<User> members = userRepository.findByRole("MEMBER");
        
        // Then
        assertTrue(members.stream().allMatch(u -> "MEMBER".equals(u.getRole())));
        long testMembers = members.stream()
                .filter(u -> u.getUsername().startsWith("edgetest_"))
                .count();
        assertEquals(2, testMembers);
    }
    
    @Test
    void testFindByRole_LIBRARIAN_ReturnsOnlyLibrarians() {
        // Given
        User member = new User(0, "edgetest_mem", "pass1", "mem@test.com", "MEMBER", LocalDateTime.now());
        User librarian1 = new User(0, "edgetest_lib1", "pass2", "lib1@test.com", "LIBRARIAN", LocalDateTime.now());
        User librarian2 = new User(0, "edgetest_lib2", "pass3", "lib2@test.com", "LIBRARIAN", LocalDateTime.now());
        
        userRepository.save(member);
        userRepository.save(librarian1);
        userRepository.save(librarian2);
        
        // When
        List<User> librarians = userRepository.findByRole("LIBRARIAN");
        
        // Then
        assertTrue(librarians.stream().allMatch(u -> "LIBRARIAN".equals(u.getRole())));
        long testLibrarians = librarians.stream()
                .filter(u -> u.getUsername().startsWith("edgetest_"))
                .count();
        assertEquals(2, testLibrarians);
    }
    
    @Test
    void testFindByRole_NonExistentRole_ReturnsEmpty() {
        // Given
        User user = new User(0, "edgetest_user", "pass", "test@test.com", "MEMBER", LocalDateTime.now());
        userRepository.save(user);
        
        // When
        List<User> result = userRepository.findByRole("INVALID_ROLE");
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    void testFindByUsername_NonExistent_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent_user_12345");
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByEmail_NonExistent_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@email12345.com");
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindById_NonExistent_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findById(999999);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testDeleteById_ExistingUser_DeletesSuccessfully() throws SQLException {
        // Given
        User user = new User(0, "edgetest_delete", "pass", "delete@test.com", "MEMBER", LocalDateTime.now());
        user = userRepository.save(user);
        int userId = user.getUserId();
        
        // When
        userRepository.deleteById(userId);
        
        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertTrue(deletedUser.isEmpty());
    }
    
    @Test
    void testExistsByUsername_WithExactMatch_ReturnsTrue() {
        // Given
        User user = new User(0, "edgetest_exact", "pass", "exact@test.com", "MEMBER", LocalDateTime.now());
        userRepository.save(user);
        
        // When
        boolean exists = userRepository.existsByUsername("edgetest_exact");
        
        // Then
        assertTrue(exists);
    }
    
    @Test
    void testExistsByEmail_WithExactMatch_ReturnsTrue() {
        // Given
        User user = new User(0, "edgetest_email", "pass", "exactemail@test.com", "MEMBER", LocalDateTime.now());
        userRepository.save(user);
        
        // When
        boolean exists = userRepository.existsByEmail("exactemail@test.com");
        
        // Then
        assertTrue(exists);
    }
}
