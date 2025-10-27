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
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;

public class SignUpPasswordActivity extends AppCompatActivity {

    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordEditText;
    private ImageButton passwordVisibilityToggle;
    private ImageButton confirmPasswordVisibilityToggle;
    private TextView passwordStrengthText;
    private ProgressBar passwordStrengthBar;
    private Button continueButton;
    
    private String userRole, fullName, email, phone;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

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
        email = getIntent().getStringExtra("email");

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        passwordLayout = findViewById(R.id.passwordLayout);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);
        confirmPasswordVisibilityToggle = findViewById(R.id.confirmPasswordVisibilityToggle);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        continueButton = findViewById(R.id.continueButton);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> validateAndContinue());

        // Password visibility toggles
        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());
        confirmPasswordVisibilityToggle.setOnClickListener(v -> toggleConfirmPasswordVisibility());

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

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordVisibilityToggle.setImageResource(android.R.drawable.ic_menu_view);
            isPasswordVisible = false;
        } else {
            passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordVisibilityToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isPasswordVisible = true;
        }
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            confirmPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            confirmPasswordVisibilityToggle.setImageResource(android.R.drawable.ic_menu_view);
            isConfirmPasswordVisible = false;
        } else {
            confirmPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            confirmPasswordVisibilityToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isConfirmPasswordVisible = true;
        }
        confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
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
        // TODO: Create user account with Supabase
        
        Intent intent = new Intent(this, SignUpWelcomeActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("email", email);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
