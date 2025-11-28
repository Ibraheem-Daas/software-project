package com.example.library.repository;

import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
import com.example.library.testcontainers.TestDatabaseContainer;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JdbcLoanRepository using Testcontainers.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcLoanRepositoryIntegrationTest {
    
    private static JdbcLoanRepository loanRepository;
    private static JdbcUserRepository userRepository;
    private static JdbcMediaItemRepository mediaItemRepository;
    
    @BeforeAll
    static void setupTestContainer() {
        TestDatabaseContainer.start();
        loanRepository = new JdbcLoanRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
    }
    
    @BeforeEach
    void cleanDatabase() {
        TestDatabaseContainer.cleanDatabase();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should save a new loan successfully")
    void testSave_NewLoan_Success() {
        // Arrange
        User user = createAndSaveUser("borrower1", "borrower1@example.com");
        MediaItem item = createAndSaveMediaItem("Book to Borrow", "Author");
        
        Loan loan = createLoan(user.getUserId(), item.getItemId());
        
        // Act
        loanRepository.save(loan);
        
        // Assert
        assertNotNull(loan.getLoanId(), "Loan ID should be generated");
        assertTrue(loan.getLoanId() > 0, "Loan ID should be positive");
    }
    
    @Test
    @Order(2)
    @DisplayName("Should find loan by ID")
    void testFindById_ExistingLoan_ReturnsLoan() {
        // Arrange
        User user = createAndSaveUser("user1", "user1@example.com");
        MediaItem item = createAndSaveMediaItem("Item 1", "Author 1");
        Loan loan = createAndSaveLoan(user.getUserId(), item.getItemId());
        
        // Act
        Optional<Loan> found = loanRepository.findById(loan.getLoanId());
        
        // Assert
        assertTrue(found.isPresent(), "Loan should be found");
        assertEquals(loan.getUserId(), found.get().getUserId());
        assertEquals(loan.getItemId(), found.get().getItemId());
        assertEquals("ACTIVE", found.get().getStatus());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should return empty when loan ID does not exist")
    void testFindById_NonExistentLoan_ReturnsEmpty() {
        // Act
        Optional<Loan> found = loanRepository.findById(9999);
        
        // Assert
        assertFalse(found.isPresent(), "Should return empty for non-existent loan");
    }
    
    @Test
    @Order(4)
    @DisplayName("Should find all loans")
    void testFindAll_MultipleLoans_ReturnsAllLoans() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item1 = createAndSaveMediaItem("Item 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Item 2", "Author 2");
        
        createAndSaveLoan(user1.getUserId(), item1.getItemId());
        createAndSaveLoan(user2.getUserId(), item2.getItemId());
        
        // Act
        List<Loan> loans = loanRepository.findAll();
        
        // Assert
        assertEquals(2, loans.size(), "Should return all 2 loans");
    }
    
    @Test
    @Order(5)
    @DisplayName("Should find loans by user ID")
    void testFindByUserId_ReturnsUserLoans() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item1 = createAndSaveMediaItem("Item 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Item 2", "Author 2");
        MediaItem item3 = createAndSaveMediaItem("Item 3", "Author 3");
        
        createAndSaveLoan(user1.getUserId(), item1.getItemId());
        createAndSaveLoan(user1.getUserId(), item2.getItemId());
        createAndSaveLoan(user2.getUserId(), item3.getItemId());
        
        // Act
        List<Loan> user1Loans = loanRepository.findByUserId(user1.getUserId());
        List<Loan> user2Loans = loanRepository.findByUserId(user2.getUserId());
        
        // Assert
        assertEquals(2, user1Loans.size(), "User 1 should have 2 loans");
        assertEquals(1, user2Loans.size(), "User 2 should have 1 loan");
    }
    
    @Test
    @Order(6)
    @DisplayName("Should find loans by item ID")
    void testFindByItemId_ReturnsItemLoans() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item = createAndSaveMediaItem("Popular Book", "Author");
        
        Loan loan1 = createAndSaveLoan(user1.getUserId(), item.getItemId());
        loan1.setReturnDate(LocalDate.now().minusDays(5));
        loan1.setStatus("RETURNED");
        loanRepository.update(loan1);
        
        createAndSaveLoan(user2.getUserId(), item.getItemId());
        
        // Act
        List<Loan> itemLoans = loanRepository.findByItemId(item.getItemId());
        
        // Assert
        assertEquals(2, itemLoans.size(), "Item should have 2 loan records");
    }
    
    @Test
    @Order(7)
    @DisplayName("Should find loans by status")
    void testFindByStatus_ReturnsLoansWithStatus() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item1 = createAndSaveMediaItem("Item 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Item 2", "Author 2");
        
        // Create active loan (not explicitly used but needed for count assertion)
        createAndSaveLoan(user1.getUserId(), item1.getItemId());
        
        Loan returnedLoan = createAndSaveLoan(user2.getUserId(), item2.getItemId());
        returnedLoan.setReturnDate(LocalDate.now());
        returnedLoan.setStatus("RETURNED");
        loanRepository.update(returnedLoan);
        
        // Act
        List<Loan> activeLoans = loanRepository.findByStatus("ACTIVE");
        List<Loan> returnedLoans = loanRepository.findByStatus("RETURNED");
        
        // Assert
        assertEquals(1, activeLoans.size(), "Should have 1 active loan");
        assertEquals(1, returnedLoans.size(), "Should have 1 returned loan");
    }
    
    @Test
    @Order(8)
    @DisplayName("Should find overdue loans")
    void testFindOverdueLoans_ReturnsOverdueLoans() {
        // Arrange
        User user = createAndSaveUser("user1", "user1@example.com");
        MediaItem item1 = createAndSaveMediaItem("Item 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Item 2", "Author 2");
        
        // Create overdue loan
        Loan overdueLoan = createLoan(user.getUserId(), item1.getItemId());
        overdueLoan.setLoanDate(LocalDate.now().minusDays(20));
        overdueLoan.setDueDate(LocalDate.now().minusDays(5)); // Overdue
        loanRepository.save(overdueLoan);
        
        // Create current loan
        Loan currentLoan = createLoan(user.getUserId(), item2.getItemId());
        currentLoan.setDueDate(LocalDate.now().plusDays(5)); // Not overdue
        loanRepository.save(currentLoan);
        
        // Act
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        
        // Assert
        assertEquals(1, overdueLoans.size(), "Should have 1 overdue loan");
        assertEquals(overdueLoan.getLoanId(), overdueLoans.get(0).getLoanId());
    }
    
    @Test
    @Order(9)
    @DisplayName("Should update existing loan")
    void testUpdate_ExistingLoan_UpdatesSuccessfully() {
        // Arrange
        User user = createAndSaveUser("user1", "user1@example.com");
        MediaItem item = createAndSaveMediaItem("Item 1", "Author 1");
        Loan loan = createAndSaveLoan(user.getUserId(), item.getItemId());
        Integer loanId = loan.getLoanId();
        
        // Act
        loan.setReturnDate(LocalDate.now());
        loan.setStatus("RETURNED");
        loanRepository.update(loan);
        
        // Assert
        Optional<Loan> updated = loanRepository.findById(loanId);
        assertTrue(updated.isPresent(), "Updated loan should exist");
        assertEquals("RETURNED", updated.get().getStatus());
        assertNotNull(updated.get().getReturnDate());
    }
    
    @Test
    @Order(10)
    @DisplayName("Should delete loan by ID")
    void testDelete_ExistingLoan_DeletesSuccessfully() {
        // Arrange
        User user = createAndSaveUser("user1", "user1@example.com");
        MediaItem item = createAndSaveMediaItem("Item 1", "Author 1");
        Loan loan = createAndSaveLoan(user.getUserId(), item.getItemId());
        Integer loanId = loan.getLoanId();
        
        // Act
        loanRepository.deleteById(loanId);
        
        // Assert
        Optional<Loan> deleted = loanRepository.findById(loanId);
        assertFalse(deleted.isPresent(), "Deleted loan should not exist");
    }
    
    @Test
    @Order(11)
    @DisplayName("Should count active loans for user")
    void testCountActiveLoansByUserId_ReturnsCorrectCount() {
        // Arrange
        User user = createAndSaveUser("user1", "user1@example.com");
        MediaItem item1 = createAndSaveMediaItem("Item 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Item 2", "Author 2");
        MediaItem item3 = createAndSaveMediaItem("Item 3", "Author 3");
        
        createAndSaveLoan(user.getUserId(), item1.getItemId());
        createAndSaveLoan(user.getUserId(), item2.getItemId());
        
        Loan returnedLoan = createAndSaveLoan(user.getUserId(), item3.getItemId());
        returnedLoan.setReturnDate(LocalDate.now());
        returnedLoan.setStatus("RETURNED");
        loanRepository.update(returnedLoan);
        
        // Act
        int activeCount = loanRepository.countActiveByUserId(user.getUserId());
        
        // Assert
        assertEquals(2, activeCount, "User should have 2 active loans");
    }
    
    @Test
    @Order(12)
    @DisplayName("Should handle cascade delete when user is deleted")
    void testCascadeDelete_UserDeleted_LoansAlsoDeleted() {
        // Arrange
        User user = createAndSaveUser("user_to_delete", "delete@example.com");
        MediaItem item = createAndSaveMediaItem("Item", "Author");
        Loan loan = createAndSaveLoan(user.getUserId(), item.getItemId());
        Integer loanId = loan.getLoanId();
        
        // Act
        userRepository.deleteById(user.getUserId());
        
        // Assert
        Optional<Loan> deletedLoan = loanRepository.findById(loanId);
        assertFalse(deletedLoan.isPresent(), "Loan should be deleted when user is deleted");
    }
    
    // Helper methods
    private User createAndSaveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed_password");
        user.setEmail(email);
        user.setRole("MEMBER");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }
    
    private MediaItem createAndSaveMediaItem(String title, String author) {
        MediaItem item = new MediaItem();
        item.setTitle(title);
        item.setAuthor(author);
        item.setType("BOOK");
        item.setIsbn("ISBN-" + System.nanoTime());
        item.setPublicationDate(LocalDate.of(2020, 1, 1));
        item.setPublisher("Test Publisher");
        item.setTotalCopies(10);
        item.setAvailableCopies(10);
        item.setLateFeesPerDay(new BigDecimal("10.00"));
        mediaItemRepository.save(item);
        return item;
    }
    
    private Loan createLoan(Integer userId, Integer itemId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setItemId(itemId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setStatus("ACTIVE");
        return loan;
    }
    
    private Loan createAndSaveLoan(Integer userId, Integer itemId) {
        Loan loan = createLoan(userId, itemId);
        loanRepository.save(loan);
        return loan;
    }
}
