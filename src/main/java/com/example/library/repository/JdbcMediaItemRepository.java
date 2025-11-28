package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.MediaItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMediaItemRepository implements MediaItemRepository {
    
    @Override
    public MediaItem save(MediaItem item) {
        String sql = "INSERT INTO media_item (title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getAuthor());
            pstmt.setString(3, item.getType());
            pstmt.setString(4, item.getIsbn());
            pstmt.setDate(5, item.getPublicationDate() != null ? Date.valueOf(item.getPublicationDate()) : null);
            pstmt.setString(6, item.getPublisher());
            pstmt.setInt(7, item.getTotalCopies());
            pstmt.setInt(8, item.getAvailableCopies());
            pstmt.setBigDecimal(9, item.getLateFeesPerDay());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new DataAccessException("Creating media item failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setItemId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("Creating media item failed, no ID obtained.");
                }
            }
            
            return item;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error saving media item: " + item.getTitle(), e);
        }
    }
    
    @Override
    public MediaItem update(MediaItem item) {
        String sql = "UPDATE media_item SET title = ?, author = ?, type = ?, isbn = ?, " +
                     "publication_date = ?, publisher = ?, total_copies = ?, available_copies = ?, " +
                     "late_fees_per_day = ? WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getAuthor());
            pstmt.setString(3, item.getType());
            pstmt.setString(4, item.getIsbn());
            pstmt.setDate(5, item.getPublicationDate() != null ? Date.valueOf(item.getPublicationDate()) : null);
            pstmt.setString(6, item.getPublisher());
            pstmt.setInt(7, item.getTotalCopies());
            pstmt.setInt(8, item.getAvailableCopies());
            pstmt.setBigDecimal(9, item.getLateFeesPerDay());
            pstmt.setInt(10, item.getItemId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new DataAccessException("Updating media item failed, no rows affected for itemId: " + item.getItemId());
            }
            
            return item;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error updating media item with id: " + item.getItemId(), e);
        }
    }
    
    @Override
    public Optional<MediaItem> findById(Integer itemId) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMediaItem(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding media item by id: " + itemId, e);
        }
    }
    
    @Override
    public Optional<MediaItem> findByIsbn(String isbn) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE isbn = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, isbn);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMediaItem(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding media item by isbn: " + isbn, e);
        }
    }
    
    @Override
    public List<MediaItem> findAll() {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(mapResultSetToMediaItem(rs));
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding all media items", e);
        }
    }
    
    @Override
    public List<MediaItem> findByType(String type) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE type = ? ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, type);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToMediaItem(rs));
                }
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding media items by type: " + type, e);
        }
    }
    
    @Override
    public List<MediaItem> findByTitleContaining(String title) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE title ILIKE ? ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + title + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToMediaItem(rs));
                }
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding media items by title: " + title, e);
        }
    }
    
    @Override
    public List<MediaItem> findByAuthorContaining(String author) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE author ILIKE ? ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + author + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToMediaItem(rs));
                }
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding media items by author: " + author, e);
        }
    }
    
    @Override
    public List<MediaItem> findAvailableItems() {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item WHERE available_copies > 0 ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(mapResultSetToMediaItem(rs));
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding available media items", e);
        }
    }
    
    @Override
    public List<MediaItem> search(String keyword) {
        String sql = "SELECT item_id, title, author, type, isbn, publication_date, publisher, " +
                     "total_copies, available_copies, late_fees_per_day FROM media_item " +
                     "WHERE title ILIKE ? OR author ILIKE ? OR isbn ILIKE ? OR type ILIKE ? ORDER BY item_id";
        List<MediaItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToMediaItem(rs));
                }
            }
            
            return items;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error searching media items with keyword: " + keyword, e);
        }
    }
    
    @Override
    public boolean deleteById(Integer itemId) {
        String sql = "DELETE FROM media_item WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting media item with id: " + itemId, e);
        }
    }
    
    @Override
    public void updateAvailableCopies(Integer itemId, Integer availableCopies) {
        String sql = "UPDATE media_item SET available_copies = ? WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, availableCopies);
            pstmt.setInt(2, itemId);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new DataAccessException("Updating available copies failed, no rows affected for itemId: " + itemId);
            }
            
        } catch (SQLException e) {
            throw new DataAccessException("Error updating available copies for item id: " + itemId, e);
        }
    }
    
    @Override
    public boolean existsByIsbn(String isbn) {
        String sql = "SELECT COUNT(*) FROM media_item WHERE isbn = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, isbn);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error checking if isbn exists: " + isbn, e);
        }
    }
    
    /**
     * Helper method to map a ResultSet row to a MediaItem object
     */
    private MediaItem mapResultSetToMediaItem(ResultSet rs) throws SQLException {
        MediaItem item = new MediaItem();
        item.setItemId(rs.getInt("item_id"));
        item.setTitle(rs.getString("title"));
        item.setAuthor(rs.getString("author"));
        item.setType(rs.getString("type"));
        item.setIsbn(rs.getString("isbn"));
        
        Date publicationDate = rs.getDate("publication_date");
        if (publicationDate != null) {
            item.setPublicationDate(publicationDate.toLocalDate());
        }
        
        item.setPublisher(rs.getString("publisher"));
        item.setTotalCopies(rs.getInt("total_copies"));
        item.setAvailableCopies(rs.getInt("available_copies"));
        item.setLateFeesPerDay(rs.getBigDecimal("late_fees_per_day"));
        
        return item;
    }
}
