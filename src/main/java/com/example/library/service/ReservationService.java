package com.example.library.service;

import com.example.library.domain.Reservation;
import java.util.List;

/**
 * Service interface for managing item reservations.
 * Allows users to reserve items that are currently unavailable.
 */
public interface ReservationService {
    
    /**
     * Creates a new reservation for an item.
     * 
     * @param userId the ID of the user making the reservation
     * @param itemId the ID of the item to reserve
     * @return the created Reservation object
     * @throws BusinessException if reservation cannot be created
     */
    Reservation createReservation(int userId, int itemId);
    
    /**
     * Cancels a reservation.
     * 
     * @param reservationId the ID of the reservation to cancel
     * @param userId the ID of the user canceling (must be the owner)
     * @throws BusinessException if reservation cannot be canceled
     */
    void cancelReservation(int reservationId, int userId);
    
    /**
     * Fulfills a reservation when the item becomes available.
     * This should be called when an item is returned.
     * 
     * @param itemId the ID of the returned item
     * @return the fulfilled reservation, or null if no active reservations
     */
    Reservation fulfillNextReservation(int itemId);
    
    /**
     * Gets all reservations for a specific user.
     * 
     * @param userId the ID of the user
     * @return list of reservations
     */
    List<Reservation> getUserReservations(int userId);
    
    /**
     * Gets active reservations for a specific user.
     * 
     * @param userId the ID of the user
     * @return list of active reservations
     */
    List<Reservation> getActiveUserReservations(int userId);
    
    /**
     * Gets the reservation queue for a specific item.
     * 
     * @param itemId the ID of the item
     * @return list of active reservations ordered by date
     */
    List<Reservation> getItemReservationQueue(int itemId);
    
    /**
     * Expires old reservations that are past their expiry date.
     * Should be run periodically (e.g., daily).
     * 
     * @return count of expired reservations
     */
    int expireOldReservations();
    
    /**
     * Gets the position of a reservation in the queue for an item.
     * 
     * @param reservationId the ID of the reservation
     * @return the position (1-based), or -1 if not found or not active
     */
    int getQueuePosition(int reservationId);
    
    /**
     * Checks if a user has an active reservation for an item.
     * 
     * @param userId the ID of the user
     * @param itemId the ID of the item
     * @return true if user has an active reservation, false otherwise
     */
    boolean hasActiveReservation(int userId, int itemId);
}
