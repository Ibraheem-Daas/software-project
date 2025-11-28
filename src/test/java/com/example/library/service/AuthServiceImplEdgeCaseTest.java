package com.example.library.service;

import com.example.library.domain.User;
import com.example.library.exception.AuthenticationException;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

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
    void testLogin_UsernameWithWhitespace_Trimmed() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setPassword("password123");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        User result = authService.login("  testuser  ", "password123");
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }
    
    @Test
    void testLogin_PasswordWithWhitespace_Trimmed() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setPassword("password123");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        User result = authService.login("testuser", "  password123  ");
        
        assertNotNull(result);
        verify(userRepository).findByUsername("testuser");
    }
    
    @Test
    void testRegister_NullUser() {
        assertThrows(AuthenticationException.class, () -> {
            authService.register(null);
        });
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testRegister_UsernameExists() {
        User user = new User();
        user.setUsername("existing");
        user.setEmail("new@example.com");
        user.setPassword("password");
        
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        
        assertThrows(AuthenticationException.class, () -> {
            authService.register(user);
        });
        
        verify(userRepository).existsByUsername("existing");
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void testRegister_EmailExists() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("existing@example.com");
        user.setPassword("password");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        assertThrows(AuthenticationException.class, () -> {
            authService.register(user);
        });
        
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
    }
}
