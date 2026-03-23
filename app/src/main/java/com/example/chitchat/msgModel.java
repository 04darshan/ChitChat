package com.example.chitchat;

import com.google.firebase.Timestamp;

/**
 * Message model — Firestore compatible.
 * Uses Firestore Timestamp instead of long for proper server-side ordering.
 */
public class msgModel {
    private String    message;
    private String    senderId;
    private Timestamp timestamp;  // Firestore server timestamp
    private boolean   isSeen;
    private String    imageUrl;   // future: image messages

    public msgModel() {} // required by Firestore

    public msgModel(String message, String senderId) {
        this.message   = message;
        this.senderId  = senderId;
        this.isSeen    = false;
        this.imageUrl  = null;
        // timestamp is set server-side via FieldValue.serverTimestamp()
    }

    public String    getMessage()   { return message; }
    public String    getSenderId()  { return senderId; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean   getIsSeen()    { return isSeen; }
    public String    getImageUrl()  { return imageUrl; }

    public void setMessage(String message)      { this.message   = message; }
    public void setSenderId(String senderId)    { this.senderId  = senderId; }
    public void setTimestamp(Timestamp ts)      { this.timestamp = ts; }
    public void setIsSeen(boolean seen)         { this.isSeen    = seen; }
    public void setImageUrl(String imageUrl)    { this.imageUrl  = imageUrl; }
}