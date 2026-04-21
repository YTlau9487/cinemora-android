package com.cinemora.movieorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for cart items.
 * Displays movie items in the cart with quantity and price information.
 */
public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    private List<CartItem> cartItems;
    private Context context;
    private OnCartItemActionListener listener;

    public interface OnCartItemActionListener {
        void onRemoveItem(String movieId);
        void onQuantityChanged(String movieId, int newQuantity);
    }

    public CartItemAdapter(Context context, List<CartItem> cartItems, OnCartItemActionListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.tvMovieName.setText(item.getMovieName());
        holder.tvPrice.setText(DateUtils.formatCurrency(item.getCost()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvItemTotal.setText(DateUtils.formatCurrency(item.getItemTotal()));

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveItem(item.getMovieId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class CartItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvMovieName;
        TextView tvPrice;
        TextView tvQuantity;
        TextView tvItemTotal;
        TextView btnRemove;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMovieName = itemView.findViewById(R.id.tvMovieName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvItemTotal);
            btnRemove = itemView.findViewById(R.id.btnRemoveItem);
        }
    }
}
