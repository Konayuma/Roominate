package com.roominate.activities.owner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OwnerProfileActivity extends AppCompatActivity {

    private static final String TAG = "OwnerProfileActivity";
    private static final String PREFS_NAME = "roominate_prefs";
    
    private MaterialToolbar toolbar;
    private ImageView profileImageView;
    private TextView fullNameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView businessNameTextView;
    private TextView businessAddressTextView;
    private TextView memberSinceTextView;
    private TextView userRoleTextView;
    private TextView totalPropertiesTextView;
    private MaterialButton editButton;
    private MaterialButton logoutButton;
    
    private SharedPreferences prefs;
    private ProgressDialog progressDialog;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_profile);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        loadOwnerProfile();
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        profileImageView = findViewById(R.id.profileImageView);
        fullNameTextView = findViewById(R.id.fullNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        businessNameTextView = findViewById(R.id.businessNameTextView);
        businessAddressTextView = findViewById(R.id.businessAddressTextView);
        memberSinceTextView = findViewById(R.id.memberSinceTextView);
        userRoleTextView = findViewById(R.id.userRoleTextView);
        totalPropertiesTextView = findViewById(R.id.totalPropertiesTextView);
        editButton = findViewById(R.id.editButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false);
    }

    private void loadOwnerProfile() {
        progressDialog.show();
        
        String userEmail = prefs.getString("user_email", "");
        
        if (!userEmail.isEmpty()) {
            emailTextView.setText(userEmail);
        }
        
        userRoleTextView.setText("Property Owner");
        
        // Load from user_data JSON
        String userDataJson = prefs.getString("user_data", null);
        if (userDataJson != null) {
            try {
                JSONObject userData = new JSONObject(userDataJson);
                
                if (userData.has("user_metadata")) {
                    JSONObject userMetadata = userData.optJSONObject("user_metadata");
                    if (userMetadata != null) {
                        String fullName = userMetadata.optString("full_name", "");
                        if (fullName.isEmpty()) {
                            fullName = userMetadata.optString("name", "");
                        }
                        if (!fullName.isEmpty()) {
                            fullNameTextView.setText(fullName);
                        }
                    }
                }
                
                String createdAt = userData.optString("created_at", "");
                if (!createdAt.isEmpty()) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
                        Date date = inputFormat.parse(createdAt);
                        if (date != null) {
                            memberSinceTextView.setText("Member since " + outputFormat.format(date));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing date", e);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing user_data", e);
            }
        }
        
        // Fetch from Supabase profiles table
        SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        String fullName = response.optString("full_name", "");
                        String phone = response.optString("phone", "");
                        String businessName = response.optString("business_name", "");
                        String businessAddress = response.optString("business_address", "");
                        String avatarUrl = response.optString("avatar_url", "");
                        
                        if (!fullName.isEmpty()) {
                            fullNameTextView.setText(fullName);
                        }
                        if (!phone.isEmpty()) {
                            phoneTextView.setText(phone);
                        }
                        if (!businessName.isEmpty()) {
                            businessNameTextView.setText(businessName);
                        }
                        if (!businessAddress.isEmpty()) {
                            businessAddressTextView.setText(businessAddress);
                        }
                        
                        if (!avatarUrl.isEmpty()) {
                            Picasso.get()
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(profileImageView);
                        }
                        
                        progressDialog.dismiss();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing profile data", e);
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading profile: " + error);
                    progressDialog.dismiss();
                });
            }
        });
        
        // Load property count
        loadPropertyCount();
    }
    
    private void loadPropertyCount() {
        SupabaseClient.getInstance().getPropertiesByOwner(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    org.json.JSONArray properties = response.optJSONArray("body");
                    if (properties != null) {
                        int count = properties.length();
                        runOnUiThread(() -> {
                            totalPropertiesTextView.setText(count + " " + (count == 1 ? "Property" : "Properties"));
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing properties count", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading properties count: " + error);
            }
        });
    }

    private void setupListeners() {
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                // TODO: Open edit profile dialog or activity
                Toast.makeText(this, "Edit profile functionality coming soon!", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }
    }
    
    private void logout() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> {
                // Clear shared preferences
                prefs.edit().clear().apply();
                
                // Navigate to login
                Intent intent = new Intent(OwnerProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
