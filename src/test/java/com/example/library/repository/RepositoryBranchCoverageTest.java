package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for uncovered branches in repository layer.
 * Focuses on empty result sets and false conditions to achieve 90% coverage.
 */
class RepositoryBranchCoverageTest {
    
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    private JdbcLoanRepository loanRepository;
    private JdbcFineRepository fineRepository;
    private JdbcReservationRepository reservationRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        loanRepository = new JdbcLoanRepository();
        fineRepository = new JdbcFineRepository();
        reservationRepository = new JdbcReservationRepository();
        
        // Clean all test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM reservation WHERE reservation_id > 0")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM fine WHERE fine_id > 0")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM loan WHERE loan_id > 0")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'BRANCH-TEST-%'")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'branch_test_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindByType_NoMatches_ReturnsEmptyList() {
        // Act
        List<MediaItem> items = mediaItemRepository.findByType("NONEXISTENT_TYPE");
        
        // Assert
        assertNotNull(items);
        assertTrue(items.isEmpty(), "Should return empty list when no items match type");
    }
    
    @Test
    void testFindByTitleContaining_NoMatches_ReturnsEmptyList() {
        // Act
        List<MediaItem> items = mediaItemRepository.findByTitleContaining("ZZZZZ_NONEXISTENT_TITLE_ZZZZZ");
        
        // Assert
        assertNotNull(items);
        assertTrue(items.isEmpty(), "Should return empty list when no titles match search");
    }
    
    @Test
    void testFindByAuthorContaining_NoMatches_ReturnsEmptyList() {
        // Act
        List<MediaItem> items = mediaItemRepository.findByAuthorContaining("ZZZZZ_NONEXISTENT_AUTHOR_ZZZZZ");
        
        // Assert
        assertNotNull(items);
        assertTrue(items.isEmpty(), "Should return empty list when no authors match search");
    }
    
    @Test
    void testFindAvailableItems_WhenAllUnavailable_ReturnsEmptyList() throws SQLException {
        // Arrange - create item with 0 available copies
        MediaItem item = new MediaItem();
        item.setTitle("Unavailable Book");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("BRANCH-TEST-001");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(0); // All copies borrowed
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        mediaItemRepository.save(item);
        
        // Act
        List<MediaItem> availableItems = mediaItemRepository.findAvailableItems();
        
        // Assert
        assertTrue(availableItems.stream().noneMatch(i -> i.getIsbn().equals("BRANCH-TEST-001")),
                "Should not include items with 0 available copies");
    }
    
    @Test
    void testExistsByIsbn_NotExists_ReturnsFalse() {
        // Act
        boolean exists = mediaItemRepository.existsByIsbn("NONEXISTENT-ISBN-999");
        
        // Assert
        assertFalse(exists, "Should return false when ISBN does not exist");
    }
    
    @Test
    void testFindByRole_NoMatches_ReturnsEmptyList() {
        // Act
        List<User> users = userRepository.findByRole("NONEXISTENT_ROLE");
        
        // Assert
        assertNotNull(users);
        assertTrue(users.isEmpty(), "Should return empty list when no users have role");
    }
    
    @Test
    void testExistsByUsername_NotExists_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent_user_999");
        
        // Assert
        assertFalse(exists, "Should return false when username does not exist");
    }
    
    @Test
    void testExistsByEmail_NotExists_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent999@example.com");
        
        // Assert
        assertFalse(exists, "Should return false when email does not exist");
    }
    
    @Test
    void testFindByUserId_NoLoans_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no loans
        User user = new User();
        user.setUsername("branch_test_user1");
        user.setPassword("password");
        user.setEmail("branch_test1@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Loan> loans = loanRepository.findByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when user has no loans");
    }
    
    @Test
    void testFindByItemId_NoLoans_ReturnsEmptyList() throws SQLException {
        // Arrange - create item with no loans
        MediaItem item = new MediaItem();
        item.setTitle("Never Borrowed Book");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("BRANCH-TEST-002");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(3);
        item.setAvailableCopies(3);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        // Act
        List<Loan> loans = loanRepository.findByItemId(savedItem.getItemId());
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when item has no loans");
    }
    
    @Test
    void testFindByStatus_Fines_NoMatches_ReturnsEmptyList() {
        // Act
        List<Fine> fines = fineRepository.findByStatus("NONEXISTENT_STATUS");
        
        // Assert
        assertNotNull(fines);
        assertTrue(fines.isEmpty(), "Should return empty list when no fines match status");
    }
    
    @Test
    void testFindUnpaidByUserId_NoFines_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no fines
        User user = new User();
        user.setUsername("branch_test_user2");
        user.setPassword("password");
        user.setEmail("branch_test2@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Fine> fines = fineRepository.findUnpaidByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(fines);
        assertTrue(fines.isEmpty(), "Should return empty list when user has no unpaid fines");
    }
    
    @Test
    void testCalculateTotalUnpaidByUserId_NoFines_ReturnsZero() throws SQLException {
        // Arrange - create user with no fines
        User user = new User();
        user.setUsername("branch_test_user3");
        user.setPassword("password");
        user.setEmail("branch_test3@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        BigDecimal total = fineRepository.calculateTotalUnpaidByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(total);
        assertEquals(BigDecimal.ZERO, total, "Should return zero when user has no unpaid fines");
    }
    
    @Test
    void testFindByUserId_Reservations_NoMatches_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no reservations
        User user = new User();
        user.setUsername("branch_test_user4");
        user.setPassword("password");
        user.setEmail("branch_test4@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Reservation> reservations = reservationRepository.findByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(reservations);
        assertTrue(reservations.isEmpty(), "Should return empty list when user has no reservations");
    }
    
    @Test
    void testFindExpiredReservations_NoExpired_ReturnsEmptyList() {
        // Act - check for expired reservations far in the past
        List<Reservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now().minusYears(10));
        
        // Assert
        assertNotNull(expired);
        assertTrue(expired.isEmpty(), "Should return empty list when no reservations expired before given time");
    }
    
    @Test
    void testCountActiveByItemId_NoReservations_ReturnsZero() throws SQLException {
        // Arrange - create item with no reservations
        MediaItem item = new MediaItem();
        item.setTitle("Unreserved Book");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("BRANCH-TEST-003");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(2);
        item.setAvailableCopies(2);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        // Act
        int count = reservationRepository.countActiveByItemId(savedItem.getItemId());
        
        // Assert
        assertEquals(0, count, "Should return 0 when item has no active reservations");
    }
}
