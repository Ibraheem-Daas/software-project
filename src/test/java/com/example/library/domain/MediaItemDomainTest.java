package com.example.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaItemDomainTest {
    
    @Test
    void testMediaItemConstructor() {
        MediaItem item = new MediaItem();
        assertNotNull(item);
    }
    
    @Test
    void testMediaItemSettersAndGetters() {
        MediaItem item = new MediaItem();
        
        item.setItemId(1);
        item.setTitle("The Great Gatsby");
        item.setAuthor("F. Scott Fitzgerald");
        item.setType("BOOK");
        item.setIsbn("9780743273565");
        item.setPublisher("Scribner");
        item.setTotalCopies(10);
        item.setAvailableCopies(7);
        
        assertEquals(1, item.getItemId());
        assertEquals("The Great Gatsby", item.getTitle());
        assertEquals("F. Scott Fitzgerald", item.getAuthor());
        assertEquals("BOOK", item.getType());
        assertEquals("9780743273565", item.getIsbn());
        assertEquals("Scribner", item.getPublisher());
        assertEquals(10, item.getTotalCopies());
        assertEquals(7, item.getAvailableCopies());
    }
    
    @Test
    void testMediaItemTypes() {
        MediaItem item = new MediaItem();
        
        item.setType("BOOK");
        assertEquals("BOOK", item.getType());
        
        item.setType("DVD");
        assertEquals("DVD", item.getType());
        
        item.setType("MAGAZINE");
        assertEquals("MAGAZINE", item.getType());
    }
    
    @Test
    void testCopiesManagement() {
        MediaItem item = new MediaItem();
        
        item.setTotalCopies(5);
        item.setAvailableCopies(5);
        
        assertEquals(5, item.getTotalCopies());
        assertEquals(5, item.getAvailableCopies());
        
        // Simulate borrowing
        item.setAvailableCopies(item.getAvailableCopies() - 1);
        assertEquals(4, item.getAvailableCopies());
        
        // Simulate returning
        item.setAvailableCopies(item.getAvailableCopies() + 1);
        assertEquals(5, item.getAvailableCopies());
    }
    
    @Test
    void testAvailability() {
        MediaItem item = new MediaItem();
        
        item.setAvailableCopies(0);
        assertEquals(0, item.getAvailableCopies());
        
        item.setAvailableCopies(1);
        assertTrue(item.getAvailableCopies() > 0);
    }
}
