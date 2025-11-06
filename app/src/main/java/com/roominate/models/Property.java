package com.roominate.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Property {
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String address;
    private double monthlyRate;
    private double securityDeposit;
    private String status; // e.g., "available", "rented", "draft"
    private List<String> imageUrls;
    private List<String> amenities;
    private String thumbnailUrl; // Keep for grid view if needed
    private double latitude;
    private double longitude;
    private int availableRooms;

    // Constructors
    public Property() {
        this.imageUrls = new ArrayList<>();
        this.amenities = new ArrayList<>();
        this.status = "draft";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.availableRooms = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getMonthlyRate() { return monthlyRate; }
    public void setMonthlyRate(double monthlyRate) { this.monthlyRate = monthlyRate; }

    public double getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(double securityDeposit) { this.securityDeposit = securityDeposit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    
    public String getThumbnailUrl() { 
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return thumbnailUrl; 
    }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }


    // JSON Serialization/Deserialization
    public static Property fromJson(JSONObject jsonObject) throws JSONException {
        Property property = new Property();
        property.setId(jsonObject.optString("id"));
        property.setOwnerId(jsonObject.optString("owner_id"));
        property.setName(jsonObject.optString("name"));
        property.setDescription(jsonObject.optString("description"));
        property.setAddress(jsonObject.optString("address"));
        property.setMonthlyRate(jsonObject.optDouble("monthly_rate", 0.0));
        property.setSecurityDeposit(jsonObject.optDouble("security_deposit", 0.0));
        property.setStatus(jsonObject.optString("status", "draft"));
        property.setLatitude(jsonObject.optDouble("latitude", 0.0));
        property.setLongitude(jsonObject.optDouble("longitude", 0.0));
        property.setAvailableRooms(jsonObject.optInt("available_rooms", 0));

        // Parse image_urls or images array (support both column names)
        JSONArray imageUrlsJson = jsonObject.optJSONArray("images");
        if (imageUrlsJson == null) {
            imageUrlsJson = jsonObject.optJSONArray("image_urls");
        }
        if (imageUrlsJson != null) {
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < imageUrlsJson.length(); i++) {
                urls.add(imageUrlsJson.getString(i));
            }
            property.setImageUrls(urls);
        }

        // Parse amenities array
        JSONArray amenitiesJson = jsonObject.optJSONArray("amenities");
        if (amenitiesJson != null) {
            List<String> amenityList = new ArrayList<>();
            for (int i = 0; i < amenitiesJson.length(); i++) {
                amenityList.add(amenitiesJson.getString(i));
            }
            property.setAmenities(amenityList);
        }

        return property;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        // Don't include id for new insertions
        if (id != null && !id.isEmpty()) {
            jsonObject.put("id", id);
        }
        jsonObject.put("owner_id", ownerId);
        jsonObject.put("name", name);
        jsonObject.put("description", description);
        jsonObject.put("address", address);
        jsonObject.put("monthly_rate", monthlyRate);
        jsonObject.put("security_deposit", securityDeposit);
        jsonObject.put("status", status);
        jsonObject.put("latitude", latitude);
        jsonObject.put("longitude", longitude);
        jsonObject.put("available_rooms", availableRooms);
        jsonObject.put("images", new JSONArray(imageUrls)); // Use "images" column name
        jsonObject.put("amenities", new JSONArray(amenities));
        return jsonObject;
    }
}
