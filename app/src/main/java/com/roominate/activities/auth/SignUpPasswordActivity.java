package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
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
    
    private String userRole, firstName, lastName, dob, phone, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_password);

        // Get data from previous screen
        userRole = getIntent().getStringExtra("userRole");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        dob = getIntent().getStringExtra("dob");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");

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
        String password = passwordEditText.getText().toString();
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        continueButton.setEnabled(false);
        
        // Create user profile data
        try {
            JSONObject userData = new JSONObject();
            userData.put("email", email);
            userData.put("password", password);
            userData.put("role", userRole);
            userData.put("first_name", firstName);
            userData.put("last_name", lastName);
            userData.put("dob", dob);
            userData.put("phone", phone);
            
            // Create user account with Supabase
            SupabaseClient.getInstance().createUser(userData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        continueButton.setEnabled(true);
                        
                        Toast.makeText(SignUpPasswordActivity.this, 
                            "Account created successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Proceed to welcome screen
                        Intent intent = new Intent(SignUpPasswordActivity.this, SignUpWelcomeActivity.class);
                        intent.putExtra("userRole", userRole);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        continueButton.setEnabled(true);
                        Toast.makeText(SignUpPasswordActivity.this, 
                            "Failed to create account: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
            
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            continueButton.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
