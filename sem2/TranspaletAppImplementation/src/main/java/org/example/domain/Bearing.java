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

    @Column(name = "diameter", nullable = false)
    private Long diameter;

    @Column(name = "mid", nullable = false)
    private Long mid;

    public Bearing() {
    }

    public Bearing(UUID id, Long diameter, Long mid) {
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

    public Long getDiameter() {
        return diameter;
    }

    public void setDiameter(Long diameter) {
        diameter = diameter;
    }

    public Long getMid() {
        return mid;
    }

    public void setMid(Long mid) {
        this.mid = mid;
    }
}
