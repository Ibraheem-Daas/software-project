package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.MediaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComprehensiveMediaItemRepositoryTest {
    
    private JdbcMediaItemRepository mediaItemRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        mediaItemRepository = new JdbcMediaItemRepository();
        
        // Clean test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'COMP-TYPE-%'")) {
                pstmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testFindByType_BOOK_ReturnsOnlyBooks() {
        // Arrange
        MediaItem book1 = createMediaItem("COMP-TYPE-BOOK-001", "BOOK");
        MediaItem book2 = createMediaItem("COMP-TYPE-BOOK-002", "BOOK");
        MediaItem dvd = createMediaItem("COMP-TYPE-DVD-001", "DVD");
        MediaItem magazine = createMediaItem("COMP-TYPE-MAG-001", "MAGAZINE");
        
        // Act
        List<MediaItem> books = mediaItemRepository.findByType("BOOK");
        
        // Assert
        assertTrue(books.size() >= 2);
        assertTrue(books.stream().allMatch(item -> "BOOK".equals(item.getType())));
    }
    
    @Test
    void testFindByType_DVD_ReturnsOnlyDVDs() {
        // Arrange
        MediaItem dvd1 = createMediaItem("COMP-TYPE-DVD-002", "DVD");
        MediaItem dvd2 = createMediaItem("COMP-TYPE-DVD-003", "DVD");
        
        // Act
        List<MediaItem> dvds = mediaItemRepository.findByType("DVD");
        
        // Assert
        assertTrue(dvds.size() >= 2);
        assertTrue(dvds.stream().allMatch(item -> "DVD".equals(item.getType())));
    }
    
    @Test
    void testFindByType_NonExistent_ReturnsEmpty() {
        // Act
        List<MediaItem> items = mediaItemRepository.findByType("INVALID_TYPE");
        
        // Assert
        assertTrue(items.isEmpty());
    }
    
    @Test
    void testExistsByIsbn_ExistingIsbn_ReturnsTrue() {
        // Arrange
        MediaItem item = createMediaItem("COMP-TYPE-EXISTS-001", "BOOK");
        
        // Act
        boolean exists = mediaItemRepository.existsByIsbn("COMP-TYPE-EXISTS-001");
        
        // Assert
        assertTrue(exists);
    }
    
    @Test
    void testExistsByIsbn_NonExistentIsbn_ReturnsFalse() {
        // Act
        boolean exists = mediaItemRepository.existsByIsbn("COMP-TYPE-NONEXISTENT");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByIsbn_NullIsbn_ReturnsFalse() {
        // Act
        boolean exists = mediaItemRepository.existsByIsbn(null);
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testExistsByIsbn_EmptyIsbn_ReturnsFalse() {
        // Act
        boolean exists = mediaItemRepository.existsByIsbn("");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void testFindAll_MultipleItems_ReturnsAll() {
        // Arrange
        createMediaItem("COMP-TYPE-ALL-001", "BOOK");
        createMediaItem("COMP-TYPE-ALL-002", "DVD");
        createMediaItem("COMP-TYPE-ALL-003", "MAGAZINE");
        
        // Act
        List<MediaItem> allItems = mediaItemRepository.findAll();
        
        // Assert
        assertTrue(allItems.size() >= 3);
    }
    
    @Test
    void testFindByType_MAGAZINE_ReturnsOnlyMagazines() {
        // Arrange
        MediaItem mag1 = createMediaItem("COMP-TYPE-MAG-002", "MAGAZINE");
        MediaItem mag2 = createMediaItem("COMP-TYPE-MAG-003", "MAGAZINE");
        
        // Act
        List<MediaItem> magazines = mediaItemRepository.findByType("MAGAZINE");
        
        // Assert
        assertTrue(magazines.size() >= 2);
        assertTrue(magazines.stream().allMatch(item -> "MAGAZINE".equals(item.getType())));
    }
    
    // Helper method
    private MediaItem createMediaItem(String isbn, String type) {
        MediaItem item = new MediaItem();
        item.setTitle("Comprehensive Test " + type);
        item.setAuthor("Test Author");
        item.setType(type);
        item.setIsbn(isbn);
        item.setPublicationDate(LocalDate.of(2020, 1, 1));
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        return mediaItemRepository.save(item);
    }
}
