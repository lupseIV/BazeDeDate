package org.example.repository.implementation;

import org.example.domain.Bearing;
import org.example.repository.BearingRepository;
import org.example.repository.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BearingDbRepository implements BearingRepository {

    @Override
    public Bearing save(Bearing entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        if (findById(entity.getId()).isPresent()) {
            return update(entity);
        }
        Connection con = JdbcUtils.getConnection();
        String sql = "INSERT INTO Bearings (bid, diameter, mid) VALUES (?, ?, ?)";
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getId().toString());
            stmt.setDouble(2, entity.getDiameter());

            if (entity.getMid() != null) {
                stmt.setLong(3, entity.getMid());
            } else {
                stmt.setNull(3, java.sql.Types.BIGINT);
            }

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Bearing", e);
        }
    }

    private Bearing update(Bearing entity) {
        String sql = "UPDATE Bearings SET diameter = ?, mid = ? WHERE bid = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setDouble(1, entity.getDiameter());
            if (entity.getMid() != null) {
                stmt.setLong(2, entity.getMid());
            } else {
                stmt.setNull(2, java.sql.Types.BIGINT);
            }
            stmt.setString(3, entity.getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating Bearing", e);
        }
    }

    @Override
    public Optional<Bearing> findById(UUID uuid) {
        String sql = "SELECT * FROM Bearings WHERE bid = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractBearing(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Bearing by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Bearing> findAll() {
        String sql = "SELECT * FROM Bearings";
        List<Bearing> bearings = new ArrayList<>();
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                bearings.add(extractBearing(rs));
            }
            return bearings;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all Bearings", e);
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        String sql = "DELETE FROM Bearings WHERE bid = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting Bearing", e);
        }
    }

    private Bearing extractBearing(ResultSet rs) throws SQLException {
        UUID bid = UUID.fromString(rs.getString("bid"));
        Double diameter = rs.getDouble("diameter");

        long midVal = rs.getLong("mid");
        Long mid = rs.wasNull() ? null : midVal;

        Bearing bearing = new Bearing();
        bearing.setId(bid);
        bearing.setDiameter(diameter);
        bearing.setMid(mid);
        return bearing;
    }
}
