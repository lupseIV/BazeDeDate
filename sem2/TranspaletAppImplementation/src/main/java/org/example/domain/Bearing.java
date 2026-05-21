package org.example.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "Bearings")
@Cacheable
public class Bearing implements Identifiable<UUID>{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "diameter", nullable = false, columnDefinition = "DECIMAL(10,4)")
    private Double diameter;

    @Column(name = "mid")
    private Long mid;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

    public Bearing() {
    }

    public Bearing(UUID id, Double diameter, Long mid) {
        this.id = id;
        this.diameter = diameter;
        this.mid = mid;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public Double getDiameter() {
        return diameter;
    }

    public void setDiameter(Double diameter) {
        this.diameter = diameter;
    }

    public Long getMid() {
        return mid;
    }

    public void setMid(Long mid) {
        this.mid = mid;
    }
}
