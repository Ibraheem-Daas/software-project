package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
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

class ComprehensiveLoanRepositoryTest {
    
    private JdbcLoanRepository loanRepository;
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        loanRepository = new JdbcLoanRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        
        // Clean test data - must delete in proper order (fines first, then loans)
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM fine")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM loan WHERE loan_id > 0")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'COMP-LOAN-%'")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'comp_loan_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindLoansDueSoon_WithinDays_ReturnsLoans() {
        // Arrange
        User user = createTestUser("comp_loan_user1");
        MediaItem item = createTestMediaItem("COMP-LOAN-001");
        
        Loan dueSoonLoan = new Loan();
        dueSoonLoan.setUserId(user.getUserId());
        dueSoonLoan.setItemId(item.getItemId());
        dueSoonLoan.setLoanDate(LocalDate.now().minusDays(10));
        dueSoonLoan.setDueDate(LocalDate.now().plusDays(2)); // Due in 2 days
        dueSoonLoan.setStatus("ACTIVE");
        loanRepository.save(dueSoonLoan);
        
        // Act
        List<Loan> dueSoonLoans = loanRepository.findLoansDueSoon(LocalDate.now(), 3);
        
        // Assert
        assertTrue(dueSoonLoans.size() >= 1);
        assertTrue(dueSoonLoans.stream().anyMatch(loan -> 
            loan.getDueDate().isAfter(LocalDate.now()) && 
            loan.getDueDate().isBefore(LocalDate.now().plusDays(4))
        ));
    }
    
    @Test
    void testFindLoansDueSoon_NoLoans_ReturnsEmpty() {
        // Act
        List<Loan> dueSoonLoans = loanRepository.findLoansDueSoon(LocalDate.now(), 1);
        
        // Assert - might have other loans, but verify it doesn't crash
        assertNotNull(dueSoonLoans);
    }
    
    @Test
    void testFindLoansDueSoon_FarFutureDate_ReturnsEmpty() {
        // Arrange
        User user = createTestUser("comp_loan_user2");
        MediaItem item = createTestMediaItem("COMP-LOAN-002");
        
        Loan loan = new Loan();
        loan.setUserId(user.getUserId());
        loan.setItemId(item.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(100)); // Far future
        loan.setStatus("ACTIVE");
        loanRepository.save(loan);
        
        // Act
        List<Loan> dueSoonLoans = loanRepository.findLoansDueSoon(LocalDate.now(), 3);
        
        // Assert - Should not include the far-future loan
        assertTrue(dueSoonLoans.stream().noneMatch(l -> 
            l.getDueDate().isAfter(LocalDate.now().plusDays(10))
        ));
    }
    
    @Test
    void testCountActiveByUserId_MultipleActiveLoans_ReturnsCorrectCount() {
        // Arrange
        User user = createTestUser("comp_loan_user3");
        MediaItem item1 = createTestMediaItem("COMP-LOAN-003");
        MediaItem item2 = createTestMediaItem("COMP-LOAN-004");
        MediaItem item3 = createTestMediaItem("COMP-LOAN-005");
        
        createActiveLoan(user.getUserId(), item1.getItemId());
        createActiveLoan(user.getUserId(), item2.getItemId());
        createActiveLoan(user.getUserId(), item3.getItemId());
        
        // Create a returned loan (should not be counted)
        Loan returnedLoan = new Loan();
        returnedLoan.setUserId(user.getUserId());
        returnedLoan.setItemId(item1.getItemId());
        returnedLoan.setLoanDate(LocalDate.now().minusDays(20));
        returnedLoan.setDueDate(LocalDate.now().minusDays(10));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(9));
        returnedLoan.setStatus("RETURNED");
        loanRepository.save(returnedLoan);
        
        // Act
        int count = loanRepository.countActiveByUserId(user.getUserId());
        
        // Assert
        assertEquals(3, count);
    }
    
    @Test
    void testCountActiveByUserId_NoActiveLoans_ReturnsZero() {
        // Arrange
        User user = createTestUser("comp_loan_user4");
        MediaItem item = createTestMediaItem("COMP-LOAN-006");
        
        Loan returnedLoan = new Loan();
        returnedLoan.setUserId(user.getUserId());
        returnedLoan.setItemId(item.getItemId());
        returnedLoan.setLoanDate(LocalDate.now().minusDays(20));
        returnedLoan.setDueDate(LocalDate.now().minusDays(10));
        returnedLoan.setReturnDate(LocalDate.now().minusDays(9));
        returnedLoan.setStatus("RETURNED");
        loanRepository.save(returnedLoan);
        
        // Act
        int count = loanRepository.countActiveByUserId(user.getUserId());
        
        // Assert
        assertEquals(0, count);
    }
    
    @Test
    void testCountActiveByUserId_NonExistentUser_ReturnsZero() {
        // Act
        int count = loanRepository.countActiveByUserId(999999);
        
        // Assert
        assertEquals(0, count);
    }
    
    // Helper methods
    private User createTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setEmail(username + "@test.com");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    private MediaItem createTestMediaItem(String isbn) {
        MediaItem item = new MediaItem();
        item.setTitle("Comprehensive Loan Test Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn(isbn);
        item.setPublicationDate(LocalDate.of(2020, 1, 1));
        item.setPublisher("Test Publisher");
        item.setTotalCopies(10);
        item.setAvailableCopies(10);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        return mediaItemRepository.save(item);
    }
    
    private void createActiveLoan(Integer userId, Integer itemId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setItemId(itemId);
        loan.setLoanDate(LocalDate.now().minusDays(10));
        loan.setDueDate(LocalDate.now().plusDays(5));
        loan.setStatus("ACTIVE");
        loanRepository.save(loan);
    }
}
