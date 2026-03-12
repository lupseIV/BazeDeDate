package org.example.domain;

import java.time.LocalDate;
import java.util.UUID;

public class PalletTruckDetails implements Identifiable<UUID>{

    private UUID id;
    private PalletTruck truck;
    private LocalDate purchaseDate;
    private String notes;
    private String manufacturer;


    public PalletTruckDetails(UUID id, PalletTruck truck, LocalDate purchaseDate, String notes, String manufacturer) {
        this.id = id;
        this.truck = truck;
        this.purchaseDate = purchaseDate;
        this.notes = notes;
        this.manufacturer = manufacturer;
    }

    public PalletTruckDetails() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public PalletTruck getTruck() {
        return truck;
    }

    public void setTruck(PalletTruck truck) {
        this.truck = truck;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
}
