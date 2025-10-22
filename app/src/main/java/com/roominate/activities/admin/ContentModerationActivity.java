package com.roominate.activities.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.roominate.R;

public class ContentModerationActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView contentRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_moderation);

        initializeViews();
        setupTabs();
        loadReportedContent("reviews");
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        contentRecyclerView = findViewById(R.id.contentRecyclerView);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Reviews"));
        tabLayout.addTab(tabLayout.newTab().setText("Listings"));
        tabLayout.addTab(tabLayout.newTab().setText("Users"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String contentType = tab.getText().toString().toLowerCase();
                loadReportedContent(contentType);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadReportedContent(String contentType) {
        // TODO: Load reported content from Supabase
    }

    private void approveContent(String contentId) {
        // TODO: Mark content as approved
    }

    private void removeContent(String contentId, String reason) {
        // TODO: Remove or hide flagged content
    }
}
