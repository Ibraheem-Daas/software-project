package com.example.library.ui;

import com.example.library.domain.User;
import com.example.library.service.*;

import javax.swing.*;
import java.awt.*;

/**
 * Login window for the Library Management System.
 * Authenticates users and routes them to appropriate interface based on role.
 */
public class LoginFrame extends JFrame {
    
    private final AuthService authService;
    private final LibraryService libraryService;
    private final PaymentService paymentService;
    private final com.example.library.repository.UserRepository userRepository;
    private final com.example.library.repository.MediaItemRepository mediaItemRepository;
    private final com.example.library.repository.FineRepository fineRepository;
    private final com.example.library.repository.LoanRepository loanRepository;
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    
    // Constructor for testing with minimal dependencies
    public LoginFrame(AuthService authService) {
        this(authService, null, null, null, null, null, null);
    }
    
    public LoginFrame(AuthService authService, LibraryService libraryService, PaymentService paymentService, 
                      com.example.library.repository.UserRepository userRepository,
                      com.example.library.repository.MediaItemRepository mediaItemRepository,
                      com.example.library.repository.FineRepository fineRepository,
                      com.example.library.repository.LoanRepository loanRepository) {
        this.authService = authService;
        this.libraryService = libraryService;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.mediaItemRepository = mediaItemRepository;
        this.fineRepository = fineRepository;
        this.loanRepository = loanRepository;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Library Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);
        
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Title label
        JLabel titleLabel = new JLabel("Library Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Form panel with GridBagLayout for better alignment
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Username label and field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel usernameLabel = new JLabel("Username:");
        formPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        usernameField = new JTextField(20);
        usernameField.setName("usernameField"); // For GUI testing
        formPanel.add(usernameField, gbc);
        
        // Password label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        JLabel passwordLabel = new JLabel("Password:");
        formPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        passwordField = new JPasswordField(20);
        passwordField.setName("passwordField"); // For GUI testing
        formPanel.add(passwordField, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Login");
        loginButton.setName("loginButton"); // For GUI testing
        loginButton.setPreferredSize(new Dimension(100, 30));
        loginButton.addActionListener(e -> performLogin());
        buttonPanel.add(loginButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Allow Enter key to trigger login
        getRootPane().setDefaultButton(loginButton);
        
        // Request focus on username field when window opens
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
    }
    
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Validate input
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your username.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your password.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
            passwordField.requestFocus();
            return;
        }
        
        // Attempt authentication
        try {
            User user = authService.login(username, password);
            
            // Clear password from memory
            passwordField.setText("");
            
            // Hide login frame
            setVisible(false);
            
            // Open appropriate frame based on role
            if (authService.isAdmin(user)) {
                AdminFrame adminFrame = new AdminFrame(user, authService, libraryService, paymentService, userRepository, mediaItemRepository, fineRepository, loanRepository);
                adminFrame.setVisible(true);
            } else {
                UserFrame userFrame = new UserFrame(user, authService, libraryService, paymentService, userRepository);
                userFrame.setVisible(true);
            }
            
            // Dispose login frame
            dispose();
            
        } catch (AuthenticationException ex) {
            // Show error message
            JOptionPane.showMessageDialog(
                    this,
                    "Login failed: " + ex.getMessage(),
                    "Authentication Error",
                    JOptionPane.ERROR_MESSAGE
            );
            
            // Clear fields and focus on username
            passwordField.setText("");
            usernameField.requestFocus();
        }
    }
}
