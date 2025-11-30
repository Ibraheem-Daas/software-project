package com.example.library.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Reservation domain class constructors and methods
 */
public class ReservationDomainTest {

    @Test
    public void testDefaultConstructor() {
        Reservation reservation = new Reservation();
        assertNotNull(reservation);
        assertNull(reservation.getReservationId());
        assertNull(reservation.getUserId());
        assertNull(reservation.getItemId());
    }

    @Test
    public void testParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusDays(7);
        
        Reservation reservation = new Reservation(
            1, 
            100, 
            200, 
            now, 
            expiry, 
            "ACTIVE"
        );
        
        assertEquals(1, reservation.getReservationId());
        assertEquals(100, reservation.getUserId());
        assertEquals(200, reservation.getItemId());
        assertEquals(now, reservation.getReservationDate());
        assertEquals(expiry, reservation.getExpiryDate());
        assertEquals("ACTIVE", reservation.getStatus());
    }

    @Test
    public void testSettersAndGetters() {
        Reservation reservation = new Reservation();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusDays(3);
        
        reservation.setReservationId(5);
        reservation.setUserId(50);
        reservation.setItemId(75);
        reservation.setReservationDate(now);
        reservation.setExpiryDate(expiry);
        reservation.setStatus("FULFILLED");
        
        assertEquals(5, reservation.getReservationId());
        assertEquals(50, reservation.getUserId());
        assertEquals(75, reservation.getItemId());
        assertEquals(now, reservation.getReservationDate());
        assertEquals(expiry, reservation.getExpiryDate());
        assertEquals("FULFILLED", reservation.getStatus());
    }

    @Test
    public void testToString() {
        LocalDateTime now = LocalDateTime.of(2024, 11, 29, 10, 30);
        LocalDateTime expiry = now.plusDays(7);
        
        Reservation reservation = new Reservation(
            10, 
            20, 
            30, 
            now, 
            expiry, 
            "EXPIRED"
        );
        
        String str = reservation.toString();
        assertNotNull(str);
        assertTrue(str.contains("reservationId=10"));
        assertTrue(str.contains("userId=20"));
        assertTrue(str.contains("itemId=30"));
        assertTrue(str.contains("EXPIRED"));
    }

    @Test
    public void testAllStatusTypes() {
        Reservation r1 = new Reservation(1, 1, 1, LocalDateTime.now(), LocalDateTime.now(), "ACTIVE");
        assertEquals("ACTIVE", r1.getStatus());
        
        Reservation r2 = new Reservation(2, 2, 2, LocalDateTime.now(), LocalDateTime.now(), "FULFILLED");
        assertEquals("FULFILLED", r2.getStatus());
        
        Reservation r3 = new Reservation(3, 3, 3, LocalDateTime.now(), LocalDateTime.now(), "EXPIRED");
        assertEquals("EXPIRED", r3.getStatus());
        
        Reservation r4 = new Reservation(4, 4, 4, LocalDateTime.now(), LocalDateTime.now(), "CANCELLED");
        assertEquals("CANCELLED", r4.getStatus());
    }

    @Test
    public void testNullDates() {
        Reservation reservation = new Reservation(
            1, 
            1, 
            1, 
            null, 
            null, 
            "ACTIVE"
        );
        
        assertNull(reservation.getReservationDate());
        assertNull(reservation.getExpiryDate());
    }
}
