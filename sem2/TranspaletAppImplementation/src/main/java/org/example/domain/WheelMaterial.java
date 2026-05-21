package org.example.domain;

import jakarta.persistence.*;

import java.util.UUID;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;

@Entity
@Table(name = "WheelMaterials")
@Cacheable
public class WheelMaterial implements Identifiable<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "materials_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "max_weight", nullable = false)
    private Long maxWeight;

    @Column(name = "density", nullable = false)
    private java.math.BigDecimal density = java.math.BigDecimal.ZERO;

    public WheelMaterial(UUID id, String type, Long maxWeight) {
        this.id = id;
        this.type = type;
        this.maxWeight = maxWeight;
    }

    public WheelMaterial() {
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

    public Long getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Long maxWeight) {
        this.maxWeight = maxWeight;
    }

    public java.math.BigDecimal getDensity() {
        return density;
    }

    public void setDensity(java.math.BigDecimal density) {
        this.density = density;
    }
}
