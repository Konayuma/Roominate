package com.roominate.activities.tenant;

public class House {
    public String id;
    public String title;
    public String location;
    public String price;
    public float rating;
    public int imageResId;

    public House(String id, String title, String location, String price, float rating, int imageResId) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.rating = rating;
        this.imageResId = imageResId;
    }
}