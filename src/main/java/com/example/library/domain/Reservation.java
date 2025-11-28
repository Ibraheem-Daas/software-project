package com.example.library.domain;

import java.time.LocalDateTime;

/**
 * Represents a reservation for a media item.
 * Users can reserve items that are currently unavailable.
 */
public class Reservation {
    private Integer reservationId;
    private Integer userId;
    private Integer itemId;
    private LocalDateTime reservationDate;
    private LocalDateTime expiryDate;
    private String status; // ACTIVE, FULFILLED, EXPIRED, CANCELLED
    
    // Constructors
    public Reservation() {
    }
    
    public Reservation(Integer reservationId, Integer userId, Integer itemId, 
                      LocalDateTime reservationDate, LocalDateTime expiryDate, String status) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.itemId = itemId;
        this.reservationDate = reservationDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }
    
    // Getters and Setters
    public Integer getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public Integer getItemId() {
        return itemId;
    }
    
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
    
    public LocalDateTime getReservationDate() {
        return reservationDate;
    }
    
    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }
    
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId=" + reservationId +
                ", userId=" + userId +
                ", itemId=" + itemId +
                ", reservationDate=" + reservationDate +
                ", expiryDate=" + expiryDate +
                ", status='" + status + '\'' +
                '}';
    }
}
