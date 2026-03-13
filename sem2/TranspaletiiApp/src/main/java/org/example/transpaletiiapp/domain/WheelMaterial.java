package org.example.transpaletiiapp.domain;

import java.util.UUID;

public class WheelMaterial implements Identifiable<UUID> {
    private UUID id;
    private String type;
    private Long maxWeight;

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

    @Override
    public String toString() {
        return
                type + '('+ maxWeight+')';
    }
}
