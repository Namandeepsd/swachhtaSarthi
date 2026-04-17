package com.example.swachhtasarthi.model;

public class Notification {
    private String message;
    private String time;
    private int iconResId;

    public Notification(String message, String time, int iconResId) {
        this.message = message;
        this.time = time;
        this.iconResId = iconResId;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public int getIconResId() {
        return iconResId;
    }
}
