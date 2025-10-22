package com.roominate.activities.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.roominate.R;

public class VerifyListingsActivity extends AppCompatActivity {

    private RecyclerView pendingListingsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_listings);

        initializeViews();
        loadPendingListings();
    }

    private void initializeViews() {
        pendingListingsRecyclerView = findViewById(R.id.pendingListingsRecyclerView);
        pendingListingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPendingListings() {
        // TODO: Load pending listings from Supabase
    }

    private void approveListing(String listingId) {
        // TODO: Update listing status to approved in Supabase
    }

    private void rejectListing(String listingId, String reason) {
        // TODO: Update listing status to rejected in Supabase with reason
    }
}
