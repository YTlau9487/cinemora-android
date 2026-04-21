package com.cinemora.movieorder;

/**
 * User model aligned with Firestore 'users' collection schema.
 * All time fields stored as Unix timestamps (seconds).
 * All credit fields stored as integers in HKD.
 */
public class User {
    private String userId;                  // Firestore document ID, same as Auth UID
    private String name;                    // User's display name
    private String email;
    private String passwordHash;            // Not used for Auth; kept for reference
    private int earnedCredit;               // Virtual credit balance in HKD, default 0
    private long createdAt;                 // Unix timestamp in seconds
    private long updatedAt;                 // Unix timestamp in seconds

    public User() {
        // Required for Firestore deserialization
    }

    public User(String userId, String name, String email, String passwordHash, int earnedCredit, long createdAt, long updatedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.earnedCredit = earnedCredit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getEarnedCredit() { return earnedCredit; }
    public void setEarnedCredit(int earnedCredit) { this.earnedCredit = earnedCredit; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}