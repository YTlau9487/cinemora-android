package com.cinemora.movieorder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Hold time for branding
    private static final int EXIT_ANIM_DURATION = 250; // Snappy exit transition

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View container = findViewById(R.id.splash_container);
        ImageView logo = findViewById(R.id.iv_logo);
        TextView slogan = findViewById(R.id.tv_slogan);

        // Initial states for staggered entrance animation
        logo.setAlpha(0f);
        logo.setScaleX(0.7f);
        logo.setScaleY(0.7f);
        slogan.setAlpha(0f);
        slogan.setTranslationY(20f);

        // 1. Icon Entrance: Scale 0.7 -> 1.0 and Alpha 0 -> 1.0
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500) // Entrance Animation (Unmodified)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 2. Slogan Entrance: Staggered Fade-in + Slide up
        slogan.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400) // Entrance Animation (Unmodified)
                .setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 3. Exit Animation & Transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Subtle exit scale-up and fade-out of the whole container
            container.animate()
                    .alpha(0f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(EXIT_ANIM_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(0, 0); 
                        finish();
                    })
                    .start();
        }, SPLASH_DURATION);
    }
}