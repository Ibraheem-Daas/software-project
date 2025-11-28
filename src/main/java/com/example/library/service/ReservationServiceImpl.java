package com.example.library.service;

import com.example.library.domain.MediaItem;
import com.example.library.domain.Reservation;
import com.example.library.domain.User;
import com.example.library.repository.MediaItemRepository;
import com.example.library.repository.ReservationRepository;
import com.example.library.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ReservationService.
 * Manages item reservations with queue functionality.
 */
public class ReservationServiceImpl implements ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final MediaItemRepository mediaItemRepository;
    private final int RESERVATION_EXPIRY_HOURS = 48; // Reservation valid for 48 hours
    
    public ReservationServiceImpl(ReservationRepository reservationRepository,
                                 UserRepository userRepository,
                                 MediaItemRepository mediaItemRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.mediaItemRepository = mediaItemRepository;
    }
    
    @Override
    public Reservation createReservation(int userId, int itemId) {
        // Validate user exists
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new BusinessException("User not found with ID: " + userId);
        }
        
        // Validate item exists
        Optional<MediaItem> itemOptional = mediaItemRepository.findById(itemId);
        if (!itemOptional.isPresent()) {
            throw new BusinessException("Item not found with ID: " + itemId);
        }
        
        MediaItem item = itemOptional.get();
        
        // Check if item is available
        if (item.getAvailableCopies() > 0) {
            throw new BusinessException("Item is currently available. Please borrow it directly instead of reserving.");
        }
        
        // Check if user already has an active reservation for this item
        if (hasActiveReservation(userId, itemId)) {
            throw new BusinessException("You already have an active reservation for this item.");
        }
        
        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setItemId(itemId);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setExpiryDate(LocalDateTime.now().plusHours(RESERVATION_EXPIRY_HOURS));
        reservation.setStatus("ACTIVE");
        
        return reservationRepository.save(reservation);
    }
    
    @Override
    public void cancelReservation(int reservationId, int userId) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(reservationId);
        
        if (!reservationOptional.isPresent()) {
            throw new BusinessException("Reservation not found with ID: " + reservationId);
        }
        
        Reservation reservation = reservationOptional.get();
        
        // Verify ownership
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("You can only cancel your own reservations.");
        }
        
        // Check if already cancelled or fulfilled
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new BusinessException("Reservation is not active and cannot be cancelled.");
        }
        
        // Update status to CANCELLED
        reservation.setStatus("CANCELLED");
        reservationRepository.update(reservation);
    }
    
    @Override
    public Reservation fulfillNextReservation(int itemId) {
        // Get active reservations for this item (ordered by date)
        List<Reservation> activeReservations = reservationRepository.findActiveByItemId(itemId);
        
        if (activeReservations.isEmpty()) {
            return null; // No reservations to fulfill
        }
        
        // Get the first (oldest) reservation
        Reservation nextReservation = activeReservations.get(0);
        
        // Mark as fulfilled
        nextReservation.setStatus("FULFILLED");
        nextReservation.setExpiryDate(LocalDateTime.now().plusHours(RESERVATION_EXPIRY_HOURS));
        
        return reservationRepository.update(nextReservation);
    }
    
    @Override
    public List<Reservation> getUserReservations(int userId) {
        return reservationRepository.findByUserId(userId);
    }
    
    @Override
    public List<Reservation> getActiveUserReservations(int userId) {
        return reservationRepository.findActiveByUserId(userId);
    }
    
    @Override
    public List<Reservation> getItemReservationQueue(int itemId) {
        return reservationRepository.findActiveByItemId(itemId);
    }
    
    @Override
    public int expireOldReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(now);
        
        int count = 0;
        for (Reservation reservation : expiredReservations) {
            reservation.setStatus("EXPIRED");
            reservationRepository.update(reservation);
            count++;
        }
        
        return count;
    }
    
    @Override
    public int getQueuePosition(int reservationId) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(reservationId);
        
        if (!reservationOptional.isPresent()) {
            return -1;
        }
        
        Reservation reservation = reservationOptional.get();
        
        if (!"ACTIVE".equals(reservation.getStatus())) {
            return -1;
        }
        
        List<Reservation> queue = reservationRepository.findActiveByItemId(reservation.getItemId());
        
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getReservationId().equals(reservationId)) {
                return i + 1; // 1-based position
            }
        }
        
        return -1;
    }
    
    @Override
    public boolean hasActiveReservation(int userId, int itemId) {
        List<Reservation> activeReservations = reservationRepository.findActiveByUserId(userId);
        
        for (Reservation reservation : activeReservations) {
            if (reservation.getItemId().equals(itemId)) {
                return true;
            }
        }
        
        return false;
    }
}
