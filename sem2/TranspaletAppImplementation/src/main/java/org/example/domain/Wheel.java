package org.example.domain;

import java.util.UUID;

public class Wheel implements Identifiable<UUID> {

    private UUID id;
    private WheelMaterial material;
    private Bearing bearing;
    private Long maxWeight;

    public Wheel(UUID id, WheelMaterial material, Bearing bearing, Long maxWeight) {
        this.id = id;
        this.material = material;
        this.bearing = bearing;
        this.maxWeight = maxWeight;
    }

    public Wheel() {
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public WheelMaterial getMaterial() {
        return material;
    }

    public void setMaterial(WheelMaterial material) {
        this.material = material;
    }

    public Bearing getBearing() {
        return bearing;
    }

    public void setBearing(Bearing bearing) {
        this.bearing = bearing;
    }

    public Long getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Long maxWeight) {
        this.maxWeight = maxWeight;
    }
}
