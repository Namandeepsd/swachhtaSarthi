package com.example.swachhtasarthi.model;

public class FeedItem {
    private String userName;
    private String location;
    private String status;
    private String description;
    private int userProfileImage;
    private int postImage;

    public FeedItem(String userName, String location, String status, String description, int userProfileImage, int postImage) {
        this.userName = userName;
        this.location = location;
        this.status = status;
        this.description = description;
        this.userProfileImage = userProfileImage;
        this.postImage = postImage;
    }

    public String getUserName() { return userName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public int getUserProfileImage() { return userProfileImage; }
    public int getPostImage() { return postImage; }
}
