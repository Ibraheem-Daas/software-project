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
}
