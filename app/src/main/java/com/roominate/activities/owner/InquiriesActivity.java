package com.roominate.activities.owner;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.roominate.R;

public class InquiriesActivity extends AppCompatActivity {

    private RecyclerView inquiriesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inquiries);

        initializeViews();
        loadInquiries();
    }

    private void initializeViews() {
        inquiriesRecyclerView = findViewById(R.id.inquiriesRecyclerView);
        inquiriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadInquiries() {
        // TODO: Load inquiries/booking requests from Supabase
    }
}
