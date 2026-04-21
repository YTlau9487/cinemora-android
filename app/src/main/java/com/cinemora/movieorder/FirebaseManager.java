package com.cinemora.movieorder;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;
    private final FirebaseStorage mStorage;

    public FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    // --- User Authentication & Profile ---

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String message);
    }

    /**
     * Registers a new user and creates their Firestore document.
     * Uses the user object from the task result to avoid race conditions with mAuth.getCurrentUser().
     */
    public void registerUser(String email, String password, String username, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            createUserDocument(firebaseUser, username, email, callback);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Registration failed");
                        }
                    }
                });
    }

    private void createUserDocument(FirebaseUser firebaseUser, String username, String email, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        long currentTimestamp = DateUtils.getCurrentTimestamp();
        User user = new User(uid, username, email, "", 0, currentTimestamp, currentTimestamp);
        
        mFirestore.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void getUserData(String uid, OnUserDataLoadedListener listener) {
        mFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    listener.onLoaded(user);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnUserDataLoadedListener {
        void onLoaded(User user);
        void onError(String message);
    }

    // --- Movie Data ---

    public interface OnMoviesLoadedListener {
        void onLoaded(List<Movie> movies);
        void onError(String message);
    }

    public void getAllMovies(OnMoviesLoadedListener listener) {
        mFirestore.collection("movies")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = queryDocumentSnapshots.toObjects(Movie.class);
                    listener.onLoaded(movies);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * ISSUE 1: Featured Movies - Sort by highest sales count (descending).
     */
    public void getBestSellingMovies(OnMoviesLoadedListener listener) {
        mFirestore.collection("movies")
                .orderBy("saleCount", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = queryDocumentSnapshots.toObjects(Movie.class);
                    listener.onLoaded(movies);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching best selling movies: " + e.getMessage());
                    listener.onError(e.getMessage());
                });
    }

    // --- Cart Management ---

    public interface OnCartLoadedListener {
        void onLoaded(Cart cart);
        void onError(String message);
    }

    public interface OnOperationCompleteListener {
        void onSuccess(String message);
        void onFailure(String message);
    }

    public interface OnPurchaseVerifyListener {
        void onAlreadyOwned();
        void onNotOwned();
        void onError(String message);
    }

    /**
     * Adds or updates an item in the user's cart.
     */
    public void addToCart(String userId, String movieId, String movieName, int cost, int quantity, OnOperationCompleteListener listener) {
        long currentTimestamp = DateUtils.getCurrentTimestamp();
        CartItem cartItem = new CartItem(movieId, movieName, cost, quantity, currentTimestamp);

        mFirestore.collection("carts").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<CartItem> items = new ArrayList<>();
                    if (documentSnapshot.exists()) {
                        // Cart exists - get existing items
                        Cart cart = documentSnapshot.toObject(Cart.class);
                        if (cart != null && cart.getItems() != null) {
                            items = new ArrayList<>(cart.getItems());
                        }
                    }
                    
                    // Add or update item
                    boolean found = false;
                    for (CartItem item : items) {
                        if (item.getMovieId().equals(movieId)) {
                            item.setQuantity(quantity);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        items.add(cartItem);
                    }

                    // Update cart
                    Map<String, Object> cartData = new HashMap<>();
                    cartData.put("userId", userId);
                    cartData.put("items", items);
                    cartData.put("updatedAt", currentTimestamp);
                    if (!documentSnapshot.exists()) {
                        cartData.put("createdAt", currentTimestamp);
                    }

                    mFirestore.collection("carts").document(userId)
                            .set(cartData)
                            .addOnSuccessListener(aVoid -> listener.onSuccess("Item added to cart"))
                            .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Removes an item from the user's cart by movieId.
     */
    public void removeFromCart(String userId, String movieId, OnOperationCompleteListener listener) {
        mFirestore.collection("carts").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Cart cart = documentSnapshot.toObject(Cart.class);
                        if (cart != null && cart.getItems() != null) {
                            // Find and remove the item
                            CartItem toRemove = null;
                            for (CartItem item : cart.getItems()) {
                                if (item.getMovieId().equals(movieId)) {
                                    toRemove = item;
                                    break;
                                }
                            }
                            if (toRemove != null) {
                                cart.getItems().remove(toRemove);
                                mFirestore.collection("carts").document(userId)
                                        .update("items", cart.getItems(), "updatedAt", DateUtils.getCurrentTimestamp())
                                        .addOnSuccessListener(aVoid -> listener.onSuccess("Item removed from cart"))
                                        .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                            } else {
                                listener.onFailure("Item not found in cart");
                            }
                        }
                    } else {
                        listener.onFailure("Cart not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    /**
     * Fetches the user's cart.
     */
    public void getUserCart(String userId, OnCartLoadedListener listener) {
        mFirestore.collection("carts").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Cart cart = documentSnapshot.toObject(Cart.class);
                        listener.onLoaded(cart);
                    } else {
                        // Return empty cart if doesn't exist
                        Cart emptyCart = new Cart(userId, new ArrayList<>(), 0, 0);
                        listener.onLoaded(emptyCart);
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // --- Order Management ---

    public interface OnOrdersLoadedListener {
        void onLoaded(List<Order> orders);
        void onError(String message);
    }

    public interface OnOrderDetailLoadedListener {
        void onLoaded(Order order);
        void onError(String message);
    }

    /**
     * Fetches all orders for a user, sorted by orderDate descending.
     * ISSUE 5: Improved logging for missing index detection.
     */
    public void getUserOrders(String userId, OnOrdersLoadedListener listener) {
        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> orders = queryDocumentSnapshots.toObjects(Order.class);
                    listener.onLoaded(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching orders for user " + userId + ": " + e.getMessage());
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Fetches order details.
     */
    public void getOrderDetails(String orderId, OnOrderDetailLoadedListener listener) {
        mFirestore.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Order order = documentSnapshot.toObject(Order.class);
                    if (order != null) {
                        listener.onLoaded(order);
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // --- Duplicate Purchase Verification ---

    /**
     * Checks if user has already purchased a movie.
     * Returns true if movie exists in any of user's orders.
     */
    public void checkUserOwnsMovie(String userId, String movieId, OnPurchaseVerifyListener listener) {
        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(ordersSnapshot -> {
                    final boolean[] isOwned = {false};
                    for (DocumentSnapshot orderDoc : ordersSnapshot) {
                        Order order = orderDoc.toObject(Order.class);
                        if (order != null) {
                            // Check if this order contains the movieId
                            orderDoc.getReference().collection("orderItems")
                                    .whereEqualTo("movieId", movieId)
                                    .get()
                                    .addOnSuccessListener(itemsSnapshot -> {
                                        if (!itemsSnapshot.isEmpty()) {
                                            listener.onAlreadyOwned();
                                            isOwned[0] = true;
                                        }
                                    });
                        }
                    }
                    if (!isOwned[0]) {
                        listener.onNotOwned();
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // --- Atomic Checkout Operation ---

    /**
     * Atomic checkout operation using Firestore transaction.
     * ISSUE 3: Fixed credit usage logic and summary calculation.
     */
    public void checkoutCart(String userId, boolean creditsOpted, OnOrderDetailLoadedListener listener) {
        mFirestore.runTransaction((Transaction.Function<String>) transaction -> {
            // 1. Fetch user's cart
            DocumentSnapshot cartSnapshot = transaction.get(mFirestore.collection("carts").document(userId));
            Cart cart = cartSnapshot.toObject(Cart.class);

            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            // 2. Fetch user's current earnedCredit
            DocumentSnapshot userSnapshot = transaction.get(mFirestore.collection("users").document(userId));
            User user = userSnapshot.toObject(User.class);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            // 3. Calculate order totals
            int subtotal = cart.getCartTotal();
            int discount = 0; // Default
            int creditsBefore = user.getEarnedCredit();
            
            // Logic Fix: creditsUsed cannot exceed total cost
            int creditsUsed = (creditsOpted && creditsBefore > 0) ? Math.min(creditsBefore, subtotal) : 0;
            int totalCost = subtotal - discount - creditsUsed;
            int creditsAfter = creditsBefore - creditsUsed;
            long currentTimestamp = DateUtils.getCurrentTimestamp();

            // 4. Create new order
            String orderId = mFirestore.collection("orders").document().getId();
            Order newOrder = new Order(
                    orderId,
                    userId,
                    currentTimestamp,
                    "Completed", // Mark as completed after transaction
                    cart.getItemCount(),
                    subtotal,
                    discount,
                    totalCost,
                    creditsBefore,
                    creditsUsed,
                    creditsAfter,
                    currentTimestamp,
                    currentTimestamp
            );

            transaction.set(mFirestore.collection("orders").document(orderId), newOrder);

            // 5. Deduct credits if used
            if (creditsUsed > 0) {
                transaction.update(
                        mFirestore.collection("users").document(userId),
                        "earnedCredit", creditsAfter,
                        "updatedAt", currentTimestamp
                );
            }

            // 6. Clear cart
            transaction.update(
                    mFirestore.collection("carts").document(userId),
                    "items", new ArrayList<>(),
                    "updatedAt", currentTimestamp
            );

            return orderId;
        }).addOnSuccessListener(orderId -> {
            // Return the created order details
            getOrderDetails((String) orderId, listener);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Checkout failed: " + e.getMessage());
            listener.onError("Checkout failed: " + e.getMessage());
        });
    }

    /**
     * Gets the current authenticated user's ID.
     * Returns null if no user is logged in.
     */
    public String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }
}
