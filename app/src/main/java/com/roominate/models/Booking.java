package com.roominate.models;

import java.util.Date;

public class Booking {
    private String id;
    private String tenantId;
    private String boardingHouseId;
    private String ownerId;
    
    // Denormalized property data for easy display
    private String propertyName;
    private String propertyAddress;
    private String propertyImageUrl;

    // Booking Details
    private Date bookingDate;
    private Date moveInDate;
    private Date moveOutDate;
    private int durationMonths;
    
    // Pricing
    private double monthlyRate;
    private double securityDeposit;
    private double totalAmount;
    private double paidAmount;
    private double remainingAmount;
    
    // Status
    private String status; // "pending", "confirmed", "active", "completed", "cancelled", "rejected"
    private String paymentStatus; // "unpaid", "partial", "paid", "refunded"
    
    // Additional Information
    private String notes;
    private String cancellationReason;
    private Date cancellationDate;
    
    // Metadata
    private Date createdAt;
    private Date updatedAt;
    private Date confirmedAt;

    // Constructors
    public Booking() {
        this.status = "pending";
        this.paymentStatus = "unpaid";
        this.paidAmount = 0.0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Booking(String id, String tenantId, String boardingHouseId, String ownerId, 
                   Date moveInDate, double monthlyRate) {
        this();
        this.id = id;
        this.tenantId = tenantId;
        this.boardingHouseId = boardingHouseId;
        this.ownerId = ownerId;
        this.moveInDate = moveInDate;
        this.monthlyRate = monthlyRate;
        this.bookingDate = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBoardingHouseId() {
        return boardingHouseId;
    }

    public void setBoardingHouseId(String boardingHouseId) {
        this.boardingHouseId = boardingHouseId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Date getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(Date moveInDate) {
        this.moveInDate = moveInDate;
    }

    public Date getMoveOutDate() {
        return moveOutDate;
    }

    public void setMoveOutDate(Date moveOutDate) {
        this.moveOutDate = moveOutDate;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
    }

    public double getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(double monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public double getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(double securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void calculateTotalAmount() {
        this.totalAmount = (monthlyRate * durationMonths) + securityDeposit;
        this.remainingAmount = totalAmount - paidAmount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
        this.remainingAmount = totalAmount - paidAmount;
        updatePaymentStatus();
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = new Date();
        if ("confirmed".equals(status) && confirmedAt == null) {
            this.confirmedAt = new Date();
        }
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    private void updatePaymentStatus() {
        if (paidAmount <= 0) {
            this.paymentStatus = "unpaid";
        } else if (paidAmount < totalAmount) {
            this.paymentStatus = "partial";
        } else if (paidAmount >= totalAmount) {
            this.paymentStatus = "paid";
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Date getCancellationDate() {
        return cancellationDate;
    }

    public void setCancellationDate(Date cancellationDate) {
        this.cancellationDate = cancellationDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Date confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    // Helper methods for UI
    public String getFormattedStatus() {
        if (status == null || status.isEmpty()) {
            return "Unknown";
        }
        return status.substring(0, 1).toUpperCase() + status.substring(1);
    }

    public String getStatusColor() {
        if (status == null) {
            return "#808080"; // Grey for unknown
        }
        switch (status.toLowerCase()) {
            case "pending":
                return "#FFA500"; // Orange
            case "confirmed":
                return "#4CAF50"; // Green
            case "active":
                return "#2196F3"; // Blue
            case "completed":
                return "#9E9E9E"; // Grey
            case "cancelled":
            case "rejected":
                return "#F44336"; // Red
            default:
                return "#808080"; // Grey
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyAddress() {
        return propertyAddress;
    }

    public void setPropertyAddress(String propertyAddress) {
        this.propertyAddress = propertyAddress;
    }

    public String getPropertyImageUrl() {
        return propertyImageUrl;
    }

    public void setPropertyImageUrl(String propertyImageUrl) {
        this.propertyImageUrl = propertyImageUrl;
    }


    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", boardingHouseId='" + boardingHouseId + '\'' +
                ", moveInDate=" + moveInDate +
                ", durationMonths=" + durationMonths +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
