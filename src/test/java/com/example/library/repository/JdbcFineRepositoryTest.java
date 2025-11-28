package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Fine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcFineRepositoryTest {
    
    private JdbcFineRepository fineRepository;
    private Integer testUserId;
    private Integer testItemId;
    private Integer testLoanId;
    
    @BeforeEach
    void setUp() throws SQLException {
        fineRepository = new JdbcFineRepository();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Clean all tables (in order of foreign key dependencies)
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM fine")) {
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM loan")) {
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item")) {
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM app_user")) {
                pstmt.executeUpdate();
            }
            
            // Insert test user
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO app_user (username, password, email, role) VALUES (?, ?, ?, ?) RETURNING user_id")) {
                pstmt.setString(1, "testuser");
                pstmt.setString(2, "password123");
                pstmt.setString(3, "testuser@example.com");
                pstmt.setString(4, "STUDENT");
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    testUserId = rs.getInt(1);
                }
            }
            
            // Insert test media item
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO media_item (title, author, type, isbn, publication_date, publisher, total_copies, available_copies, late_fees_per_day) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING item_id")) {
                pstmt.setString(1, "Test Book");
                pstmt.setString(2, "Test Author");
                pstmt.setString(3, "BOOK");
                pstmt.setString(4, "TEST-ISBN-001");
                pstmt.setDate(5, java.sql.Date.valueOf(LocalDate.of(2020, 1, 1)));
                pstmt.setString(6, "Test Publisher");
                pstmt.setInt(7, 5);
                pstmt.setInt(8, 5);
                pstmt.setBigDecimal(9, new BigDecimal("1.00"));
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    testItemId = rs.getInt(1);
                }
            }
            
            // Insert test loan (ACTIVE)
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO loan (user_id, item_id, loan_date, due_date, return_date, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING loan_id")) {
                pstmt.setInt(1, testUserId);
                pstmt.setInt(2, testItemId);
                pstmt.setDate(3, java.sql.Date.valueOf(LocalDate.now().minusDays(20)));
                pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now().minusDays(5)));
                pstmt.setNull(5, java.sql.Types.DATE);
                pstmt.setString(6, "ACTIVE");
                var rs = pstmt.executeQuery();
                if (rs.next()) {
                    testLoanId = rs.getInt(1);
                }
            }
        }
    }
    
    @Test
    void testSaveAndFindById() {
        // Arrange
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("15.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fine.setPaidDate(null);
        
        // Act - Save
        Fine saved = fineRepository.save(fine);
        
        // Assert - Generated ID
        assertNotNull(saved.getFineId(), "Fine ID should be generated");
        assertTrue(saved.getFineId() > 0, "Fine ID should be positive");
        
        // Act - Find by ID
        Optional<Fine> result = fineRepository.findById(saved.getFineId());
        
        // Assert - Found and fields match
        assertTrue(result.isPresent(), "Fine should be found by ID");
        
        Fine found = result.get();
        assertEquals(saved.getFineId(), found.getFineId(), "Fine ID should match");
        assertEquals(testLoanId, found.getLoanId(), "Loan ID should match");
        assertEquals(0, new BigDecimal("15.00").compareTo(found.getAmount()), "Amount should match");
        assertEquals(fine.getIssuedDate(), found.getIssuedDate(), "Issued date should match");
        assertEquals("UNPAID", found.getStatus(), "Status should be UNPAID");
        assertNull(found.getPaidDate(), "Paid date should be null");
    }
    
    @Test
    void testFindUnpaidFinesByUser() {
        // Arrange - Insert 2 unpaid fines
        Fine unpaidFine1 = new Fine();
        unpaidFine1.setLoanId(testLoanId);
        unpaidFine1.setAmount(new BigDecimal("10.00"));
        unpaidFine1.setIssuedDate(LocalDate.now().minusDays(5));
        unpaidFine1.setStatus("UNPAID");
        fineRepository.save(unpaidFine1);
        
        Fine unpaidFine2 = new Fine();
        unpaidFine2.setLoanId(testLoanId);
        unpaidFine2.setAmount(new BigDecimal("20.00"));
        unpaidFine2.setIssuedDate(LocalDate.now().minusDays(3));
        unpaidFine2.setStatus("UNPAID");
        fineRepository.save(unpaidFine2);
        
        // Arrange - Insert 1 paid fine
        Fine paidFine = new Fine();
        paidFine.setLoanId(testLoanId);
        paidFine.setAmount(new BigDecimal("5.00"));
        paidFine.setIssuedDate(LocalDate.now().minusDays(10));
        paidFine.setStatus("PAID");
        paidFine.setPaidDate(LocalDate.now().minusDays(2));
        fineRepository.save(paidFine);
        
        // Act
        List<Fine> unpaidFines = fineRepository.findUnpaidByUserId(testUserId);
        
        // Assert
        assertEquals(2, unpaidFines.size(), "Should find exactly 2 unpaid fines");
        
        // Verify all returned fines are UNPAID
        for (Fine fine : unpaidFines) {
            assertEquals("UNPAID", fine.getStatus(), "All fines should have UNPAID status");
            assertNull(fine.getPaidDate(), "Unpaid fines should have null paid date");
        }
    }
    
    @Test
    void testGetTotalUnpaidByUser() {
        // Arrange - Insert 2 unpaid fines: 10.00 and 15.50
        Fine unpaidFine1 = new Fine();
        unpaidFine1.setLoanId(testLoanId);
        unpaidFine1.setAmount(new BigDecimal("10.00"));
        unpaidFine1.setIssuedDate(LocalDate.now().minusDays(5));
        unpaidFine1.setStatus("UNPAID");
        fineRepository.save(unpaidFine1);
        
        Fine unpaidFine2 = new Fine();
        unpaidFine2.setLoanId(testLoanId);
        unpaidFine2.setAmount(new BigDecimal("15.50"));
        unpaidFine2.setIssuedDate(LocalDate.now().minusDays(3));
        unpaidFine2.setStatus("UNPAID");
        fineRepository.save(unpaidFine2);
        
        // Arrange - Insert 1 paid fine (should not be counted)
        Fine paidFine = new Fine();
        paidFine.setLoanId(testLoanId);
        paidFine.setAmount(new BigDecimal("100.00"));
        paidFine.setIssuedDate(LocalDate.now().minusDays(10));
        paidFine.setStatus("PAID");
        paidFine.setPaidDate(LocalDate.now().minusDays(2));
        fineRepository.save(paidFine);
        
        // Act
        BigDecimal total = fineRepository.calculateTotalUnpaidByUserId(testUserId);
        
        // Assert
        assertEquals(0, new BigDecimal("25.50").compareTo(total), "Total unpaid fines should be 25.50");
    }
    
    @Test
    void testMarkAsPaid() {
        // Arrange - Insert 1 UNPAID fine
        Fine unpaidFine = new Fine();
        unpaidFine.setLoanId(testLoanId);
        unpaidFine.setAmount(new BigDecimal("12.50"));
        unpaidFine.setIssuedDate(LocalDate.now().minusDays(7));
        unpaidFine.setStatus("UNPAID");
        unpaidFine.setPaidDate(null);
        Fine saved = fineRepository.save(unpaidFine);
        
        // Verify initial state
        Optional<Fine> beforeUpdate = fineRepository.findById(saved.getFineId());
        assertTrue(beforeUpdate.isPresent());
        assertEquals("UNPAID", beforeUpdate.get().getStatus(), "Initial status should be UNPAID");
        assertNull(beforeUpdate.get().getPaidDate(), "Initial paid date should be null");
        
        // Act - Mark as paid
        LocalDate paymentDate = LocalDate.now();
        fineRepository.markAsPaid(saved.getFineId(), paymentDate);
        
        // Assert - Retrieve and verify updated status
        Optional<Fine> afterUpdate = fineRepository.findById(saved.getFineId());
        assertTrue(afterUpdate.isPresent(), "Fine should still exist after update");
        
        Fine updated = afterUpdate.get();
        assertEquals("PAID", updated.getStatus(), "Status should be PAID");
        assertNotNull(updated.getPaidDate(), "Paid date should not be null");
        assertEquals(paymentDate, updated.getPaidDate(), "Paid date should match payment date");
    }
    
    @Test
    void testFindAll() {
        // Arrange - Create a fine
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("10.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fineRepository.save(fine);
        
        // Act
        List<Fine> allFines = fineRepository.findAll();
        
        // Assert
        assertNotNull(allFines, "Fine list should not be null");
        assertFalse(allFines.isEmpty(), "Should have at least one fine");
    }
    
    @Test
    void testDeleteById() {
        // Arrange
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("5.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        Fine saved = fineRepository.save(fine);
        
        // Act
        boolean deleted = fineRepository.deleteById(saved.getFineId());
        
        // Assert
        assertTrue(deleted, "Delete operation should return true");
        Optional<Fine> found = fineRepository.findById(saved.getFineId());
        assertFalse(found.isPresent(), "Deleted fine should not be found");
    }
    
    @Test
    void testFindByLoanId() {
        // Arrange
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("8.00"));
        fine.setIssuedDate(LocalDate.now().minusDays(3));
        fine.setStatus("UNPAID");
        fineRepository.save(fine);
        
        // Act
        Optional<Fine> found = fineRepository.findByLoanId(testLoanId);
        
        // Assert
        assertTrue(found.isPresent(), "Should find fine by loan ID");
        assertEquals(testLoanId, found.get().getLoanId(), "Loan ID should match");
    }
    
    @Test
    void testFindByLoanId_NotFound() {
        // Act
        Optional<Fine> found = fineRepository.findByLoanId(999999);
        
        // Assert
        assertFalse(found.isPresent(), "Should not find fine for non-existent loan");
    }
    
    @Test
    void testUpdate() {
        // Arrange
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("10.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        Fine saved = fineRepository.save(fine);
        
        // Act - Update the fine
        saved.setAmount(new BigDecimal("15.00"));
        saved.setStatus("PAID");
        saved.setPaidDate(LocalDate.now());
        Fine updated = fineRepository.update(saved);
        
        // Assert
        assertEquals(new BigDecimal("15.00"), updated.getAmount(), "Amount should be updated");
        assertEquals("PAID", updated.getStatus(), "Status should be PAID");
        assertNotNull(updated.getPaidDate(), "Paid date should be set");
    }
    
    @Test
    void testExistsByLoanId_ExistingLoan_ReturnsTrue() {
        // Arrange - Create a fine for the test loan
        Fine fine = new Fine();
        fine.setLoanId(testLoanId);
        fine.setAmount(new BigDecimal("5.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fineRepository.save(fine);
        
        // Act
        boolean exists = fineRepository.existsByLoanId(testLoanId);
        
        // Assert
        assertTrue(exists, "Should return true for existing loan with fine");
    }
    
    @Test
    void testExistsByLoanId_NonExistentLoan_ReturnsFalse() {
        // Act
        boolean exists = fineRepository.existsByLoanId(999999);
        
        // Assert
        assertFalse(exists, "Should return false for non-existent loan");
    }
    
    @Test
    void testFindByUserId_WithMultipleFines_ReturnsAllUserFines() throws SQLException {
        // Arrange - Create multiple fines for the test user
        Fine fine1 = new Fine();
        fine1.setLoanId(testLoanId);
        fine1.setAmount(new BigDecimal("10.00"));
        fine1.setIssuedDate(LocalDate.now().minusDays(3));
        fine1.setStatus("UNPAID");
        fineRepository.save(fine1);
        
        // Create another loan for the same user
        Integer testLoanId2;
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO loan (user_id, item_id, loan_date, due_date, return_date, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING loan_id")) {
                pstmt.setInt(1, testUserId);
                pstmt.setInt(2, testItemId);
                pstmt.setDate(3, java.sql.Date.valueOf(LocalDate.now().minusDays(15)));
                pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now().minusDays(3)));
                pstmt.setNull(5, java.sql.Types.DATE);
                pstmt.setString(6, "ACTIVE");
                var rs = pstmt.executeQuery();
                rs.next();
                testLoanId2 = rs.getInt(1);
            }
        }
        
        Fine fine2 = new Fine();
        fine2.setLoanId(testLoanId2);
        fine2.setAmount(new BigDecimal("5.00"));
        fine2.setIssuedDate(LocalDate.now().minusDays(1));
        fine2.setStatus("PAID");
        fine2.setPaidDate(LocalDate.now());
        fineRepository.save(fine2);
        
        // Act
        List<Fine> userFines = fineRepository.findByUserId(testUserId);
        
        // Assert
        assertEquals(2, userFines.size(), "Should find 2 fines for the user");
        assertTrue(userFines.stream().anyMatch(f -> f.getStatus().equals("UNPAID")), 
                   "Should contain UNPAID fine");
        assertTrue(userFines.stream().anyMatch(f -> f.getStatus().equals("PAID")), 
                   "Should contain PAID fine");
    }
}
