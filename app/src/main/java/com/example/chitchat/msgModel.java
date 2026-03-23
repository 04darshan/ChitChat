package com.example.chitchat;

/**
 * ENHANCED msgModel
 *
 * IMPROVEMENT: Added imageUrl field to support image sharing (future feature).
 * The existing isSeen + timestamp fields are unchanged and fully wired up.
 */
public class msgModel {

    private String message;
    private String senderId;
    private long   timestamp;
    private boolean isSeen;
    private String imageUrl; // NEW: for image message support

    // Required no-arg constructor for Firebase DataSnapshot.getValue()
    public msgModel() {}

    public msgModel(String message, String senderId, long timestamp) {
        this.message   = message;
        this.senderId  = senderId;
        this.timestamp = timestamp;
        this.isSeen    = false;
        this.imageUrl  = null;
    }

    // --- Getters & Setters ---

    public String getMessage()  { return message; }
    public void   setMessage(String message) { this.message = message; }

    public String getSenderId() { return senderId; }
    public void   setSenderId(String senderId) { this.senderId = senderId; }

    public long   getTimestamp() { return timestamp; }
    public void   setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean getIsSeen() { return isSeen; }
    public void    setIsSeen(boolean seen) { this.isSeen = seen; }

    public String getImageUrl() { return imageUrl; }
    public void   setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}