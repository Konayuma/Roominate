package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private RadioGroup userTypeRadioGroup;
    private RadioButton tenantRadioButton;
    private RadioButton ownerRadioButton;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        userTypeRadioGroup = findViewById(R.id.userTypeRadioGroup);
        tenantRadioButton = findViewById(R.id.tenantRadioButton);
        ownerRadioButton = findViewById(R.id.ownerRadioButton);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());

        loginTextView.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void attemptRegister() {
        // Reset errors
        fullNameEditText.setError(null);
        emailEditText.setError(null);
        phoneEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        // Get values
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
        int selectedUserTypeId = userTypeRadioGroup.getCheckedRadioButtonId();
        String userType = selectedUserTypeId == tenantRadioButton.getId() ? "tenant" : "owner";

        // Validate
        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(confirmPassword) || !password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            focusView = confirmPasswordEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            focusView = passwordEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("Phone number is required");
            focusView = phoneEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            emailEditText.setError("Invalid email address");
            focusView = emailEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Full name is required");
            focusView = fullNameEditText;
            cancel = true;
        }

        if (selectedUserTypeId == -1) {
            Toast.makeText(this, "Please select user type (Tenant or Owner)", Toast.LENGTH_SHORT).show();
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            performRegister(fullName, email, phone, password, userType);
        }
    }

    private void performRegister(String fullName, String email, String phone, 
                                 String password, String userType) {
        showProgress(true);

        // TODO: Implement Supabase registration
        // Example:
        // AuthRepository.register(fullName, email, phone, password, userType, new AuthCallback() {
        //     @Override
        //     public void onSuccess(User user) {
        //         showProgress(false);
        //         Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
        //         redirectToDashboard(userType);
        //     }
        //
        //     @Override
        //     public void onError(String error) {
        //         showProgress(false);
        //         Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
        //     }
        // });

        // Temporary simulation
        Toast.makeText(this, "Registration functionality to be implemented", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }

    private void redirectToDashboard(String userType) {
        Intent intent;
        switch (userType) {
            case "tenant":
                // intent = new Intent(this, TenantDashboardActivity.class);
                break;
            case "owner":
                // intent = new Intent(this, OwnerDashboardActivity.class);
                break;
            default:
                return;
        }
        // startActivity(intent);
        // finish();
    }
}
