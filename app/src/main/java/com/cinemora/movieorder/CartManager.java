package com.cinemora.movieorder;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String PREF_NAME = "CinemoraCartPrefs";
    private static final String KEY_CART_ITEMS = "cart_items";
    private static CartManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    private CartManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Adds an item to the cart. 
     * Requirement: User can only add a movie once. 
     * If the movie already exists, we do nothing.
     */
    public void addToCart(CartItem item) {
        List<CartItem> cartItems = getCartItems();
        boolean exists = false;
        for (CartItem existingItem : cartItems) {
            if (existingItem.getMovieId().equals(item.getMovieId())) {
                // Task: User could only add movie once. No quantity increment.
                exists = true;
                break;
            }
        }
        if (!exists) {
            cartItems.add(item);
        }
        saveCartItems(cartItems);
    }

    public List<CartItem> getCartItems() {
        String json = sharedPreferences.getString(KEY_CART_ITEMS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void removeItem(String movieId) {
        List<CartItem> cartItems = getCartItems();
        cartItems.removeIf(item -> item.getMovieId().equals(movieId));
        saveCartItems(cartItems);
    }

    public void clearCart() {
        sharedPreferences.edit().remove(KEY_CART_ITEMS).apply();
    }

    private void saveCartItems(List<CartItem> cartItems) {
        String json = gson.toJson(cartItems);
        sharedPreferences.edit().putString(KEY_CART_ITEMS, json).apply();
    }

    public int getCartTotal() {
        int total = 0;
        for (CartItem item : getCartItems()) {
            total += item.getItemTotal();
        }
        return total;
    }
}
