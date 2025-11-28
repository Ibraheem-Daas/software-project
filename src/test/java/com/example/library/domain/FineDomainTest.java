package com.example.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
        LocalDate issueDate = LocalDate.of(2024, 1, 1);
        LocalDate paidDate = LocalDate.of(2024, 1, 10);
        
        fine.setFineId(1);
        fine.setLoanId(100);
        fine.setAmount(15.50);
        fine.setIssueDate(issueDate);
        fine.setPaidDate(paidDate);
        fine.setStatus("PAID");
        
        assertEquals(1, fine.getFineId());
        assertEquals(100, fine.getLoanId());
        assertEquals(15.50, fine.getAmount(), 0.01);
        assertEquals(issueDate, fine.getIssueDate());
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
        
        fine.setAmount(10.00);
        assertEquals(10.00, fine.getAmount(), 0.01);
        
        // Test amount update
        fine.setAmount(fine.getAmount() + 5.00);
        assertEquals(15.00, fine.getAmount(), 0.01);
    }
    
    @Test
    void testFineDates() {
        Fine fine = new Fine();
        LocalDate issueDate = LocalDate.now().minusDays(10);
        LocalDate paidDate = LocalDate.now();
        
        fine.setIssueDate(issueDate);
        fine.setPaidDate(paidDate);
        
        assertEquals(issueDate, fine.getIssueDate());
        assertEquals(paidDate, fine.getPaidDate());
        assertTrue(fine.getPaidDate().isAfter(fine.getIssueDate()));
    }
    
    @Test
    void testFineAmountPrecision() {
        Fine fine = new Fine();
        
        fine.setAmount(12.345);
        assertEquals(12.345, fine.getAmount(), 0.001);
        
        fine.setAmount(0.50);
        assertEquals(0.50, fine.getAmount(), 0.01);
    }
}
