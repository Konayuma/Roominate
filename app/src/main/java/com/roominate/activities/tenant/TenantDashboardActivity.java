package com.roominate.activities.tenant;

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
import com.google.android.material.navigation.NavigationView;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.services.SupabaseClient;
import com.roominate.ui.fragments.SearchFragment;
import com.roominate.ui.fragments.MyBookingsFragment;
import com.roominate.ui.fragments.ProfileFragment;
import org.json.JSONObject;

public class TenantDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "TenantDashboard";
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    
    // Header views
    private TextView profileNameTextView;
    private TextView profileEmailTextView;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_dashboard);

        initializeViews();
        setupDrawer();
        setupBottomNavigation();
        
        // Load default fragment
        loadFragment(new HomeFragment());
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        
        // Get header view and initialize header views
        View headerView = navigationView.getHeaderView(0);
        profileImageView = headerView.findViewById(R.id.profileImage);
        profileNameTextView = headerView.findViewById(R.id.profileName);
        profileEmailTextView = headerView.findViewById(R.id.profileEmail);
        
        // Check if owner is using tenant view
        checkIfOwnerUsingTenantView();
        
        // Load user data into header
        loadUserProfile();
    }
    
    private void checkIfOwnerUsingTenantView() {
        try {
            SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
            
            // Check if user is owner
            String userJson = prefs.getString("user_data", null);
            boolean isOwner = false;
            
            if (userJson != null) {
                JSONObject user = new JSONObject(userJson);
                JSONObject userMetadata = user.optJSONObject("user_metadata");
                if (userMetadata != null) {
                    String role = userMetadata.optString("role", "tenant");
                    isOwner = "owner".equalsIgnoreCase(role);
                }
            }
            
            // Show "Switch to Owner View" menu item if user is owner
            if (isOwner) {
                MenuItem switchItem = navigationView.getMenu().findItem(R.id.nav_switch_to_owner);
                if (switchItem != null) {
                    switchItem.setVisible(true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking owner status", e);
        }
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
                        
                        // TODO: Load profile image if available
                        // String imageUrl = response.optString("avatar_url", null);
                        // if (imageUrl != null) {
                        //     Picasso.get().load(imageUrl).into(profileImageView);
                        // }
                        
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
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_bookings) {
                selectedFragment = new MyBookingsFragment();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
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
            loadFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_favorites) {
            loadFragment(new FavoritesFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_profile) {
            loadFragment(new ProfileFragment());
            bottomNavigationView.setSelectedItemId(id);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_switch_to_owner) {
            switchToOwnerView();
        } else if (id == R.id.nav_sign_out) {
            signOut();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void switchToOwnerView() {
        // Clear tenant view preference
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        prefs.edit().remove("owner_using_tenant_view").apply();
        
        // Navigate back to owner dashboard
        Intent intent = new Intent(this, com.roominate.activities.owner.OwnerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
    
    // Public method to open drawer from fragments
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
    
    // Public method to navigate to home tab
    public void navigateToHome() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
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
