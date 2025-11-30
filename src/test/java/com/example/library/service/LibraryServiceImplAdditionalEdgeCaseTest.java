package com.example.library.service;

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

class LibraryServiceImplAdditionalEdgeCaseTest {
    
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
    void testAddMediaItem_Null() {
        assertThrows(BusinessException.class, () -> {
            libraryService.addMediaItem(null);
        });
        
        verify(mediaItemRepository, never()).save(any());
    }
    
    @Test
    void testBorrowItem_UserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        
        assertThrows(BusinessException.class, () -> {
            libraryService.borrowItem(999, 1, LocalDate.now());
        });
        
        verify(userRepository).findById(999);
        verify(mediaItemRepository, never()).findById(any());
        verify(loanRepository, never()).save(any());
    }
    
    @Test
    void testBorrowItem_UnknownMediaType_DVD() {
        User user = new User();
        user.setUserId(1);
        
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setType("DVD"); // Not BOOK or CD
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("2.00"));
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(java.util.Collections.emptyList());
        when(fineRepository.calculateTotalUnpaidByUserId(1)).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(mediaItemRepository).updateAvailableCopies(anyInt(), anyInt());
        
        var loan = libraryService.borrowItem(1, 1, LocalDate.now());
        
        // Default loan period should be 14 days for unknown types
        assertNotNull(loan);
        assertEquals(LocalDate.now().plusDays(14), loan.getDueDate());
    }
    
    @Test
    void testBorrowItem_UnknownMediaType_MAGAZINE() {
        User user = new User();
        user.setUserId(1);
        
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setType("MAGAZINE"); // Not BOOK or CD
        item.setAvailableCopies(3);
        item.setLateFeesPerDay(new BigDecimal("0.50"));
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(java.util.Collections.emptyList());
        when(fineRepository.calculateTotalUnpaidByUserId(1)).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(mediaItemRepository).updateAvailableCopies(anyInt(), anyInt());
        
        var loan = libraryService.borrowItem(1, 1, LocalDate.now());
        
        // Default loan period should be 14 days for unknown types
        assertNotNull(loan);
        assertEquals(LocalDate.now().plusDays(14), loan.getDueDate());
    }
    
    @Test
    void testReturnItem_MediaItemDeleted() {
        var loan = new com.example.library.domain.Loan();
        loan.setLoanId(1);
        loan.setItemId(999);
        loan.setUserId(1);
        loan.setStatus("ACTIVE");
        loan.setLoanDate(LocalDate.now().minusDays(7));
        loan.setDueDate(LocalDate.now().plusDays(7));
        
        when(loanRepository.findById(1)).thenReturn(Optional.of(loan));
        when(mediaItemRepository.findById(999)).thenReturn(Optional.empty());
        
        assertThrows(BusinessException.class, () -> {
            libraryService.returnItem(1, LocalDate.now());
        });
        
        verify(loanRepository).findById(1);
        verify(mediaItemRepository).findById(999);
        verify(loanRepository, never()).update(any());
    }
    
    @Test
    void testBorrowItem_CD_Type() {
        User user = new User();
        user.setUserId(1);
        
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setType("CD"); // CD type - 7 days loan period
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(java.util.Collections.emptyList());
        when(fineRepository.calculateTotalUnpaidByUserId(1)).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(mediaItemRepository).updateAvailableCopies(anyInt(), anyInt());
        
        var loan = libraryService.borrowItem(1, 1, LocalDate.now());
        
        // CD loan period should be 7 days
        assertNotNull(loan);
        assertEquals(LocalDate.now().plusDays(7), loan.getDueDate());
        verify(loanRepository).save(any());
    }
    
    @Test
    void testBorrowItem_UserNotEligible_WithUnpaidFines() {
        User user = new User();
        user.setUserId(1);
        
        MediaItem item = new MediaItem();
        item.setItemId(1);
        item.setType("BOOK");
        item.setAvailableCopies(5);
        
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(mediaItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(loanRepository.findOverdueLoans(any(LocalDate.class))).thenReturn(java.util.Collections.emptyList());
        when(fineRepository.calculateTotalUnpaidByUserId(1)).thenReturn(new BigDecimal("25.00")); // Has unpaid fines
        
        assertThrows(BusinessException.class, () -> {
            libraryService.borrowItem(1, 1, LocalDate.now());
        }, "User is not eligible to borrow");
        
        verify(loanRepository, never()).save(any());
    }
}
