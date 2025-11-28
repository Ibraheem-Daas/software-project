package com.example.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

class LoanDomainTest {
    
    @Test
    void testLoanConstructor() {
        Loan loan = new Loan();
        assertNotNull(loan);
    }
    
    @Test
    void testLoanSettersAndGetters() {
        Loan loan = new Loan();
        LocalDate loanDate = LocalDate.of(2024, 1, 1);
        LocalDate dueDate = LocalDate.of(2024, 1, 15);
        LocalDate returnDate = LocalDate.of(2024, 1, 14);
        
        loan.setLoanId(1);
        loan.setUserId(100);
        loan.setItemId(200);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(returnDate);
        loan.setStatus("ACTIVE");
        
        assertEquals(1, loan.getLoanId());
        assertEquals(100, loan.getUserId());
        assertEquals(200, loan.getItemId());
        assertEquals(loanDate, loan.getLoanDate());
        assertEquals(dueDate, loan.getDueDate());
        assertEquals(returnDate, loan.getReturnDate());
        assertEquals("ACTIVE", loan.getStatus());
    }
    
    @Test
    void testLoanStatus() {
        Loan loan = new Loan();
        
        loan.setStatus("ACTIVE");
        assertEquals("ACTIVE", loan.getStatus());
        
        loan.setStatus("RETURNED");
        assertEquals("RETURNED", loan.getStatus());
        
        loan.setStatus("OVERDUE");
        assertEquals("OVERDUE", loan.getStatus());
    }
    
    @Test
    void testLoanDates() {
        Loan loan = new Loan();
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(14);
        
        loan.setLoanDate(today);
        loan.setDueDate(futureDate);
        
        assertEquals(today, loan.getLoanDate());
        assertEquals(futureDate, loan.getDueDate());
        assertTrue(loan.getDueDate().isAfter(loan.getLoanDate()));
    }
}
