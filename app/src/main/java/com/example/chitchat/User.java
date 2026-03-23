package com.example.chitchat;

/**
 * User model — unchanged in structure, added defensive getters.
 */
public class User {
    private String uid;
    private String username;
    private String mail;
    private String profilepic;
    private String status;

    // Required no-arg constructor for Firebase
    public User() {}

    public User(String uid, String username, String mail, String profilepic, String status) {
        this.uid        = uid;
        this.username   = username;
        this.mail       = mail;
        this.profilepic = profilepic;
        this.status     = status;
    }

    public String getUid()        { return uid != null ? uid : ""; }
    public void   setUid(String uid) { this.uid = uid; }

    public String getUsername()   { return username != null ? username : ""; }
    public void   setUsername(String username) { this.username = username; }

    public String getMail()       { return mail != null ? mail : ""; }
    public void   setMail(String mail) { this.mail = mail; }

    public String getProfilepic() { return profilepic != null ? profilepic : ""; }
    public void   setProfilepic(String profilepic) { this.profilepic = profilepic; }

    public String getStatus()     { return status != null ? status : ""; }
    public void   setStatus(String status) { this.status = status; }
}