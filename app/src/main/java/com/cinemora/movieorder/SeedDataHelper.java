package com.cinemora.movieorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeedDataHelper {
    private static final String TAG = "SeedDataHelper";
    private static final String PREFS_NAME = "CinemoraPrefs";
    private static final String KEY_SEED_VERSION = "seedVersion";
    // TASK 1: Bumped version to 8 for updated URLs, subcollections, and strictly integer costs
    private static final int REQUIRED_SEED_VERSION = 8; 

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnSeedCompleteListener {
        void onSuccess(String message);
        void onFailure(String message);
    }

    /**
     * Checks if seeding is required based on the version number.
     */
    public static boolean isSeedRequired(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentVersion = prefs.getInt(KEY_SEED_VERSION, 0);
        return currentVersion < REQUIRED_SEED_VERSION;
    }

    /**
     * Updates the seed version in SharedPreferences.
     */
    private static void updateSeedVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SEED_VERSION, REQUIRED_SEED_VERSION).apply();
    }

    /**
     * Main entry point to run the reseed operation.
     */
    public static void runReseed(Context context, OnSeedCompleteListener listener) {
        Log.d(TAG, "Starting reseed operation...");

        deleteAllDataRecursive()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                Log.d(TAG, "✓ Step 1: Deletion complete");
                return seedAuthUsersTask();
            })
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                Map<String, String> userIds = task.getResult();
                Log.d(TAG, "✓ Step 2: Auth users seeded/updated");
                return seedFirestoreDataTask(userIds);
            })
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ Step 3: Firestore data seeded");
                updateSeedVersion(context);
                listener.onSuccess("Data reseeded successfully to version " + REQUIRED_SEED_VERSION);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Reseed failed: " + e.getMessage(), e);
                listener.onFailure("Reseed failed: " + e.getMessage());
            });
    }

    private static Task<Void> deleteAllDataRecursive() {
        String[] collections = {"movies", "moviePreviews", "users", "carts"};
        List<Task<Void>> tasks = new ArrayList<>();
        tasks.add(deleteOrdersCollectionRecursive());
        for (String col : collections) {
            tasks.add(deleteCollectionTask(col));
        }
        return Tasks.whenAll(tasks);
    }

    private static Task<Void> deleteOrdersCollectionRecursive() {
        return db.collection("orders").get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            List<Task<Void>> deleteTasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                deleteTasks.add(deleteCollectionTask(doc.getReference().collection("orderItems")));
                deleteTasks.add(doc.getReference().delete());
            }
            return Tasks.whenAll(deleteTasks);
        });
    }

    private static Task<Void> deleteCollectionTask(String collectionName) {
        return deleteCollectionTask(db.collection(collectionName));
    }

    private static Task<Void> deleteCollectionTask(CollectionReference collection) {
        return collection.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : task.getResult()) {
                batch.delete(doc.getReference());
            }
            return batch.commit();
        });
    }

    private static Task<Map<String, String>> seedAuthUsersTask() {
        Map<String, String> userIds = new HashMap<>();
        return createOrUpdateUser("alice@example.com", "password123")
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                userIds.put("alice", task.getResult());
                return createOrUpdateUser("bob@example.com", "password123");
            })
            .continueWith(task -> {
                if (!task.isSuccessful()) throw task.getException();
                userIds.put("bob", task.getResult());
                return userIds;
            });
    }

    private static Task<String> createOrUpdateUser(String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    return Tasks.forResult(task.getResult().getUser().getUid());
                } else {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        return auth.signInWithEmailAndPassword(email, password)
                            .continueWithTask(signInTask -> {
                                if (signInTask.isSuccessful()) {
                                    return Tasks.forResult(signInTask.getResult().getUser().getUid());
                                } else {
                                    throw signInTask.getException();
                                }
                            });
                    }
                    throw task.getException();
                }
            });
    }

    private static Task<Void> seedFirestoreDataTask(Map<String, String> userIds) {
        return db.runTransaction(transaction -> {
            seedMovies(transaction);
            seedMoviePreviews(transaction);
            seedUsersInFirestore(transaction, userIds);
            seedOrders(transaction, userIds);
            seedCarts(transaction, userIds);
            return null;
        });
    }

    private static void seedMovies(Transaction transaction) {
        long currentTime = System.currentTimeMillis() / 1000;
        
        // Movie 1: Avengers: Endgame
        Map<String, Object> movie1 = new HashMap<>();
        movie1.put("id", "movie_1");
        movie1.put("movieName", "Avengers: Endgame");
        movie1.put("cost", 130);
        movie1.put("rating", 4);
        movie1.put("genres", new ArrayList<String>() {{ add("Action"); add("Adventure"); add("Sci-Fi"); }});
        movie1.put("duration", 181);
        movie1.put("releaseDate", 1554681600L);
        movie1.put("overview", "After the devastating events, the Avengers assemble once more to reverse Thanos' actions and restore balance to the universe.");
        movie1.put("director", "Anthony & Joe Russo");
        movie1.put("cast", new ArrayList<String>() {{ add("Robert Downey Jr."); add("Chris Evans"); add("Mark Ruffalo"); }});
        movie1.put("language", "English");
        movie1.put("subtitles", new ArrayList<String>() {{ add("English"); add("Chinese"); add("Japanese"); }});
        movie1.put("resolution", "4K");
        movie1.put("saleCount", 150);
        movie1.put("posterUrl", "https://drive.google.com/uc?id=1H8pYkOwxSQmwL1S1VlzjydC93WHOzbt5");
        movie1.put("createdAt", currentTime);
        movie1.put("updatedAt", currentTime);
        transaction.set(db.collection("movies").document("movie_1"), movie1);

        // Movie 2: Avatar
        Map<String, Object> movie2 = new HashMap<>();
        movie2.put("id", "movie_2");
        movie2.put("movieName", "Avatar");
        movie2.put("cost", 120);
        movie2.put("rating", 4);
        movie2.put("genres", new ArrayList<String>() {{ add("Action"); add("Adventure"); add("Fantasy"); }});
        movie2.put("duration", 162);
        movie2.put("releaseDate", 1261353600L);
        movie2.put("overview", "A paraplegic Marine dispatched to the moon Pandora on a unique mission becomes torn between following his orders and protecting the world he feels is his home.");
        movie2.put("director", "James Cameron");
        movie2.put("cast", new ArrayList<String>() {{ add("Sam Worthington"); add("Zoe Saldana"); add("Stephen Lang"); }});
        movie2.put("language", "English");
        movie2.put("subtitles", new ArrayList<String>() {{ add("English"); add("Chinese"); }});
        movie2.put("resolution", "4K");
        movie2.put("saleCount", 200);
        movie2.put("posterUrl", "https://drive.google.com/uc?id=1tKZQpjTxhZ95x4-ep2se8_37VgqTcj53");
        movie2.put("createdAt", currentTime);
        movie2.put("updatedAt", currentTime);
        transaction.set(db.collection("movies").document("movie_2"), movie2);

        // Movie 3: The Dark Knight
        Map<String, Object> movie3 = new HashMap<>();
        movie3.put("id", "movie_3");
        movie3.put("movieName", "The Dark Knight");
        movie3.put("cost", 90);
        movie3.put("rating", 5);
        movie3.put("genres", new ArrayList<String>() {{ add("Action"); add("Crime"); add("Drama"); }});
        movie3.put("duration", 152);
        movie3.put("releaseDate", 1218393600L);
        movie3.put("overview", "When the menace known as the Joker wreaks havoc on Gotham, Batman must accept one of the greatest tests to fight injustice.");
        movie3.put("director", "Christopher Nolan");
        movie3.put("cast", new ArrayList<String>() {{ add("Christian Bale"); add("Heath Ledger"); add("Aaron Eckhart"); }});
        movie3.put("language", "English");
        movie3.put("subtitles", new ArrayList<String>() {{ add("English"); add("Chinese"); add("Korean"); }});
        movie3.put("resolution", "4K");
        movie3.put("saleCount", 180);
        movie3.put("posterUrl", "https://drive.google.com/uc?id=11f2Y4Nrm6w-tibGxkn0gqS7joH2B393J");
        movie3.put("createdAt", currentTime);
        movie3.put("updatedAt", currentTime);
        transaction.set(db.collection("movies").document("movie_3"), movie3);

        // Movie 4: Inception
        Map<String, Object> movie4 = new HashMap<>();
        movie4.put("id", "movie_4");
        movie4.put("movieName", "Inception");
        movie4.put("cost", 100);
        movie4.put("rating", 4);
        movie4.put("genres", new ArrayList<String>() {{ add("Action"); add("Sci-Fi"); add("Thriller"); }});
        movie4.put("duration", 148);
        movie4.put("releaseDate", 1278892800L);
        movie4.put("overview", "A skilled thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea.");
        movie4.put("director", "Christopher Nolan");
        movie4.put("cast", new ArrayList<String>() {{ add("Leonardo DiCaprio"); add("Joseph Gordon-Levitt"); add("Ellen Page"); }});
        movie4.put("language", "English");
        movie4.put("subtitles", new ArrayList<String>() {{ add("English"); add("Chinese"); }});
        movie4.put("resolution", "4K");
        movie4.put("saleCount", 160);
        movie4.put("posterUrl", "https://drive.google.com/uc?id=1PrRdyvIcxiZXvZhzN4x8zzygMilB7i-u");
        movie4.put("createdAt", currentTime);
        movie4.put("updatedAt", currentTime);
        transaction.set(db.collection("movies").document("movie_4"), movie4);

        // Movie 5: Spider-Man: No Way Home
        Map<String, Object> movie5 = new HashMap<>();
        movie5.put("id", "movie_5");
        movie5.put("movieName", "Spider-Man: No Way Home");
        movie5.put("cost", 150);
        movie5.put("rating", 4);
        movie5.put("genres", new ArrayList<String>() {{ add("Action"); add("Adventure"); add("Sci-Fi"); }});
        movie5.put("duration", 159);
        movie5.put("releaseDate", 1640188800L);
        movie5.put("overview", "With Spider-Man's identity now revealed, Peter asks Doctor Strange for help. When Strange casts a spell, the multiverse breaks open.");
        movie5.put("director", "Jon Watts");
        movie5.put("cast", new ArrayList<String>() {{ add("Tom Holland"); add("Zendaya"); add("Benedict Cumberbatch"); }});
        movie5.put("language", "English");
        movie5.put("subtitles", new ArrayList<String>() {{ add("English"); add("Chinese"); add("Spanish"); }});
        movie5.put("resolution", "4K");
        movie5.put("saleCount", 190);
        movie5.put("posterUrl", "https://drive.google.com/uc?id=1mUVhqMaEaaGE41JsA6CpV18BfzlQY3uc");
        movie5.put("createdAt", currentTime);
        movie5.put("updatedAt", currentTime);
        transaction.set(db.collection("movies").document("movie_5"), movie5);
    }

    private static void seedMoviePreviews(Transaction transaction) {
        long currentTime = System.currentTimeMillis() / 1000;

        // Requirement 1A: Exact YouTube URLs based on movie name
        transaction.set(db.collection("moviePreviews").document("preview_1"), new HashMap<String, Object>() {{
            put("movieId", "movie_1"); put("movieUrl", "https://www.youtube.com/watch?v=TcMBFSGVi1c"); // Avengers: Endgame
            put("previewType", "trailer"); put("platform", "youtube"); put("createdAt", currentTime);
        }});
        transaction.set(db.collection("moviePreviews").document("preview_2"), new HashMap<String, Object>() {{
            put("movieId", "movie_2"); put("movieUrl", "https://www.youtube.com/watch?v=5PSNL1qE6VY"); // Avatar
            put("previewType", "trailer"); put("platform", "youtube"); put("createdAt", currentTime);
        }});
        transaction.set(db.collection("moviePreviews").document("preview_3"), new HashMap<String, Object>() {{
            put("movieId", "movie_3"); put("movieUrl", "https://www.youtube.com/watch?v=EXeTwQWrcwY"); // The Dark Knight
            put("previewType", "trailer"); put("platform", "youtube"); put("createdAt", currentTime);
        }});
        transaction.set(db.collection("moviePreviews").document("preview_4"), new HashMap<String, Object>() {{
            put("movieId", "movie_4"); put("movieUrl", "https://www.youtube.com/watch?v=YoHD9XEInc0"); // Inception
            put("previewType", "trailer"); put("platform", "youtube"); put("createdAt", currentTime);
        }});
        transaction.set(db.collection("moviePreviews").document("preview_5"), new HashMap<String, Object>() {{
            put("movieId", "movie_5"); put("movieUrl", "https://www.youtube.com/watch?v=JfVOs4VSpmA"); // Spider-Man: No Way Home
            put("previewType", "trailer"); put("platform", "youtube"); put("createdAt", currentTime);
        }});
    }

    private static void seedUsersInFirestore(Transaction transaction, Map<String, String> userIds) {
        long currentTime = System.currentTimeMillis() / 1000;
        String aliceUid = userIds.get("alice");
        transaction.set(db.collection("users").document(aliceUid), new HashMap<String, Object>() {{
            put("userId", aliceUid); put("name", "Alice Wong"); put("email", "alice@example.com");
            put("earnedCredit", 150); put("createdAt", currentTime); put("updatedAt", currentTime);
        }});

        String bobUid = userIds.get("bob");
        transaction.set(db.collection("users").document(bobUid), new HashMap<String, Object>() {{
            put("userId", bobUid); put("name", "Bob Chan"); put("email", "bob@example.com");
            put("earnedCredit", 80); put("createdAt", currentTime); put("updatedAt", currentTime);
        }});
    }

    private static void seedOrders(Transaction transaction, Map<String, String> userIds) {
        long currentTime = System.currentTimeMillis() / 1000;
        String user1Id = userIds.get("alice");
        String user2Id = userIds.get("bob");

        // Order 1: Alice
        String orderId1 = "order_001";
        transaction.set(db.collection("orders").document(orderId1), new HashMap<String, Object>() {{
            put("orderId", orderId1); put("userId", user1Id); put("orderDate", currentTime - 2592000);
            put("progress", "Delivered"); put("itemCount", 3); put("subtotal", 380);
            put("discount", 0); put("totalCost", 280); put("creditsUsed", 100);
            put("createdAt", currentTime - 2592000); put("updatedAt", currentTime);
        }});
        // TASK 1B: Add orderItems subcollection with INTEGER costs
        seedOrderItem(transaction, orderId1, "item_1", "movie_1", "Avengers: Endgame", 130, 1);
        seedOrderItem(transaction, orderId1, "item_2", "movie_4", "Inception", 100, 1);
        seedOrderItem(transaction, orderId1, "item_3", "movie_5", "Spider-Man: No Way Home", 150, 1);

        // Order 2: Alice
        String orderId2 = "order_002";
        transaction.set(db.collection("orders").document(orderId2), new HashMap<String, Object>() {{
            put("orderId", orderId2); put("userId", user1Id); put("orderDate", currentTime - 604800);
            put("progress", "Processing"); put("itemCount", 1); put("subtotal", 120);
            put("totalCost", 120); put("createdAt", currentTime - 604800); put("updatedAt", currentTime);
        }});
        seedOrderItem(transaction, orderId2, "item_1", "movie_2", "Avatar", 120, 1);

        // Order 3: Bob
        String orderId3 = "order_003";
        transaction.set(db.collection("orders").document(orderId3), new HashMap<String, Object>() {{
            put("orderId", orderId3); put("userId", user2Id); put("orderDate", currentTime - 1209600);
            put("progress", "Shipped"); put("itemCount", 2); put("subtotal", 190);
            put("totalCost", 140); put("creditsUsed", 50);
            put("createdAt", currentTime - 1209600); put("updatedAt", currentTime);
        }});
        seedOrderItem(transaction, orderId3, "item_1", "movie_3", "The Dark Knight", 90, 1);
        seedOrderItem(transaction, orderId3, "item_2", "movie_4", "Inception", 100, 1);
    }

    private static void seedOrderItem(Transaction transaction, String orderId, String itemId, String movieId, String movieName, int cost, int quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("movieId", movieId);
        item.put("movieName", movieName);
        item.put("cost", cost); // Corrected to integer
        item.put("quantity", quantity);
        transaction.set(db.collection("orders").document(orderId).collection("orderItems").document(itemId), item);
    }

    private static void seedCarts(Transaction transaction, Map<String, String> userIds) {
        long currentTime = System.currentTimeMillis() / 1000;
        String user1Id = userIds.get("alice");
        String user2Id = userIds.get("bob");

        transaction.set(db.collection("carts").document(user1Id), new HashMap<String, Object>() {{
            put("userId", user1Id); put("updatedAt", currentTime);
            put("items", new ArrayList<Map<String, Object>>() {{
                add(new HashMap<String, Object>() {{ put("movieId", "movie_3"); put("movieName", "The Dark Knight"); put("cost", 90); put("quantity", 1); }});
            }});
        }});

        transaction.set(db.collection("carts").document(user2Id), new HashMap<String, Object>() {{
            put("userId", user2Id); put("updatedAt", currentTime);
            put("items", new ArrayList<Map<String, Object>>() {{
                add(new HashMap<String, Object>() {{ put("movieId", "movie_1"); put("movieName", "Avengers: Endgame"); put("cost", 130); put("quantity", 1); }});
            }});
        }});
    }
}
