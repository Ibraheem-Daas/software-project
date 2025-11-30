package com.example.library.service;

import com.example.library.domain.MediaItem;
import com.example.library.domain.Reservation;
import com.example.library.domain.User;
import com.example.library.repository.MediaItemRepository;
import com.example.library.repository.ReservationRepository;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationServiceImpl using Mockito.
 */
class ReservationServiceImplTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MediaItemRepository mediaItemRepository;
    
    private ReservationService reservationService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reservationService = new ReservationServiceImpl(
                reservationRepository, 
                userRepository, 
                mediaItemRepository
        );
    }
    
    @Test
    @DisplayName("Should create reservation for unavailable item")
    void testCreateReservation_Success() {
        // Arrange
        int userId = 1;
        int itemId = 1;
        
        User user = createUser(userId, "john", "john@example.com");
        MediaItem item = createMediaItem(itemId, "Java Book", 2, 0); // No available copies
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findActiveByUserId(userId)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation res = invocation.getArgument(0);
            res.setReservationId(1);
            return res;
        });
        
        // Act
        Reservation result = reservationService.createReservation(userId, itemId);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(itemId, result.getItemId());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getReservationDate());
        assertNotNull(result.getExpiryDate());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void testCreateReservation_UserNotFound() {
        // Arrange
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.createReservation(1, 1));
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when item not found")
    void testCreateReservation_ItemNotFound() {
        // Arrange
        User user = createUser(1, "john", "john@example.com");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.createReservation(1, 1));
        
        assertTrue(exception.getMessage().contains("Item not found"));
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when item is available")
    void testCreateReservation_ItemAvailable() {
        // Arrange
        User user = createUser(1, "john", "john@example.com");
        MediaItem item = createMediaItem(1, "Java Book", 2, 1); // 1 available copy
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.createReservation(1, 1));
        
        assertTrue(exception.getMessage().contains("currently available"));
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when user already has active reservation for item")
    void testCreateReservation_DuplicateReservation() {
        // Arrange
        int userId = 1;
        int itemId = 1;
        
        User user = createUser(userId, "john", "john@example.com");
        MediaItem item = createMediaItem(itemId, "Java Book", 2, 0);
        
        Reservation existingReservation = new Reservation();
        existingReservation.setUserId(userId);
        existingReservation.setItemId(itemId);
        existingReservation.setStatus("ACTIVE");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.findActiveByUserId(userId))
            .thenReturn(Arrays.asList(existingReservation));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.createReservation(userId, itemId));
        
        assertTrue(exception.getMessage().contains("already have an active reservation"));
        verify(reservationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should cancel reservation successfully")
    void testCancelReservation_Success() {
        // Arrange
        int reservationId = 1;
        int userId = 1;
        
        Reservation reservation = new Reservation();
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setItemId(1);
        reservation.setStatus("ACTIVE");
        
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(reservation));
        when(reservationRepository.update(any(Reservation.class)))
            .thenReturn(reservation);
        
        // Act
        reservationService.cancelReservation(reservationId, userId);
        
        // Assert
        verify(reservationRepository, times(1)).update(argThat(res -> 
            "CANCELLED".equals(res.getStatus())
        ));
    }
    
    @Test
    @DisplayName("Should throw exception when canceling non-existent reservation")
    void testCancelReservation_NotFound() {
        // Arrange
        when(reservationRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.cancelReservation(1, 1));
        
        assertTrue(exception.getMessage().contains("Reservation not found"));
        verify(reservationRepository, never()).update(any());
    }
    
    @Test
    @DisplayName("Should throw exception when canceling other user's reservation")
    void testCancelReservation_WrongUser() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setReservationId(1);
        reservation.setUserId(1);
        reservation.setStatus("ACTIVE");
        
        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.cancelReservation(1, 2)); // Different user ID
        
        assertTrue(exception.getMessage().contains("your own reservations"));
        verify(reservationRepository, never()).update(any());
    }
    
    @Test
    @DisplayName("Should fulfill next reservation in queue")
    void testFulfillNextReservation_Success() {
        // Arrange
        int itemId = 1;
        
        Reservation oldestReservation = new Reservation();
        oldestReservation.setReservationId(1);
        oldestReservation.setUserId(1);
        oldestReservation.setItemId(itemId);
        oldestReservation.setStatus("ACTIVE");
        oldestReservation.setReservationDate(LocalDateTime.now().minusDays(2));
        
        when(reservationRepository.findActiveByItemId(itemId))
            .thenReturn(Arrays.asList(oldestReservation));
        when(reservationRepository.update(any(Reservation.class)))
            .thenReturn(oldestReservation);
        
        // Act
        Reservation result = reservationService.fulfillNextReservation(itemId);
        
        // Assert
        assertNotNull(result);
        verify(reservationRepository, times(1)).update(argThat(res -> 
            "FULFILLED".equals(res.getStatus())
        ));
    }
    
    @Test
    @DisplayName("Should return null when no reservations to fulfill")
    void testFulfillNextReservation_NoReservations() {
        // Arrange
        when(reservationRepository.findActiveByItemId(anyInt()))
            .thenReturn(Collections.emptyList());
        
        // Act
        Reservation result = reservationService.fulfillNextReservation(1);
        
        // Assert
        assertNull(result);
        verify(reservationRepository, never()).update(any());
    }
    
    @Test
    @DisplayName("Should get user reservations")
    void testGetUserReservations() {
        // Arrange
        int userId = 1;
        List<Reservation> reservations = Arrays.asList(
            createReservation(1, userId, 1),
            createReservation(2, userId, 2)
        );
        
        when(reservationRepository.findByUserId(userId)).thenReturn(reservations);
        
        // Act
        List<Reservation> result = reservationService.getUserReservations(userId);
        
        // Assert
        assertEquals(2, result.size());
        verify(reservationRepository, times(1)).findByUserId(userId);
    }
    
    @Test
    @DisplayName("Should get active user reservations")
    void testGetActiveUserReservations() {
        // Arrange
        int userId = 1;
        List<Reservation> activeReservations = Arrays.asList(
            createReservation(1, userId, 1)
        );
        
        when(reservationRepository.findActiveByUserId(userId))
            .thenReturn(activeReservations);
        
        // Act
        List<Reservation> result = reservationService.getActiveUserReservations(userId);
        
        // Assert
        assertEquals(1, result.size());
        verify(reservationRepository, times(1)).findActiveByUserId(userId);
    }
    
    @Test
    @DisplayName("Should get item reservation queue")
    void testGetItemReservationQueue() {
        // Arrange
        int itemId = 1;
        List<Reservation> queue = Arrays.asList(
            createReservation(1, 1, itemId),
            createReservation(2, 2, itemId),
            createReservation(3, 3, itemId)
        );
        
        when(reservationRepository.findActiveByItemId(itemId)).thenReturn(queue);
        
        // Act
        List<Reservation> result = reservationService.getItemReservationQueue(itemId);
        
        // Assert
        assertEquals(3, result.size());
        verify(reservationRepository, times(1)).findActiveByItemId(itemId);
    }
    
    @Test
    @DisplayName("Should expire old reservations")
    void testExpireOldReservations() {
        // Arrange
        List<Reservation> expiredReservations = Arrays.asList(
            createReservation(1, 1, 1),
            createReservation(2, 2, 2)
        );
        
        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
            .thenReturn(expiredReservations);
        when(reservationRepository.update(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        int count = reservationService.expireOldReservations();
        
        // Assert
        assertEquals(2, count);
        verify(reservationRepository, times(2)).update(argThat(res -> 
            "EXPIRED".equals(res.getStatus())
        ));
    }
    
    @Test
    @DisplayName("Should get queue position correctly")
    void testGetQueuePosition() {
        // Arrange
        int reservationId = 2;
        int itemId = 1;
        
        Reservation targetReservation = createReservation(reservationId, 2, itemId);
        
        List<Reservation> queue = Arrays.asList(
            createReservation(1, 1, itemId),
            targetReservation,
            createReservation(3, 3, itemId)
        );
        
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(targetReservation));
        when(reservationRepository.findActiveByItemId(itemId)).thenReturn(queue);
        
        // Act
        int position = reservationService.getQueuePosition(reservationId);
        
        // Assert
        assertEquals(2, position); // Second in queue
    }
    
    @Test
    @DisplayName("Should return -1 for non-existent reservation")
    void testGetQueuePosition_NotFound() {
        // Arrange
        when(reservationRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        // Act
        int position = reservationService.getQueuePosition(999);
        
        // Assert
        assertEquals(-1, position);
    }
    
    @Test
    @DisplayName("Should check if user has active reservation")
    void testHasActiveReservation() {
        // Arrange
        int userId = 1;
        int itemId = 1;
        
        Reservation reservation = createReservation(1, userId, itemId);
        when(reservationRepository.findActiveByUserId(userId))
            .thenReturn(Arrays.asList(reservation));
        
        // Act
        boolean result = reservationService.hasActiveReservation(userId, itemId);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should return false when user has no active reservation for item")
    void testHasActiveReservation_NoReservation() {
        // Arrange
        int userId = 1;
        int itemId = 1;
        
        when(reservationRepository.findActiveByUserId(userId))
            .thenReturn(Collections.emptyList());
        
        // Act
        boolean result = reservationService.hasActiveReservation(userId, itemId);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should return false when user has reservations for different items")
    void testHasActiveReservation_DifferentItem() {
        // Arrange
        int userId = 1;
        int targetItemId = 5;
        
        // User has reservations for items 1 and 2, but not for item 5
        Reservation res1 = createReservation(1, userId, 1);
        Reservation res2 = createReservation(2, userId, 2);
        
        when(reservationRepository.findActiveByUserId(userId))
            .thenReturn(Arrays.asList(res1, res2));
        
        // Act
        boolean result = reservationService.hasActiveReservation(userId, targetItemId);
        
        // Assert - should return false because item 5 is not in the list
        assertFalse(result);
    }
    
    // Helper methods
    
    private User createUser(int userId, String username, String email) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole("MEMBER");
        return user;
    }
    
    private MediaItem createMediaItem(int itemId, String title, int totalCopies, int availableCopies) {
        MediaItem item = new MediaItem();
        item.setItemId(itemId);
        item.setTitle(title);
        item.setAuthor("Author");
        item.setType("BOOK");
        item.setPublicationDate(LocalDate.now().minusYears(1));
        item.setTotalCopies(totalCopies);
        item.setAvailableCopies(availableCopies);
        item.setLateFeesPerDay(new BigDecimal("10.00"));
        return item;
    }
    
    private Reservation createReservation(int reservationId, int userId, int itemId) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setItemId(itemId);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setExpiryDate(LocalDateTime.now().plusHours(48));
        reservation.setStatus("ACTIVE");
        return reservation;
    }
    
    @Test
    @DisplayName("Should throw exception when canceling non-active reservation")
    void testCancelReservation_NotActive() {
        // Arrange - reservation is already fulfilled
        Reservation reservation = new Reservation();
        reservation.setReservationId(1);
        reservation.setUserId(1);
        reservation.setStatus("FULFILLED"); // Not ACTIVE
        
        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> reservationService.cancelReservation(1, 1));
        
        assertTrue(exception.getMessage().contains("not active"));
        verify(reservationRepository, never()).update(any());
    }
    
    @Test
    @DisplayName("Should return -1 for queue position when reservation is not active")
    void testGetQueuePosition_NotActive() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setReservationId(1);
        reservation.setUserId(1);
        reservation.setItemId(1);
        reservation.setStatus("FULFILLED"); // Not ACTIVE
        
        when(reservationRepository.findById(1)).thenReturn(Optional.of(reservation));
        
        // Act
        int position = reservationService.getQueuePosition(1);
        
        // Assert
        assertEquals(-1, position);
    }
    
    @Test
    @DisplayName("Should return -1 when reservation not found in queue")
    void testGetQueuePosition_NotInQueue() {
        // Arrange
        int reservationId = 99;
        int itemId = 1;
        
        Reservation targetReservation = new Reservation();
        targetReservation.setReservationId(reservationId);
        targetReservation.setUserId(5);
        targetReservation.setItemId(itemId);
        targetReservation.setStatus("ACTIVE");
        
        // Queue doesn't contain the target reservation
        List<Reservation> queue = Arrays.asList(
            createReservation(1, 1, itemId),
            createReservation(2, 2, itemId)
        );
        
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(targetReservation));
        when(reservationRepository.findActiveByItemId(itemId)).thenReturn(queue);
        
        // Act
        int position = reservationService.getQueuePosition(reservationId);
        
        // Assert
        assertEquals(-1, position); // Not found in queue
    }
}

