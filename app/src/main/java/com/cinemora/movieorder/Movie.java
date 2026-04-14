package com.cinemora.movieorder;

import com.google.firebase.Timestamp;

public class Movie {
    private String id;
    private String title;
    private String description;
    private double price;
    private String genre;
    private String posterUrl;
    private float rating;
    private boolean isBestSelling;
    private Timestamp createdAt;

    public Movie() {
        // Required for Firestore
    }

    public Movie(String id, String title, String description, double price, String genre, String posterUrl, float rating, boolean isBestSelling, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.genre = genre;
        this.posterUrl = posterUrl;
        this.rating = rating;
        this.isBestSelling = isBestSelling;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public boolean getIsBestSelling() { return isBestSelling; }
    public void setIsBestSelling(boolean bestSelling) { isBestSelling = bestSelling; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}