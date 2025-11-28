package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Fine;
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

class ComprehensiveFineRepositoryTest {
    
    private JdbcFineRepository fineRepository;
    private JdbcLoanRepository loanRepository;
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        fineRepository = new JdbcFineRepository();
        loanRepository = new JdbcLoanRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        
        // Clean all tables
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM fine")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM loan")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item")) {
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'comprehensive_%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindByStatus_PAID_ReturnsOnlyPaidFines() throws SQLException {
        // Arrange
        User user = createTestUser("comprehensive_user1");
        MediaItem item = createTestMediaItem("COMP-ISBN-001");
        Loan loan1 = createTestLoan(user.getUserId(), item.getItemId());
        Loan loan2 = createTestLoan(user.getUserId(), item.getItemId());
        
        Fine unpaidFine = new Fine();
        unpaidFine.setLoanId(loan1.getLoanId());
        unpaidFine.setAmount(new BigDecimal("10.00"));
        unpaidFine.setIssuedDate(LocalDate.now());
        unpaidFine.setStatus("UNPAID");
        fineRepository.save(unpaidFine);
        
        Fine paidFine = new Fine();
        paidFine.setLoanId(loan2.getLoanId());
        paidFine.setAmount(new BigDecimal("5.00"));
        paidFine.setIssuedDate(LocalDate.now().minusDays(10));
        paidFine.setStatus("PAID");
        paidFine.setPaidDate(LocalDate.now().minusDays(2));
        fineRepository.save(paidFine);
        
        // Act
        List<Fine> paidFines = fineRepository.findByStatus("PAID");
        
        // Assert
        assertEquals(1, paidFines.size());
        assertEquals("PAID", paidFines.get(0).getStatus());
        assertNotNull(paidFines.get(0).getPaidDate());
    }
    
    @Test
    void testFindByStatus_NonExistentStatus_ReturnsEmpty() {
        // Act
        List<Fine> fines = fineRepository.findByStatus("INVALID_STATUS");
        
        // Assert
        assertTrue(fines.isEmpty());
    }
    
    @Test
    void testCalculateTotalUnpaidByUserId_MultipleUnpaidFines_ReturnsSumCorrectly() throws SQLException {
        // Arrange
        User user = createTestUser("comprehensive_user2");
        MediaItem item = createTestMediaItem("COMP-ISBN-002");
        
        Loan loan1 = createTestLoan(user.getUserId(), item.getItemId());
        Loan loan2 = createTestLoan(user.getUserId(), item.getItemId());
        Loan loan3 = createTestLoan(user.getUserId(), item.getItemId());
        
        Fine fine1 = new Fine();
        fine1.setLoanId(loan1.getLoanId());
        fine1.setAmount(new BigDecimal("10.50"));
        fine1.setIssuedDate(LocalDate.now());
        fine1.setStatus("UNPAID");
        fineRepository.save(fine1);
        
        Fine fine2 = new Fine();
        fine2.setLoanId(loan2.getLoanId());
        fine2.setAmount(new BigDecimal("5.25"));
        fine2.setIssuedDate(LocalDate.now());
        fine2.setStatus("UNPAID");
        fineRepository.save(fine2);
        
        // Paid fine should not be included
        Fine fine3 = new Fine();
        fine3.setLoanId(loan3.getLoanId());
        fine3.setAmount(new BigDecimal("100.00"));
        fine3.setIssuedDate(LocalDate.now());
        fine3.setStatus("PAID");
        fine3.setPaidDate(LocalDate.now());
        fineRepository.save(fine3);
        
        // Act
        BigDecimal total = fineRepository.calculateTotalUnpaidByUserId(user.getUserId());
        
        // Assert
        assertEquals(new BigDecimal("15.75"), total);
    }
    
    @Test
    void testCalculateTotalUnpaidByUserId_NoUnpaidFines_ReturnsZero() throws SQLException {
        // Arrange
        User user = createTestUser("comprehensive_user3");
        MediaItem item = createTestMediaItem("COMP-ISBN-003");
        Loan loan = createTestLoan(user.getUserId(), item.getItemId());
        
        Fine paidFine = new Fine();
        paidFine.setLoanId(loan.getLoanId());
        paidFine.setAmount(new BigDecimal("50.00"));
        paidFine.setIssuedDate(LocalDate.now());
        paidFine.setStatus("PAID");
        paidFine.setPaidDate(LocalDate.now());
        fineRepository.save(paidFine);
        
        // Act
        BigDecimal total = fineRepository.calculateTotalUnpaidByUserId(user.getUserId());
        
        // Assert
        assertEquals(BigDecimal.ZERO, total);
    }
    
    @Test
    void testExistsByLoanId_LoanWithoutFine_ReturnsFalse() throws SQLException {
        // Arrange
        User user = createTestUser("comprehensive_user4");
        MediaItem item = createTestMediaItem("COMP-ISBN-004");
        Loan loan = createTestLoan(user.getUserId(), item.getItemId());
        
        // Act
        boolean exists = fineRepository.existsByLoanId(loan.getLoanId());
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testFindByStatus_EmptyResult_HandlesGracefully() {
        // Act
        List<Fine> fines = fineRepository.findByStatus("CANCELLED");
        
        // Assert
        assertNotNull(fines);
        assertTrue(fines.isEmpty());
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
        item.setTitle("Comprehensive Test Book");
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
    
    private Loan createTestLoan(Integer userId, Integer itemId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setItemId(itemId);
        loan.setLoanDate(LocalDate.now().minusDays(15));
        loan.setDueDate(LocalDate.now().minusDays(5));
        loan.setStatus("ACTIVE");
        return loanRepository.save(loan);
    }
}
