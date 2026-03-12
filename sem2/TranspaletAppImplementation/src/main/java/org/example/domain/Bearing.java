package org.example.domain;

import java.util.UUID;

public class Bearing implements Identifiable<UUID>{

    private UUID id;
    private Long Diameter;
    private Long mid;

    public Bearing() {
    }

    public Bearing(UUID id, Long diameter, Long mid) {
        this.id = id;
        Diameter = diameter;
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
        return Diameter;
    }

    public void setDiameter(Long diameter) {
        Diameter = diameter;
    }

    public Long getMid() {
        return mid;
    }

    public void setMid(Long mid) {
        this.mid = mid;
    }
}
