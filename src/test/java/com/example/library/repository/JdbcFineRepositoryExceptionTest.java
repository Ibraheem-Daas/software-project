package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Fine;
import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
import org.junit.jupiter.api.AfterEach;
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

class JdbcFineRepositoryExceptionTest {
    
    private JdbcFineRepository fineRepository;
    private JdbcLoanRepository loanRepository;
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    private User testUser;
    private MediaItem testItem;
    private Loan testLoan;
    
    @BeforeEach
    void setUp() throws SQLException {
        fineRepository = new JdbcFineRepository();
        loanRepository = new JdbcLoanRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM fine WHERE loan_id IN (SELECT loan_id FROM loan WHERE user_id IN (SELECT user_id FROM app_user WHERE username = 'exception_fine_test_user'))")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM loan WHERE user_id IN (SELECT user_id FROM app_user WHERE username = 'exception_fine_test_user')")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn = 'EXCEPTION-FINE-TEST'")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE username = 'exception_fine_test_user'")) {
                stmt.executeUpdate();
            }
        }
        
        // Create test user
        testUser = new User();
        testUser.setUsername("exception_fine_test_user");
        testUser.setPassword("password");
        testUser.setEmail("fineexception@example.com");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
        
        // Create test media item
        testItem = new MediaItem();
        testItem.setTitle("Exception Fine Test Book");
        testItem.setAuthor("Test Author");
        testItem.setType("BOOK");
        testItem.setIsbn("EXCEPTION-FINE-TEST");
        testItem.setPublicationDate(LocalDate.now());
        testItem.setPublisher("Test Publisher");
        testItem.setTotalCopies(5);
        testItem.setAvailableCopies(5);
        testItem.setLateFeesPerDay(new BigDecimal("1.00"));
        testItem = mediaItemRepository.save(testItem);
        
        // Create test loan
        testLoan = new Loan();
        testLoan.setUserId(testUser.getUserId());
        testLoan.setItemId(testItem.getItemId());
        testLoan.setLoanDate(LocalDate.now().minusDays(20));
        testLoan.setDueDate(LocalDate.now().minusDays(5));
        testLoan.setStatus("OVERDUE");
        testLoan = loanRepository.save(testLoan);
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM fine WHERE loan_id = ?")) {
                stmt.setInt(1, testLoan.getLoanId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM loan WHERE loan_id = ?")) {
                stmt.setInt(1, testLoan.getLoanId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item WHERE item_id = ?")) {
                stmt.setInt(1, testItem.getItemId());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE user_id = ?")) {
                stmt.setInt(1, testUser.getUserId());
                stmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testUpdate_NonExistentFine_ThrowsException() {
        Fine fine = new Fine();
        fine.setFineId(99999); // Non-existent ID
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(new BigDecimal("50.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        
        assertThrows(DataAccessException.class, () -> {
            fineRepository.update(fine);
        });
    }
    
    @Test
    void testSave_WithInvalidLoanId_ThrowsException() {
        Fine fine = new Fine();
        fine.setLoanId(99999); // Non-existent loan ID
        fine.setAmount(new BigDecimal("50.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        
        assertThrows(DataAccessException.class, () -> {
            fineRepository.save(fine);
        });
    }
    
    @Test
    void testSave_WithNullAmount_ThrowsException() {
        Fine fine = new Fine();
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(null); // Null amount
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        
        assertThrows(DataAccessException.class, () -> {
            fineRepository.save(fine);
        });
    }
    
    @Test
    void testSave_WithPaidDate_Success() throws SQLException {
        Fine fine = new Fine();
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(new BigDecimal("25.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("PAID");
        fine.setPaidDate(LocalDate.now()); // With paid date
        
        Fine saved = fineRepository.save(fine);
        
        assertNotNull(saved.getFineId());
        assertNotNull(saved.getPaidDate());
        assertEquals("PAID", saved.getStatus());
    }
    
    @Test
    void testUpdate_ChangePaidDate_Success() throws SQLException {
        Fine fine = new Fine();
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(new BigDecimal("30.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fine.setPaidDate(null);
        
        Fine saved = fineRepository.save(fine);
        
        saved.setPaidDate(LocalDate.now());
        saved.setStatus("PAID");
        
        Fine updated = fineRepository.update(saved);
        
        assertNotNull(updated.getPaidDate());
        assertEquals("PAID", updated.getStatus());
    }
    
    @Test
    void testMarkAsPaid_NonExistentFine_ThrowsException() {
        assertThrows(DataAccessException.class, () -> {
            fineRepository.markAsPaid(99999, LocalDate.now());
        });
    }
    
    @Test
    void testMarkAsPaid_Success() throws SQLException {
        Fine fine = new Fine();
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(new BigDecimal("40.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        
        Fine saved = fineRepository.save(fine);
        
        assertDoesNotThrow(() -> {
            fineRepository.markAsPaid(saved.getFineId(), LocalDate.now());
        });
    }
    
    @Test
    void testDeleteById_NonExistentFine_ReturnsFalse() {
        boolean result = fineRepository.deleteById(99999);
        assertFalse(result);
    }
    
    @Test
    void testDeleteById_ValidId_ReturnsTrue() throws SQLException {
        Fine fine = new Fine();
        fine.setLoanId(testLoan.getLoanId());
        fine.setAmount(new BigDecimal("35.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        
        Fine saved = fineRepository.save(fine);
        
        boolean result = fineRepository.deleteById(saved.getFineId());
        assertTrue(result);
    }
    
    @Test
    void testFindById_InvalidId_ReturnsEmpty() {
        var result = fineRepository.findById(-1);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByLoanId_NonExistentLoan_ReturnsEmpty() {
        var result = fineRepository.findByLoanId(99999);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByStatus_NullStatus_ReturnsEmpty() {
        List<Fine> result = fineRepository.findByStatus(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByUserId_NonExistentUser_ReturnsEmpty() {
        List<Fine> result = fineRepository.findByUserId(99999);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindAll_ReturnsNonNull() {
        List<Fine> result = fineRepository.findAll();
        assertNotNull(result);
    }
}
