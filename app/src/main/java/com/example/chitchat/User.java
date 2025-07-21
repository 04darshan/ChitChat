package com.example.chitchat;

import de.hdodenhof.circleimageview.CircleImageView;

public class User {
    String profilepic, mail, username, password, uid, lastmessage, status;
    CircleImageView img;

    public User() {
    }

    public User(String uid, String username, String mail, String password, String profilepic, String status) {
        this.uid = uid;
        this.username = username;
        this.mail = mail;
        this.password = password;
        this.profilepic = profilepic;
        this.status = status;
        this.lastmessage = ""; // Default to empty string
    }

    public CircleImageView getImg() {
        return img;
    }

    public void setImg(CircleImageView img) {
        this.img = img;
    }

    public User(String id, String name, String email, String pass, CircleImageView img, String status) {
        this.uid = uid;
        this.username = username;
        this.mail = mail;
        this.password = password;
        this.img = img;
        this.status = status;
        this.lastmessage = "";
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLastmessage() {
        return lastmessage;
    }

    public void setLastmessage(String lastmessage) {
        this.lastmessage = lastmessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
