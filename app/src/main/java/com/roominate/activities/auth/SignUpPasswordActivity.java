package com.roominate.activities.auth;

import android.content.Intent;
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
        userType = getIntent().getStringExtra("userType");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        phone = getIntent().getStringExtra("phone");
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
