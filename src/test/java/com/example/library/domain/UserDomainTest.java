package com.example.library.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserDomainTest {
    
    @Test
    void testUserConstructor() {
        User user = new User();
        assertNotNull(user);
    }
    
    @Test
    void testUserSettersAndGetters() {
        User user = new User();
        
        user.setUserId(1);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("securepass");
        user.setRole("MEMBER");
        
        assertEquals(1, user.getUserId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("securepass", user.getPassword());
        assertEquals("MEMBER", user.getRole());
    }
    
    @Test
    void testUserRoles() {
        User user = new User();
        
        user.setRole("MEMBER");
        assertEquals("MEMBER", user.getRole());
        
        user.setRole("ADMIN");
        assertEquals("ADMIN", user.getRole());
        
        user.setRole("LIBRARIAN");
        assertEquals("LIBRARIAN", user.getRole());
    }
    
    @Test
    void testUserEmailValidation() {
        User user = new User();
        user.setEmail("test@example.com");
        
        assertNotNull(user.getEmail());
        assertTrue(user.getEmail().contains("@"));
        assertTrue(user.getEmail().contains("."));
    }
    
    @Test
    void testUserPasswordHandling() {
        User user = new User();
        String password = "MySecurePassword123!";
        
        user.setPassword(password);
        
        assertNotNull(user.getPassword());
        assertEquals(password, user.getPassword());
    }
}
