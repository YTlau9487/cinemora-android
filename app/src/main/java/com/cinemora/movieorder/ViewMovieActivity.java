package com.cinemora.movieorder;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ViewMovieActivity extends AppCompatActivity {

    private String movieName, posterUrl;
    private TextView tvMovieTitle;
    private ImageView imgMovieBackdrop;
    private MaterialButton btnDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_movie);

        movieName = getIntent().getStringExtra("MOVIE_NAME");
        posterUrl = getIntent().getStringExtra("POSTER_URL");

        initializeViews();
        setupData();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        imgMovieBackdrop = findViewById(R.id.imgMovieBackdrop);
        btnDownload = findViewById(R.id.btnDownload);

        btnDownload.setOnClickListener(v -> simulateDownload());
    }

    private void setupData() {
        if (tvMovieTitle != null) tvMovieTitle.setText(movieName);
        if (imgMovieBackdrop != null && posterUrl != null) {
            Glide.with(this)
                    .load(posterUrl)
                    .centerCrop()
                    .into(imgMovieBackdrop);
        }
    }

    private void simulateDownload() {
        btnDownload.setEnabled(false);
        btnDownload.setText("Downloading...");
        
        // Simple simulation delay
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Toast.makeText(this, "Download Complete: " + movieName, Toast.LENGTH_SHORT).show();
                btnDownload.setText("Downloaded");
                btnDownload.setIconResource(android.R.drawable.stat_sys_download_done);
            }
        }, 3000);
    }
}
