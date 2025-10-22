package com.roominate.activities.admin;

import android.os.Bundle;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.roominate.R;

public class UserManagementActivity extends AppCompatActivity {

    private SearchView searchView;
    private TabLayout tabLayout;
    private RecyclerView usersRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        initializeViews();
        setupTabs();
        loadUsers("all");
    }

    private void initializeViews() {
        searchView = findViewById(R.id.searchView);
        tabLayout = findViewById(R.id.tabLayout);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Tenants"));
        tabLayout.addTab(tabLayout.newTab().setText("Owners"));
        tabLayout.addTab(tabLayout.newTab().setText("Admins"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String userType = tab.getText().toString().toLowerCase();
                loadUsers(userType);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadUsers(String userType) {
        // TODO: Load users from Supabase based on user type
    }
}
