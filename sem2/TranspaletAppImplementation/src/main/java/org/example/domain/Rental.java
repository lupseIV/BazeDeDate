package org.example.domain;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Rentals")
@SQLDelete(sql = "" +
        "UPDATE Rentals SET " +
        "is_deleted = 1," +
        " deleted_at = GETDATE()," +
        " deleted_by = 'system'" +
        " WHERE rental_id = ?")
@FilterDef(
        name = "deletedFilter",
        parameters = @ParamDef(name = "isDeleted",
                type = Boolean.class))
@Filter(name = "deletedFilter",
        condition = "is_deleted = :isDeleted")
public class Rental extends AuditableEntity implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rental_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id", nullable = false)
    private PalletTruck truck;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "return_status", nullable = false)
    private String returnStatus;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Rental() {
    }

    public Rental(PalletTruck truck, LocalDate startDate, LocalDate endDate,
                  BigDecimal dailyRate, BigDecimal totalCost, String returnStatus) {
        this.truck = truck;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyRate = dailyRate;
        this.totalCost = totalCost;
        this.returnStatus = returnStatus;
    }

    public void softDelete(String username) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = username;
    }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(String returnStatus) {
        this.returnStatus = returnStatus;
    }
}
