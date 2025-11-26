package com.roominate.activities.auth;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private TextView forgotPasswordTextView;
    private ProgressBar progressBar;
    
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_login);

        // Ensure SupabaseClient has an application context for SharedPreferences access
        try {
            com.roominate.services.SupabaseClient.init(getApplicationContext());
        } catch (Exception e) {
            android.util.Log.w("LoginActivity", "Failed to init SupabaseClient context", e);
        }

        setupLocationPermissionLauncher();
        initializeViews();
        setupListeners();
    }
    
    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean fineLocationGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseLocationGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                
                if (fineLocationGranted || coarseLocationGranted) {
                    Log.d("LoginActivity", "Location permission granted");
                } else {
                    Log.d("LoginActivity", "Location permission denied");
                    Toast.makeText(this, "Location permission denied. Some features may be limited.", Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
        // Prefill email from last OTP/email submission if available
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
            String lastOtpEmail = prefs.getString("last_otp_email", null);
            if (lastOtpEmail == null) lastOtpEmail = prefs.getString("last_signed_email", null);
            if (lastOtpEmail != null) {
                emailEditText.setText(lastOtpEmail);
            }
        } catch (Exception ignored) {}
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerTextView.setOnClickListener(v -> {
            // Route to RoleSelectionActivity - the defined auth flow entry point
            Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        // Reset errors
        emailEditText.setError(null);
        passwordEditText.setError(null);

        // Get values
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate
        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            focusView = passwordEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            focusView = emailEditText;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailEditText.setError("Invalid email address");
            focusView = emailEditText;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {
        showProgress(true);
        SupabaseClient.getInstance().signIn(email, password, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("LoginActivity", "signIn success: " + response.toString());

                // Store session data for persistence
                String roleFromAuth = null;
                String accessToken = null;
                String refreshToken = null;
                long expiresAt = 0;
                
                try {
                    // Extract session tokens
                    if (response.has("access_token")) {
                        accessToken = response.getString("access_token");
                    }
                    if (response.has("refresh_token")) {
                        refreshToken = response.getString("refresh_token");
                    }
                    if (response.has("expires_at")) {
                        expiresAt = response.getLong("expires_at");
                    } else if (response.has("expires_in")) {
                        // Calculate expires_at from expires_in
                        long expiresIn = response.getLong("expires_in");
                        expiresAt = System.currentTimeMillis() / 1000 + expiresIn;
                    }
                    
                    if (response.has("user")) {
                        JSONObject user = response.getJSONObject("user");
                        android.content.SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        
                        // Store user data
                        editor.putString("user_data", user.toString())
                              .putString("user_id", user.optString("id", ""))
                              .putString("user_email", email)
                              .putString("last_signed_email", email);
                        
                        // Store session tokens
                        if (accessToken != null) {
                            editor.putString("access_token", accessToken);
                        }
                        if (refreshToken != null) {
                            editor.putString("refresh_token", refreshToken);
                        }
                        if (expiresAt > 0) {
                            editor.putLong("token_expires_at", expiresAt);
                        }
                        
                        // Mark as logged in
                        editor.putBoolean("is_logged_in", true);
                        editor.apply();
                        
                        // Try to get role from user_metadata first (most reliable)
                        if (user.has("user_metadata")) {
                            JSONObject userMetadata = user.getJSONObject("user_metadata");
                            roleFromAuth = userMetadata.optString("role", null);
                            Log.d("LoginActivity", "Role from user_metadata: " + roleFromAuth);
                        }
                    }
                } catch (Exception e) {
                    Log.e("LoginActivity", "Error storing user data", e);
                }

                final String finalRoleFromAuth = roleFromAuth;

                // Get user profile to determine role (fallback if not in user_metadata)
                SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject profileResponse) {
                        runOnUiThread(() -> {
                            showProgress(false);
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            try {
                                // Prefer role from user_metadata, fallback to profile table
                                String role = finalRoleFromAuth != null ? finalRoleFromAuth : profileResponse.optString("role", "tenant");
                                Log.d("LoginActivity", "Final role determined: " + role);
                                
                                // Extract profile details
                                String firstName = profileResponse.optString("first_name", "");
                                String lastName = profileResponse.optString("last_name", "");
                                String phone = profileResponse.optString("phone", "");
                                String fullName = (firstName + " " + lastName).trim();
                                
                                // Store the final role and profile details in SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                                prefs.edit()
                                    .putString("user_role", role)
                                    .putString("first_name", firstName)
                                    .putString("last_name", lastName)
                                    .putString("phone", phone)
                                    .putString("user_name", fullName)
                                    .apply();
                                
                                redirectToDashboard(role);
                            } catch (Exception e) {
                                Log.e("LoginActivity", "Error parsing user role", e);
                                // Use auth role if we have it, otherwise default to tenant
                                String fallbackRole = finalRoleFromAuth != null ? finalRoleFromAuth : "tenant";
                                SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                                prefs.edit().putString("user_role", fallbackRole).apply();
                                redirectToDashboard(fallbackRole);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("LoginActivity", "Error getting user profile: " + error);
                        runOnUiThread(() -> {
                            showProgress(false);
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            // Use role from auth if available, otherwise default to tenant
                            String fallbackRole = finalRoleFromAuth != null ? finalRoleFromAuth : "tenant";
                            Log.d("LoginActivity", "Using fallback role: " + fallbackRole);
                            SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                            prefs.edit().putString("user_role", fallbackRole).apply();
                            redirectToDashboard(fallbackRole);
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.d("LoginActivity", "signIn error: " + error);
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }

    private void redirectToDashboard(String userType) {
        // Request location permission before redirecting
        requestLocationPermission();
        
        // Check if owner is using tenant view
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        boolean ownerUsingTenantView = prefs.getBoolean("owner_using_tenant_view", false);
        
        Intent intent;
        
        // If owner is using tenant view, route to tenant dashboard regardless
        if ("owner".equalsIgnoreCase(userType) && ownerUsingTenantView) {
            intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
        } else {
            // Normal routing based on user type
            switch (userType.toLowerCase()) {
                case "tenant":
                    intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
                    break;
                case "owner":
                    intent = new Intent(this, com.roominate.activities.owner.OwnerDashboardActivity.class);
                    break;
                case "admin":
                    intent = new Intent(this, com.roominate.activities.admin.AdminDashboardActivity.class);
                    break;
                default:
                    intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
                    break;
            }
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void requestLocationPermission() {
        // Check if we already have permission
        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, 
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        if (!hasFineLocation || !hasCoarseLocation) {
            // Request both location permissions
            locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            Log.d("LoginActivity", "Location permissions already granted");
        }
    }
}
