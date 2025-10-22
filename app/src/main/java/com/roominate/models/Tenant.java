package com.roominate.models;

import java.util.ArrayList;
import java.util.List;

public class Tenant extends User {
    private String occupation;
    private String preferredLocation;
    private double maxBudget;
    private List<String> favoriteListings;
    private List<String> bookingHistory;

    // Constructors
    public Tenant() {
        super();
        this.favoriteListings = new ArrayList<>();
        this.bookingHistory = new ArrayList<>();
    }

    public Tenant(String id, String email, String fullName, String phoneNumber) {
        super(id, email, fullName, phoneNumber, "tenant");
        this.favoriteListings = new ArrayList<>();
        this.bookingHistory = new ArrayList<>();
    }

    // Getters and Setters
    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public double getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(double maxBudget) {
        this.maxBudget = maxBudget;
    }

    public List<String> getFavoriteListings() {
        return favoriteListings;
    }

    public void setFavoriteListings(List<String> favoriteListings) {
        this.favoriteListings = favoriteListings;
    }

    public void addFavoriteListing(String listingId) {
        if (!this.favoriteListings.contains(listingId)) {
            this.favoriteListings.add(listingId);
        }
    }

    public void removeFavoriteListing(String listingId) {
        this.favoriteListings.remove(listingId);
    }

    public List<String> getBookingHistory() {
        return bookingHistory;
    }

    public void setBookingHistory(List<String> bookingHistory) {
        this.bookingHistory = bookingHistory;
    }

    public void addBooking(String bookingId) {
        this.bookingHistory.add(bookingId);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id='" + getId() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", occupation='" + occupation + '\'' +
                ", preferredLocation='" + preferredLocation + '\'' +
                ", maxBudget=" + maxBudget +
                ", favoriteCount=" + favoriteListings.size() +
                ", bookingCount=" + bookingHistory.size() +
                '}';
    }
}
