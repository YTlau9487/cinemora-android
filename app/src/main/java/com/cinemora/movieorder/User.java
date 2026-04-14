package com.cinemora.movieorder;

import com.google.firebase.Timestamp;

public class User {
    private String uid;
    private String username;
    private String email;
    private int credits;
    private int totalOrders;
    private Timestamp createdAt;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String username, String email, int credits, int totalOrders, Timestamp createdAt) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.credits = credits;
        this.totalOrders = totalOrders;
        this.createdAt = createdAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}