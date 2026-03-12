package org.example.domain;

import java.util.UUID;

public class PalletTruck implements Identifiable<UUID>{

    private String serialNumber;
    private UUID id;
    private String type;
    private String model;
    private Long capacityKg;
    private String status;
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
