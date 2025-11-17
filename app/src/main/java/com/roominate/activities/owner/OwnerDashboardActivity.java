package com.roominate.activities.owner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.activities.tenant.TenantDashboardActivity;
import com.roominate.services.SupabaseClient;
import com.roominate.ui.fragments.ProfileFragment;
import org.json.JSONObject;

public class OwnerDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "OwnerDashboard";
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton addPropertyFab;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    
    // Header views
    private TextView profileNameTextView;
    private TextView profileEmailTextView;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_owner_dashboard);

        initializeViews();
        setupDrawer();
        setupBottomNavigation();
        setupListeners();
        
        // Load default fragment
        loadFragment(new OwnerHomeFragment());
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        addPropertyFab = findViewById(R.id.addPropertyFab);
    }

    private void setupDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        
        // Get header view and initialize header views
        View headerView = navigationView.getHeaderView(0);
        profileImageView = headerView.findViewById(R.id.profileImage);
        profileNameTextView = headerView.findViewById(R.id.profileName);
        profileEmailTextView = headerView.findViewById(R.id.profileEmail);
        
        // Load user data into header
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        // Try to get user data from login response stored in SharedPreferences
        try {
            SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
            
            // Try to get from stored login response first
            String userJson = prefs.getString("user_data", null);
            if (userJson != null) {
                JSONObject user = new JSONObject(userJson);
                JSONObject userMetadata = user.optJSONObject("user_metadata");
                if (userMetadata != null) {
                    String firstName = userMetadata.optString("first_name", "");
                    String lastName = userMetadata.optString("last_name", "");
                    String email = userMetadata.optString("email", user.optString("email", ""));
                    
                    // Capitalize both first and last name
                    if (!firstName.isEmpty()) {
                        firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
                    }
                    if (!lastName.isEmpty()) {
                        lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
                    }
                    
                    String fullName = (firstName + " " + lastName).trim();
                    if (!fullName.isEmpty()) {
                        profileNameTextView.setText(fullName);
                    }
                    if (!email.isEmpty()) {
                        profileEmailTextView.setText(email);
                    }
                    return; // Successfully loaded from stored data
                }
            }
            
            // Fallback: try email only
            String email = prefs.getString("last_signed_email", null);
            if (email == null) {
                email = prefs.getString("last_otp_email", null);
            }
            
            if (email != null) {
                profileEmailTextView.setText(email);
                // Extract name from email as fallback
                String name = email.split("@")[0];
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                profileNameTextView.setText(name);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user from preferences", e);
        }
        
        // Fetch full profile from Supabase
        SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        // Try to get name from profile response
                        String firstName = response.optString("first_name", "");
                        String lastName = response.optString("last_name", "");
                        
                        // Capitalize both names
                        if (!firstName.isEmpty()) {
                            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
                        }
                        if (!lastName.isEmpty()) {
                            lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
                        }
                        
                        String fullName = (firstName + " " + lastName).trim();
                        
                        if (!fullName.isEmpty()) {
                            profileNameTextView.setText(fullName);
                        }
                        
                        String email = response.optString("email", null);
                        if (email != null && !email.isEmpty()) {
                            profileEmailTextView.setText(email);
                        }
                        
                        Log.d(TAG, "User profile loaded successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user profile", e);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching user profile: " + error);
                // Keep the fallback values from SharedPreferences
            }
        });
    }

    private void setupBottomNavigation() {
        // Set Home as selected by default
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new OwnerHomeFragment();
            } else if (itemId == R.id.nav_listings) {
                selectedFragment = new MyListingsFragment();
            } else if (itemId == R.id.nav_bookings) {
                selectedFragment = new OwnerBookingsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadFragment(new OwnerHomeFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_listings) {
            loadFragment(new MyListingsFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_bookings) {
            loadFragment(new OwnerBookingsFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_profile) {
            loadFragment(new ProfileFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, com.roominate.activities.tenant.SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_switch_to_tenant) {
            switchToTenantView();
        } else if (id == R.id.nav_sign_out) {
            signOut();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void switchToTenantView() {
        // Store preference to use tenant view
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("owner_using_tenant_view", true).apply();
        
        // Navigate to tenant dashboard
        Intent intent = new Intent(this, TenantDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupListeners() {
        addPropertyFab.setOnClickListener(v -> {
            Intent intent = new Intent(OwnerDashboardActivity.this, AddPropertyActivity.class);
            startActivity(intent);
        });
    }
    
    private void signOut() {
        // Clear Supabase session
        SupabaseClient.getInstance().signOut(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Supabase sign out successful");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Supabase sign out error: " + error);
            }
        });
        
        // Clear local session data including login persistence
        try {
            SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
            prefs.edit()
                .remove("is_logged_in")
                .remove("user_id")
                .remove("user_email")
                .remove("user_role")
                .remove("user_data")
                .remove("last_signed_email")
                .remove("last_otp_email")
                .remove("access_token")
                .remove("refresh_token")
                .remove("token_expires_at")
                .remove("owner_using_tenant_view")
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing session", e);
        }
        
        // Navigate to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile data when returning from profile activity
        loadUserProfile();
    }
    
    // Public method to open drawer from fragments
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
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
