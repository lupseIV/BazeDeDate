package org.example.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "Wheels")
public class Wheel implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wheels_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "materials_id", nullable = false)
    private WheelMaterial material;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bid", nullable = false)
    private Bearing bearing;

    @Column(name = "max_weight", nullable = false)
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
