package com.cinemora.movieorder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RecentSearches";
    private static final String KEY_RECENT_SEARCHES = "recent_list";
    private static final int MAX_RECENT_SEARCHES = 10;

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
    private ChipGroup cgRecentSearches;
    private TextView tvClearRecent;
    private TextView tvSearchBtn;
    
    private boolean isFilterVisible = false;
    private boolean isInternalUpdating = false; 
    private Gson gson;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        gson = new Gson();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupListeners();
        setupYearSpinner();
        setupGenreChips();
        loadRecentSearches();
        updateFilterSummary();
    }

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
        cgRecentSearches = findViewById(R.id.cgRecentSearches);
        tvClearRecent = findViewById(R.id.tvClearRecent);
        tvSearchBtn = findViewById(R.id.tvSearchBtn);
        
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());
    }

    private void setupGenreChips() {
        int[][] states = new int[][] {
            new int[] {android.R.attr.state_checked},
            new int[] {-android.R.attr.state_checked}
        };
        int bluePrimary = ContextCompat.getColor(this, R.color.blue_primary);
        int[] bgColors = new int[] { bluePrimary, Color.WHITE };
        int[] textColors = new int[] { Color.WHITE, ContextCompat.getColor(this, R.color.genre_chip_text_color) };
        
        ColorStateList bgStateList = new ColorStateList(states, bgColors);
        ColorStateList textStateList = new ColorStateList(states, textColors);

        for (int i = 0; i < cgGenres.getChildCount(); i++) {
            View child = cgGenres.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChipBackgroundColor(bgStateList);
                chip.setTextColor(textStateList);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!isInternalUpdating) {
                        updateSearchInputFromFilters();
                    }
                    updateFilterSummary();
                });
            }
        }
    }

    private void setupListeners() {
        btnAdvancedFilters.setOnClickListener(v -> {
            isFilterVisible = !isFilterVisible;
            llAccordionContainer.setSelected(isFilterVisible);
            if (isFilterVisible) {
                filterPanel.setVisibility(View.VISIBLE);
                filterPanel.setAlpha(0f);
                filterPanel.animate().alpha(1f).setDuration(250).start();
            } else {
                filterPanel.animate().alpha(0f).setDuration(200).withEndAction(() -> 
                    filterPanel.setVisibility(View.GONE)).start();
            }
            ivFilterArrow.animate().rotation(isFilterVisible ? 180f : 0f).setDuration(250).start();
        });

        tvSearchBtn.setOnClickListener(v -> {
            tvSearchBtn.setEnabled(false);
            performSearch();
        });
        
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isInternalUpdating) {
                    autoDetectFilters(s.toString());
                }
            }
        });

        tvClearRecent.setOnClickListener(v -> clearAllRecentSearches());

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInternalUpdating) {
                    updateSearchInputFromFilters();
                }
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
                if (!isInternalUpdating) {
                    updateSearchInputFromFilters();
                }
                updateFilterSummary();
            }
        });
    }

    private void autoDetectFilters(String input) {
        isInternalUpdating = true;
        
        cgGenres.clearCheck();
        spinnerYear.setSelection(0);

        String[] parts = input.split("\\s+");
        for (String part : parts) {
            if (part.contains(":")) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].toLowerCase();
                    String value = kv[1];
                    
                    if (key.equals("genre")) {
                        String[] genres = value.split(",");
                        for (String g : genres) {
                            checkChipByText(g);
                        }
                    } else if (key.equals("year")) {
                        if (value.equals("<2020")) {
                            selectSpinnerByText("Before 2020");
                        } else {
                            selectSpinnerByText(value);
                        }
                    } else if (key.equals("director")) {
                        if (!etDirector.getText().toString().equalsIgnoreCase(value)) {
                            etDirector.setText(value);
                        }
                    }
                }
            }
        }
        
        isInternalUpdating = false;
        updateFilterSummary();
    }

    private void checkChipByText(String text) {
        for (int i = 0; i < cgGenres.getChildCount(); i++) {
            Chip chip = (Chip) cgGenres.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(text)) {
                chip.setChecked(true);
                break;
            }
        }
    }

    private void selectSpinnerByText(String text) {
        ArrayAdapter adapter = (ArrayAdapter) spinnerYear.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(text)) {
                spinnerYear.setSelection(i);
                break;
            }
        }
    }

    private void updateSearchInputFromFilters() {
        isInternalUpdating = true;
        String currentInput = etSearchInput.getText().toString().trim();
        
        // Remove existing tags
        currentInput = currentInput.replaceAll("\\b(genre|year|director):\\S+", "").trim().replaceAll("\\s+", " ");
        
        StringBuilder sb = new StringBuilder(currentInput);
        
        // Add Genres
        List<Integer> checkedIds = cgGenres.getCheckedChipIds();
        if (!checkedIds.isEmpty()) {
            List<String> genres = new ArrayList<>();
            for (int id : checkedIds) {
                Chip chip = findViewById(id);
                if (chip != null) genres.add(chip.getText().toString().toLowerCase());
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append("genre:").append(String.join(",", genres));
        }
        
        // Add Year
        String selectedYear = spinnerYear.getSelectedItem().toString();
        if (!selectedYear.equals("All Years")) {
            String tagValue = selectedYear.equals("Before 2020") ? "<2020" : selectedYear;
            if (sb.length() > 0) sb.append(" ");
            sb.append("year:").append(tagValue);
        }
        
        // Add Director
        String director = etDirector.getText().toString().trim();
        if (!director.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("director:").append(director.toLowerCase());
        }
        
        String result = sb.toString();
        etSearchInput.setText(result);
        etSearchInput.setSelection(result.length());
        isInternalUpdating = false;
    }

    private void setupYearSpinner() {
        String[] years = {"All Years", "2024", "2023", "2022", "2021", "2020", "Before 2020"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(adapter);
    }

    private void updateFilterSummary() {
        List<String> activeFilters = new ArrayList<>();
        
        List<Integer> checkedIds = cgGenres.getCheckedChipIds();
        if (!checkedIds.isEmpty()) {
            List<String> genres = new ArrayList<>();
            for (int id : checkedIds) {
                Chip chip = findViewById(id);
                if (chip != null) genres.add(chip.getText().toString().toLowerCase());
            }
            activeFilters.add("genre:" + String.join(",", genres));
        }

        String selectedYear = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "";
        if (!selectedYear.isEmpty() && !selectedYear.equals("All Years")) {
            String tagValue = selectedYear.equals("Before 2020") ? "<2020" : selectedYear;
            activeFilters.add("year:" + tagValue);
        }
        String director = etDirector.getText().toString().trim();
        if (!director.isEmpty()) {
            activeFilters.add("director:" + director.toLowerCase());
        }
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

    private void performSearch() {
        String query = etSearchInput.getText().toString().trim();
        
        List<String> genresList = new ArrayList<>();
        List<Integer> checkedIds = cgGenres.getCheckedChipIds();
        for (int id : checkedIds) {
            Chip chip = findViewById(id);
            if (chip != null) genresList.add(chip.getText().toString());
        }
        String genre = String.join(",", genresList);

        String year = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "";
        String director = etDirector.getText().toString().trim();

        if (query.isEmpty() && genre.isEmpty() && (year.isEmpty() || year.equals("All Years")) && director.isEmpty()) {
            Toast.makeText(this, "Please enter a search term or select a filter", Toast.LENGTH_SHORT).show();
            tvSearchBtn.setEnabled(true);
            return;
        }

        if (!query.isEmpty()) saveRecentSearch(query);

        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra("QUERY", query);
        intent.putExtra("GENRE", genre); // This now contains comma-separated values
        intent.putExtra("YEAR", year);
        intent.putExtra("DIRECTOR", director);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        tvSearchBtn.postDelayed(() -> tvSearchBtn.setEnabled(true), 1000);
    }

    private void saveRecentSearch(String query) {
        Set<String> recentSearches = getRecentSearchesSet();
        recentSearches.remove(query);
        List<String> list = new ArrayList<>(recentSearches);
        list.add(0, query);
        if (list.size() > MAX_RECENT_SEARCHES) list = list.subList(0, MAX_RECENT_SEARCHES);
        prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(list)).apply();
        loadRecentSearches();
    }

    private Set<String> getRecentSearchesSet() {
        String json = prefs.getString(KEY_RECENT_SEARCHES, null);
        if (json == null) return new LinkedHashSet<>();
        Type type = new TypeToken<List<String>>() {}.getType();
        return new LinkedHashSet<>(gson.fromJson(json, type));
    }

    private void loadRecentSearches() {
        cgRecentSearches.removeAllViews();
        Set<String> recentSearches = getRecentSearchesSet();
        for (String search : recentSearches) {
            addRecentSearchChip(search);
        }
    }

    private void addRecentSearchChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(true);
        
        // Match the design from the image: Light purple-ish background, subtle border
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F9F5FF")));
        chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#E9D7FE")));
        chip.setChipStrokeWidth(1f);
        chip.setChipCornerRadius(24f); // More rounded
        chip.setTextColor(Color.parseColor("#333333"));
        chip.setCloseIconTint(ColorStateList.valueOf(Color.parseColor("#666666")));

        chip.setOnClickListener(v -> {
            etSearchInput.setText(text);
            performSearch();
        });
        chip.setOnCloseIconClickListener(v -> removeRecentSearch(text));
        cgRecentSearches.addView(chip);
    }

    private void removeRecentSearch(String text) {
        Set<String> recentSearches = getRecentSearchesSet();
        recentSearches.remove(text);
        prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(new ArrayList<>(recentSearches))).apply();
        loadRecentSearches();
    }

    private void clearAllRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply();
        cgRecentSearches.removeAllViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvSearchBtn.setEnabled(true);
    }
}