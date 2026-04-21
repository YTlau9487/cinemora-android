package com.cinemora.movieorder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieList;
    private Context context;
    private boolean isFeatured;

    public MovieAdapter(Context context, List<Movie> movieList, boolean isFeatured) {
        this.context = context;
        this.movieList = movieList;
        this.isFeatured = isFeatured;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isFeatured ? R.layout.item_movie_featured : R.layout.item_movie_list;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.tvTitle.setText(movie.getMovieName());
        holder.tvRating.setText(String.valueOf(movie.getRating()));
        holder.tvPrice.setText(DateUtils.formatCurrency(movie.getCost()));

        // Use Glide for image loading
        Glide.with(context)
                .load(movie.getPosterUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.imgPoster);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("MOVIE_ID", movie.getId());
            context.startActivity(intent);
        });

        // Add to cart logic (Placeholder for now)
        View btnAdd = holder.itemView.findViewById(isFeatured ? R.id.btnAddFeatured : R.id.btnAddMovie);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                // Implement cart logic
            });
        }
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle, tvRating, tvPrice;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgMoviePoster);
            tvTitle = itemView.findViewById(R.id.tvMovieTitle);
            tvRating = itemView.findViewById(R.id.tvMovieRating);
            tvPrice = itemView.findViewById(R.id.tvMoviePrice);
        }
    }
}