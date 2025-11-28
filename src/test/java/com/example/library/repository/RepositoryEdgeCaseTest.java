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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryEdgeCaseTest {
    
    private JdbcUserRepository userRepository;
    private JdbcMediaItemRepository mediaItemRepository;
    private JdbcLoanRepository loanRepository;
    private JdbcFineRepository fineRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
        loanRepository = new JdbcLoanRepository();
        fineRepository = new JdbcFineRepository();
        
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM fine")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM loan")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item")) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM app_user WHERE username != 'admin'")) {
                stmt.executeUpdate();
            }
        }
    }
    
    // User Repository Edge Cases
    
    @Test
    void testUserFindByUsername_EmptyString() {
        Optional<User> result = userRepository.findByUsername("");
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testUserFindByEmail_EmptyString() {
        Optional<User> result = userRepository.findByEmail("");
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testUserExistsByUsername_EmptyString() {
        boolean exists = userRepository.existsByUsername("");
        assertFalse(exists);
    }
    
    @Test
    void testUserExistsByEmail_EmptyString() {
        boolean exists = userRepository.existsByEmail("");
        assertFalse(exists);
    }
    
    @Test
    void testUserFindByRole_NonExistent() {
        List<User> users = userRepository.findByRole("NONEXISTENT_ROLE");
        assertTrue(users.isEmpty());
    }
    
    // MediaItem Repository Edge Cases
    
    @Test
    void testMediaItemSave_WithNullPublicationDate() throws SQLException {
        MediaItem item = new MediaItem();
        item.setTitle("No Date Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("NULL-DATE-ISBN");
        item.setPublicationDate(null); // Null date
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        MediaItem saved = mediaItemRepository.save(item);
        
        assertNotNull(saved.getItemId());
        assertNull(saved.getPublicationDate());
    }
    
    @Test
    void testMediaItemUpdate_WithNullPublicationDate() throws SQLException {
        // First create an item
        MediaItem item = new MediaItem();
        item.setTitle("Update Date Test");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("UPDATE-NULL-ISBN");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(3);
        item.setAvailableCopies(3);
        item.setLateFeesPerDay(new BigDecimal("0.50"));
        
        MediaItem saved = mediaItemRepository.save(item);
        
        // Update with null date
        saved.setPublicationDate(null);
        MediaItem updated = mediaItemRepository.update(saved);
        
        assertNull(updated.getPublicationDate());
    }
    
    @Test
    void testMediaItemFindByType_EmptyString() {
        List<MediaItem> items = mediaItemRepository.findByType("");
        assertTrue(items.isEmpty());
    }
    
    @Test
    void testMediaItemFindByAuthor_EmptyString() {
        List<MediaItem> items = mediaItemRepository.findByAuthorContaining("");
        assertTrue(items.isEmpty());
    }
    
    @Test
    void testMediaItemSearch_EmptyKeyword() {
        List<MediaItem> items = mediaItemRepository.search("");
        // Should return all items or empty list
        assertNotNull(items);
    }
    
    @Test
    void testMediaItemSearch_NoMatches() {
        List<MediaItem> items = mediaItemRepository.search("ZZZZZ_NO_MATCH_KEYWORD_ZZZZZ");
        assertTrue(items.isEmpty());
    }
    
    @Test
    void testMediaItemFindAvailable() {
        List<MediaItem> items = mediaItemRepository.findAvailableItems();
        assertNotNull(items);
        // All returned items should have available copies > 0
        items.forEach(item -> assertTrue(item.getAvailableCopies() > 0));
    }
    
    // Loan Repository Edge Cases
    
    @Test
    void testLoanSave_WithNullReturnDate() throws SQLException {
        // Create prerequisite data
        User user = new User();
        user.setUsername("loan_test_user");
        user.setPassword("password");
        user.setEmail("loantest@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        MediaItem item = new MediaItem();
        item.setTitle("Loan Test Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("LOAN-TEST-ISBN");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        Loan loan = new Loan();
        loan.setUserId(savedUser.getUserId());
        loan.setItemId(savedItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setReturnDate(null); // Active loan
        loan.setStatus("ACTIVE");
        
        Loan saved = loanRepository.save(loan);
        
        assertNotNull(saved.getLoanId());
        assertNull(saved.getReturnDate());
    }
    
    @Test
    void testLoanUpdate_WithNullReturnDate() throws SQLException {
        // Create prerequisite data
        User user = new User();
        user.setUsername("loan_update_user");
        user.setPassword("password");
        user.setEmail("loanupdate@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        MediaItem item = new MediaItem();
        item.setTitle("Loan Update Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("LOAN-UPDATE-ISBN");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        Loan loan = new Loan();
        loan.setUserId(savedUser.getUserId());
        loan.setItemId(savedItem.getItemId());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setReturnDate(null);
        loan.setStatus("ACTIVE");
        
        Loan saved = loanRepository.save(loan);
        
        // Update without return date
        saved.setStatus("OVERDUE");
        Loan updated = loanRepository.update(saved);
        
        assertEquals("OVERDUE", updated.getStatus());
        assertNull(updated.getReturnDate());
    }
    
    @Test
    void testLoanFindByUserId_NonExistent() {
        List<Loan> loans = loanRepository.findByUserId(99999);
        assertTrue(loans.isEmpty());
    }
    
    @Test
    void testLoanFindByStatus_EmptyString() {
        List<Loan> loans = loanRepository.findByStatus("");
        assertTrue(loans.isEmpty());
    }
    
    @Test
    void testLoanFindOverdueLoans() {
        List<Loan> loans = loanRepository.findOverdueLoans(LocalDate.now());
        assertNotNull(loans);
        // All returned loans should be overdue
        loans.forEach(loan -> assertTrue(loan.getDueDate().isBefore(LocalDate.now())));
    }
    
    // Fine Repository Edge Cases
    
    @Test
    void testFineSave_WithNullPaidDate() throws SQLException {
        // Create prerequisite loan
        User user = new User();
        user.setUsername("fine_test_user");
        user.setPassword("password");
        user.setEmail("finetest@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        MediaItem item = new MediaItem();
        item.setTitle("Fine Test Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("FINE-TEST-ISBN");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        Loan loan = new Loan();
        loan.setUserId(savedUser.getUserId());
        loan.setItemId(savedItem.getItemId());
        loan.setLoanDate(LocalDate.now().minusDays(20));
        loan.setDueDate(LocalDate.now().minusDays(5));
        loan.setReturnDate(null);
        loan.setStatus("OVERDUE");
        Loan savedLoan = loanRepository.save(loan);
        
        Fine fine = new Fine();
        fine.setLoanId(savedLoan.getLoanId());
        fine.setAmount(new BigDecimal("5.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fine.setPaidDate(null); // Unpaid fine
        
        Fine saved = fineRepository.save(fine);
        
        assertNotNull(saved.getFineId());
        assertNull(saved.getPaidDate());
    }
    
    @Test
    void testFineUpdate_WithNullPaidDate() throws SQLException {
        // Create prerequisite data
        User user = new User();
        user.setUsername("fine_update_user");
        user.setPassword("password");
        user.setEmail("fineupdate@example.com");
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        MediaItem item = new MediaItem();
        item.setTitle("Fine Update Book");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("FINE-UPDATE-ISBN");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        MediaItem savedItem = mediaItemRepository.save(item);
        
        Loan loan = new Loan();
        loan.setUserId(savedUser.getUserId());
        loan.setItemId(savedItem.getItemId());
        loan.setLoanDate(LocalDate.now().minusDays(20));
        loan.setDueDate(LocalDate.now().minusDays(5));
        loan.setReturnDate(null);
        loan.setStatus("OVERDUE");
        Loan savedLoan = loanRepository.save(loan);
        
        Fine fine = new Fine();
        fine.setLoanId(savedLoan.getLoanId());
        fine.setAmount(new BigDecimal("5.00"));
        fine.setIssuedDate(LocalDate.now());
        fine.setStatus("UNPAID");
        fine.setPaidDate(null);
        
        Fine saved = fineRepository.save(fine);
        
        // Update without paid date
        saved.setAmount(new BigDecimal("7.50"));
        Fine updated = fineRepository.update(saved);
        
        assertEquals(0, new BigDecimal("7.50").compareTo(updated.getAmount()));
        assertNull(updated.getPaidDate());
    }
    
    @Test
    void testFineFindByLoanId_NonExistent() {
        Optional<Fine> fine = fineRepository.findByLoanId(99999);
        assertFalse(fine.isPresent());
    }
    
    @Test
    void testFineFindByStatus_EmptyString() {
        List<Fine> fines = fineRepository.findByStatus("");
        assertTrue(fines.isEmpty());
    }
    
    @Test
    void testFineFindByStatus_Unpaid() {
        List<Fine> fines = fineRepository.findByStatus("UNPAID");
        assertNotNull(fines);
        // All returned fines should be unpaid
        fines.forEach(fine -> assertEquals("UNPAID", fine.getStatus()));
    }
}
