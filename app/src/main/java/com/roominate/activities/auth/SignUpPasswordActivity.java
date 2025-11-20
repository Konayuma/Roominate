package com.roominate.activities.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class SignUpPasswordActivity extends AppCompatActivity {

    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordEditText;
    private TextView passwordStrengthText;
    private ProgressBar passwordStrengthBar;
    private Button continueButton;
    private ProgressBar progressBar;
    
    private String email, userType, firstName, lastName, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_password);

        // Get data from previous screen
        email = getIntent().getStringExtra("email");
        userType = getIntent().getStringExtra("userRole");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        phone = getIntent().getStringExtra("phone");
        
        Log.d("SignUpPasswordActivity", "Received user role: " + userType);
        initializeViews();

        setupListeners();
    }

    private void initializeViews() {
        passwordLayout = findViewById(R.id.passwordLayout);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> validateAndContinue());

        // Password strength checker
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Real-time validation
        confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePasswordMatch();
            }
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);
        
        passwordStrengthBar.setProgress(strength);
        
        if (strength < 25) {
            passwordStrengthText.setText("Weak");
            passwordStrengthText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            passwordStrengthBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.holo_red_dark)));
        } else if (strength < 50) {
            passwordStrengthText.setText("Fair");
            passwordStrengthText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            passwordStrengthBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.holo_orange_dark)));
        } else if (strength < 75) {
            passwordStrengthText.setText("Good");
            passwordStrengthText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            passwordStrengthBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.holo_blue_dark)));
        } else {
            passwordStrengthText.setText("Strong");
            passwordStrengthText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            passwordStrengthBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(android.R.color.holo_green_dark)));
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;
        
        if (password.length() >= 8) strength += 25;
        if (password.matches(".*[a-z].*")) strength += 15;
        if (password.matches(".*[A-Z].*")) strength += 20;
        if (password.matches(".*[0-9].*")) strength += 20;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength += 20;
        
        return Math.min(strength, 100);
    }

    private boolean validatePasswordMatch() {
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            return false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            return false;
        } else {
            passwordLayout.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm your password");
            return false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            return false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return true;
    }

    private void validateAndContinue() {
        if (validatePasswordMatch()) {
            proceedToVerification();
        }
    }

    private void proceedToVerification() {
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        String otp = getIntent().getStringExtra("otp");
        if (otp == null || otp.isEmpty()) {
            Toast.makeText(this, "Missing verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        continueButton.setEnabled(false);

        // Complete signup with OTP verification and user creation
        SupabaseClient.getInstance().completeSignup(email, password, otp, firstName, lastName, phone, userType, null, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    continueButton.setEnabled(true);

                    try {
                        // Save user session data to SharedPreferences
                        JSONObject user = response.optJSONObject("user");
                        JSONObject profile = response.optJSONObject("profile");
                        JSONObject session = response.optJSONObject("session");
                        
                        if (user != null && profile != null) {
                            SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            
                            String userId = user.optString("id");
                            String userEmail = user.optString("email");
                            String userFirstName = profile.optString("first_name");
                            String userLastName = profile.optString("last_name");
                            String userPhone = profile.optString("phone");
                            String userRole = profile.optString("role");
                            
                            editor.putString("user_id", userId);
                            editor.putString("email", userEmail);
                            editor.putString("user_email", userEmail);  // For ProfileFragment compatibility
                            editor.putString("user_role", userRole);
                            editor.putString("first_name", userFirstName);
                            editor.putString("last_name", userLastName);
                            editor.putString("full_name", (userFirstName + " " + userLastName).trim());  // Combined name
                            editor.putString("user_name", (userFirstName + " " + userLastName).trim());  // For compatibility
                            editor.putString("phone", userPhone);
                            editor.putBoolean("is_logged_in", true);
                            
                            // Save session tokens if available
                            if (session != null) {
                                String accessToken = session.optString("access_token");
                                String refreshToken = session.optString("refresh_token");
                                if (!accessToken.isEmpty()) {
                                    editor.putString("access_token", accessToken);
                                    Log.d("SignUpPassword", "Access token saved");
                                }
                                if (!refreshToken.isEmpty()) {
                                    editor.putString("refresh_token", refreshToken);
                                    Log.d("SignUpPassword", "Refresh token saved");
                                }
                            }
                            
                            // Create user_data JSON for backward compatibility with AddPropertyActivity and others
                            // Structure must match what login stores: { id, email, user_metadata: { role, first_name, last_name, email } }
                            JSONObject userData = new JSONObject();
                            userData.put("id", userId);
                            userData.put("email", user.optString("email"));
                            
                            JSONObject userMetadata = new JSONObject();
                            userMetadata.put("first_name", profile.optString("first_name"));
                            userMetadata.put("last_name", profile.optString("last_name"));
                            userMetadata.put("role", profile.optString("role"));
                            userMetadata.put("email", user.optString("email"));
                            userData.put("user_metadata", userMetadata);
                            
                            editor.putString("user_data", userData.toString());
                            Log.d("SignUpPassword", "user_data JSON saved with structure matching login");
                            
                            // Also save to user_session SharedPreferences for consistency
                            SharedPreferences userSession = getSharedPreferences("user_session", MODE_PRIVATE);
                            SharedPreferences.Editor sessionEditor = userSession.edit();
                            sessionEditor.putString("user_id", userId);
                            sessionEditor.putString("email", user.optString("email"));
                            sessionEditor.putString("full_name", profile.optString("first_name") + " " + profile.optString("last_name"));
                            sessionEditor.putString("phone", profile.optString("phone"));
                            sessionEditor.apply();
                            
                            editor.apply();
                            
                            Log.d("SignUpPassword", "Session saved - user_id: " + userId + ", role: " + profile.optString("role"));
                        } else {
                            Log.w("SignUpPassword", "Missing user or profile in response");
                        }
                    } catch (Exception e) {
                        Log.e("SignUpPassword", "Error saving session", e);
                    }

                    Toast.makeText(SignUpPasswordActivity.this, "Registration completed successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to dashboard
                    navigateToDashboard();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    continueButton.setEnabled(true);
                    Toast.makeText(SignUpPasswordActivity.this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void navigateToDashboard() {
        Intent intent;
        if ("tenant".equals(userType)) {
            intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
        } else {
            intent = new Intent(this, com.roominate.activities.owner.OwnerDashboardActivity.class);
        }

        intent.putExtra("userType", userType);
        intent.putExtra("email", email);

        // Clear activity stack so user can't go back to auth flow
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
