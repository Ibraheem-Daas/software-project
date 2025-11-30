package com.example.library.ui;

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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Admin interface for the Library Management System.
 * Provides functionality for adding media items, searching, and viewing overdue loans.
 */
public class AdminFrame extends JFrame {
    
    private final User currentUser;
    private final LibraryService libraryService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final com.example.library.repository.UserRepository userRepository;
    private final com.example.library.repository.MediaItemRepository mediaItemRepository;
    private final com.example.library.repository.FineRepository fineRepository;
    private final com.example.library.repository.LoanRepository loanRepository;
    
    private JTabbedPane tabbedPane;
    
    public AdminFrame(User currentUser, AuthService authService, LibraryService libraryService, PaymentService paymentService, com.example.library.repository.UserRepository userRepository, com.example.library.repository.MediaItemRepository mediaItemRepository, com.example.library.repository.FineRepository fineRepository, com.example.library.repository.LoanRepository loanRepository) {
        this.currentUser = currentUser;
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
        setTitle("Library Management System - Admin Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Add tabs
        tabbedPane.addTab("Add Media Item", createAddMediaItemPanel());
        tabbedPane.addTab("Search Items", createSearchItemsPanel());
        tabbedPane.addTab("User Management", createUserManagementPanel());
        tabbedPane.addTab("User Loans", createUserLoansPanel());
        tabbedPane.addTab("Overdue Loans", createOverdueLoansPanel());
        tabbedPane.addTab("Fines Overview", createFinesOverviewPanel());
        tabbedPane.addTab("Profile", createProfilePanel());
        
        // Add tabbed pane to frame
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getUsername() + " (Admin)");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
    }
    
