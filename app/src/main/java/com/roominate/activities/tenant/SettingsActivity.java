package com.roominate.activities.tenant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "roominate_prefs";
    
    private MaterialToolbar toolbar;
    private TextView userNameTextView;
    private TextView userEmailTextView;
    private TextView userRoleTextView;
    private TextView appVersionTextView;
    
    // Account Section
    private LinearLayout editProfileLayout;
    private LinearLayout changePasswordLayout;
    private LinearLayout deleteAccountLayout;
    
    // Notifications Section
    private SwitchMaterial notificationSwitch;
    private SwitchMaterial bookingAlertsSwitch;
    private SwitchMaterial messagesSwitch;
    
    // Appearance Section
    private SwitchMaterial darkModeSwitch;
    
    // About Section
    private LinearLayout aboutAppLayout;
    private LinearLayout privacyPolicyLayout;
    private LinearLayout termsLayout;
    
    // Logout
    private LinearLayout logoutLayout;
    
    private SharedPreferences prefs;
    private String userId;
    private String userEmail;
    private String userName;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadUserData();
        
        initializeViews();
        loadSettings();
        setupListeners();
    }
    
    private void loadUserData() {
        userId = prefs.getString("user_id", null);
        userEmail = prefs.getString("user_email", "");
        userRole = prefs.getString("user_role", "tenant");
        
        // Try to get user name from user_data JSON
        String userDataJson = prefs.getString("user_data", null);
        if (userDataJson != null) {
            try {
                JSONObject userData = new JSONObject(userDataJson);
                JSONObject userMetadata = userData.optJSONObject("user_metadata");
                if (userMetadata != null) {
                    userName = userMetadata.optString("full_name", "");
                    if (userName.isEmpty()) {
                        userName = userMetadata.optString("name", "");
                    }
                }
                if (userName == null || userName.isEmpty()) {
                    userName = userData.optString("email", userEmail).split("@")[0];
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing user data", e);
                userName = userEmail.split("@")[0];
            }
        } else {
            userName = userEmail.split("@")[0];
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        userNameTextView = findViewById(R.id.userNameTextView);
        userEmailTextView = findViewById(R.id.userEmailTextView);
        userRoleTextView = findViewById(R.id.userRoleTextView);
        appVersionTextView = findViewById(R.id.appVersionTextView);
        
        // Account Section
        editProfileLayout = findViewById(R.id.editProfileLayout);
        changePasswordLayout = findViewById(R.id.changePasswordLayout);
        deleteAccountLayout = findViewById(R.id.deleteAccountLayout);
        
        // Notifications Section
        notificationSwitch = findViewById(R.id.notificationSwitch);
        bookingAlertsSwitch = findViewById(R.id.bookingAlertsSwitch);
        messagesSwitch = findViewById(R.id.messagesSwitch);
        
        // Appearance Section
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        
        // About Section
        aboutAppLayout = findViewById(R.id.aboutAppLayout);
        privacyPolicyLayout = findViewById(R.id.privacyPolicyLayout);
        termsLayout = findViewById(R.id.termsLayout);
        
        // Logout
        logoutLayout = findViewById(R.id.logoutLayout);
        
        // Display user info
        if (userNameTextView != null) {
            userNameTextView.setText(userName);
        }
        if (userEmailTextView != null) {
            userEmailTextView.setText(userEmail);
        }
        if (userRoleTextView != null) {
            String roleCapitalized = userRole.substring(0, 1).toUpperCase() + userRole.substring(1);
            userRoleTextView.setText(roleCapitalized);
        }
        if (appVersionTextView != null) {
            appVersionTextView.setText("Version 1.0.0");
        }
    }

    private void setupListeners() {
        
        // Account Section
        if (editProfileLayout != null) {
            editProfileLayout.setOnClickListener(v -> openEditProfile());
        }
        if (changePasswordLayout != null) {
            changePasswordLayout.setOnClickListener(v -> changePassword());
        }
        if (deleteAccountLayout != null) {
            deleteAccountLayout.setOnClickListener(v -> confirmDeleteAccount());
        }
        
        // Notifications Section
        if (notificationSwitch != null) {
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
                Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }
        if (bookingAlertsSwitch != null) {
            bookingAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("booking_alerts_enabled", isChecked).apply();
            });
        }
        if (messagesSwitch != null) {
            messagesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("messages_enabled", isChecked).apply();
            });
        }
        
        // Appearance Section
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply();
                applyTheme(isChecked);
            });
        }
        
        // About Section
        if (aboutAppLayout != null) {
            aboutAppLayout.setOnClickListener(v -> showAboutDialog());
        }
        if (privacyPolicyLayout != null) {
            privacyPolicyLayout.setOnClickListener(v -> showPrivacyPolicy());
        }
        if (termsLayout != null) {
            termsLayout.setOnClickListener(v -> showTermsAndConditions());
        }
        
        // Logout
        if (logoutLayout != null) {
            logoutLayout.setOnClickListener(v -> confirmLogout());
        }
    }
    
    private void loadSettings() {
        // Load notification settings - default to disabled for cleaner appearance
        if (notificationSwitch != null) {
            notificationSwitch.setChecked(prefs.getBoolean("notifications_enabled", false));
        }
        if (bookingAlertsSwitch != null) {
            bookingAlertsSwitch.setChecked(prefs.getBoolean("booking_alerts_enabled", false));
        }
        if (messagesSwitch != null) {
            messagesSwitch.setChecked(prefs.getBoolean("messages_enabled", false));
        }
        
        // Load appearance settings - light mode is default
        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(prefs.getBoolean("dark_mode_enabled", false));
        }
    }
    
    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }
    
    private void changePassword() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton submitButton = dialogView.findViewById(R.id.submitButton);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        submitButton.setOnClickListener(v -> {
            // TODO: Implement password change via Supabase
            Toast.makeText(this, "Password change functionality coming soon", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
    
    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.")
            .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void deleteAccount() {
        // TODO: Implement account deletion via Supabase
        Toast.makeText(this, "Account deletion functionality coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void applyTheme(boolean darkMode) {
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("About Roominate")
            .setMessage("Roominate v1.0.0\n\n" +
                       "A comprehensive property rental and management platform " +
                       "connecting property owners with tenants in Zambia.\n\n" +
                       "© 2025 Roominate. All rights reserved.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showPrivacyPolicy() {
        new AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("Your privacy is important to us. Roominate collects and uses your personal information " +
                       "solely for the purpose of providing property rental services.\n\n" +
                       "We do not share your information with third parties without your consent.\n\n" +
                       "For full details, visit our website.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTermsAndConditions() {
        new AlertDialog.Builder(this)
            .setTitle("Terms and Conditions")
            .setMessage("By using Roominate, you agree to:\n\n" +
                       "1. Provide accurate information\n" +
                       "2. Respect other users\n" +
                       "3. Follow local laws and regulations\n" +
                       "4. Not misuse the platform\n\n" +
                       "For full terms, visit our website.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void confirmLogout() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> logout())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void logout() {
        // Clear all session data
        prefs.edit()
            .remove("user_id")
            .remove("user_email")
            .remove("user_data")
            .remove("user_role")
            .remove("access_token")
            .remove("refresh_token")
            .remove("token_expires_at")
            .putBoolean("is_logged_in", false)
            .apply();
        
        // Navigate to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}
