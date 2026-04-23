package com.cinemora.movieorder;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieList;
    private Context context;
    private boolean isFeatured;
    private Set<String> ownedMovieIds = new HashSet<>();
    private Set<String> cartMovieIds = new HashSet<>();

    public MovieAdapter(Context context, List<Movie> movieList, boolean isFeatured) {
        this.context = context;
        this.movieList = movieList;
        this.isFeatured = isFeatured;
    }

    /**
     * Updates the set of owned movie IDs to enforce purchase restrictions.
     */
    public void setOwnedMovieIds(Set<String> ownedMovieIds) {
        this.ownedMovieIds = ownedMovieIds != null ? ownedMovieIds : new HashSet<>();
        notifyDataSetChanged();
    }

    /**
     * Updates the set of movie IDs currently in the cart to prevent duplicate additions.
     */
    public void setCartMovieIds(Set<String> cartMovieIds) {
        this.cartMovieIds = cartMovieIds != null ? cartMovieIds : new HashSet<>();
        notifyDataSetChanged();
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
        
        holder.tvRating.setText(String.format(Locale.US, "%.1f", (double) movie.getRating()));
        holder.tvPrice.setText(String.format(Locale.getDefault(), "HKD %d", movie.getCost()));

        // Improved Image Loading: Use a neutral placeholder and smooth cross-fade
        Glide.with(context)
                .load(movie.getPosterUrl())
                .placeholder(new ColorDrawable(ContextCompat.getColor(context, R.color.shimmer_placeholder)))
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(new ColorDrawable(ContextCompat.getColor(context, R.color.shimmer_placeholder)))
                .into(holder.imgPoster);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("MOVIE_ID", movie.getId());
            context.startActivity(intent);
        });

        View btnAdd = holder.itemView.findViewById(isFeatured ? R.id.btnAddFeatured : R.id.btnAddMovie);
        if (btnAdd != null) {
            // Task: Purchase & Cart Restriction Logic
            boolean isOwned = ownedMovieIds.contains(movie.getId());
            boolean isInCart = cartMovieIds.contains(movie.getId());
            
            if (isOwned || isInCart) {
                // Task Refinement: Make the button completely invisible if already bought or in cart
                btnAdd.setVisibility(View.GONE);
                btnAdd.setOnClickListener(null);
            } else {
                btnAdd.setVisibility(View.VISIBLE);
                btnAdd.setEnabled(true);
                btnAdd.setAlpha(1.0f);
                
                btnAdd.setOnClickListener(v -> {
                    btnAdd.setEnabled(false);
                    CartItem item = new CartItem(
                            movie.getId(),
                            movie.getMovieName(),
                            movie.getPosterUrl(),
                            movie.getCost(),
                            1,
                            System.currentTimeMillis() / 1000
                    );
                    CartManager.getInstance(context).addToCart(item);
                    
                    // Update local state and UI immediately
                    cartMovieIds.add(movie.getId());
                    btnAdd.setVisibility(View.GONE);
                    
                    Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show();
                });
            }
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