    private JPanel createAddMediaItemPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Form fields
        JTextField titleField = new JTextField(30);
        JTextField authorField = new JTextField(30);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"BOOK", "CD"});
        JTextField isbnField = new JTextField(30);
        JTextField publisherField = new JTextField(30);
        JTextField publicationDateField = new JTextField(30);
        JTextField totalCopiesField = new JTextField(30);
        JTextField lateFeesField = new JTextField(30);
        
        // Add fields to form
        int row = 0;
        addFormField(formPanel, gbc, row++, "Title*:", titleField);
        addFormField(formPanel, gbc, row++, "Author*:", authorField);
        addFormField(formPanel, gbc, row++, "Type*:", typeCombo);
        addFormField(formPanel, gbc, row++, "ISBN:", isbnField);
        addFormField(formPanel, gbc, row++, "Publisher:", publisherField);
        addFormField(formPanel, gbc, row++, "Publication Date (YYYY-MM-DD):", publicationDateField);
        addFormField(formPanel, gbc, row++, "Total Copies*:", totalCopiesField);
        addFormField(formPanel, gbc, row++, "Late Fee Per Day*:", lateFeesField);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Save Media Item");
        JButton clearButton = new JButton("Clear");
        
        saveButton.addActionListener(e -> {
            saveMediaItem(titleField, authorField, typeCombo, isbnField, publisherField,
                    publicationDateField, totalCopiesField, lateFeesField);
        });
        
        clearButton.addActionListener(e -> {
            titleField.setText("");
            authorField.setText("");
            typeCombo.setSelectedIndex(0);
            isbnField.setText("");
            publisherField.setText("");
            publicationDateField.setText("");
            totalCopiesField.setText("");
            lateFeesField.setText("");
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }
    
    private void saveMediaItem(JTextField titleField, JTextField authorField, JComboBox<String> typeCombo,
                                JTextField isbnField, JTextField publisherField, JTextField publicationDateField,
                                JTextField totalCopiesField, JTextField lateFeesField) {
        try {
            // Validate required fields
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String totalCopiesStr = totalCopiesField.getText().trim();
            String lateFeesStr = lateFeesField.getText().trim();
            
            if (title.isEmpty() || author.isEmpty() || totalCopiesStr.isEmpty() || lateFeesStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields (*)",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse values
            int totalCopies = Integer.parseInt(totalCopiesStr);
            BigDecimal lateFees = new BigDecimal(lateFeesStr);
            
            if (totalCopies <= 0) {
                JOptionPane.showMessageDialog(this, "Total copies must be greater than 0",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (lateFees.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "Late fees cannot be negative",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Note: When adding a new item, available copies = total copies by default
            
            // Create media item
            MediaItem item = new MediaItem();
            item.setTitle(title);
            item.setAuthor(author);
            item.setType(type);
            item.setIsbn(isbnField.getText().trim());
            item.setPublisher(publisherField.getText().trim());
            
            // Parse publication date if provided
            String pubDateStr = publicationDateField.getText().trim();
            if (!pubDateStr.isEmpty()) {
                try {
                    LocalDate pubDate = LocalDate.parse(pubDateStr);
                    item.setPublicationDate(pubDate);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            item.setTotalCopies(totalCopies);
            item.setAvailableCopies(totalCopies);
            item.setLateFeesPerDay(lateFees);
            
            // Save to database
            MediaItem saved = libraryService.addMediaItem(item);
            
            JOptionPane.showMessageDialog(this,
                    "Media item added successfully!\nItem ID: " + saved.getItemId(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear form
            titleField.setText("");
            authorField.setText("");
            typeCombo.setSelectedIndex(0);
            isbnField.setText("");
            publisherField.setText("");
            publicationDateField.setText("");
            totalCopiesField.setText("");
            lateFeesField.setText("");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format: " + ex.getMessage(),
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding media item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
        JButton showAllButton = new JButton("Show All");
        searchPanel.add(showAllButton);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"ID", "Title", "Author", "Type", "ISBN", "Publisher", "Available/Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Action buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setPreferredSize(new Dimension(120, 30));
        viewDetailsButton.addActionListener(e -> viewItemDetails(table));
        buttonPanel.add(viewDetailsButton);
        
        JButton editButton = new JButton("Edit Item");
        editButton.setPreferredSize(new Dimension(120, 30));
        editButton.addActionListener(e -> editItem(table, tableModel));
        buttonPanel.add(editButton);
        
        JButton deleteButton = new JButton("Delete Item");
        deleteButton.setPreferredSize(new Dimension(120, 30));
        deleteButton.addActionListener(e -> deleteItem(table, tableModel));
        buttonPanel.add(deleteButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Search action
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            searchItems(tableModel, keyword);
        });
        
        // Show All action
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            searchItems(tableModel, "");
        });
        
        // Allow Enter key to trigger search
        searchField.addActionListener(e -> searchButton.doClick());
        
        // Load all items initially
        searchItems(tableModel, "");
        
        return panel;
    }
    
    private void searchItems(DefaultTableModel tableModel, String keyword) {
        try {
            List<MediaItem> items;
            
            // If keyword is a number, search by exact ID
            if (keyword.trim().matches("\\d+")) {
                try {
                    int itemId = Integer.parseInt(keyword.trim());
                    Optional<MediaItem> item = mediaItemRepository.findById(itemId);
                    items = item.isPresent() ? java.util.Collections.singletonList(item.get()) : new ArrayList<>();
                } catch (NumberFormatException e) {
                    items = new ArrayList<>();
                }
            } else {
                // Otherwise, search by keyword across title, author, ISBN, type
                items = libraryService.searchItems(keyword);
            }
            
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
    
    private void viewItemDetails(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item first",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int itemId = (int) table.getValueAt(selectedRow, 0);
            MediaItem item = mediaItemRepository.findById(itemId).orElse(null);
            
            if (item == null) {
                JOptionPane.showMessageDialog(this, "Item not found",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create details dialog
            JDialog dialog = new JDialog(this, "Item Details", true);
            dialog.setSize(500, 450);
            dialog.setLocationRelativeTo(this);
            
            JPanel detailsPanel = new JPanel(new GridBagLayout());
            detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            
            int row = 0;
            addDetailField(detailsPanel, gbc, row++, "Item ID:", String.valueOf(item.getItemId()));
            addDetailField(detailsPanel, gbc, row++, "Title:", item.getTitle());
            addDetailField(detailsPanel, gbc, row++, "Author:", item.getAuthor());
            addDetailField(detailsPanel, gbc, row++, "Type:", item.getType());
            addDetailField(detailsPanel, gbc, row++, "ISBN:", item.getIsbn() != null ? item.getIsbn() : "N/A");
            addDetailField(detailsPanel, gbc, row++, "Publisher:", item.getPublisher() != null ? item.getPublisher() : "N/A");
            addDetailField(detailsPanel, gbc, row++, "Publication Date:", 
                    item.getPublicationDate() != null ? item.getPublicationDate().toString() : "N/A");
            addDetailField(detailsPanel, gbc, row++, "Total Copies:", String.valueOf(item.getTotalCopies()));
            addDetailField(detailsPanel, gbc, row++, "Available Copies:", String.valueOf(item.getAvailableCopies()));
            addDetailField(detailsPanel, gbc, row++, "Late Fee Per Day:", "$" + item.getLateFeesPerDay());
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            detailsPanel.add(closeButton, gbc);
            
            dialog.add(detailsPanel);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error viewing item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addDetailField(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; gbc.gridwidth = 1;
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(labelComp, gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(new JLabel(value), gbc);
    }
    
    private void editItem(JTable table, DefaultTableModel tableModel) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item first",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int itemId = (int) table.getValueAt(selectedRow, 0);
            MediaItem item = mediaItemRepository.findById(itemId).orElse(null);
            
            if (item == null) {
                JOptionPane.showMessageDialog(this, "Item not found",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create edit dialog
            JDialog dialog = new JDialog(this, "Edit Item", true);
            dialog.setSize(500, 550);
            dialog.setLocationRelativeTo(this);
            
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            JTextField titleField = new JTextField(item.getTitle(), 25);
            JTextField authorField = new JTextField(item.getAuthor(), 25);
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"BOOK", "CD", "DVD"});
            typeCombo.setSelectedItem(item.getType());
            JTextField isbnField = new JTextField(item.getIsbn() != null ? item.getIsbn() : "", 25);
            JTextField publisherField = new JTextField(item.getPublisher() != null ? item.getPublisher() : "", 25);
            JTextField pubDateField = new JTextField(item.getPublicationDate() != null ? item.getPublicationDate().toString() : "", 25);
            JTextField totalCopiesField = new JTextField(String.valueOf(item.getTotalCopies()), 25);
            JTextField availableCopiesField = new JTextField(String.valueOf(item.getAvailableCopies()), 25);
            JTextField lateFeesField = new JTextField(item.getLateFeesPerDay().toString(), 25);
            
            int row = 0;
            addFormField(formPanel, gbc, row++, "Title*:", titleField);
            addFormField(formPanel, gbc, row++, "Author*:", authorField);
            addFormField(formPanel, gbc, row++, "Type*:", typeCombo);
            addFormField(formPanel, gbc, row++, "ISBN:", isbnField);
            addFormField(formPanel, gbc, row++, "Publisher:", publisherField);
            addFormField(formPanel, gbc, row++, "Publication Date (YYYY-MM-DD):", pubDateField);
            addFormField(formPanel, gbc, row++, "Total Copies*:", totalCopiesField);
            addFormField(formPanel, gbc, row++, "Available Copies*:", availableCopiesField);
            addFormField(formPanel, gbc, row++, "Late Fee Per Day*:", lateFeesField);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton saveButton = new JButton("Save Changes");
            JButton cancelButton = new JButton("Cancel");
            
            saveButton.addActionListener(e -> {
                try {
                    item.setTitle(titleField.getText().trim());
                    item.setAuthor(authorField.getText().trim());
                    item.setType((String) typeCombo.getSelectedItem());
                    item.setIsbn(isbnField.getText().trim());
                    item.setPublisher(publisherField.getText().trim());
                    
                    String pubDate = pubDateField.getText().trim();
                    if (!pubDate.isEmpty()) {
                        item.setPublicationDate(LocalDate.parse(pubDate));
                    }
                    
                    int totalCopies = Integer.parseInt(totalCopiesField.getText().trim());
                    int availableCopies = Integer.parseInt(availableCopiesField.getText().trim());
                    
                    // Validation: available copies cannot exceed total copies
                    if (availableCopies > totalCopies) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Available copies (" + availableCopies + ") cannot exceed total copies (" + totalCopies + ")",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    if (availableCopies < 0 || totalCopies <= 0) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Total copies must be greater than 0 and available copies cannot be negative",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    item.setTotalCopies(totalCopies);
                    item.setAvailableCopies(availableCopies);
                    item.setLateFeesPerDay(new BigDecimal(lateFeesField.getText().trim()));
                    
                    mediaItemRepository.update(item);
                    
                    JOptionPane.showMessageDialog(dialog, "Item updated successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh table
                    searchItems(tableModel, "");
                    dialog.dispose();
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error updating item: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            
            cancelButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
            formPanel.add(buttonPanel, gbc);
            
            dialog.add(formPanel);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error editing item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteItem(JTable table, DefaultTableModel tableModel) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item first",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int itemId = (int) table.getValueAt(selectedRow, 0);
            String title = (String) table.getValueAt(selectedRow, 1);
            
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete:\n" + title + " (ID: " + itemId + ")?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                mediaItemRepository.deleteById(itemId);
                
                JOptionPane.showMessageDialog(this, "Item deleted successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh table
                searchItems(tableModel, "");
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting item: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createOverdueLoansPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        headerPanel.add(refreshButton);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Loan ID", "User ID", "Item ID", "Loan Date", "Due Date", "Days Overdue", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh action
        refreshButton.addActionListener(e -> loadOverdueLoans(tableModel));
        
        // Load overdue loans initially
        loadOverdueLoans(tableModel);
        
        return panel;
    }
    
    private void loadOverdueLoans(DefaultTableModel tableModel) {
        try {
            LocalDate today = LocalDate.now();
            List<Loan> overdueLoans = libraryService.getOverdueLoans(today);
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add loans to table
            for (Loan loan : overdueLoans) {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), today);
                Object[] row = {
                        loan.getLoanId(),
                        loan.getUserId(),
                        loan.getItemId(),
                        loan.getLoanDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        daysOverdue,
                        loan.getStatus()
                };
                tableModel.addRow(row);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading overdue loans: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createUserLoansPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Filter by User ID:"));
        JTextField userIdField = new JTextField(10);
        inputPanel.add(userIdField);
        JButton searchButton = new JButton("Search");
        inputPanel.add(searchButton);
        JButton showAllButton = new JButton("Show All");
        inputPanel.add(showAllButton);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Loan ID", "User ID", "Item ID", "Loan Date", "Due Date", "Return Date", "Status"};
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
            String userIdStr = userIdField.getText().trim();
            if (userIdStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User ID to search",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                int userId = Integer.parseInt(userIdStr);
                loadUserLoansForAdmin(tableModel, userId);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "User ID must be a number",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Show All action
        showAllButton.addActionListener(e -> {
            userIdField.setText("");
            loadAllLoansForAdmin(tableModel);
        });
        
        // Load all loans initially
        loadAllLoansForAdmin(tableModel);
        
        return panel;
    }
    
    private void loadUserLoansForAdmin(DefaultTableModel tableModel, int userId) {
        try {
            List<Loan> loans = libraryService.getUserLoans(userId);
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add loans to table
            for (Loan loan : loans) {
                Object[] row = {
                        loan.getLoanId(),
                        loan.getUserId(),
                        loan.getItemId(),
                        loan.getLoanDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        loan.getReturnDate() != null ? loan.getReturnDate().format(formatter) : "",
                        loan.getStatus()
                };
                tableModel.addRow(row);
            }
            
            if (loans.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No loans found for user ID: " + userId,
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading user loans: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAllLoansForAdmin(DefaultTableModel tableModel) {
        try {
            // Get all loans from all users
            List<Loan> allLoans = new ArrayList<>();
            
            // Get all users and their loans
            List<User> users = userRepository.findAll();
            for (User user : users) {
                List<Loan> userLoans = libraryService.getUserLoans(user.getUserId());
                allLoans.addAll(userLoans);
            }
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add all loans to table
            for (Loan loan : allLoans) {
                Object[] row = {
                        loan.getLoanId(),
                        loan.getUserId(),
                        loan.getItemId(),
                        loan.getLoanDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        loan.getReturnDate() != null ? loan.getReturnDate().format(formatter) : "",
                        loan.getStatus()
                };
                tableModel.addRow(row);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading all loans: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createFinesOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Filter by User ID:"));
        JTextField userIdField = new JTextField(10);
        inputPanel.add(userIdField);
        JButton searchButton = new JButton("Search");
        inputPanel.add(searchButton);
        JButton showAllButton = new JButton("Show All");
        inputPanel.add(showAllButton);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"Fine ID", "User ID", "Loan ID", "Amount (NIS)", "Issued Date", "Status", "Paid Date"};
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
        
        // Search action
        searchButton.addActionListener(e -> {
            String userIdStr = userIdField.getText().trim();
            if (userIdStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a User ID to search",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                int userId = Integer.parseInt(userIdStr);
                loadFinesForAdmin(tableModel, totalLabel, userId);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "User ID must be a number",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Show All action
        showAllButton.addActionListener(e -> {
            userIdField.setText("");
            loadAllFinesForAdmin(tableModel, totalLabel);
        });
        
        // Load all fines initially
        loadAllFinesForAdmin(tableModel, totalLabel);
        
        return panel;
    }
    
    private void loadFinesForAdmin(DefaultTableModel tableModel, JLabel totalLabel, int searchUserId) {
        try {
            List<com.example.library.domain.Fine> allFines = fineRepository.findAll();
            List<com.example.library.domain.Fine> filteredFines = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            
            // Filter fines by userId
            for (com.example.library.domain.Fine fine : allFines) {
                Optional<Loan> loanOpt = loanRepository.findById(fine.getLoanId());
                if (loanOpt.isPresent() && loanOpt.get().getUserId().equals(searchUserId)) {
                    filteredFines.add(fine);
                    if ("UNPAID".equals(fine.getStatus())) {
                        total = total.add(fine.getAmount());
                    }
                }
            }
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add fines to table
            for (com.example.library.domain.Fine fine : filteredFines) {
                Object[] row = {
                        fine.getFineId(),
                        searchUserId,
                        fine.getLoanId(),
                        String.format("%.2f", fine.getAmount()),
                        fine.getIssuedDate().format(formatter),
                        fine.getStatus(),
                        fine.getPaidDate() != null ? fine.getPaidDate().format(formatter) : ""
                };
                tableModel.addRow(row);
            }
            
            totalLabel.setText(String.format("Total Unpaid: %.2f NIS", total));
            
            if (filteredFines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No fines found for user ID: " + searchUserId,
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAllFinesForAdmin(DefaultTableModel tableModel, JLabel totalLabel) {
        try {
            // Get all fines
            List<com.example.library.domain.Fine> allFines = fineRepository.findAll();
            BigDecimal totalUnpaid = BigDecimal.ZERO;
            
            // Clear table
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            // Add all fines to table
            for (com.example.library.domain.Fine fine : allFines) {
                // Get userId from loan
                Integer userId = null;
                Optional<Loan> loanOpt = loanRepository.findById(fine.getLoanId());
                if (loanOpt.isPresent()) {
                    userId = loanOpt.get().getUserId();
                }
                
                Object[] row = {
                        fine.getFineId(),
                        userId != null ? userId : "N/A",
                        fine.getLoanId(),
                        String.format("%.2f", fine.getAmount()),
                        fine.getIssuedDate().format(formatter),
                        fine.getStatus(),
                        fine.getPaidDate() != null ? fine.getPaidDate().format(formatter) : ""
                };
                tableModel.addRow(row);
                
                // Calculate total unpaid
                if ("UNPAID".equals(fine.getStatus())) {
                    totalUnpaid = totalUnpaid.add(fine.getAmount());
                }
            }
            
            totalLabel.setText(String.format("Total Unpaid: %.2f NIS", totalUnpaid));
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading all fines: " + ex.getMessage(),
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
        JLabel titleLabel = new JLabel("Administrator Profile");
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
        roleLabel.setForeground(Color.BLUE);
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
    
    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Top panel with button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addUserButton = new JButton("Add New User");
        addUserButton.setFont(new Font("Arial", Font.BOLD, 14));
        addUserButton.setPreferredSize(new Dimension(150, 35));
        addUserButton.addActionListener(e -> createNewUser());
        topPanel.add(addUserButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshUserTable());
        topPanel.add(refreshButton);
        
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Table to display users
        String[] columns = {"User ID", "Username", "Email", "Role", "Created At"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(userTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Store table model for refresh
        panel.putClientProperty("tableModel", tableModel);
        
        // Load users initially
        loadUsers(tableModel);
        
        return panel;
    }
    
    private void loadUsers(DefaultTableModel tableModel) {
        try {
            List<User> users = userRepository.findAll();
            tableModel.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (User user : users) {
                Object[] row = {
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCreatedAt().format(formatter)
                };
                tableModel.addRow(row);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshUserTable() {
        JPanel userManagementPanel = (JPanel) tabbedPane.getComponentAt(2); // User Management is 3rd tab (index 2)
        DefaultTableModel tableModel = (DefaultTableModel) userManagementPanel.getClientProperty("tableModel");
        loadUsers(tableModel);
    }
    
    private void createNewUser() {
        JDialog dialog = new JDialog(this, "Create New User", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Username field
        JTextField usernameField = new JTextField(20);
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);
        
        // Password field
        JPasswordField passwordField = new JPasswordField(20);
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);
        
        // Confirm password field
        JPasswordField confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Confirm Password*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(confirmPasswordField, gbc);
        
        // Email field
        JTextField emailField = new JTextField(20);
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Email*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);
        
        // Role selection
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Role*:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleCombo, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Create User");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            String email = emailField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            
            // Validation
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all required fields (*)",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog, "Passwords do not match",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!email.contains("@")) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid email address",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Check if username already exists
                if (userRepository.findByUsername(username).isPresent()) {
                    JOptionPane.showMessageDialog(dialog, "Username already exists",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check if email already exists
                if (userRepository.findByEmail(email).isPresent()) {
                    JOptionPane.showMessageDialog(dialog, "Email already exists",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Create new user
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setPassword(password);
                newUser.setEmail(email);
                newUser.setRole(role);
                newUser.setCreatedAt(java.time.LocalDateTime.now());
                
                User savedUser = userRepository.save(newUser);
                
                JOptionPane.showMessageDialog(dialog,
                        "User created successfully!\nUser ID: " + savedUser.getUserId(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh user table
                refreshUserTable();
                
                dialog.dispose();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating user: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);
        
        dialog.add(formPanel);
        dialog.setVisible(true);
    }
    
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            // Reopen login frame
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame(authService, libraryService, paymentService, userRepository, mediaItemRepository, fineRepository, loanRepository);
                loginFrame.setVisible(true);
            });
        }
    }
}
