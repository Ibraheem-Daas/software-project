package com.example.library.ui;

import com.example.library.domain.Fine;
import com.example.library.domain.Loan;
import com.example.library.domain.MediaItem;
import com.example.library.domain.User;
import com.example.library.service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * User interface for the Library Management System.
 * Provides functionality for searching items, borrowing, returning, and managing fines.
 */
public class UserFrame extends JFrame {
    
    private final User currentUser;
    private final LibraryService libraryService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final com.example.library.repository.UserRepository userRepository;
    
    private JTabbedPane tabbedPane;
    
    public UserFrame(User currentUser, AuthService authService, LibraryService libraryService, PaymentService paymentService, com.example.library.repository.UserRepository userRepository) {
        this.currentUser = currentUser;
        this.authService = authService;
        this.libraryService = libraryService;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Library Management System - User Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Search Items", createSearchItemsPanel());
        tabbedPane.addTab("Borrow Item", createBorrowItemPanel());
        tabbedPane.addTab("My Active Loans", createActiveLoansPanel());
        tabbedPane.addTab("Return Item", createReturnItemPanel());
        tabbedPane.addTab("My Fines", createFinesPanel());
        tabbedPane.addTab("Profile", createProfilePanel());
        
        // Add tabbed pane to frame
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getUsername());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
    }
    
    private JPanel createSearchItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        JTextField searchField = new JTextField(30);
        searchPanel.add(searchField);
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchButton);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Item ID", "Title", "Author", "Type", "ISBN", "Publisher", "Available/Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Search action
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            searchItems(tableModel, keyword);
        });
        
        // Allow Enter key to trigger search
        searchField.addActionListener(e -> searchButton.doClick());
        
        // Load all items initially
        searchItems(tableModel, "");
        
        return panel;
    }
    
    private void searchItems(DefaultTableModel tableModel, String keyword) {
        try {
            List<MediaItem> items = libraryService.searchItems(keyword);
            
            // Clear table
            tableModel.setRowCount(0);
            
            // Add items to table
            for (MediaItem item : items) {
                Object[] row = {
                        item.getItemId(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getType(),
                        item.getIsbn() != null ? item.getIsbn() : "",
                        item.getPublisher() != null ? item.getPublisher() : "",
                        item.getAvailableCopies() + "/" + item.getTotalCopies()
                };
                tableModel.addRow(row);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error searching items: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createBorrowItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Form panel
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formPanel.add(new JLabel("Item ID:"));
        JTextField itemIdField = new JTextField(10);
        formPanel.add(itemIdField);
        JButton borrowButton = new JButton("Borrow Item");
        formPanel.add(borrowButton);
        
        panel.add(formPanel, BorderLayout.NORTH);
        
        // Info panel
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoArea.setText("Enter an Item ID and click 'Borrow Item' to borrow.\n\n" +
                "Note: You can borrow up to 3 items at a time.\n" +
                "You must not have overdue loans or unpaid fines.");
        JScrollPane scrollPane = new JScrollPane(infoArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Borrow action
        borrowButton.addActionListener(e -> {
            String itemIdStr = itemIdField.getText().trim();
            if (itemIdStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter an Item ID",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                int itemId = Integer.parseInt(itemIdStr);
                borrowItem(itemId, infoArea);
                itemIdField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Item ID must be a number",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }
    
    private void borrowItem(int itemId, JTextArea infoArea) {
        try {
            LocalDate today = LocalDate.now();
            Loan loan = libraryService.borrowItem(currentUser.getUserId(), itemId, today);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String message = "✓ Item borrowed successfully!\n\n" +
                    "Loan ID: " + loan.getLoanId() + "\n" +
                    "Item ID: " + loan.getItemId() + "\n" +
                    "Loan Date: " + loan.getLoanDate().format(formatter) + "\n" +
                    "Due Date: " + loan.getDueDate().format(formatter) + "\n" +
                    "Status: " + loan.getStatus();
            
            infoArea.setText(message);
            
            JOptionPane.showMessageDialog(this, "Item borrowed successfully!\nDue date: " + loan.getDueDate(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (BusinessException ex) {
            JOptionPane.showMessageDialog(this, "Cannot borrow item: " + ex.getMessage(),
                    "Business Error", JOptionPane.WARNING_MESSAGE);
            infoArea.setText("✗ Cannot borrow item:\n" + ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error borrowing item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            infoArea.setText("✗ Error:\n" + ex.getMessage());
        }
    }
    
    private JPanel createActiveLoansPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("My Active Loans - Potential Fines");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Loan ID", "Item ID", "Title", "Loan Date", "Due Date", "Days Until Due", "Late Fee/Day", "Return"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        
        // Add button column renderer
        table.getColumnModel().getColumn(7).setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            JButton button = new JButton("Return Now");
            return button;
        });
        
        // Add button column editor
        table.getColumnModel().getColumn(7).setCellEditor(new javax.swing.DefaultCellEditor(new JCheckBox()) {
            private JButton button = new JButton("Return Now");
            
            {
                button.addActionListener(e -> {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        int loanId = (Integer) tableModel.getValueAt(row, 0);
                        returnItemQuick(loanId, tableModel);
                    }
                    fireEditingStopped();
                });
            }
            
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                return button;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadActiveLoans(tableModel));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load initial data
        loadActiveLoans(tableModel);
        
        return panel;
    }
    
    private void loadActiveLoans(DefaultTableModel tableModel) {
        try {
            List<Loan> loans = libraryService.getUserLoans(currentUser.getUserId());
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();
            
            // Only show ACTIVE loans
            for (Loan loan : loans) {
                if ("ACTIVE".equals(loan.getStatus())) {
                    // Get item details
                    MediaItem item = libraryService.searchItems("").stream()
                            .filter(i -> i.getItemId() == loan.getItemId())
                            .findFirst()
                            .orElse(null);
                    
                    String itemTitle = item != null ? item.getTitle() : "Unknown";
                    LocalDate dueDate = loan.getDueDate();
                    long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
                    
                    String daysUntilDueStr;
                    if (daysUntilDue > 0) {
                        daysUntilDueStr = daysUntilDue + " days";
                    } else if (daysUntilDue == 0) {
                        daysUntilDueStr = "DUE TODAY!";
                    } else {
                        daysUntilDueStr = "OVERDUE " + Math.abs(daysUntilDue) + " days";
                    }
                    
                    // Get late fee from item
                    BigDecimal lateFee = item != null && item.getLateFeesPerDay() != null ? item.getLateFeesPerDay() : BigDecimal.ZERO;
                    
                    Object[] row = {
                        loan.getLoanId(),
                        loan.getItemId(),
                        itemTitle,
                        loan.getLoanDate().format(formatter),
                        dueDate.format(formatter),
                        daysUntilDueStr,
                        lateFee + " NIS",
                        "Return"
                    };
                    tableModel.addRow(row);
                }
            }
            
            if (tableModel.getRowCount() == 0) {
                Object[] emptyRow = {"No active loans", "", "", "", "", "", "", ""};
                tableModel.addRow(emptyRow);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading active loans: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void returnItemQuick(int loanId, DefaultTableModel tableModel) {
        try {
            libraryService.returnItem(loanId, LocalDate.now());
            JOptionPane.showMessageDialog(this, "Item returned successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            loadActiveLoans(tableModel); // Refresh
        } catch (BusinessException ex) {
            JOptionPane.showMessageDialog(this, "Cannot return item: " + ex.getMessage(),
                    "Business Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error returning item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createReturnItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Form panel
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formPanel.add(new JLabel("Loan ID:"));
        JTextField loanIdField = new JTextField(10);
        formPanel.add(loanIdField);
        JButton returnButton = new JButton("Return Item");
        formPanel.add(returnButton);
        JButton viewLoansButton = new JButton("View My Loans");
        formPanel.add(viewLoansButton);
        
        panel.add(formPanel, BorderLayout.NORTH);
        
        // Table for displaying user's loans
        String[] columns = {"Loan ID", "Item ID", "Loan Date", "Due Date", "Return Date", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Return action
        returnButton.addActionListener(e -> {
            String loanIdStr = loanIdField.getText().trim();
            if (loanIdStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Loan ID",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                int loanId = Integer.parseInt(loanIdStr);
                returnItem(loanId);
                loanIdField.setText("");
                loadUserLoans(tableModel); // Refresh loans table
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Loan ID must be a number",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // View loans action
        viewLoansButton.addActionListener(e -> loadUserLoans(tableModel));
        
        // Load user loans initially
        loadUserLoans(tableModel);
        
        return panel;
    }
    
    private void loadUserLoans(DefaultTableModel tableModel) {
        try {
            List<Loan> loans = libraryService.getUserLoans(currentUser.getUserId());
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add loans to table
            for (Loan loan : loans) {
                Object[] row = {
                        loan.getLoanId(),
                        loan.getItemId(),
                        loan.getLoanDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        loan.getReturnDate() != null ? loan.getReturnDate().format(formatter) : "",
                        loan.getStatus()
                };
                tableModel.addRow(row);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading loans: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void returnItem(int loanId) {
        try {
            LocalDate today = LocalDate.now();
            libraryService.returnItem(loanId, today);
            
            JOptionPane.showMessageDialog(this, "Item returned successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Check if fine was created
            List<Fine> fines = paymentService.getUnpaidFines(currentUser.getUserId());
            if (!fines.isEmpty()) {
                BigDecimal total = paymentService.getTotalUnpaid(currentUser.getUserId());
                JOptionPane.showMessageDialog(this,
                        "Item returned, but you have unpaid fines.\nTotal unpaid: " + total + " NIS\n" +
                                "Please check the 'My Fines' tab.",
                        "Fine Notice", JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (BusinessException ex) {
            JOptionPane.showMessageDialog(this, "Cannot return item: " + ex.getMessage(),
                    "Business Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error returning item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createFinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton payAllButton = new JButton("Pay All Fines");
        JButton paySelectedButton = new JButton("Pay Selected Fine");
        buttonPanel.add(refreshButton);
        buttonPanel.add(payAllButton);
        buttonPanel.add(paySelectedButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Fine ID", "Loan ID", "Amount (NIS)", "Issued Date", "Status", "Paid Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Total panel
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel totalLabel = new JLabel("Total Unpaid: 0.00 NIS");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalPanel.add(totalLabel);
        panel.add(totalPanel, BorderLayout.SOUTH);
        
        // Refresh action
        refreshButton.addActionListener(e -> loadFines(tableModel, totalLabel));
        
        // Pay all action
        payAllButton.addActionListener(e -> {
            payAllFines(tableModel, totalLabel);
        });
        
        // Pay selected action
        paySelectedButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a fine to pay",
                        "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int fineId = (int) tableModel.getValueAt(selectedRow, 0);
            String status = (String) tableModel.getValueAt(selectedRow, 4);
            
            if ("PAID".equals(status)) {
                JOptionPane.showMessageDialog(this, "This fine has already been paid",
                        "Payment Error", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            payFine(fineId, tableModel, totalLabel);
        });
        
        // Load fines initially
        loadFines(tableModel, totalLabel);
        
        return panel;
    }
    
    private void loadFines(DefaultTableModel tableModel, JLabel totalLabel) {
        try {
            List<Fine> fines = paymentService.getUnpaidFines(currentUser.getUserId());
            BigDecimal total = paymentService.getTotalUnpaid(currentUser.getUserId());
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add fines to table
            for (Fine fine : fines) {
                Object[] row = {
                        fine.getFineId(),
                        fine.getLoanId(),
                        String.format("%.2f", fine.getAmount()),
                        fine.getIssuedDate().format(formatter),
                        fine.getStatus(),
                        fine.getPaidDate() != null ? fine.getPaidDate().format(formatter) : ""
                };
                tableModel.addRow(row);
            }
            
            totalLabel.setText(String.format("Total Unpaid: %.2f NIS", total));
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void payAllFines(DefaultTableModel tableModel, JLabel totalLabel) {
        try {
            BigDecimal total = paymentService.getTotalUnpaid(currentUser.getUserId());
            
            if (total.compareTo(BigDecimal.ZERO) == 0) {
                JOptionPane.showMessageDialog(this, "You have no unpaid fines.",
                        "Payment Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int choice = JOptionPane.showConfirmDialog(this,
                    String.format("Pay all fines?\nTotal amount: %.2f NIS", total),
                    "Confirm Payment", JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                paymentService.payAllFinesForUser(currentUser.getUserId());
                JOptionPane.showMessageDialog(this, "All fines paid successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                loadFines(tableModel, totalLabel);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error paying fines: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void payFine(int fineId, DefaultTableModel tableModel, JLabel totalLabel) {
        try {
            paymentService.payFine(fineId);
            JOptionPane.showMessageDialog(this, "Fine paid successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            loadFines(tableModel, totalLabel);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error paying fine: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Profile info panel
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("User Profile");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        infoPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        
        // User ID
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        infoPanel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        infoPanel.add(new JLabel(String.valueOf(currentUser.getUserId())), gbc);
        
        gbc.gridy++;
        
        // Username
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        infoPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        infoPanel.add(new JLabel(currentUser.getUsername()), gbc);
        
        gbc.gridy++;
        
        // Email
        gbc.gridx = 0;
        infoPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(new JLabel(currentUser.getEmail()), gbc);
        
        gbc.gridy++;
        
        // Role
        gbc.gridx = 0;
        infoPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        JLabel roleLabel = new JLabel(currentUser.getRole());
        roleLabel.setForeground(new Color(0, 128, 0));
        roleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(roleLabel, gbc);
        
        gbc.gridy++;
        
        // Created At
        gbc.gridx = 0;
        infoPanel.add(new JLabel("Member Since:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(new JLabel(currentUser.getCreatedAt().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))), gbc);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Logout button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(150, 35));
        logoutButton.addActionListener(e -> logout());
        buttonPanel.add(logoutButton);
        
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            // Reopen login frame
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame(authService, libraryService, paymentService, userRepository, null, null, null);
                loginFrame.setVisible(true);
            });
        }
    }
}
