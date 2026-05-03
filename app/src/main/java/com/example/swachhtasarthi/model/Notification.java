package com.example.swachhtasarthi.model;

public class Notification {
    private String id;
    private String type;
    private String title;
    private String message;
    private String reason;
    private String time;
    private int iconResId;
    private boolean inviteActionable;
    private String inviteStatus;
    private String communityId;
    private String communityName;

    public Notification(String message, String time, int iconResId) {
        this.id = null;
        this.type = "general";
        this.title = "Update";
        this.message = message;
        this.reason = "";
        this.time = time;
        this.iconResId = iconResId;
        this.inviteActionable = false;
        this.inviteStatus = null;
        this.communityId = null;
        this.communityName = null;
    }

    public Notification(String id, String type, String title, String message, String reason, String time, int iconResId,
                        boolean inviteActionable, String inviteStatus,
                        String communityId, String communityName) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.reason = reason;
        this.time = time;
        this.iconResId = iconResId;
        this.inviteActionable = inviteActionable;
        this.inviteStatus = inviteStatus;
        this.communityId = communityId;
        this.communityName = communityName;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getReason() {
        return reason;
    }

    public String getTime() {
        return time;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isInviteActionable() {
        return inviteActionable;
    }

    public String getInviteStatus() {
        return inviteStatus;
    }

    public String getCommunityId() {
        return communityId;
    }

    public String getCommunityName() {
        return communityName;
    }
}
