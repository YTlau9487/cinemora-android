package com.cinemora.movieorder;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mFirestore;
    private final FirebaseStorage mStorage;

    // --- Interfaces for Callbacks ---
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String message);
    }

    public interface OnUserDataLoadedListener {
        void onLoaded(User user);
        void onError(String message);
    }

    public interface OnMoviesLoadedListener {
        void onLoaded(List<Movie> movies);
        void onError(String message);
    }

    public interface OnOrdersLoadedListener {
        void onLoaded(List<Order> orders);
        void onError(String message);
    }

    public interface OnOrderDetailLoadedListener {
        void onLoaded(Order order);
        void onError(String message);
    }

    public interface OnPurchaseVerifyListener {
        void onResult(boolean isOwned);
        void onError(String message);
    }

    public interface OnOwnedMoviesLoadedListener {
        void onLoaded(Set<String> movieIds);
        void onError(String message);
    }

    public FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    // --- User Logic ---
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

    // --- Movie Logic ---
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

    // --- Order & Purchase Logic ---
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

    public void checkUserOwnsMovie(String userId, String movieId, OnPurchaseVerifyListener listener) {
        if (userId == null) {
            listener.onResult(false);
            return;
        }

        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    final boolean[] found = {false};
                    final int totalOrders = queryDocumentSnapshots.size();
                    
                    if (totalOrders == 0) {
                        listener.onResult(false);
                        return;
                    }

                    final int[] counter = {0};
                    for (DocumentSnapshot orderDoc : queryDocumentSnapshots) {
                        orderDoc.getReference().collection("orderItems")
                                .whereEqualTo("movieId", movieId)
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    counter[0]++;
                                    if (!itemSnapshots.isEmpty()) {
                                        found[0] = true;
                                    }
                                    
                                    if (found[0]) {
                                        listener.onResult(true);
                                    } else if (counter[0] == totalOrders) {
                                        listener.onResult(false);
                                    }
                                });
                        if (found[0]) break;
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void getUserOwnedMovieIds(String userId, OnOwnedMoviesLoadedListener listener) {
        if (userId == null) {
            listener.onLoaded(new HashSet<>());
            return;
        }

        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> ownedIds = new HashSet<>();
                    final int totalOrders = queryDocumentSnapshots.size();
                    if (totalOrders == 0) {
                        listener.onLoaded(ownedIds);
                        return;
                    }

                    final int[] counter = {0};
                    for (DocumentSnapshot orderDoc : queryDocumentSnapshots) {
                        orderDoc.getReference().collection("orderItems").get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    for (DocumentSnapshot itemDoc : itemSnapshots) {
                                        String movieId = itemDoc.getString("movieId");
                                        if (movieId != null) ownedIds.add(movieId);
                                    }
                                    counter[0]++;
                                    if (counter[0] == totalOrders) {
                                        listener.onLoaded(ownedIds);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void processCheckout(String userId, List<CartItem> cartItems, boolean creditsOpted, OnOrderDetailLoadedListener listener) {
        mFirestore.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(mFirestore.collection("users").document(userId));
            User user = userSnapshot.toObject(User.class);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            int subtotal = 0;
            for (CartItem item : cartItems) {
                subtotal += item.getItemTotal();
            }

            int creditGain = (int) Math.ceil(subtotal / 10.0);
            int creditsBefore = user.getEarnedCredit();
            int creditsUsed = (creditsOpted && creditsBefore > 0) ? Math.min(creditsBefore, subtotal) : 0;
            int totalCost = subtotal - creditsUsed;
            int creditsAfter = creditsBefore - creditsUsed + creditGain;
            long currentTimestamp = DateUtils.getCurrentTimestamp();

            String orderId = mFirestore.collection("orders").document().getId();
            Order newOrder = new Order(
                    orderId, userId, currentTimestamp, "Completed",
                    cartItems.size(), subtotal, 0, totalCost,
                    creditsBefore, creditsUsed, creditsAfter,
                    currentTimestamp, currentTimestamp
            );

            transaction.set(mFirestore.collection("orders").document(orderId), newOrder);
            
            for (CartItem item : cartItems) {
                String itemId = mFirestore.collection("orders").document(orderId).collection("orderItems").document().getId();
                transaction.set(mFirestore.collection("orders").document(orderId).collection("orderItems").document(itemId), item);
                transaction.update(mFirestore.collection("movies").document(item.getMovieId()), "saleCount", com.google.firebase.firestore.FieldValue.increment(1));
            }

            transaction.update(mFirestore.collection("users").document(userId), "earnedCredit", creditsAfter, "updatedAt", currentTimestamp);

            return orderId;
        }).addOnSuccessListener(orderId -> {
            getOrderDetails(orderId, listener);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Checkout failed: " + e.getMessage());
            listener.onError("Checkout failed: " + e.getMessage());
        });
    }

    public String getCurrentUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }
}
