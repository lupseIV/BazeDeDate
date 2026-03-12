package org.example.transpaletiiapp.repository.implementation;

import org.example.transpaletiiapp.domain.Bearing;
import org.example.transpaletiiapp.domain.Wheel;
import org.example.transpaletiiapp.domain.WheelMaterial;
import org.example.transpaletiiapp.domain.exceptions.RepositoryException;
import org.example.transpaletiiapp.repository.WheelsRepository;
import org.example.transpaletiiapp.repository.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class WheelDbRepository implements WheelsRepository {

    private final JdbcUtils dbUtils;

    public WheelDbRepository(Properties props) {
        this.dbUtils = new JdbcUtils(props);
    }

    @Override
    public Wheel save(Wheel entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        if (findById(entity.getId()).isPresent()) {
            return update(entity);
        }

        String sql = "INSERT INTO Wheels (wheels_id, materials_id, max_weight, bid) VALUES (?, ?, ?, ?)";
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getId().toString());
            stmt.setString(2, entity.getMaterial().getId().toString());
            stmt.setLong(3, entity.getMaxWeight());
            stmt.setString(4, entity.getBearing().getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RepositoryException("Error saving Wheel", e);
        }
    }

    private Wheel update(Wheel entity) {
        String sql = "UPDATE Wheels SET materials_id = ?, max_weight = ?, bid = ? WHERE wheels_id = ?";
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1,  entity.getMaterial().getId().toString());
            stmt.setLong(2, entity.getMaxWeight());
            stmt.setString(3, entity.getBearing().getId().toString());
            stmt.setString(4, entity.getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RepositoryException("Error updating Wheel", e);
        }
    }

    @Override
    public Optional<Wheel> findById(UUID uuid) {
        String sql = "SELECT * FROM Wheels WHERE wheels_id = ?";
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractWheel(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding Wheel by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Wheel> findAll() {
        String sql = "SELECT * FROM Wheels";
        List<Wheel> wheels = new ArrayList<>();
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                wheels.add(extractWheel(rs));
            }
            return wheels;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching all Wheels", e);
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        String sql = "DELETE FROM Wheels WHERE wheels_id = ?";
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting Wheel", e);
        }
    }

    private Wheel extractWheel(ResultSet rs) throws SQLException {
        Wheel wheel = new Wheel();
        wheel.setId(UUID.fromString(rs.getString("wheels_id")));
        wheel.setMaterial(getWheelMaterial(UUID.fromString(rs.getString("materials_id"))));
        wheel.setMaxWeight(rs.getLong("max_weight"));
        wheel.setBearing(getBearing(UUID.fromString(rs.getString("bid"))));
        return wheel;
    }

    private Bearing getBearing(UUID id){
        String sql = "SELECT * FROM Bearings WHERE bid = ?";
        Bearing bearing = new Bearing();
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bearing.setId(UUID.fromString(rs.getString("bid")));
                    bearing.setDiameter(rs.getLong("diameter"));
                    bearing.setMid(rs.getLong("mid"));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding Bearing by ID", e);
        }
        return bearing;
    }

    private WheelMaterial getWheelMaterial(UUID id){
        String sql = "SELECT * FROM WheelMaterials WHERE materials_id = ?";
        WheelMaterial wheelMaterial = new WheelMaterial();
        try (Connection con = dbUtils.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    wheelMaterial.setId(UUID.fromString(rs.getString("materials_id")));
                    wheelMaterial.setType(rs.getString("type"));
                    wheelMaterial.setMaxWeight(rs.getLong("max_weight"));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding WheelMaterial by ID", e);
        }
        return wheelMaterial;
    }
}