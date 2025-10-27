package com.roominate.activities.tenant;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.roominate.R;

public class TenantDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_tenant_dashboard);

        initializeViews();
        setupBottomNavigation();
        
        // Load default fragment
        // loadFragment(new HomeFragment());
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            // TODO: Replace with actual fragment navigation
            // if (itemId == R.id.nav_home) {
            //     selectedFragment = new HomeFragment();
            // } else if (itemId == R.id.nav_search) {
            //     selectedFragment = new SearchFragment();
            // } else if (itemId == R.id.nav_favorites) {
            //     selectedFragment = new FavoritesFragment();
            // } else if (itemId == R.id.nav_profile) {
            //     selectedFragment = new TenantProfileFragment();
            // }

            // return loadFragment(selectedFragment);
            return true;
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
