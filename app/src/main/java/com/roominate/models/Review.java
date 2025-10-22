package com.roominate.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Review {
    private String id;
    private String boardingHouseId;
    private String tenantId;
    private String tenantName;
    private String tenantProfileImageUrl;
    
    // Rating (1-5 stars)
    private float overallRating;
    private float cleanlinessRating;
    private float locationRating;
    private float valueRating;
    private float facilitiesRating;
    private float landlordRating;
    
    // Review Content
    private String title;
    private String comment;
    private List<String> imageUrls;
    
    // Review Details
    private String lengthOfStay; // "1 month", "3 months", etc.
    private boolean isVerifiedBooking; // true if user actually booked this place
    
    // Interaction
    private int helpfulCount;
    private int reportCount;
    private boolean isReported;
    private boolean isApproved;
    
    // Response
    private String ownerResponse;
    private Date ownerResponseDate;
    
    // Metadata
    private Date createdAt;
    private Date updatedAt;

    // Constructors
    public Review() {
        this.imageUrls = new ArrayList<>();
        this.helpfulCount = 0;
        this.reportCount = 0;
        this.isReported = false;
        this.isApproved = false;
        this.isVerifiedBooking = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Review(String id, String boardingHouseId, String tenantId, String tenantName, 
                  float overallRating, String comment) {
        this();
        this.id = id;
        this.boardingHouseId = boardingHouseId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.overallRating = overallRating;
        this.comment = comment;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoardingHouseId() {
        return boardingHouseId;
    }

    public void setBoardingHouseId(String boardingHouseId) {
        this.boardingHouseId = boardingHouseId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantProfileImageUrl() {
        return tenantProfileImageUrl;
    }

    public void setTenantProfileImageUrl(String tenantProfileImageUrl) {
        this.tenantProfileImageUrl = tenantProfileImageUrl;
    }

    public float getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(float overallRating) {
        this.overallRating = overallRating;
    }

    public float getCleanlinessRating() {
        return cleanlinessRating;
    }

    public void setCleanlinessRating(float cleanlinessRating) {
        this.cleanlinessRating = cleanlinessRating;
    }

    public float getLocationRating() {
        return locationRating;
    }

    public void setLocationRating(float locationRating) {
        this.locationRating = locationRating;
    }

    public float getValueRating() {
        return valueRating;
    }

    public void setValueRating(float valueRating) {
        this.valueRating = valueRating;
    }

    public float getFacilitiesRating() {
        return facilitiesRating;
    }

    public void setFacilitiesRating(float facilitiesRating) {
        this.facilitiesRating = facilitiesRating;
    }

    public float getLandlordRating() {
        return landlordRating;
    }

    public void setLandlordRating(float landlordRating) {
        this.landlordRating = landlordRating;
    }

    public void calculateOverallRating() {
        float sum = cleanlinessRating + locationRating + valueRating + 
                    facilitiesRating + landlordRating;
        this.overallRating = sum / 5.0f;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void addImageUrl(String imageUrl) {
        this.imageUrls.add(imageUrl);
    }

    public void removeImageUrl(String imageUrl) {
        this.imageUrls.remove(imageUrl);
    }

    public String getLengthOfStay() {
        return lengthOfStay;
    }

    public void setLengthOfStay(String lengthOfStay) {
        this.lengthOfStay = lengthOfStay;
    }

    public boolean isVerifiedBooking() {
        return isVerifiedBooking;
    }

    public void setVerifiedBooking(boolean verifiedBooking) {
        isVerifiedBooking = verifiedBooking;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(int helpfulCount) {
        this.helpfulCount = helpfulCount;
    }

    public void incrementHelpfulCount() {
        this.helpfulCount++;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public boolean isReported() {
        return isReported;
    }

    public void setReported(boolean reported) {
        isReported = reported;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getOwnerResponse() {
        return ownerResponse;
    }

    public void setOwnerResponse(String ownerResponse) {
        this.ownerResponse = ownerResponse;
        this.ownerResponseDate = new Date();
    }

    public Date getOwnerResponseDate() {
        return ownerResponseDate;
    }

    public void setOwnerResponseDate(Date ownerResponseDate) {
        this.ownerResponseDate = ownerResponseDate;
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

    @Override
    public String toString() {
        return "Review{" +
                "id='" + id + '\'' +
                ", boardingHouseId='" + boardingHouseId + '\'' +
                ", tenantName='" + tenantName + '\'' +
                ", overallRating=" + overallRating +
                ", isVerifiedBooking=" + isVerifiedBooking +
                ", helpfulCount=" + helpfulCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
