package com.roominate.models;

import java.util.ArrayList;
import java.util.List;

public class Owner extends User {
    private String businessName;
    private String businessAddress;
    private String taxId;
    private boolean isVerifiedOwner;
    private List<String> propertyListings;
    private double averageRating;
    private int totalReviews;

    // Constructors
    public Owner() {
        super();
        this.propertyListings = new ArrayList<>();
        this.isVerifiedOwner = false;
        this.averageRating = 0.0;
        this.totalReviews = 0;
    }

    public Owner(String id, String email, String fullName, String phoneNumber) {
        super(id, email, fullName, phoneNumber, "owner");
        this.propertyListings = new ArrayList<>();
        this.isVerifiedOwner = false;
        this.averageRating = 0.0;
        this.totalReviews = 0;
    }

    // Getters and Setters
    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public boolean isVerifiedOwner() {
        return isVerifiedOwner;
    }

    public void setVerifiedOwner(boolean verifiedOwner) {
        isVerifiedOwner = verifiedOwner;
    }

    public List<String> getPropertyListings() {
        return propertyListings;
    }

    public void setPropertyListings(List<String> propertyListings) {
        this.propertyListings = propertyListings;
    }

    public void addPropertyListing(String propertyId) {
        if (!this.propertyListings.contains(propertyId)) {
            this.propertyListings.add(propertyId);
        }
    }

    public void removePropertyListing(String propertyId) {
        this.propertyListings.remove(propertyId);
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    @Override
    public String toString() {
        return "Owner{" +
                "id='" + getId() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", businessName='" + businessName + '\'' +
                ", isVerifiedOwner=" + isVerifiedOwner +
                ", propertyCount=" + propertyListings.size() +
                ", averageRating=" + averageRating +
                ", totalReviews=" + totalReviews +
                '}';
    }
}
