package com.example.chitchat;

import com.google.firebase.Timestamp;

/**
 * User model — works with Firestore DocumentSnapshot.toObject(User.class)
 * All fields must have a no-arg constructor + public getters/setters.
 */
public class User {
    private String uid;
    private String username;
    private String email;
    private String profilepic;
    private String status;       // "Online" | "Offline"
    private Timestamp lastSeen;  // NEW: Firestore Timestamp for last seen

    public User() {} // required by Firestore

    public User(String uid, String username, String email,
                String profilepic, String status) {
        this.uid        = uid;
        this.username   = username;
        this.email      = email;
        this.profilepic = profilepic;
        this.status     = status;
    }

    public String    getUid()        { return uid        != null ? uid        : ""; }
    public String    getUsername()   { return username   != null ? username   : ""; }
    public String    getMail()       { return email      != null ? email      : ""; }
    public String    getProfilepic() { return profilepic != null ? profilepic : ""; }
    public String    getStatus()     { return status     != null ? status     : ""; }
    public Timestamp getLastSeen()   { return lastSeen; }

    public void setUid(String uid)               { this.uid        = uid; }
    public void setUsername(String username)     { this.username   = username; }
    public void setMail(String email)            { this.email      = email; }
    public void setProfilepic(String profilepic) { this.profilepic = profilepic; }
    public void setStatus(String status)         { this.status     = status; }
    public void setLastSeen(Timestamp lastSeen)  { this.lastSeen   = lastSeen; }
}