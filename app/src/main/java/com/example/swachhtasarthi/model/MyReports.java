package com.example.swachhtasarthi.model;

public class MyReports {
    private String title;
    private String description;
    private String location;
    private String time;
    private String status;
    private int imageResId; // Using resource ID for demo, eventually this will be a URL

    public MyReports(String title, String description, String location, String time, String status, int imageResId) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.time = time;
        this.status = status;
        this.imageResId = imageResId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public int getImageResId() { return imageResId; }
}
