package com.roominate.models;

import java.util.Date;

/**
 * Model representing a user's favorite boarding house
 */
public class Favorite {
    private String id;
    private String userId;
    private String listingId;
    private Date createdAt;
    
    // Extended property details (for display purposes)
    private BoardingHouse boardingHouse;

    public Favorite() {
        this.createdAt = new Date();
    }

    public Favorite(String id, String userId, String listingId) {
        this.id = id;
        this.userId = userId;
        this.listingId = listingId;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BoardingHouse getBoardingHouse() {
        return boardingHouse;
    }

    public void setBoardingHouse(BoardingHouse boardingHouse) {
        this.boardingHouse = boardingHouse;
    }

    @Override
    public String toString() {
        return "Favorite{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", listingId='" + listingId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
