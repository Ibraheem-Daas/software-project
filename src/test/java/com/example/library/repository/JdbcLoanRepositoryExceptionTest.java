package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
import com.example.library.repository.DataAccessException;
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

class JdbcLoanRepositoryExceptionTest {
    
    private JdbcLoanRepository loanRepository;
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    private User testUser;
    private MediaItem testItem;
    
    @BeforeEach
    void setUp() throws SQLException {
        loanRepository = new JdbcLoanRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM loan WHERE user_id IN (SELECT user_id FROM app_user WHERE username = 'exception_loan_test_user')")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn = 'EXCEPTION-LOAN-TEST'")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE username = 'exception_loan_test_user'")) {
                stmt.executeUpdate();
            }
        }
        
        // Create test user
        testUser = new User();
        testUser.setUsername("exception_loan_test_user");
        testUser.setPassword("password");
        testUser.setEmail("loanexception@example.com");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
        
        // Create test media item
        testItem = new MediaItem();
        testItem.setTitle("Exception Loan Test Book");
        testItem.setAuthor("Test Author");
        testItem.setType("BOOK");
        testItem.setIsbn("EXCEPTION-LOAN-TEST");
        testItem.setPublicationDate(LocalDate.now());
        testItem.setPublisher("Test Publisher");
        testItem.setTotalCopies(5);
        testItem.setAvailableCopies(5);
        testItem.setLateFeesPerDay(new BigDecimal("1.00"));
        testItem = mediaItemRepository.save(testItem);
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM loan WHERE user_id = ?")) {
                stmt.setInt(1, testUser.getUserId());
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
    void testUpdate_NonExistentLoan_ThrowsException() {
        Loan loan = new Loan();
        loan.setLoanId(99999); // Non-existent ID
        loan.setUserId(testUser.getUserId());
        loan.setItemId(testItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        
        assertThrows(DataAccessException.class, () -> {
            loanRepository.update(loan);
        });
    }
    
    @Test
    void testSave_WithInvalidUserId_ThrowsException() {
        Loan loan = new Loan();
        loan.setUserId(99999); // Non-existent user ID
        loan.setItemId(testItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        
        assertThrows(DataAccessException.class, () -> {
            loanRepository.save(loan);
        });
    }
    
    @Test
    void testSave_WithInvalidItemId_ThrowsException() {
        Loan loan = new Loan();
        loan.setUserId(testUser.getUserId());
        loan.setItemId(99999); // Non-existent item ID
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        
        assertThrows(DataAccessException.class, () -> {
            loanRepository.save(loan);
        });
    }
    
    @Test
    void testUpdateStatus_NonExistentLoan_ThrowsException() {
        assertThrows(DataAccessException.class, () -> {
            loanRepository.updateStatus(99999, "RETURNED", LocalDate.now());
        });
    }
    
    @Test
    void testUpdateStatus_Success() throws SQLException {
        Loan loan = new Loan();
        loan.setUserId(testUser.getUserId());
        loan.setItemId(testItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        
        Loan saved = loanRepository.save(loan);
        
        assertDoesNotThrow(() -> {
            loanRepository.updateStatus(saved.getLoanId(), "RETURNED", LocalDate.now());
        });
    }
    
    @Test
    void testDeleteById_NonExistentLoan_ReturnsFalse() {
        boolean result = loanRepository.deleteById(99999);
        assertFalse(result);
    }
    
    @Test
    void testDeleteById_ValidId_ReturnsTrue() throws SQLException {
        Loan loan = new Loan();
        loan.setUserId(testUser.getUserId());
        loan.setItemId(testItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        
        Loan saved = loanRepository.save(loan);
        
        boolean result = loanRepository.deleteById(saved.getLoanId());
        assertTrue(result);
    }
    
    @Test
    void testFindById_InvalidId_ReturnsEmpty() {
        var result = loanRepository.findById(-1);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByUserId_NonExistentUser_ReturnsEmpty() {
        List<Loan> result = loanRepository.findByUserId(99999);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByItemId_NonExistentItem_ReturnsEmpty() {
        List<Loan> result = loanRepository.findByItemId(99999);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByStatus_NullStatus_ReturnsEmpty() {
        List<Loan> result = loanRepository.findByStatus(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindOverdueLoans_WithFutureDate_ReturnsEmpty() {
        List<Loan> result = loanRepository.findOverdueLoans(LocalDate.now().plusYears(10));
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindLoansDueSoon_WithPastDate_ReturnsEmpty() {
        List<Loan> result = loanRepository.findLoansDueSoon(LocalDate.now().minusYears(10), 7);
        assertTrue(result.isEmpty());
    }
}
