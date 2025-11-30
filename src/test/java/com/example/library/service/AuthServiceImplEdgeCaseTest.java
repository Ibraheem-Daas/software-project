package com.example.library.service;

import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplEdgeCaseTest {
    
    @Mock
    private UserRepository userRepository;
    
    private AuthServiceImpl authService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(userRepository);
    }
    
    @Test
    void testLogin_NullUsername() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login(null, "password");
        });
        
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    void testLogin_EmptyUsernameAfterTrim() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("   ", "password");
        });
        
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    void testLogin_NullPassword() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("user", null);
        });
        
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    void testLogin_EmptyPasswordAfterTrim() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("user", "   ");
        });
        
        verify(userRepository, never()).findByUsername(any());
    }
    
    @Test
    void testIsAdmin_UserWithNullRole() {
        com.example.library.domain.User user = new com.example.library.domain.User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setPassword("pass");
        user.setEmail("test@test.com");
        user.setRole(null); // Explicitly null role
        
        boolean result = authService.isAdmin(user);
        assertFalse(result, "User with null role should not be admin");
    }
    
    @Test
    void testIsAdmin_NullUser() {
        // Test when user parameter is null
        assertThrows(IllegalArgumentException.class, () -> {
            authService.isAdmin(null);
        }, "Should throw IllegalArgumentException for null user");
    }
}
