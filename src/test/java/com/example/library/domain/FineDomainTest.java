package com.example.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;

class FineDomainTest {
    
    @Test
    void testFineConstructor() {
        Fine fine = new Fine();
        assertNotNull(fine);
    }
    
    @Test
    void testFineSettersAndGetters() {
        Fine fine = new Fine();
        LocalDate issuedDate = LocalDate.of(2024, 1, 1);
        LocalDate paidDate = LocalDate.of(2024, 1, 10);
        
        fine.setFineId(1);
        fine.setLoanId(100);
        fine.setAmount(new BigDecimal("15.50"));
        fine.setIssuedDate(issuedDate);
        fine.setPaidDate(paidDate);
        fine.setStatus("PAID");
        
        assertEquals(1, fine.getFineId());
        assertEquals(100, fine.getLoanId());
        assertEquals(0, new BigDecimal("15.50").compareTo(fine.getAmount()));
        assertEquals(issuedDate, fine.getIssuedDate());
        assertEquals(paidDate, fine.getPaidDate());
        assertEquals("PAID", fine.getStatus());
    }
    
    @Test
    void testFineStatus() {
        Fine fine = new Fine();
        
        fine.setStatus("UNPAID");
        assertEquals("UNPAID", fine.getStatus());
        
        fine.setStatus("PAID");
        assertEquals("PAID", fine.getStatus());
        
        fine.setStatus("WAIVED");
        assertEquals("WAIVED", fine.getStatus());
    }
    
    @Test
    void testFineAmountCalculations() {
        Fine fine = new Fine();
        
        fine.setAmount(new BigDecimal("10.00"));
        assertEquals(0, new BigDecimal("10.00").compareTo(fine.getAmount()));
        
        // Test amount update
        fine.setAmount(fine.getAmount().add(new BigDecimal("5.00")));
        assertEquals(0, new BigDecimal("15.00").compareTo(fine.getAmount()));
    }
    
    @Test
    void testFineDates() {
        Fine fine = new Fine();
        LocalDate issuedDate = LocalDate.now().minusDays(10);
        LocalDate paidDate = LocalDate.now();
        
        fine.setIssuedDate(issuedDate);
        fine.setPaidDate(paidDate);
        
        assertEquals(issuedDate, fine.getIssuedDate());
        assertEquals(paidDate, fine.getPaidDate());
        assertTrue(fine.getPaidDate().isAfter(fine.getIssuedDate()));
    }
    
    @Test
    void testFineAmountPrecision() {
        Fine fine = new Fine();
        
        fine.setAmount(new BigDecimal("12.345"));
        assertEquals(0, new BigDecimal("12.345").compareTo(fine.getAmount()));
        
        fine.setAmount(new BigDecimal("0.50"));
        assertEquals(0, new BigDecimal("0.50").compareTo(fine.getAmount()));
    }
}
