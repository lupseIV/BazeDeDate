package org.example.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "PalletTrucks")
public class PalletTruck extends AuditableEntity implements Identifiable<UUID>{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "truck_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "capacity_kg", nullable = false)
    private Long capacityKg;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wheels_id", nullable = false)
    private Wheel wheel;

    public PalletTruck(String serialNumber, UUID id, String type, String model, Long capacityKg, String status, Wheel wheel) {
        this.serialNumber = serialNumber;
        this.id = id;
        this.type = type;
        this.model = model;
        this.capacityKg = capacityKg;
        this.status = status;
        this.wheel = wheel;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(Long capacityKg) {
        this.capacityKg = capacityKg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PalletTruck() {
    }

    public Wheel getWheel() {
        return wheel;
    }

    public void setWheel(Wheel wheel) {
        this.wheel = wheel;
    }
}
