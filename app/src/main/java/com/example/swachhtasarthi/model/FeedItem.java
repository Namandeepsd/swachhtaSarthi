package com.example.swachhtasarthi.model;

public class FeedItem {
    private String reportId;
    private String reportOwnerId;
    private String userName;
    private String location;
    private String status;
    private String description;
    private String address;
    private String imageUrl;
    private String uploaderProfileUrl;
    private long upvoteCount;
    private long commentCount;
    private boolean upvotedByCurrentUser;
    private boolean descriptionExpanded;
    private int userProfileImage;
    private int fallbackPostImage;

    public FeedItem(
            String reportId,
            String reportOwnerId,
            String userName,
            String location,
            String status,
            String description,
            String address,
            String imageUrl,
            String uploaderProfileUrl,
            long upvoteCount,
            long commentCount,
            boolean upvotedByCurrentUser,
            int userProfileImage,
            int fallbackPostImage
    ) {
        this.reportId = reportId;
        this.reportOwnerId = reportOwnerId;
        this.userName = userName;
        this.location = location;
        this.status = status;
        this.description = description;
        this.address = address;
        this.imageUrl = imageUrl;
        this.uploaderProfileUrl = uploaderProfileUrl;
        this.upvoteCount = upvoteCount;
        this.commentCount = commentCount;
        this.upvotedByCurrentUser = upvotedByCurrentUser;
        this.descriptionExpanded = false;
        this.userProfileImage = userProfileImage;
        this.fallbackPostImage = fallbackPostImage;
    }

    public String getReportId() { return reportId; }
    public String getReportOwnerId() { return reportOwnerId; }
    public String getUserName() { return userName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getImageUrl() { return imageUrl; }
    public String getUploaderProfileUrl() { return uploaderProfileUrl; }
    public long getUpvoteCount() { return upvoteCount; }
    public long getCommentCount() { return commentCount; }
    public boolean isUpvotedByCurrentUser() { return upvotedByCurrentUser; }
    public boolean isDescriptionExpanded() { return descriptionExpanded; }
    public int getUserProfileImage() { return userProfileImage; }
    public int getFallbackPostImage() { return fallbackPostImage; }

    public void setUpvoteCount(long upvoteCount) { this.upvoteCount = upvoteCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setUpvotedByCurrentUser(boolean upvotedByCurrentUser) { this.upvotedByCurrentUser = upvotedByCurrentUser; }
    public void setDescriptionExpanded(boolean descriptionExpanded) { this.descriptionExpanded = descriptionExpanded; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUploaderProfileUrl(String uploaderProfileUrl) { this.uploaderProfileUrl = uploaderProfileUrl; }
}
