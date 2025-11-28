package com.example.library.repository;

import com.example.library.domain.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing reservations.
 */
public interface ReservationRepository {
    
    /**
     * Save a new reservation to the database
     * @param reservation the reservation to save
     * @return the saved reservation with generated ID
     */
    Reservation save(Reservation reservation);
    
    /**
     * Update an existing reservation
     * @param reservation the reservation to update
     * @return the updated reservation
     */
    Reservation update(Reservation reservation);
    
    /**
     * Find a reservation by ID
     * @param reservationId the reservation ID
     * @return Optional containing the reservation if found
     */
    Optional<Reservation> findById(Integer reservationId);
    
    /**
     * Find all reservations
     * @return list of all reservations
     */
    List<Reservation> findAll();
    
    /**
     * Find reservations by user ID
     * @param userId the user ID
     * @return list of reservations for the specified user
     */
    List<Reservation> findByUserId(Integer userId);
    
    /**
     * Find reservations by item ID
     * @param itemId the item ID
     * @return list of reservations for the specified item
     */
    List<Reservation> findByItemId(Integer itemId);
    
    /**
     * Find active reservations by item ID (ordered by reservation date)
     * @param itemId the item ID
     * @return list of active reservations for the item, ordered by reservation date
     */
    List<Reservation> findActiveByItemId(Integer itemId);
    
    /**
     * Find expired reservations
     * @param currentDateTime the current date and time
     * @return list of expired reservations
     */
    List<Reservation> findExpiredReservations(LocalDateTime currentDateTime);
    
    /**
     * Find active reservations for a user
     * @param userId the user ID
     * @return list of active reservations for the user
     */
    List<Reservation> findActiveByUserId(Integer userId);
    
    /**
     * Delete a reservation by ID
     * @param reservationId the reservation ID
     */
    void deleteById(Integer reservationId);
    
    /**
     * Count active reservations for an item
     * @param itemId the item ID
     * @return count of active reservations
     */
    int countActiveByItemId(Integer itemId);
}
