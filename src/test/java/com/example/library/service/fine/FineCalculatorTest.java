package com.example.library.service.fine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FineCalculatorTest {
    
    private FineCalculator fineCalculator;
    
    @BeforeEach
    void setUp() {
        fineCalculator = new FineCalculator();
    }
    
    @Test
    void testCalculateFine_Book_ZeroDays_ReturnsZero() {
        // Arrange
        String mediaType = "BOOK";
        long overdueDays = 0;
        
        // Act
        BigDecimal result = fineCalculator.calculateFine(mediaType, overdueDays);
        
        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(result), 
                "Fine for 0 overdue days should be 0");
    }
    
    @Test
    void testCalculateFine_Book_ThreeDays_ReturnsThirty() {
        // Arrange
        String mediaType = "BOOK";
        long overdueDays = 3;
        BigDecimal expected = new BigDecimal("30.00");
        
        // Act
        BigDecimal result = fineCalculator.calculateFine(mediaType, overdueDays);
        
        // Assert
        assertEquals(0, expected.compareTo(result), 
                "Fine for 3 overdue days for a BOOK should be 30.00 (10 NIS × 3)");
    }
    
    @Test
    void testCalculateFine_CD_TwoDays_ReturnsForty() {
        // Arrange
        String mediaType = "CD";
        long overdueDays = 2;
        BigDecimal expected = new BigDecimal("40.00");
        
        // Act
        BigDecimal result = fineCalculator.calculateFine(mediaType, overdueDays);
        
        // Assert
        assertEquals(0, expected.compareTo(result), 
                "Fine for 2 overdue days for a CD should be 40.00 (20 NIS × 2)");
    }
    
    @Test
    void testCalculateFine_UnsupportedMediaType_ThrowsIllegalArgumentException() {
        // Arrange
        String mediaType = "MAGAZINE";
        long overdueDays = 5;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.calculateFine(mediaType, overdueDays),
                "Should throw IllegalArgumentException for unsupported media type"
        );
        
        assertTrue(exception.getMessage().contains("Unsupported media type"),
                "Exception message should indicate unsupported media type");
    }
    
    @Test
    void testCalculateFine_NullMediaType_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.calculateFine(null, 5)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    void testCalculateFine_EmptyMediaType_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.calculateFine("  ", 5)
        );
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testCalculateFine_CaseInsensitive() {
        // Test lowercase
        BigDecimal result1 = fineCalculator.calculateFine("book", 1);
        assertEquals(new BigDecimal("10.00"), result1);
        
        // Test mixed case
        BigDecimal result2 = fineCalculator.calculateFine("BoOk", 1);
        assertEquals(new BigDecimal("10.00"), result2);
    }
    
    @Test
    void testRegisterStrategy_CustomMediaType() {
        // Arrange
        FineStrategy customStrategy = overdueDays -> new BigDecimal("15.00").multiply(BigDecimal.valueOf(overdueDays));
        
        // Act
        fineCalculator.registerStrategy("DVD", customStrategy);
        BigDecimal result = fineCalculator.calculateFine("DVD", 2);
        
        // Assert
        assertEquals(new BigDecimal("30.00"), result);
    }
    
    @Test
    void testRegisterStrategy_NullMediaType_ThrowsException() {
        // Arrange
        FineStrategy strategy = overdueDays -> BigDecimal.TEN;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.registerStrategy(null, strategy)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    void testRegisterStrategy_NullStrategy_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.registerStrategy("DVD", null)
        );
        assertTrue(exception.getMessage().contains("Strategy cannot be null"));
    }
    
    @Test
    void testRegisterStrategy_EmptyMediaType_ThrowsException() {
        // Arrange
        FineStrategy strategy = overdueDays -> BigDecimal.TEN;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.registerStrategy("   ", strategy)
        );
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void testCalculateFine_ActuallyEmptyString_ThrowsException() {
        // Test truly empty string (not whitespace)
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fineCalculator.calculateFine("", 5)
        );
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }
}
