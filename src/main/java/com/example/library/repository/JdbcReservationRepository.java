package com.example.library.repository;

import com.example.library.DatabaseConnection;
import com.example.library.domain.Reservation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ReservationRepository.
 */
public class JdbcReservationRepository implements ReservationRepository {
    
    @Override
    public Reservation save(Reservation reservation) {
        String sql = "INSERT INTO reservation (user_id, item_id, reservation_date, expiry_date, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, reservation.getUserId());
            pstmt.setInt(2, reservation.getItemId());
            pstmt.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            pstmt.setTimestamp(4, Timestamp.valueOf(reservation.getExpiryDate()));
            pstmt.setString(5, reservation.getStatus());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setReservationId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("Creating reservation failed, no ID obtained.");
                }
            }
            
            return reservation;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error saving reservation", e);
        }
    }
    
    @Override
    public Reservation update(Reservation reservation) {
        String sql = "UPDATE reservation SET user_id = ?, item_id = ?, reservation_date = ?, " +
                     "expiry_date = ?, status = ? WHERE reservation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reservation.getUserId());
            pstmt.setInt(2, reservation.getItemId());
            pstmt.setTimestamp(3, Timestamp.valueOf(reservation.getReservationDate()));
            pstmt.setTimestamp(4, Timestamp.valueOf(reservation.getExpiryDate()));
            pstmt.setString(5, reservation.getStatus());
            pstmt.setInt(6, reservation.getReservationId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Updating reservation failed, no rows affected.");
            }
            
            return reservation;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error updating reservation", e);
        }
    }
    
    @Override
    public Optional<Reservation> findById(Integer reservationId) {
        String sql = "SELECT * FROM reservation WHERE reservation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reservationId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReservation(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding reservation by ID", e);
        }
    }
    
    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservation ORDER BY reservation_date DESC";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding all reservations", e);
        }
    }
    
    @Override
    public List<Reservation> findByUserId(Integer userId) {
        String sql = "SELECT * FROM reservation WHERE user_id = ? ORDER BY reservation_date DESC";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding reservations by user ID", e);
        }
    }
    
    @Override
    public List<Reservation> findByItemId(Integer itemId) {
        String sql = "SELECT * FROM reservation WHERE item_id = ? ORDER BY reservation_date";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding reservations by item ID", e);
        }
    }
    
    @Override
    public List<Reservation> findActiveByItemId(Integer itemId) {
        String sql = "SELECT * FROM reservation WHERE item_id = ? AND status = 'ACTIVE' " +
                     "ORDER BY reservation_date";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding active reservations by item ID", e);
        }
    }
    
    @Override
    public List<Reservation> findExpiredReservations(LocalDateTime currentDateTime) {
        String sql = "SELECT * FROM reservation WHERE status = 'ACTIVE' AND expiry_date < ?";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, Timestamp.valueOf(currentDateTime));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding expired reservations", e);
        }
    }
    
    @Override
    public List<Reservation> findActiveByUserId(Integer userId) {
        String sql = "SELECT * FROM reservation WHERE user_id = ? AND status = 'ACTIVE' " +
                     "ORDER BY reservation_date DESC";
        List<Reservation> reservations = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
            
            return reservations;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error finding active reservations by user ID", e);
        }
    }
    
    @Override
    public void deleteById(Integer reservationId) {
        String sql = "DELETE FROM reservation WHERE reservation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reservationId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting reservation", e);
        }
    }
    
    @Override
    public int countActiveByItemId(Integer itemId) {
        String sql = "SELECT COUNT(*) FROM reservation WHERE item_id = ? AND status = 'ACTIVE'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            throw new DataAccessException("Error counting active reservations", e);
        }
    }
    
    /**
     * Maps a ResultSet row to a Reservation object
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getInt("reservation_id"));
        reservation.setUserId(rs.getInt("user_id"));
        reservation.setItemId(rs.getInt("item_id"));
        reservation.setReservationDate(rs.getTimestamp("reservation_date").toLocalDateTime());
        reservation.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
        reservation.setStatus(rs.getString("status"));
        return reservation;
    }
}
