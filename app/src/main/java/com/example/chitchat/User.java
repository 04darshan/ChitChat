package com.example.chitchat;

public class User {
    String profilepic, mail, username, uid, status;

    // Default constructor is required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    // UPDATED: Cleaned up the constructor. Removed password and CircleImageView.
    public User(String uid, String username, String mail, String profilepic, String status) {
        this.uid = uid;
        this.username = username;
        this.mail = mail;
        this.profilepic = profilepic;
        this.status = status;
    }

    // Getters and Setters
    public String getProfilepic() {
        return profilepic;
    }

    public void setProfilepic(String profilepic) {
        this.profilepic = profilepic;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}