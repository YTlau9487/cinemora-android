package com.cinemora.movieorder;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

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
        holder.tvMovieCost.setText(DateUtils.formatCurrency(item.getCost()));

        Glide.with(holder.itemView.getContext())
                .load(item.getPosterUrl())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.ivMoviePoster);

        // Simulated Portal: open ViewMovieActivity
        holder.btnViewMovie.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ViewMovieActivity.class);
            intent.putExtra("MOVIE_ID", item.getMovieId());
            intent.putExtra("MOVIE_NAME", item.getMovieName());
            intent.putExtra("POSTER_URL", item.getPosterUrl());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMoviePoster;
        TextView tvMovieName, tvMovieCost;
        MaterialButton btnViewMovie;

        ViewHolder(View itemView) {
            super(itemView);
            ivMoviePoster = itemView.findViewById(R.id.ivMoviePoster);
            tvMovieName = itemView.findViewById(R.id.tvMovieName);
            tvMovieCost = itemView.findViewById(R.id.tvMovieCost);
            btnViewMovie = itemView.findViewById(R.id.btnViewMovie);
        }
    }
}
