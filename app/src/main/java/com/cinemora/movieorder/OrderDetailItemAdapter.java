package com.cinemora.movieorder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying order items in Order Detail page using item_order_detail.xml.
 */
public class OrderDetailItemAdapter extends RecyclerView.Adapter<OrderDetailItemAdapter.ViewHolder> {

    private List<CartItem> items = new ArrayList<>();

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.tvMovieName.setText(item.getMovieName());
        holder.tvMovieCost.setText("HKD " + item.getCost());

        // Assuming you have a way to get posterUrl, possibly by fetching movie details or storing it in CartItem
        // For now, if CartItem doesn't have it, we might need a placeholder or update CartItem
        // Glide.with(holder.itemView.getContext()).load(item.getPosterUrl()).into(holder.ivMoviePoster);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMoviePoster;
        TextView tvMovieName, tvMovieCost;

        ViewHolder(View itemView) {
            super(itemView);
            ivMoviePoster = itemView.findViewById(R.id.ivMoviePoster);
            tvMovieName = itemView.findViewById(R.id.tvMovieName);
            tvMovieCost = itemView.findViewById(R.id.tvMovieCost);
        }
    }
}
