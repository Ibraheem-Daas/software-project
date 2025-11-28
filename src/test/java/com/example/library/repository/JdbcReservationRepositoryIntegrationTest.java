package com.example.library.repository;

import com.example.library.domain.MediaItem;
import com.example.library.domain.Reservation;
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
 * Integration tests for JdbcReservationRepository using Testcontainers.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcReservationRepositoryIntegrationTest {
    
    private static ReservationRepository reservationRepository;
    private static UserRepository userRepository;
    private static MediaItemRepository mediaItemRepository;
    
    @BeforeAll
    static void setupTestContainer() {
        TestDatabaseContainer.start();
        reservationRepository = new JdbcReservationRepository();
        userRepository = new JdbcUserRepository();
        mediaItemRepository = new JdbcMediaItemRepository();
    }
    
    @BeforeEach
    void setUp() {
        TestDatabaseContainer.cleanDatabase();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should save and retrieve reservation")
    void testSaveAndFindById() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Java Programming", "Author A");
        
        Reservation reservation = new Reservation();
        reservation.setUserId(user.getUserId());
        reservation.setItemId(item.getItemId());
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setExpiryDate(LocalDateTime.now().plusHours(48));
        reservation.setStatus("ACTIVE");
        
        // Act
        Reservation saved = reservationRepository.save(reservation);
        Optional<Reservation> found = reservationRepository.findById(saved.getReservationId());
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(user.getUserId(), found.get().getUserId());
        assertEquals(item.getItemId(), found.get().getItemId());
        assertEquals("ACTIVE", found.get().getStatus());
    }
    
    @Test
    @Order(2)
    @DisplayName("Should find reservations by user ID")
    void testFindByUserId() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item1 = createAndSaveMediaItem("Book 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Book 2", "Author 2");
        
        createAndSaveReservation(user1.getUserId(), item1.getItemId());
        createAndSaveReservation(user1.getUserId(), item2.getItemId());
        createAndSaveReservation(user2.getUserId(), item1.getItemId());
        
        // Act
        List<Reservation> user1Reservations = reservationRepository.findByUserId(user1.getUserId());
        List<Reservation> user2Reservations = reservationRepository.findByUserId(user2.getUserId());
        
        // Assert
        assertEquals(2, user1Reservations.size());
        assertEquals(1, user2Reservations.size());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should find active reservations by item ID in chronological order")
    void testFindActiveByItemId() throws InterruptedException {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        User user3 = createAndSaveUser("user3", "user3@example.com");
        MediaItem item = createAndSaveMediaItem("Popular Book", "Famous Author");
        
        // Create reservations with slight time differences
        Reservation res1 = createAndSaveReservation(user1.getUserId(), item.getItemId());
        Thread.sleep(10); // Small delay to ensure different timestamps
        Reservation res2 = createAndSaveReservation(user2.getUserId(), item.getItemId());
        Thread.sleep(10);
        Reservation res3 = createAndSaveReservation(user3.getUserId(), item.getItemId());
        
        // Act
        List<Reservation> queue = reservationRepository.findActiveByItemId(item.getItemId());
        
        // Assert
        assertEquals(3, queue.size());
        // Verify FIFO order - first reservation should be first in queue
        assertEquals(res1.getReservationId(), queue.get(0).getReservationId());
        assertEquals(res2.getReservationId(), queue.get(1).getReservationId());
        assertEquals(res3.getReservationId(), queue.get(2).getReservationId());
    }
    
    @Test
    @Order(4)
    @DisplayName("Should update reservation status")
    void testUpdateReservationStatus() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        Reservation reservation = createAndSaveReservation(user.getUserId(), item.getItemId());
        
        // Act
        reservation.setStatus("FULFILLED");
        reservationRepository.update(reservation);
        
        Optional<Reservation> updated = reservationRepository.findById(reservation.getReservationId());
        
        // Assert
        assertTrue(updated.isPresent());
        assertEquals("FULFILLED", updated.get().getStatus());
    }
    
    @Test
    @Order(5)
    @DisplayName("Should find expired reservations")
    void testFindExpiredReservations() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        
        Reservation expiredReservation = new Reservation();
        expiredReservation.setUserId(user.getUserId());
        expiredReservation.setItemId(item.getItemId());
        expiredReservation.setReservationDate(LocalDateTime.now().minusDays(3));
        expiredReservation.setExpiryDate(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        expiredReservation.setStatus("ACTIVE");
        reservationRepository.save(expiredReservation);
        
        Reservation activeReservation = createAndSaveReservation(user.getUserId(), item.getItemId());
        
        // Act
        List<Reservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        
        // Assert
        assertEquals(1, expired.size());
        assertEquals("ACTIVE", expired.get(0).getStatus());
    }
    
    @Test
    @Order(6)
    @DisplayName("Should count active reservations for item")
    void testCountActiveByItemId() {
        // Arrange
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        
        createAndSaveReservation(user1.getUserId(), item.getItemId());
        Reservation res2 = createAndSaveReservation(user2.getUserId(), item.getItemId());
        
        // Cancel one reservation
        res2.setStatus("CANCELLED");
        reservationRepository.update(res2);
        
        // Act
        int activeCount = reservationRepository.countActiveByItemId(item.getItemId());
        
        // Assert
        assertEquals(1, activeCount);
    }
    
    @Test
    @Order(7)
    @DisplayName("Should delete reservation by ID")
    void testDeleteById() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        Reservation reservation = createAndSaveReservation(user.getUserId(), item.getItemId());
        Integer reservationId = reservation.getReservationId();
        
        // Act
        reservationRepository.deleteById(reservationId);
        Optional<Reservation> deleted = reservationRepository.findById(reservationId);
        
        // Assert
        assertFalse(deleted.isPresent());
    }
    
    @Test
    @Order(8)
    @DisplayName("Should handle cascade delete when user is deleted")
    void testCascadeDelete_User() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        Reservation reservation = createAndSaveReservation(user.getUserId(), item.getItemId());
        Integer reservationId = reservation.getReservationId();
        
        // Act
        userRepository.deleteById(user.getUserId());
        Optional<Reservation> deleted = reservationRepository.findById(reservationId);
        
        // Assert
        assertFalse(deleted.isPresent(), "Reservation should be deleted when user is deleted");
    }
    
    @Test
    @Order(9)
    @DisplayName("Should handle cascade delete when item is deleted")
    void testCascadeDelete_Item() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item = createAndSaveMediaItem("Book", "Author");
        Reservation reservation = createAndSaveReservation(user.getUserId(), item.getItemId());
        Integer reservationId = reservation.getReservationId();
        
        // Act
        mediaItemRepository.deleteById(item.getItemId());
        Optional<Reservation> deleted = reservationRepository.findById(reservationId);
        
        // Assert
        assertFalse(deleted.isPresent(), "Reservation should be deleted when item is deleted");
    }
    
    @Test
    @Order(10)
    @DisplayName("Should find active reservations by user ID")
    void testFindActiveByUserId() {
        // Arrange
        User user = createAndSaveUser("john", "john@example.com");
        MediaItem item1 = createAndSaveMediaItem("Book 1", "Author 1");
        MediaItem item2 = createAndSaveMediaItem("Book 2", "Author 2");
        
        createAndSaveReservation(user.getUserId(), item1.getItemId());
        Reservation res2 = createAndSaveReservation(user.getUserId(), item2.getItemId());
        
        // Cancel one
        res2.setStatus("CANCELLED");
        reservationRepository.update(res2);
        
        // Act
        List<Reservation> activeReservations = reservationRepository.findActiveByUserId(user.getUserId());
        
        // Assert
        assertEquals(1, activeReservations.size());
        assertEquals("ACTIVE", activeReservations.get(0).getStatus());
    }
    
    // Helper methods
    
    private User createAndSaveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setEmail(email);
        user.setRole("MEMBER");
        return userRepository.save(user);
    }
    
    private MediaItem createAndSaveMediaItem(String title, String author) {
        MediaItem item = new MediaItem();
        item.setTitle(title);
        item.setAuthor(author);
        item.setType("BOOK");
        item.setPublicationDate(LocalDate.now().minusYears(1));
        item.setTotalCopies(2);
        item.setAvailableCopies(0); // No copies available
        item.setLateFeesPerDay(new BigDecimal("10.00"));
        return mediaItemRepository.save(item);
    }
    
    private Reservation createAndSaveReservation(Integer userId, Integer itemId) {
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setItemId(itemId);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setExpiryDate(LocalDateTime.now().plusHours(48));
        reservation.setStatus("ACTIVE");
        return reservationRepository.save(reservation);
    }
}
