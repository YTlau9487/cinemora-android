package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Main search screen for the application.
 * Features a refined UI with an expandable advanced filter accordion and recent searches.
 */
public class SearchActivity extends AppCompatActivity {

    // UI Components
    private EditText etSearchInput;
    private View btnAdvancedFilters;
    private View llAccordionContainer;
    private LinearLayout filterPanel;
    private ImageView ivFilterArrow;
    private Spinner spinnerYear;
    private ChipGroup cgGenres;
    private EditText etDirector;
    private TextView tvFilterSummary;
    private TextView tvFilterCount;
    
    // State flag for the accordion visibility
    private boolean isFilterVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupListeners();
        setupYearSpinner();
        
        // Initial summary update based on default values
        updateFilterSummary();
    }

    /**
     * Initializes all view references from the layout.
     */
    private void initViews() {
        etSearchInput = findViewById(R.id.etSearchInput);
        btnAdvancedFilters = findViewById(R.id.btnAdvancedFilters);
        llAccordionContainer = findViewById(R.id.llAccordionContainer);
        filterPanel = findViewById(R.id.filterPanel);
        ivFilterArrow = findViewById(R.id.ivFilterArrow);
        spinnerYear = findViewById(R.id.spinnerYear);
        cgGenres = findViewById(R.id.cgGenres);
        etDirector = findViewById(R.id.etDirector);
        tvFilterSummary = findViewById(R.id.tvFilterSummary);
        tvFilterCount = findViewById(R.id.tvFilterCount);
        
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());
    }

    /**
     * Sets up interaction listeners for search, filtering, and animations.
     */
    private void setupListeners() {
        // Toggle the advanced filters panel with a smooth animation
        btnAdvancedFilters.setOnClickListener(v -> {
            isFilterVisible = !isFilterVisible;
            
            // Highlight the container when expanded
            llAccordionContainer.setSelected(isFilterVisible);
            
            // Smoothly fade the filter panel in/out
            if (isFilterVisible) {
                filterPanel.setVisibility(View.VISIBLE);
                filterPanel.setAlpha(0f);
                filterPanel.animate().alpha(1f).setDuration(250).start();
            } else {
                filterPanel.animate().alpha(0f).setDuration(200).withEndAction(() -> 
                    filterPanel.setVisibility(View.GONE)).start();
            }
            
            // Animate the chevron rotation for the accordion effect
            ivFilterArrow.animate()
                    .rotation(isFilterVisible ? 180f : 0f)
                    .setDuration(250)
                    .start();
        });

        // Trigger search on "Search" button click
        findViewById(R.id.tvSearchBtn).setOnClickListener(v -> performSearch());
        
        // Trigger search on keyboard "Enter" (Search) action
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // Auto-update the filter summary when inputs change
        cgGenres.setOnCheckedStateChangeListener((group, checkedIds) -> updateFilterSummary());
        
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFilterSummary();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etDirector.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateFilterSummary();
            }
        });
    }

    /**
     * Populates the release year dropdown with static data.
     */
    private void setupYearSpinner() {
        String[] years = {"All Years", "2024", "2023", "2022", "2021", "2020", "Before 2020"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(adapter);
    }

    /**
     * Aggregates active filters and updates the collapsed state summary and badge count.
     */
    private void updateFilterSummary() {
        List<String> activeFilters = new ArrayList<>();
        
        // Add selected genre to summary
        int checkedId = cgGenres.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = findViewById(checkedId);
            if (chip != null) activeFilters.add(chip.getText().toString());
        }

        // Add selected year to summary (if not "All Years")
        String selectedYear = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "";
        if (!selectedYear.isEmpty() && !selectedYear.equals("All Years")) {
            activeFilters.add(selectedYear);
        }

        // Add director name to summary
        String director = etDirector.getText().toString().trim();
        if (!director.isEmpty()) {
            activeFilters.add(director);
        }

        // Update the visibility of the summary row and badge based on active selections
        if (activeFilters.isEmpty()) {
            tvFilterSummary.setVisibility(View.GONE);
            tvFilterCount.setVisibility(View.GONE);
        } else {
            tvFilterSummary.setVisibility(View.VISIBLE);
            tvFilterSummary.setText(String.join(" • ", activeFilters));
            
            tvFilterCount.setVisibility(View.VISIBLE);
            tvFilterCount.setText(String.valueOf(activeFilters.size()));
        }
    }

    /**
     * Navigates to the SearchResultActivity with the user's query.
     */
    private void performSearch() {
        String query = etSearchInput.getText().toString().trim();
        
        // Extract filters
        String genre = "";
        int checkedId = cgGenres.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = findViewById(checkedId);
            if (chip != null) genre = chip.getText().toString();
        }

        String year = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "";
        String director = etDirector.getText().toString().trim();

        if (query.isEmpty() && genre.isEmpty() && (year.isEmpty() || year.equals("All Years")) && director.isEmpty()) {
            Toast.makeText(this, "Please enter a search term or select a filter", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra("QUERY", query);
        intent.putExtra("GENRE", genre);
        intent.putExtra("YEAR", year);
        intent.putExtra("DIRECTOR", director);
        
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Handle incoming queries when activity is already in front
        String query = intent.getStringExtra("QUERY");
        if (query != null && etSearchInput != null) {
            etSearchInput.setText(query);
        }
    }
}