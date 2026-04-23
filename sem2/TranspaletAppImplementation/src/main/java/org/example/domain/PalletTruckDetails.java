package org.example.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "PalletTruckDetails")
@Cacheable
public class PalletTruckDetails{
    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id", nullable = false)
    private PalletTruck truck;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "manufacturer", nullable = false)
    private String manufacturer;


    public PalletTruckDetails(PalletTruck truck, LocalDate purchaseDate, String notes, String manufacturer) {
        this.truck = truck;
        this.purchaseDate = purchaseDate;
        this.notes = notes;
        this.manufacturer = manufacturer;
    }

    public PalletTruckDetails() {
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
