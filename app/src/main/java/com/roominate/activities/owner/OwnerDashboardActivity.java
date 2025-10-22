package com.roominate.activities.owner;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.roominate.R;

public class OwnerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton addPropertyFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        initializeViews();
        setupBottomNavigation();
        setupListeners();
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        addPropertyFab = findViewById(R.id.addPropertyFab);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            // TODO: Replace with actual fragment navigation
            // if (itemId == R.id.nav_home) {
            //     selectedFragment = new OwnerHomeFragment();
            // } else if (itemId == R.id.nav_listings) {
            //     selectedFragment = new MyListingsFragment();
            // } else if (itemId == R.id.nav_bookings) {
            //     selectedFragment = new BookingsFragment();
            // } else if (itemId == R.id.nav_profile) {
            //     selectedFragment = new OwnerProfileFragment();
            // }

            // return loadFragment(selectedFragment);
            return true;
        });
    }

    private void setupListeners() {
        addPropertyFab.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, AddPropertyActivity.class);
            startActivity(intent);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
