package com.cinemora.movieorder;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart model for Firestore 'carts' collection.
 * One cart document per user. Document ID = userId.
 */
public class Cart {
    private String userId;                  // Matches the user's Auth UID
    private List<CartItem> items;           // Array of cart items
    private long createdAt;                 // Unix timestamp in seconds
    private long updatedAt;                 // Unix timestamp in seconds

    public Cart() {
        // Required for Firestore deserialization
        this.items = new ArrayList<>();
    }

    public Cart(String userId, List<CartItem> items, long createdAt, long updatedAt) {
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public int getCartTotal() {
        int total = 0;
        if (items != null) {
            for (CartItem item : items) {
                total += item.getItemTotal();
            }
        }
        return total;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public CartItem findItemByMovieId(String movieId) {
        if (items != null) {
            for (CartItem item : items) {
                if (item.getMovieId().equals(movieId)) {
                    return item;
                }
            }
        }
        return null;
    }
}

