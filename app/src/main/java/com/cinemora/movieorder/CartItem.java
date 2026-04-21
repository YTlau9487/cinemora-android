package com.cinemora.movieorder;

/**
 * CartItem model for cart items within Firestore 'carts' collection.
 * Each item in the items array of a cart document.
 * Also used for orderItems sub-collection.
 */
public class CartItem {
    private String movieId;                 // Reference to movies collection
    private String movieName;               // Movie name at time of adding
    private String posterUrl;               // Poster URL at time of adding
    private int cost;                       // Cost in HKD at time of adding
    private int quantity;                   // Quantity in cart
    private long createdAt;                 // Unix timestamp in seconds

    public CartItem() {
        // Required for Firestore deserialization
    }

    public CartItem(String movieId, String movieName, int cost, int quantity, long createdAt) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.posterUrl = "";
        this.cost = cost;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public CartItem(String movieId, String movieName, String posterUrl, int cost, int quantity, long createdAt) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.posterUrl = posterUrl;
        this.cost = cost;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getItemTotal() {
        return cost * quantity;
    }
}
