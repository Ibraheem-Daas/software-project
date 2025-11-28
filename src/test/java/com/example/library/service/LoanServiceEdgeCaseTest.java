package com.example.library.service;

import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
import com.example.library.repository.FineRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.MediaItemRepository;
import com.example.library.repository.UserRepository;
import com.example.library.service.fine.FineCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LibraryServiceImplEdgeCaseTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private MediaItemRepository mediaItemRepository;
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private FineRepository fineRepository;
    
    @Mock
    private FineCalculator fineCalculator;
    
    private LibraryServiceImpl libraryService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        libraryService = new LibraryServiceImpl(userRepository, mediaItemRepository, 
                loanRepository, fineRepository, fineCalculator);
    }
    
    @Test
    void testBorrowItem_MediaItemNotFound() {
        User user = new User();
        user.setUserId(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(999)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            libraryService.borrowItem(1, 999, LocalDate.now());
        });
        
        verify(mediaItemRepository).findById(999);
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void testBorrowItem_NoAvailableCopies() {
        User user = new User();
        user.setUserId(1);
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setTitle("Test Book");
        item.setAvailableCopies(0);
        item.setTotalCopies(1);
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        
        assertThrows(RuntimeException.class, () -> {
            libraryService.borrowItem(1, 1, LocalDate.now());
        });
        
        verify(mediaItemRepository).findById(1);
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void testBorrowItem_Success() {
        User user = new User();
        user.setUserId(1);
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setTitle("Test Book");
        item.setType("BOOK");
        item.setAvailableCopies(5);
        item.setTotalCopies(10);
        
        Loan savedLoan = new Loan();
        savedLoan.setLoanId(1);
        savedLoan.setUserId(1);
        savedLoan.setItemId(1);
        savedLoan.setStatus("ACTIVE");
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(java.util.Collections.emptyList());
        when(fineRepository.calculateTotalUnpaidByUserId(1)).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);
        doNothing().when(mediaItemRepository).updateAvailableCopies(anyInt(), anyInt());
        
        Loan result = libraryService.borrowItem(1, 1, LocalDate.now());
        
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(mediaItemRepository).updateAvailableCopies(1, 4);
    }
    
    @Test
    void testReturnItem_LoanNotFound() {
        when(loanRepository.findById(999)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            libraryService.returnItem(999, LocalDate.now());
        });
        
        verify(loanRepository).findById(999);
        verify(mediaItemRepository, never()).updateAvailableCopies(anyInt(), anyInt());
    }
    
    @Test
    void testReturnItem_AlreadyReturned() {
        Loan loan = new Loan();
        loan.setLoanId(1);
        loan.setItemId(1);
        loan.setStatus("RETURNED");
        loan.setReturnDate(LocalDate.now().minusDays(1));
        
        when(loanRepository.findById(1)).thenReturn(Optional.of(loan));
        
        assertThrows(RuntimeException.class, () -> {
            libraryService.returnItem(1, LocalDate.now());
        });
        
        verify(loanRepository).findById(1);
        verify(loanRepository, never()).updateStatus(anyInt(), anyString(), any());
    }
    
    @Test
    void testReturnItem_Success() {
        Loan loan = new Loan();
        loan.setLoanId(1);
        loan.setItemId(1);
        loan.setUserId(1);
        loan.setStatus("ACTIVE");
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(7));
        
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setAvailableCopies(5);
        item.setTotalCopies(10);
        
        when(loanRepository.findById(1)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(fineCalculator.calculateFine(anyString(), anyLong())).thenReturn(BigDecimal.ZERO);
        
        assertDoesNotThrow(() -> libraryService.returnItem(1, LocalDate.now()));
        
        verify(loanRepository).updateStatus(eq(1), eq("RETURNED"), any());
        verify(mediaItemRepository).updateAvailableCopies(eq(1), eq(6));
    }
    
}
