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
 * Additional tests for uncovered branches in repository layer methods.
 * Targets findLoansDueSoon, countActiveByUserId, findByStatus, findActiveByUserId, findOverdueLoans.
 */
class RepositoryAdditionalBranchTest {
    
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
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'ADD-BRANCH-%'")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'add_branch_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindLoansDueSoon_NoLoans_ReturnsEmptyList() {
        // Act - check for loans due soon when no loans exist
        List<Loan> loans = loanRepository.findLoansDueSoon(LocalDate.now(), 7);
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when no loans due soon");
    }
    
    @Test
    void testFindByStatus_Loans_NoMatches_ReturnsEmptyList() {
        // Act
        List<Loan> loans = loanRepository.findByStatus("NONEXISTENT_STATUS");
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when no loans match status");
    }
    
    @Test
    void testFindActiveByUserId_NoActiveLoans_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no active loans
        User user = new User();
        user.setUsername("add_branch_user1");
        user.setPassword("password");
        user.setEmail("add_branch1@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Loan> loans = loanRepository.findActiveByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when user has no active loans");
    }
    
    @Test
    void testFindOverdueLoans_NoOverdue_ReturnsEmptyList() {
        // Act - check for overdue loans far in the future (no loans should be overdue)
        List<Loan> loans = loanRepository.findOverdueLoans(LocalDate.now().plusYears(10));
        
        // Assert
        assertNotNull(loans);
        assertTrue(loans.isEmpty(), "Should return empty list when no loans are overdue");
    }
    
    @Test
    void testCountActiveByUserId_NoActiveLoans_ReturnsZero() throws SQLException {
        // Arrange - create user with no active loans
        User user = new User();
        user.setUsername("add_branch_user2");
        user.setPassword("password");
        user.setEmail("add_branch2@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        int count = loanRepository.countActiveByUserId(savedUser.getUserId());
        
        // Assert
        assertEquals(0, count, "Should return 0 when user has no active loans");
    }
    
    @Test
    void testFindByUserId_Fines_NoFines_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no fines
        User user = new User();
        user.setUsername("add_branch_user3");
        user.setPassword("password");
        user.setEmail("add_branch3@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Fine> fines = fineRepository.findByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(fines);
        assertTrue(fines.isEmpty(), "Should return empty list when user has no fines");
    }
    
    @Test
    void testExistsByLoanId_NoFine_ReturnsFalse() throws SQLException {
        // Arrange - create a loan without a fine
        User user = new User();
        user.setUsername("add_branch_user4");
        user.setPassword("password");
        user.setEmail("add_branch4@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        MediaItem item = new MediaItem();
        item.setTitle("Test Book");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("ADD-BRANCH-001");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(3);
        item.setAvailableCopies(3);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        Loan loan = new Loan();
        loan.setUserId(savedUser.getUserId());
        loan.setItemId(savedItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        Loan savedLoan = loanRepository.save(loan);
        
        // Act
        boolean exists = fineRepository.existsByLoanId(savedLoan.getLoanId());
        
        // Assert
        assertFalse(exists, "Should return false when loan has no fine");
    }
    
    @Test
    void testFindByItemId_Reservations_NoMatches_ReturnsEmptyList() throws SQLException {
        // Arrange - create item with no reservations
        MediaItem item = new MediaItem();
        item.setTitle("Unreserved Item");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("ADD-BRANCH-002");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(2);
        item.setAvailableCopies(2);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        // Act
        List<Reservation> reservations = reservationRepository.findByItemId(savedItem.getItemId());
        
        // Assert
        assertNotNull(reservations);
        assertTrue(reservations.isEmpty(), "Should return empty list when item has no reservations");
    }
    
    @Test
    void testFindActiveByItemId_NoActive_ReturnsEmptyList() throws SQLException {
        // Arrange - create item with no active reservations
        MediaItem item = new MediaItem();
        item.setTitle("Item Without Active Reservations");
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setIsbn("ADD-BRANCH-003");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Publisher");
        item.setTotalCopies(2);
        item.setAvailableCopies(2);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        // Act
        List<Reservation> reservations = reservationRepository.findActiveByItemId(savedItem.getItemId());
        
        // Assert
        assertNotNull(reservations);
        assertTrue(reservations.isEmpty(), "Should return empty list when no active reservations exist");
    }
    
    @Test
    void testFindActiveByUserId_Reservations_NoActive_ReturnsEmptyList() throws SQLException {
        // Arrange - create user with no active reservations
        User user = new User();
        user.setUsername("add_branch_user5");
        user.setPassword("password");
        user.setEmail("add_branch5@example.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Act
        List<Reservation> reservations = reservationRepository.findActiveByUserId(savedUser.getUserId());
        
        // Assert
        assertNotNull(reservations);
        assertTrue(reservations.isEmpty(), "Should return empty list when user has no active reservations");
    }
    
    @Test
    void testDeleteById_Reservation_NonExistent_NoException() {
        // Act & Assert - try to delete non-existent reservation should not throw exception
        assertDoesNotThrow(() -> reservationRepository.deleteById(999999),
                "Should not throw exception when deleting non-existent reservation");
    }
    
    @Test
    void testDeleteById_MediaItem_NonExistent_ReturnsFalse() {
        // Act - try to delete non-existent media item
        boolean deleted = mediaItemRepository.deleteById(999999);
        
        // Assert
        assertFalse(deleted, "Should return false when deleting non-existent media item");
    }
    
    @Test
    void testDeleteById_Loan_NonExistent_ReturnsFalse() {
        // Act - try to delete non-existent loan
        boolean deleted = loanRepository.deleteById(999999);
        
        // Assert
        assertFalse(deleted, "Should return false when deleting non-existent loan");
    }
    
    @Test
    void testDeleteById_User_NonExistent_NoException() {
        // Act & Assert - try to delete non-existent user should not throw exception
        assertDoesNotThrow(() -> userRepository.deleteById(999999),
                "Should not throw exception when deleting non-existent user");
    }
    
    @Test
    void testDeleteById_Fine_NonExistent_ReturnsFalse() {
        // Act - try to delete non-existent fine
        boolean deleted = fineRepository.deleteById(999999);
        
        // Assert
        assertFalse(deleted, "Should return false when deleting non-existent fine");
    }
}
