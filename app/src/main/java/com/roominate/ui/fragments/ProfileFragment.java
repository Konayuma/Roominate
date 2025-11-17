package com.roominate.ui.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.activities.tenant.EditProfileActivity;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PREFS_NAME = "roominate_prefs";
    
    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView fullNameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private TextView occupationTextView;
    private TextView memberSinceTextView;
    private MaterialButton editButton;
    private MaterialButton logoutButton;
    private ProgressBar progressBar;
    
    private SharedPreferences prefs;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            Toast.makeText(requireContext(), "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        initializeViews(view);
        loadUserProfile();
        setupListeners();
    }

    private void initializeViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        userNameTextView = view.findViewById(R.id.userNameTextView);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        fullNameTextView = view.findViewById(R.id.fullNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        occupationTextView = view.findViewById(R.id.occupationTextView);
        memberSinceTextView = view.findViewById(R.id.memberSinceTextView);
        editButton = view.findViewById(R.id.editButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void loadUserProfile() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Load from SharedPreferences first for immediate display
        String userEmail = prefs.getString("user_email", "");
        String userName = prefs.getString("user_name", "");
        String fullName = prefs.getString("full_name", "");
        String firstName = prefs.getString("first_name", "");
        String lastName = prefs.getString("last_name", "");
        
        if (!userEmail.isEmpty()) {
            emailTextView.setText(userEmail);
            userEmailTextView.setText(userEmail);
        }
        
        // Try to get name in order of preference: full_name > user_name > first_name + last_name > email username
        String displayName = "";
        if (!fullName.isEmpty()) {
            displayName = fullName;
        } else if (!userName.isEmpty()) {
            displayName = userName;
        } else if (!firstName.isEmpty() || !lastName.isEmpty()) {
            displayName = (firstName + " " + lastName).trim();
        } else if (!userEmail.isEmpty()) {
            // Fallback to email username
            displayName = userEmail.split("@")[0];
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
        }
        
        if (!displayName.isEmpty()) {
            userNameTextView.setText(displayName);
            fullNameTextView.setText(displayName);
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
                        String metadataName = userMetadata.optString("full_name", "");
                        if (metadataName.isEmpty()) {
                            metadataName = userMetadata.optString("name", "");
                        }
                        if (metadataName.isEmpty()) {
                            String metaFirst = userMetadata.optString("first_name", "");
                            String metaLast = userMetadata.optString("last_name", "");
                            if (!metaFirst.isEmpty() || !metaLast.isEmpty()) {
                                metadataName = (metaFirst + " " + metaLast).trim();
                            }
                        }
                        if (!metadataName.isEmpty()) {
                            userNameTextView.setText(metadataName);
                            fullNameTextView.setText(metadataName);
                            // Also save it to prefs for next time
                            prefs.edit().putString("full_name", metadataName).apply();
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
                            memberSinceTextView.setText(outputFormat.format(date));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing date", e);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing user_data", e);
            }
        }
        
        // Fetch from Supabase profiles table for additional info
        SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;
                
                requireActivity().runOnUiThread(() -> {
                    try {
                        // Update fields with profile data
                        String fullName = response.optString("full_name", "");
                        if (fullName.isEmpty()) {
                            fullName = response.optString("first_name", "") + " " + response.optString("last_name", "");
                            fullName = fullName.trim();
                        }
                        if (!fullName.isEmpty()) {
                            userNameTextView.setText(fullName);
                            fullNameTextView.setText(fullName);
                        }
                        
                        String phone = response.optString("phone", "");
                        if (!phone.isEmpty()) {
                            phoneTextView.setText(phone);
                        }
                        
                        String bio = response.optString("bio", "");
                        if (!bio.isEmpty()) {
                            occupationTextView.setText(bio);
                        }
                        
                        String avatarUrl = response.optString("avatar_url", "");
                        if (!avatarUrl.isEmpty()) {
                            Picasso.get()
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(profileImageView);
                        }
                        
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating profile UI", e);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error loading profile: " + error);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    // We already have basic info from SharedPreferences, so don't show error
                });
            }
        });
    }

    private void setupListeners() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });
        
        logoutButton.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> performLogout())
            .setNegativeButton("No", null)
            .show();
    }

    private void performLogout() {
        // Clear all stored data
        prefs.edit().clear().apply();
        
        // Navigate to login screen
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
