package com.roominate.activities.tenant;

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
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TenantProfileActivity extends AppCompatActivity {

    private static final String TAG = "TenantProfileActivity";
    private static final String PREFS_NAME = "roominate_prefs";
    
    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView fullNameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView occupationTextView;
    private TextView memberSinceDisplayTextView;
    private MaterialButton editButton;
    private MaterialButton logoutButton;
    
    private SharedPreferences prefs;
    private ProgressDialog progressDialog;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_profile);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        loadUserProfile();
        setupListeners();
    }

    private void initializeViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);
        fullNameTextView = findViewById(R.id.fullNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        occupationTextView = findViewById(R.id.occupationTextView);
        memberSinceDisplayTextView = findViewById(R.id.memberSinceDisplayTextView);
        editButton = findViewById(R.id.editButton);
        logoutButton = findViewById(R.id.logoutButton);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading profile...");
        progressDialog.setCancelable(false);
    }

    private void loadUserProfile() {
        progressDialog.show();
        
        // Load from SharedPreferences first for immediate display
        String userEmail = prefs.getString("user_email", "");
        String userName = prefs.getString("user_name", "");
        
        if (!userEmail.isEmpty()) {
            emailTextView.setText(userEmail);
            userEmailTextView.setText(userEmail);
        }
        
        if (!userName.isEmpty()) {
            userNameTextView.setText(userName);
            fullNameTextView.setText(userName);
        }
        
        // Try to get data from user_data JSON
        String userDataJson = prefs.getString("user_data", null);
        if (userDataJson != null) {
            try {
                JSONObject userData = new JSONObject(userDataJson);
                
                // Get name from user_metadata
                if (userData.has("user_metadata")) {
                    JSONObject userMetadata = userData.optJSONObject("user_metadata");
                    if (userMetadata != null) {
                        String fullName = userMetadata.optString("full_name", "");
                        if (fullName.isEmpty()) {
                            fullName = userMetadata.optString("name", "");
                        }
                        if (!fullName.isEmpty()) {
                            userNameTextView.setText(fullName);
                            fullNameTextView.setText(fullName);
                        }
                    }
                }
                
                // Get created_at for member since
                String createdAt = userData.optString("created_at", "");
                if (!createdAt.isEmpty()) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
                        Date date = inputFormat.parse(createdAt);
                        if (date != null) {
                            memberSinceDisplayTextView.setText(outputFormat.format(date));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing date", e);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing user_data", e);
            }
        }
        
        // Now fetch from Supabase profiles table for additional info
        SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        // Update fields with profile data
                        String fullName = response.optString("full_name", "");
                        String phone = response.optString("phone", "");
                        String occupation = response.optString("occupation", "");
                        String avatarUrl = response.optString("avatar_url", "");
                        
                        if (!fullName.isEmpty()) {
                            userNameTextView.setText(fullName);
                            fullNameTextView.setText(fullName);
                        }
                        if (!phone.isEmpty()) {
                            phoneTextView.setText(phone);
                        }
                        if (!occupation.isEmpty()) {
                            occupationTextView.setText(occupation);
                        }
                        
                        // Load profile image
                        if (!avatarUrl.isEmpty()) {
                            Picasso.get()
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
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
                    // Don't show error if we at least have basic data from SharedPreferences
                });
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
                Intent intent = new Intent(TenantProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
