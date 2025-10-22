package com.roominate.activities.tenant;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.roominate.R;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button filterButton;
    private RecyclerView resultsRecyclerView;
    private TextView resultsCountTextView;
    
    // Filter UI elements
    private SeekBar priceRangeSeekBar;
    private TextView priceTextView;
    private ChipGroup amenitiesChipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        resultsCountTextView = findViewById(R.id.resultsCountTextView);
    }

    private void setupRecyclerView() {
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Set adapter
        // BoardingHouseAdapter adapter = new BoardingHouseAdapter(boardingHouseList);
        // resultsRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        filterButton.setOnClickListener(v -> showFilterDialog());
        
        // TODO: Implement search functionality
        // searchEditText.addTextChangedListener(new TextWatcher() {
        //     @Override
        //     public void afterTextChanged(Editable s) {
        //         performSearch(s.toString());
        //     }
        // });
    }

    private void showFilterDialog() {
        // TODO: Implement filter dialog
    }

    private void performSearch(String query) {
        // TODO: Implement search with Supabase
    }

    private void applyFilters(double maxPrice, String[] amenities) {
        // TODO: Implement filter application
    }
}
