package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests focusing on "happy path" scenarios with actual data to cover TRUE branches,
 * multiple-row loops, and UPDATE/DELETE operations in repository methods.
 */
class HappyPathRepositoryTest {

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
        
        cleanTestData();
    }

    @AfterEach
    void tearDown() throws SQLException {
        cleanTestData();
    }

    private void cleanTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.prepareStatement("DELETE FROM reservation WHERE user_id IN (SELECT user_id FROM app_user WHERE username LIKE 'happy_%')").executeUpdate();
            conn.prepareStatement("DELETE FROM fine WHERE loan_id IN (SELECT loan_id FROM loan WHERE user_id IN (SELECT user_id FROM app_user WHERE username LIKE 'happy_%'))").executeUpdate();
            conn.prepareStatement("DELETE FROM loan WHERE user_id IN (SELECT user_id FROM app_user WHERE username LIKE 'happy_%')").executeUpdate();
            conn.prepareStatement("DELETE FROM media_item WHERE title LIKE 'Happy Test%'").executeUpdate();
            conn.prepareStatement("DELETE FROM app_user WHERE username LIKE 'happy_%'").executeUpdate();
        }
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setEmail(email);
        user.setRole("STUDENT");
        return userRepository.save(user);
    }

    private MediaItem createBook(String title, String type) {
        MediaItem item = new MediaItem();
        item.setTitle(title);
        item.setAuthor("Test Author");
        item.setIsbn("ISBN-" + System.currentTimeMillis());
        item.setPublisher("Test Publisher");
        item.setPublicationDate(LocalDate.of(2024, 1, 1));
        item.setType(type);
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        return mediaItemRepository.save(item);
    }

    private Loan createLoan(Integer userId, Integer itemId, LocalDate dueDate) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setItemId(itemId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(dueDate);
        loan.setStatus("ACTIVE");
        return loanRepository.save(loan);
    }

    private Fine createFine(Integer loanId, BigDecimal amount, String status) {
        Fine fine = new Fine();
        fine.setLoanId(loanId);
        fine.setAmount(amount);
        fine.setStatus(status);
        fine.setIssuedDate(LocalDate.now());
        return fineRepository.save(fine);
    }

    private Reservation createReservation(Integer userId, Integer itemId) {
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setItemId(itemId);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setExpiryDate(LocalDateTime.now().plusDays(7));
        reservation.setStatus("ACTIVE");
        return reservationRepository.save(reservation);
    }

    // ============================================================================
    // JdbcUserRepository - Happy Path Tests (2 tests)
    // ============================================================================

    @Test
    @DisplayName("existsByUsername returns TRUE when user exists")
    void existsByUsername_returnsTrue_whenUserExists() {
        createUser("happy_user1", "happy_user1@test.com");
        boolean exists = userRepository.existsByUsername("happy_user1");
        assertTrue(exists);
    }

    @Test
    @DisplayName("findAll returns multiple users with while loop iterations")
    void findAll_returnsMultipleUsers_withMultipleLoopIterations() {
        createUser("happy_u1", "happy_u1@test.com");
        createUser("happy_u2", "happy_u2@test.com");
        createUser("happy_u3", "happy_u3@test.com");
        
        List<User> users = userRepository.findAll();
        assertTrue(users.size() >= 3);
        assertTrue(users.stream().anyMatch(u -> u.getUsername().startsWith("happy_u")));
    }

    // ============================================================================
    // JdbcMediaItemRepository - Happy Path Tests (2 tests)
    // ============================================================================

    @Test
    @DisplayName("findByType returns items when type exists with multiple rows")
    void findByType_returnsItems_whenTypeExists() {
        createBook("Happy Test Book 1", "BOOK");
        createBook("Happy Test Book 2", "BOOK");
        
        List<MediaItem> books = mediaItemRepository.findByType("BOOK");
        assertTrue(books.size() >= 2);
        assertTrue(books.stream().anyMatch(b -> b.getTitle().startsWith("Happy Test")));
    }

    @Test
    @DisplayName("findAvailableItems returns items when copies available")
    void findAvailableItems_returnsItems_whenCopiesAvailable() {
        MediaItem item = createBook("Happy Test Available Book", "BOOK");
        List<MediaItem> found = mediaItemRepository.findAvailableItems();
        assertFalse(found.isEmpty());
        assertTrue(found.stream().anyMatch(i -> i.getItemId().equals(item.getItemId())));
    }

    // ============================================================================
    // JdbcLoanRepository - Happy Path Tests (2 tests)
    // ============================================================================

    @Test
    @DisplayName("findLoansDueSoon returns loans when due in next days")
    void findLoansDueSoon_returnsLoans_whenDueInNextDays() {
        User user = createUser("happy_loan_user", "happy_loan@test.com");
        MediaItem item = createBook("Happy Test Loan Book", "BOOK");
        LocalDate dueDate = LocalDate.now().plusDays(3);
        Loan loan = createLoan(user.getUserId(), item.getItemId(), dueDate);
        
        List<Loan> loans = loanRepository.findLoansDueSoon(LocalDate.now(), 7);
        
        assertFalse(loans.isEmpty());
        assertTrue(loans.stream().anyMatch(l -> l.getLoanId().equals(loan.getLoanId())));
    }

    @Test
    @DisplayName("findByUserId returns all user loans with multiple rows")
    void findByUserId_returnsAllUserLoans_withMultipleRows() {
        User user = createUser("happy_multi_loan_user", "happy_multi_loan@test.com");
        MediaItem item1 = createBook("Happy Test Loan1", "BOOK");
        MediaItem item2 = createBook("Happy Test Loan2", "BOOK");
        
        Loan loan1 = createLoan(user.getUserId(), item1.getItemId(), LocalDate.now().plusDays(7));
        Loan loan2 = createLoan(user.getUserId(), item2.getItemId(), LocalDate.now().plusDays(14));
        
        List<Loan> loans = loanRepository.findByUserId(user.getUserId());
        assertTrue(loans.size() >= 2);
        assertTrue(loans.stream().anyMatch(l -> l.getLoanId().equals(loan1.getLoanId())));
        assertTrue(loans.stream().anyMatch(l -> l.getLoanId().equals(loan2.getLoanId())));
    }

    // ============================================================================
    // JdbcFineRepository - Happy Path Tests (2 tests)
    // ============================================================================

    @Test
    @DisplayName("findByStatus returns fines when status exists with multiple rows")
    void findByStatus_returnsFines_whenStatusExists() {
        User user = createUser("happy_fine_user", "happy_fine@test.com");
        MediaItem item = createBook("Happy Test Fine Book", "BOOK");
        Loan loan1 = createLoan(user.getUserId(), item.getItemId(), LocalDate.now().plusDays(7));
        
        Fine fine1 = createFine(loan1.getLoanId(), new BigDecimal("5.00"), "UNPAID");
        Fine fine2 = createFine(loan1.getLoanId(), new BigDecimal("3.50"), "UNPAID");
        
        List<Fine> fines = fineRepository.findByStatus("UNPAID");
        assertTrue(fines.size() >= 2);
        assertTrue(fines.stream().anyMatch(f -> f.getFineId().equals(fine1.getFineId())));
        assertTrue(fines.stream().anyMatch(f -> f.getFineId().equals(fine2.getFineId())));
    }

    @Test
    @DisplayName("calculateTotalUnpaidByUserId returns positive amount when user has unpaid fines")
    void calculateTotalUnpaidByUserId_returnsPositiveAmount_whenUserHasUnpaidFines() {
        User user = createUser("happy_sum_user", "happy_sum@test.com");
        MediaItem item = createBook("Happy Test Sum Book", "BOOK");
        Loan loan = createLoan(user.getUserId(), item.getItemId(), LocalDate.now().plusDays(7));
        
        createFine(loan.getLoanId(), new BigDecimal("5.00"), "UNPAID");
        createFine(loan.getLoanId(), new BigDecimal("3.50"), "UNPAID");
        
        BigDecimal total = fineRepository.calculateTotalUnpaidByUserId(user.getUserId());
        assertEquals(new BigDecimal("8.50"), total);
    }

    // ============================================================================
    // JdbcReservationRepository - Happy Path Tests (2 tests)
    // ============================================================================

    @Test
    @DisplayName("findByItemId returns reservations when item has queue with multiple rows")
    void findByItemId_returnsReservations_whenItemHasQueue() {
        User u1 = createUser("happy_r1", "happy_r1@test.com");
        User u2 = createUser("happy_r2", "happy_r2@test.com");
        MediaItem item = createBook("Happy Test Reserved Book", "BOOK");
        
        Reservation res1 = createReservation(u1.getUserId(), item.getItemId());
        Reservation res2 = createReservation(u2.getUserId(), item.getItemId());
        
        List<Reservation> reservations = reservationRepository.findByItemId(item.getItemId());
        assertTrue(reservations.size() >= 2);
        assertTrue(reservations.stream().anyMatch(r -> r.getReservationId().equals(res1.getReservationId())));
        assertTrue(reservations.stream().anyMatch(r -> r.getReservationId().equals(res2.getReservationId())));
    }

    @Test
    @DisplayName("findActiveByItemId returns active reservations with multiple rows")
    void findActiveByItemId_returnsActiveReservations_withMultipleRows() {
        User u1 = createUser("happy_active1", "happy_active1@test.com");
        User u2 = createUser("happy_active2", "happy_active2@test.com");
        MediaItem item = createBook("Happy Test Active Reserved", "BOOK");
        
        createReservation(u1.getUserId(), item.getItemId());
        createReservation(u2.getUserId(), item.getItemId());
        
        List<Reservation> activeReservations = reservationRepository.findActiveByItemId(item.getItemId());
        assertTrue(activeReservations.size() >= 2);
        assertTrue(activeReservations.stream().allMatch(r -> "ACTIVE".equals(r.getStatus())));
    }
}
