package com.cinemora.movieorder;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

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
        // Fixed: Added default value for totalOrders (0) to match the new User constructor
        User user = new User(uid, username, email, 0, 0, Timestamp.now());
        
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

    public void getBestSellingMovies(OnMoviesLoadedListener listener) {
        mFirestore.collection("movies")
                .whereEqualTo("isBestSelling", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = queryDocumentSnapshots.toObjects(Movie.class);
                    listener.onLoaded(movies);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // --- Seeding Data (Testing Only) ---

    /**
     * Seeds sample users sequentially to prevent Auth state collisions.
     */
    public void seedSampleUsers() {
        // Mary Chan
        registerUser("mary@example.com", "mary123", "MaryChan", new AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Mary Chan seeded successfully. Proceeding to John Doe...");
                mAuth.signOut();
                
                // John Doe (Sequential call)
                registerUser("john@example.com", "john123", "JohnDoe", new AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Log.d(TAG, "John Doe seeded successfully");
                        mAuth.signOut();
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e(TAG, "John Doe seeding failed: " + message);
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Mary Chan seeding failed: " + message);
            }
        });
    }

    public void seedSampleMovies(Uri sampleImageUri) {
        // 1. Upload sample image to Storage
        StorageReference ref = mStorage.getReference().child("posters/sample_movie.jpg");
        ref.putFile(sampleImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    
                    // 2. Add movies to Firestore
                    List<Movie> movies = new ArrayList<>();
                    movies.add(new Movie(null, "Inception", "A thief who steals corporate secrets through the use of dream-sharing technology.", 50.0, "Sci-Fi", url, 4.8f, true, Timestamp.now()));
                    movies.add(new Movie(null, "The Dark Knight", "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham.", 45.0, "Action", url, 4.9f, true, Timestamp.now()));
                    movies.add(new Movie(null, "Interstellar", "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.", 40.0, "Sci-Fi", url, 4.7f, false, Timestamp.now()));

                    for (Movie movie : movies) {
                        mFirestore.collection("movies").add(movie)
                                .addOnSuccessListener(documentReference -> {
                                    String id = documentReference.getId();
                                    documentReference.update("id", id);
                                });
                    }
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding movies: " + e.getMessage()));
    }
}
