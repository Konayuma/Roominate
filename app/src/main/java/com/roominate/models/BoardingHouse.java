package com.roominate.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BoardingHouse {
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String address;
    private double latitude;
    private double longitude;
    private String city;
    private String province;
    private String zipCode;
    
    // Pricing
    private double pricePerMonth;
    private double securityDeposit;
    private boolean electricityIncluded;
    private boolean waterIncluded;
    private boolean internetIncluded;
    
    // Facilities and Amenities
    private List<String> amenities; // wifi, parking, laundry, kitchen, etc.
    private int totalRooms;
    private int availableRooms;
    private String roomType; // single, double, shared
    private boolean hasPrivateBathroom;
    private boolean isFurnished;
    
    // Images and Media
    private List<String> imageUrls;
    private String thumbnailUrl;
    
    // Contact Information
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    
    // Status and Verification
    private String status; // "pending", "active", "inactive", "rejected"
    private boolean isVerified;
    private String verifiedBy; // admin ID
    private Date verifiedAt;
    
    // Rating and Reviews
    private double averageRating;
    private int totalReviews;
    
    // Metadata
    private Date createdAt;
    private Date updatedAt;
    private int viewCount;
    private int inquiryCount;

    // Constructors
    public BoardingHouse() {
        this.amenities = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.status = "pending";
        this.isVerified = false;
        this.averageRating = 0.0;
        this.totalReviews = 0;
        this.viewCount = 0;
        this.inquiryCount = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public BoardingHouse(String id, String ownerId, String name, String address, double pricePerMonth) {
        this();
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.address = address;
        this.pricePerMonth = pricePerMonth;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public double getPricePerMonth() {
        return pricePerMonth;
    }

    public void setPricePerMonth(double pricePerMonth) {
        this.pricePerMonth = pricePerMonth;
    }

    public double getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(double securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public boolean isElectricityIncluded() {
        return electricityIncluded;
    }

    public void setElectricityIncluded(boolean electricityIncluded) {
        this.electricityIncluded = electricityIncluded;
    }

    public boolean isWaterIncluded() {
        return waterIncluded;
    }

    public void setWaterIncluded(boolean waterIncluded) {
        this.waterIncluded = waterIncluded;
    }

    public boolean isInternetIncluded() {
        return internetIncluded;
    }

    public void setInternetIncluded(boolean internetIncluded) {
        this.internetIncluded = internetIncluded;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public void addAmenity(String amenity) {
        if (!this.amenities.contains(amenity)) {
            this.amenities.add(amenity);
        }
    }

    public void removeAmenity(String amenity) {
        this.amenities.remove(amenity);
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public boolean isHasPrivateBathroom() {
        return hasPrivateBathroom;
    }

    public void setHasPrivateBathroom(boolean hasPrivateBathroom) {
        this.hasPrivateBathroom = hasPrivateBathroom;
    }

    public boolean isFurnished() {
        return isFurnished;
    }

    public void setFurnished(boolean furnished) {
        isFurnished = furnished;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
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

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public int getInquiryCount() {
        return inquiryCount;
    }

    public void setInquiryCount(int inquiryCount) {
        this.inquiryCount = inquiryCount;
    }

    public void incrementInquiryCount() {
        this.inquiryCount++;
    }

    @Override
    public String toString() {
        return "BoardingHouse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", pricePerMonth=" + pricePerMonth +
                ", availableRooms=" + availableRooms +
                ", status='" + status + '\'' +
                ", isVerified=" + isVerified +
                ", averageRating=" + averageRating +
                '}';
    }
}
