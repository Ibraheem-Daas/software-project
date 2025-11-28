package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.MediaItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcMediaItemRepositoryExceptionTest {
    
    private JdbcMediaItemRepository mediaItemRepository;
    
    @BeforeEach
    void setUp() throws SQLException {
        mediaItemRepository = new JdbcMediaItemRepository();
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'EXCEPTION-TEST-%'")) {
                stmt.executeUpdate();
            }
        }
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        // Clean up test data
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media_item WHERE isbn LIKE 'EXCEPTION-TEST-%'")) {
                stmt.executeUpdate();
            }
        }
    }
    
    @Test
    void testUpdate_NonExistentItem_ThrowsException() {
        MediaItem item = new MediaItem();
        item.setItemId(99999); // Non-existent ID
        item.setTitle("Exception Test");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("EXCEPTION-TEST-001");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        assertThrows(DataAccessException.class, () -> {
            mediaItemRepository.update(item);
        });
    }
    
    @Test
    void testSave_WithNullTitle_ThrowsException() {
        MediaItem item = new MediaItem();
        item.setTitle(null); // Null title
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("EXCEPTION-TEST-NULL-TITLE");
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        assertThrows(DataAccessException.class, () -> {
            mediaItemRepository.save(item);
        });
    }
    
    @Test
    void testUpdateAvailableCopies_NonExistentItem_ThrowsException() {
        assertThrows(DataAccessException.class, () -> {
            mediaItemRepository.updateAvailableCopies(99999, 5);
        });
    }
    
    @Test
    void testUpdateAvailableCopies_ValidItem_Success() throws SQLException {
        MediaItem item = new MediaItem();
        item.setTitle("Update Copies Test");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("UPDATE-" + UUID.randomUUID().toString().substring(0, 13));
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        MediaItem saved = mediaItemRepository.save(item);
        
        // Should allow updating available copies
        assertDoesNotThrow(() -> {
            mediaItemRepository.updateAvailableCopies(saved.getItemId(), 3);
        });
    }
    
    @Test
    void testDeleteById_ValidId_ReturnsTrue() throws SQLException {
        MediaItem item = new MediaItem();
        item.setTitle("Delete Test");
        item.setAuthor("Test Author");
        item.setType("BOOK");
        item.setIsbn("DELETE-" + UUID.randomUUID().toString().substring(0, 13));
        item.setPublicationDate(LocalDate.now());
        item.setPublisher("Test Publisher");
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        item.setLateFeesPerDay(new BigDecimal("1.00"));
        
        MediaItem saved = mediaItemRepository.save(item);
        
        boolean result = mediaItemRepository.deleteById(saved.getItemId());
        assertTrue(result);
    }
    
    @Test
    void testFindById_InvalidId_ReturnsEmpty() {
        var result = mediaItemRepository.findById(-1);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIsbn_NullIsbn_ReturnsEmpty() {
        var result = mediaItemRepository.findByIsbn(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByIsbn_NonExistent_ReturnsEmpty() {
        var result = mediaItemRepository.findByIsbn("NONEXISTENT-ISBN-12345");
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByType_NullType_ReturnsEmpty() {
        List<MediaItem> result = mediaItemRepository.findByType(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByTitleContaining_NullTitle_ReturnsEmpty() {
        List<MediaItem> result = mediaItemRepository.findByTitleContaining(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testFindByAuthorContaining_NullAuthor_ReturnsEmpty() {
        List<MediaItem> result = mediaItemRepository.findByAuthorContaining(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testSearch_NullKeyword_ReturnsEmpty() {
        List<MediaItem> result = mediaItemRepository.search(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testSearch_SpecialCharacters_Success() {
        List<MediaItem> result = mediaItemRepository.search("O'Brien & Co.");
        assertNotNull(result);
        // Should handle special characters without SQL injection
    }
    
    @Test
    void testExistsByIsbn_NullIsbn_ReturnsFalse() {
        boolean result = mediaItemRepository.existsByIsbn(null);
        assertFalse(result);
    }
}
