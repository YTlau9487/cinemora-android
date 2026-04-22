package com.cinemora.movieorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

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
        holder.tvQuantity.setText("x" + item.getQuantity());
        holder.tvItemTotal.setText(DateUtils.formatCurrency(item.getItemTotal()));

        // Fix Thumbnail issue in Cart
        Glide.with(context)
                .load(item.getPosterUrl())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.imgCartPoster);

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
        ImageView imgCartPoster;
        TextView tvMovieName, tvPrice, tvQuantity, tvItemTotal, btnRemove;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCartPoster = itemView.findViewById(R.id.imgCartPoster);
            tvMovieName = itemView.findViewById(R.id.tvMovieName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvItemTotal);
            btnRemove = itemView.findViewById(R.id.btnRemoveItem);
        }
    }
}
