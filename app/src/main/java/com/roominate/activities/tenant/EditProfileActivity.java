package com.roominate.activities.tenant;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final String PREFS_NAME = "roominate_prefs";

    private ImageView profileImageView;
    private EditText fullNameEditText;
    private EditText phoneEditText;
    private EditText occupationEditText;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private ProgressBar progressBar;

    private SharedPreferences prefs;
    private String userId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        if (userId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        loadProfileData();
        setupListeners();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        occupationEditText = findViewById(R.id.occupationEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving profile...");
        progressDialog.setCancelable(false);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadProfileData() {
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.VISIBLE);
        }

        // Load from SharedPreferences first
        String fullName = prefs.getString("user_name", "");
        if (!fullName.isEmpty()) {
            fullNameEditText.setText(fullName);
        }

        // Fetch from Supabase profiles table
        SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        String firstName = response.optString("first_name", "");
                        String lastName = response.optString("last_name", "");
                        String fullNameFromDb = response.optString("full_name", "");

                        if (fullNameFromDb.isEmpty() && (!firstName.isEmpty() || !lastName.isEmpty())) {
                            fullNameFromDb = (firstName + " " + lastName).trim();
                        }

                        String phone = response.optString("phone", "");
                        String occupation = response.optString("occupation", "");
                        String avatarUrl = response.optString("avatar_url", "");

                        if (!fullNameFromDb.isEmpty()) {
                            fullNameEditText.setText(fullNameFromDb);
                        }
                        if (!phone.isEmpty()) {
                            phoneEditText.setText(phone);
                        }
                        if (!occupation.isEmpty()) {
                            occupationEditText.setText(occupation);
                        }

                        if (!avatarUrl.isEmpty()) {
                            Picasso.get()
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .into(profileImageView);
                        }

                        if (progressBar != null) {
                            progressBar.setVisibility(android.view.View.GONE);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error loading profile data", e);
                        if (progressBar != null) {
                            progressBar.setVisibility(android.view.View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error fetching profile: " + error);
                    if (progressBar != null) {
                        progressBar.setVisibility(android.view.View.GONE);
                    }
                    // Continue with whatever data we have
                });
            }
        });
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProfileChanges());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveProfileChanges() {
        // Validate inputs
        String fullName = fullNameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String occupation = occupationEditText.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            showError("Full name is required");
            return;
        }

        if (!TextUtils.isEmpty(phone) && !isValidPhone(phone)) {
            showError("Invalid phone number format");
            return;
        }

        // Show progress dialog
        progressDialog.show();

        try {
            // Build update object
            // Note: profiles table has first_name and last_name, not full_name
            JSONObject updateData = new JSONObject();

            // Split full name into first and last name
            String[] nameParts = fullName.trim().split("\\s+", 2);
            String firstName = nameParts.length > 0 ? nameParts[0] : fullName;
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            updateData.put("first_name", firstName);
            updateData.put("last_name", lastName);
            updateData.put("phone", phone);
            updateData.put("occupation", occupation);

            // Update profile in Supabase
            SupabaseClient.getInstance().updateUserProfile(updateData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();

                        // Update SharedPreferences with new details
                        prefs.edit()
                                .putString("user_name", fullName)
                                .putString("first_name", firstName)
                                .putString("last_name", lastName)
                                .putString("phone", phone)
                                .apply();

                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT)
                                .show();

                        // Return to profile screen
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError("Failed to update profile: " + error);
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error building update data", e);
            showError("Error processing your changes");
        }
    }

    private boolean isValidPhone(String phone) {
        // Allow empty phone
        if (TextUtils.isEmpty(phone)) {
            return true;
        }
        // Basic validation: contains only digits, spaces, hyphens, and parentheses
        return phone.matches("[\\d\\s\\-()]+") && phone.replaceAll("[^0-9]", "").length() >= 10;
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
