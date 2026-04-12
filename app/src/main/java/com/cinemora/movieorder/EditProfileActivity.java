package com.cinemora.movieorder;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Activity for editing user profile information.
 */
public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // We removed getSupportActionBar().setDisplayShowTitleEnabled(false);
        // because we want the title set in XML (or via toolbar.setTitle) to be visible.

        // Handle back navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Buttons
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnCancel = findViewById(R.id.btnCancel);

        // For now, both buttons simply return to the previous screen
        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                // TODO: Implement save logic here
                finish();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }
}