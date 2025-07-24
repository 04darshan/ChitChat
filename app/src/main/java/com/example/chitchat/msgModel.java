package com.example.chitchat;

public class msgModel {
    String message, senderId;
    long timestamp;
    boolean isSeen; // NEW: Field to track if the message has been read

    public msgModel(String message, String senderId, long timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.isSeen = false; // NEW: Default to not seen
    }

    public msgModel() {
    }

    // Getters and Setters for all fields...
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // NEW: Getter and Setter for isSeen
    public boolean getIsSeen() {
        return isSeen;
    }

    public void setIsSeen(boolean seen) {
        isSeen = seen;
    }
}
