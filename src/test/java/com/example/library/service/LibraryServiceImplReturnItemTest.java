package com.example.library.service;

import com.example.library.domain.Fine;
import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.repository.FineRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.MediaItemRepository;
import com.example.library.repository.UserRepository;
import com.example.library.service.fine.FineCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LibraryServiceImplReturnItemTest {
    
    private UserRepository userRepository;
    private MediaItemRepository mediaItemRepository;
    private LoanRepository loanRepository;
    private FineRepository fineRepository;
    private FineCalculator fineCalculator;
    
    private LibraryServiceImpl libraryService;
    
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mediaItemRepository = mock(MediaItemRepository.class);
        loanRepository = mock(LoanRepository.class);
        fineRepository = mock(FineRepository.class);
        fineCalculator = mock(FineCalculator.class);
        
        libraryService = new LibraryServiceImpl(
                userRepository,
                mediaItemRepository,
                loanRepository,
                fineRepository,
                fineCalculator
        );
    }
    
    @Test
    void testReturnItem_noOverdue_noFineCreated() {
        // Arrange
        int loanId = 1;
        int itemId = 10;
        LocalDate loanDate = LocalDate.of(2025, 11, 1);
        LocalDate dueDate = LocalDate.of(2025, 11, 29);
        LocalDate returnDate = LocalDate.of(2025, 11, 29); // On time
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(1);
        loan.setItemId(itemId);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("ACTIVE");
        
        MediaItem mediaItem = new MediaItem();
        mediaItem.setItemId(itemId);
        mediaItem.setType("BOOK");
        mediaItem.setAvailableCopies(3);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(mediaItem));
        
        // Act
        libraryService.returnItem(loanId, returnDate);
        
        // Assert
        verify(loanRepository).updateStatus(loanId, "RETURNED", returnDate);
        verify(mediaItemRepository).updateAvailableCopies(itemId, 4);
        verify(fineRepository, never()).save(any(Fine.class));
        verify(fineCalculator, never()).calculateFine(anyString(), anyLong());
    }
    
    @Test
    void testReturnItem_overdueBook_createsFine() {
        // Arrange
        int loanId = 2;
        int itemId = 20;
        LocalDate loanDate = LocalDate.of(2025, 11, 1);
        LocalDate dueDate = LocalDate.of(2025, 11, 29);
        LocalDate returnDate = LocalDate.of(2025, 12, 2); // 3 days late
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(2);
        loan.setItemId(itemId);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("ACTIVE");
        
        MediaItem mediaItem = new MediaItem();
        mediaItem.setItemId(itemId);
        mediaItem.setType("BOOK");
        mediaItem.setAvailableCopies(5);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(mediaItem));
        when(fineCalculator.calculateFine("BOOK", 3L)).thenReturn(new BigDecimal("30.00"));
        
        // Act
        libraryService.returnItem(loanId, returnDate);
        
        // Assert
        verify(loanRepository).updateStatus(loanId, "RETURNED", returnDate);
        verify(mediaItemRepository).updateAvailableCopies(itemId, 6);
        verify(fineCalculator).calculateFine("BOOK", 3L);
        
        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fineCaptor.capture());
        
        Fine savedFine = fineCaptor.getValue();
        assertEquals(loanId, savedFine.getLoanId(), "Fine should be linked to loan");
        assertEquals(0, new BigDecimal("30.00").compareTo(savedFine.getAmount()), 
                "Fine amount should be 30.00");
        assertEquals(returnDate, savedFine.getIssuedDate(), "Fine issued date should match return date");
        assertEquals("UNPAID", savedFine.getStatus(), "Fine status should be UNPAID");
        assertNull(savedFine.getPaidDate(), "Paid date should be null");
    }
    
    @Test
    void testReturnItem_overdueCd_createsFine() {
        // Arrange
        int loanId = 3;
        int itemId = 30;
        LocalDate loanDate = LocalDate.of(2025, 11, 20);
        LocalDate dueDate = LocalDate.of(2025, 11, 27);
        LocalDate returnDate = LocalDate.of(2025, 11, 29); // 2 days late
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(3);
        loan.setItemId(itemId);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("ACTIVE");
        
        MediaItem mediaItem = new MediaItem();
        mediaItem.setItemId(itemId);
        mediaItem.setType("CD");
        mediaItem.setAvailableCopies(2);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(mediaItem));
        when(fineCalculator.calculateFine("CD", 2L)).thenReturn(new BigDecimal("40.00"));
        
        // Act
        libraryService.returnItem(loanId, returnDate);
        
        // Assert
        verify(loanRepository).updateStatus(loanId, "RETURNED", returnDate);
        verify(mediaItemRepository).updateAvailableCopies(itemId, 3);
        verify(fineCalculator).calculateFine("CD", 2L);
        
        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fineCaptor.capture());
        
        Fine savedFine = fineCaptor.getValue();
        assertEquals(loanId, savedFine.getLoanId(), "Fine should be linked to loan");
        assertEquals(0, new BigDecimal("40.00").compareTo(savedFine.getAmount()), 
                "Fine amount should be 40.00");
        assertEquals(returnDate, savedFine.getIssuedDate(), "Fine issued date should match return date");
        assertEquals("UNPAID", savedFine.getStatus(), "Fine status should be UNPAID");
        assertNull(savedFine.getPaidDate(), "Paid date should be null");
    }
    
    @Test
    void testReturnItem_loanNotFound_throws() {
        // Arrange
        int loanId = 999;
        LocalDate returnDate = LocalDate.now();
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());
        
        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libraryService.returnItem(loanId, returnDate),
                "Should throw BusinessException when loan not found"
        );
        
        assertTrue(exception.getMessage().contains("Loan not found"),
                "Exception message should indicate loan not found");
        assertTrue(exception.getMessage().contains(String.valueOf(loanId)),
                "Exception message should include loan ID");
        
        verify(loanRepository, never()).updateStatus(anyInt(), anyString(), any(LocalDate.class));
        verify(mediaItemRepository, never()).updateAvailableCopies(anyInt(), anyInt());
        verify(fineRepository, never()).save(any(Fine.class));
    }
    
    @Test
    void testReturnItem_alreadyReturned_throws() {
        // Arrange
        int loanId = 5;
        int itemId = 50;
        LocalDate previousReturnDate = LocalDate.of(2025, 11, 25);
        LocalDate returnDate = LocalDate.now();
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(5);
        loan.setItemId(itemId);
        loan.setLoanDate(LocalDate.of(2025, 11, 1));
        loan.setDueDate(LocalDate.of(2025, 11, 29));
        loan.setReturnDate(previousReturnDate); // Already returned
        loan.setStatus("RETURNED");
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libraryService.returnItem(loanId, returnDate),
                "Should throw BusinessException when loan already returned"
        );
        
        assertTrue(exception.getMessage().contains("already been returned"),
                "Exception message should indicate loan already returned");
        
        verify(loanRepository, never()).updateStatus(anyInt(), anyString(), any(LocalDate.class));
        verify(mediaItemRepository, never()).findById(anyInt());
        verify(mediaItemRepository, never()).updateAvailableCopies(anyInt(), anyInt());
        verify(fineRepository, never()).save(any(Fine.class));
    }
    
    @Test
    void testReturnItem_statusReturnedButNoReturnDate_throws() {
        // Arrange - tests the branch where returnDate is null but status is "RETURNED"
        int loanId = 6;
        LocalDate returnDate = LocalDate.now();
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(6);
        loan.setItemId(60);
        loan.setLoanDate(LocalDate.of(2025, 11, 1));
        loan.setDueDate(LocalDate.of(2025, 11, 29));
        loan.setReturnDate(null); // No return date
        loan.setStatus("RETURNED"); // But status is RETURNED
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> libraryService.returnItem(loanId, returnDate),
                "Should throw BusinessException when status is RETURNED even if returnDate is null"
        );
        
        assertTrue(exception.getMessage().contains("already been returned"));
        verify(loanRepository, never()).updateStatus(anyInt(), anyString(), any(LocalDate.class));
    }
    
    @Test
    void testReturnItem_overdue_zeroFineAmount_noFineSaved() {
        // Arrange - tests the branch where fineAmount == 0 (line 197: if fineAmount > 0)
        int loanId = 7;
        int itemId = 70;
        LocalDate loanDate = LocalDate.of(2025, 11, 1);
        LocalDate dueDate = LocalDate.of(2025, 11, 15);
        LocalDate returnDate = LocalDate.of(2025, 11, 20); // 5 days overdue
        
        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setUserId(7);
        loan.setItemId(itemId);
        loan.setLoanDate(loanDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("ACTIVE");
        
        MediaItem item = new MediaItem();
        item.setItemId(itemId);
        item.setTitle("Zero Fine Item");
        item.setType("MAGAZINE");
        item.setAvailableCopies(2);
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        // Mock FineCalculator to return ZERO fine
        when(fineCalculator.calculateFine("MAGAZINE", 5)).thenReturn(BigDecimal.ZERO);
        
        // Act
        libraryService.returnItem(loanId, returnDate);
        
        // Assert - fine should NOT be saved when amount is zero
        verify(loanRepository).updateStatus(loanId, "RETURNED", returnDate);
        verify(mediaItemRepository).updateAvailableCopies(itemId, 3);
        verify(fineRepository, never()).save(any(Fine.class)); // Key assertion
    }
}
