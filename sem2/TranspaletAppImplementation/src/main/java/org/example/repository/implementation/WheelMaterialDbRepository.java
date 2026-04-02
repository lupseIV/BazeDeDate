package org.example.repository.implementation;

import org.example.domain.WheelMaterial;
import org.example.repository.WheelMaterialRepository;
import org.example.repository.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class WheelMaterialDbRepository implements WheelMaterialRepository {


    @Override
    public WheelMaterial save(WheelMaterial entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        if (findById(entity.getId()).isPresent()) {
            return update(entity);
        }

        String sql = "INSERT INTO WheelMaterials (materials_id, type, max_weight) VALUES (?, ?, ?)";
        Connection con = JdbcUtils.getConnection();

        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getId().toString());
            stmt.setString(2, entity.getType());
            stmt.setLong(3, entity.getMaxWeight());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving WheelMaterial", e);
        }
    }

    private WheelMaterial update(WheelMaterial entity) {
        String sql = "UPDATE WheelMaterials SET type = ?, max_weight = ? WHERE materials_id = ?";
        Connection con = JdbcUtils.getConnection();

        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getType());
            stmt.setLong(2, entity.getMaxWeight());
            stmt.setString(3, entity.getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating WheelMaterial", e);
        }
    }

    @Override
    public Optional<WheelMaterial> findById(UUID uuid) {
        String sql = "SELECT * FROM WheelMaterials WHERE materials_id = ?";
        Connection con = JdbcUtils.getConnection();

        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractWheelMaterial(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding WheelMaterial by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<WheelMaterial> findAll() {
        String sql = "SELECT * FROM WheelMaterials";
        List<WheelMaterial> materials = new ArrayList<>();
        Connection con = JdbcUtils.getConnection();

        try (
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                materials.add(extractWheelMaterial(rs));
            }
            return materials;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all WheelMaterials", e);
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        String sql = "DELETE FROM WheelMaterials WHERE materials_id = ?";
        Connection con = JdbcUtils.getConnection();

        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting WheelMaterial", e);
        }
    }

    private WheelMaterial extractWheelMaterial(ResultSet rs) throws SQLException {
        WheelMaterial material = new WheelMaterial();
        material.setId(UUID.fromString(rs.getString("materials_id")));
        material.setType(rs.getString("type"));
        material.setMaxWeight(rs.getLong("max_weight"));
        return material;
    }
}