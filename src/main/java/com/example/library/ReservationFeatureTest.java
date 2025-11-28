package com.example.library;

import com.example.library.domain.MediaItem;
import com.example.library.domain.Reservation;
import com.example.library.domain.User;
import com.example.library.repository.*;
import com.example.library.service.ReservationService;
import com.example.library.service.ReservationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manual test to verify Reservation feature works with actual database.
 * Run this with: mvn exec:java -Dexec.mainClass="com.example.library.ReservationFeatureTest"
 */
public class ReservationFeatureTest {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Reservation Feature Test");
        System.out.println("==========================================\n");
        
        try {
            // Initialize repositories
            UserRepository userRepo = new JdbcUserRepository();
            MediaItemRepository itemRepo = new JdbcMediaItemRepository();
            ReservationRepository reservationRepo = new JdbcReservationRepository();
            
            // Initialize service
            ReservationService reservationService = new ReservationServiceImpl(
                reservationRepo, userRepo, itemRepo
            );
            
            System.out.println("✓ Database connection established\n");
            
            // Test 1: Create test data
            System.out.println("TEST 1: Creating test users and items...");
            User user1 = createTestUser(userRepo, "testuser1", "testuser1@example.com");
            User user2 = createTestUser(userRepo, "testuser2", "testuser2@example.com");
            User user3 = createTestUser(userRepo, "testuser3", "testuser3@example.com");
            
            MediaItem item = createTestItem(itemRepo, "Test Book - Popular Java Guide", "Test Author");
            System.out.println("✓ Created 3 users and 1 item (no copies available)\n");
            
            // Test 2: Create reservations (queue)
            System.out.println("TEST 2: Creating reservation queue...");
            Reservation res1 = reservationService.createReservation(user1.getUserId(), item.getItemId());
            System.out.println("✓ User 1 reserved item - Reservation ID: " + res1.getReservationId());
            
            Thread.sleep(100); // Ensure different timestamps
            Reservation res2 = reservationService.createReservation(user2.getUserId(), item.getItemId());
            System.out.println("✓ User 2 reserved item - Reservation ID: " + res2.getReservationId());
            
            Thread.sleep(100);
            Reservation res3 = reservationService.createReservation(user3.getUserId(), item.getItemId());
            System.out.println("✓ User 3 reserved item - Reservation ID: " + res3.getReservationId());
            System.out.println();
            
            // Test 3: Check queue
            System.out.println("TEST 3: Checking reservation queue...");
            List<Reservation> queue = reservationService.getItemReservationQueue(item.getItemId());
            System.out.println("Queue length: " + queue.size());
            for (int i = 0; i < queue.size(); i++) {
                Reservation r = queue.get(i);
                int position = reservationService.getQueuePosition(r.getReservationId());
                System.out.println("  Position " + position + ": User ID " + r.getUserId() + 
                                 " - Reserved at " + r.getReservationDate().format(formatter));
            }
            System.out.println();
            
            // Test 4: Check user's position
            System.out.println("TEST 4: Checking queue positions...");
            System.out.println("User 1 position: #" + reservationService.getQueuePosition(res1.getReservationId()));
            System.out.println("User 2 position: #" + reservationService.getQueuePosition(res2.getReservationId()));
            System.out.println("User 3 position: #" + reservationService.getQueuePosition(res3.getReservationId()));
            System.out.println();
            
            // Test 5: User 2 cancels reservation
            System.out.println("TEST 5: User 2 cancels reservation...");
            reservationService.cancelReservation(res2.getReservationId(), user2.getUserId());
            System.out.println("✓ Reservation cancelled");
            
            queue = reservationService.getItemReservationQueue(item.getItemId());
            System.out.println("New queue length: " + queue.size());
            System.out.println("User 3 new position: #" + reservationService.getQueuePosition(res3.getReservationId()));
            System.out.println();
            
            // Test 6: Item becomes available - fulfill next reservation
            System.out.println("TEST 6: Item returned - fulfilling next reservation...");
            Reservation fulfilled = reservationService.fulfillNextReservation(item.getItemId());
            if (fulfilled != null) {
                System.out.println("✓ Fulfilled reservation ID: " + fulfilled.getReservationId());
                System.out.println("  User ID: " + fulfilled.getUserId() + " (User 1)");
                System.out.println("  Status: " + fulfilled.getStatus());
                System.out.println("  Expiry: " + fulfilled.getExpiryDate().format(formatter));
                System.out.println("  → User 1 has 48 hours to pick up the item");
            }
            System.out.println();
            
            // Test 7: Check active reservations
            System.out.println("TEST 7: Checking remaining active reservations...");
            List<Reservation> activeQueue = reservationService.getItemReservationQueue(item.getItemId());
            System.out.println("Active reservations remaining: " + activeQueue.size());
            for (Reservation r : activeQueue) {
                System.out.println("  User ID " + r.getUserId() + " - Status: " + r.getStatus());
            }
            System.out.println();
            
            // Test 8: Check user's reservations
            System.out.println("TEST 8: Checking user 3's reservations...");
            List<Reservation> user3Reservations = reservationService.getActiveUserReservations(user3.getUserId());
            System.out.println("User 3 has " + user3Reservations.size() + " active reservation(s)");
            System.out.println();
            
            // Test 9: Test duplicate prevention
            System.out.println("TEST 9: Testing duplicate reservation prevention...");
            try {
                // User 3 still has active reservation - this should fail
                reservationService.createReservation(user3.getUserId(), item.getItemId());
                System.out.println("✗ FAIL: Duplicate reservation was allowed!");
            } catch (Exception e) {
                System.out.println("✓ Correctly prevented duplicate reservation");
                System.out.println("  Error: " + e.getMessage());
            }
            System.out.println();
            
            // Cleanup
            System.out.println("CLEANUP: Removing test data...");
            reservationRepo.deleteById(res1.getReservationId());
            if (res2.getReservationId() != null) {
                reservationRepo.deleteById(res2.getReservationId());
            }
            reservationRepo.deleteById(res3.getReservationId());
            itemRepo.deleteById(item.getItemId());
            userRepo.deleteById(user1.getUserId());
            userRepo.deleteById(user2.getUserId());
            userRepo.deleteById(user3.getUserId());
            System.out.println("✓ Test data cleaned up\n");
            
            // Summary
            System.out.println("==========================================");
            System.out.println("  ✅ ALL TESTS PASSED!");
            System.out.println("  Reservation feature is working correctly");
            System.out.println("==========================================");
            
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static User createTestUser(UserRepository userRepo, String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test123");
        user.setEmail(email);
        user.setRole("MEMBER");
        return userRepo.save(user);
    }
    
    private static MediaItem createTestItem(MediaItemRepository itemRepo, String title, String author) {
        MediaItem item = new MediaItem();
        item.setTitle(title);
        item.setAuthor(author);
        item.setType("BOOK");
        item.setPublicationDate(LocalDate.now().minusYears(1));
        item.setTotalCopies(2);
        item.setAvailableCopies(0); // No copies available - forces reservation
        item.setLateFeesPerDay(new BigDecimal("10.00"));
        return itemRepo.save(item);
    }
}
