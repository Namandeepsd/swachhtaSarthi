package com.example.swachhtasarthi.model;

public class MyReports {
    private String title;
    private String description;
    private String location;
    private String time;
    private String status;
    private String imageUrl;

    public MyReports(String title, String description, String location, String time, String status, String imageUrl) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
}
