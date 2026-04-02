package org.example.repository.implementation;
import org.example.domain.PalletTruck;
import org.example.domain.Wheel;
import org.example.domain.exceptions.RepositoryException;
import org.example.repository.PalletTruckRepository;
import org.example.repository.WheelsRepository;
import org.example.repository.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PalletTruckDbRepository implements PalletTruckRepository {

    private final WheelsRepository wheelRepo;

    public PalletTruckDbRepository(WheelsRepository wheelRepo) {
        this.wheelRepo = wheelRepo;
    }

    @Override
    public PalletTruck save(PalletTruck entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        if (findById(entity.getId()).isPresent()) {
            return update(entity);
        }

        String sql = "INSERT INTO PalletTrucks (truck_id, serial_number, type, model, capacity_kg, status, wheels_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getId().toString());
            stmt.setString(2, entity.getSerialNumber());
            stmt.setString(3, entity.getType());
            stmt.setString(4, entity.getModel());
            stmt.setLong(5, entity.getCapacityKg());
            stmt.setString(6, entity.getStatus());
            stmt.setString(7, entity.getWheel().getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RepositoryException("Error saving PalletTruck", e);
        }
    }

    private PalletTruck update(PalletTruck entity) {
        String sql = "UPDATE PalletTrucks SET serial_number = ?, type = ?, model = ?, capacity_kg = ?, status = ?, wheels_id = ? " +
                "WHERE truck_id = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, entity.getSerialNumber());
            stmt.setString(2, entity.getType());
            stmt.setString(3, entity.getModel());
            stmt.setLong(4, entity.getCapacityKg());
            stmt.setString(5, entity.getStatus());
            stmt.setString(6,entity.getWheel().getId().toString());
            stmt.setString(7, entity.getId().toString());

            stmt.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RepositoryException("Error updating PalletTruck", e);
        }
    }

    @Override
    public Optional<PalletTruck> findById(UUID uuid) {
        String sql = "SELECT * FROM PalletTrucks WHERE truck_id = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractPalletTruck(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error finding PalletTruck by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<PalletTruck> findAll() {
        String sql = "SELECT * FROM PalletTrucks";
        List<PalletTruck> trucks = new ArrayList<>();
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                trucks.add(extractPalletTruck(rs));
            }
            return trucks;
        } catch (SQLException e) {
            throw new RepositoryException("Error fetching all PalletTrucks", e);
        }
    }

    @Override
    public void deleteById(UUID uuid) {
        String sql = "DELETE FROM PalletTrucks WHERE truck_id = ?";
        Connection con = JdbcUtils.getConnection();
        try (
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Error deleting PalletTruck", e);
        }
    }

    private PalletTruck extractPalletTruck(ResultSet rs) throws SQLException {
        PalletTruck truck = new PalletTruck();
        truck.setId(UUID.fromString(rs.getString("truck_id")));
        truck.setSerialNumber(rs.getString("serial_number"));
        truck.setType(rs.getString("type"));
        truck.setModel(rs.getString("model"));
        truck.setCapacityKg(rs.getLong("capacity_kg"));
        truck.setStatus(rs.getString("status"));
        truck.setWheel(getWheel(UUID.fromString(rs.getString("wheels_id"))));
        return truck;
    }

    private Wheel getWheel(UUID wheelId){

        var wheelOp = wheelRepo.findById(wheelId);
        if(wheelOp.isPresent()){
            return wheelOp.get();
        }
        throw new RepositoryException("Wheel with ID " + wheelId + " not found");
    }
}