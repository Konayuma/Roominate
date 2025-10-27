package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;

public class SignUpBasicInfoActivity extends AppCompatActivity {

    private TextInputLayout fullNameLayout;
    private TextInputEditText fullNameEditText;
    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout phoneLayout;
    private TextInputEditText phoneEditText;
    private Button continueButton;
    private ProgressBar progressBar;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_basic_info);

        // Get user role from previous screen
        userRole = getIntent().getStringExtra("userRole");

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        fullNameLayout = findViewById(R.id.fullNameLayout);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailLayout = findViewById(R.id.emailLayout);
        emailEditText = findViewById(R.id.emailEditText);
        phoneLayout = findViewById(R.id.phoneLayout);
        phoneEditText = findViewById(R.id.phoneEditText);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> validateAndContinue());

        // Real-time validation
        fullNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateFullName();
            }
        });

        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        phoneEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePhone();
            }
        });
    }

    private boolean validateFullName() {
        String fullName = fullNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(fullName)) {
            fullNameLayout.setError("Full name is required");
            return false;
        } else if (fullName.length() < 3) {
            fullNameLayout.setError("Name must be at least 3 characters");
            return false;
        } else {
            fullNameLayout.setError(null);
            return true;
        }
    }

    private boolean validateEmail() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email address");
            return false;
        } else {
            emailLayout.setError(null);
            return true;
        }
    }

    private boolean validatePhone() {
        String phone = phoneEditText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError("Phone number is required");
            return false;
        } else if (phone.length() < 10) {
            phoneLayout.setError("Invalid phone number");
            return false;
        } else {
            phoneLayout.setError(null);
            return true;
        }
    }

    private void validateAndContinue() {
        boolean isValidName = validateFullName();
        boolean isValidEmail = validateEmail();
        boolean isValidPhone = validatePhone();

        if (isValidName && isValidEmail && isValidPhone) {
            proceedToPasswordScreen();
        }
    }

    private void proceedToPasswordScreen() {
        Intent intent = new Intent(this, SignUpPasswordActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("fullName", fullNameEditText.getText().toString().trim());
        intent.putExtra("email", emailEditText.getText().toString().trim());
        intent.putExtra("phone", phoneEditText.getText().toString().trim());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
