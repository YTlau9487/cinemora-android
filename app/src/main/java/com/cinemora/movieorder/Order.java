package com.cinemora.movieorder;

/**
 * Order model aligned with Firestore 'orders' collection schema.
 * All time fields stored as Unix timestamps (seconds).
 * All cost fields stored as integers in HKD.
 */
public class Order {
    // Removed @DocumentId because 'orderId' is already a field in the Firestore document data.
    // Having both causes a crash: 'orderId' was found from document..., cannot apply @DocumentId.
    private String orderId;                 // Firestore document ID (also stored in data)
    private String userId;                  // Reference to users collection
    private long orderDate;                 // Unix timestamp in seconds
    private String progress;                // "Pending", "Processing", "Shipped", or "Delivered"
    private int itemCount;                  // Number of films in this order
    private int subtotal;                   // Sum of all item costs (HKD)
    private int discount;                   // Promo/discount amount (HKD), default 0
    private int totalCost;                  // Final amount after discount (HKD)
    private int creditsBefore;              // User's credit balance before order (HKD)
    private int creditsUsed;                // Credits user chose to apply (HKD)
    private int creditsAfter;               // User's credit balance after order (HKD)
    private long createdAt;                 // Unix timestamp in seconds
    private long updatedAt;                 // Unix timestamp in seconds

    public Order() {
        // Required for Firestore deserialization
    }

    public Order(String orderId, String userId, long orderDate, String progress, int itemCount,
                 int subtotal, int discount, int totalCost, int creditsBefore, int creditsUsed,
                 int creditsAfter, long createdAt, long updatedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderDate = orderDate;
        this.progress = progress;
        this.itemCount = itemCount;
        this.subtotal = subtotal;
        this.discount = discount;
        this.totalCost = totalCost;
        this.creditsBefore = creditsBefore;
        this.creditsUsed = creditsUsed;
        this.creditsAfter = creditsAfter;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getOrderDate() { return orderDate; }
    public void setOrderDate(long orderDate) { this.orderDate = orderDate; }

    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }

    public int getDiscount() { return discount; }
    public void setDiscount(int discount) { this.discount = discount; }

    public int getTotalCost() { return totalCost; }
    public void setTotalCost(int totalCost) { this.totalCost = totalCost; }

    public int getCreditsBefore() { return creditsBefore; }
    public void setCreditsBefore(int creditsBefore) { this.creditsBefore = creditsBefore; }

    public int getCreditsUsed() { return creditsUsed; }
    public void setCreditsUsed(int creditsUsed) { this.creditsUsed = creditsUsed; }

    public int getCreditsAfter() { return creditsAfter; }
    public void setCreditsAfter(int creditsAfter) { this.creditsAfter = creditsAfter; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Utility method to get progress badge color
    public int getProgressColor() {
        switch (progress != null ? progress : "") {
            case "Pending":
                return 0xFF9E9E9E;  // Grey
            case "Processing":
                return 0xFF2196F3;  // Blue
            case "Shipped":
                return 0xFFFF9800;  // Orange
            case "Delivered":
                return 0xFF4CAF50;  // Green
            default:
                return 0xFF9E9E9E;  // Grey
        }
    }
}
