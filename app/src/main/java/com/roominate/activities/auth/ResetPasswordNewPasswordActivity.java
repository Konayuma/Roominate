package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;
import com.roominate.activities.MainActivity;
import org.json.JSONObject;

public class ResetPasswordNewPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ResetPwdNewPassword";

    private TextInputLayout passwordLayout, confirmPasswordLayout;
    private TextInputEditText passwordInput, confirmPasswordInput;
    private MaterialButton resetButton;
    private ProgressBar progressBar;
    private String email;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_reset_password_new_password);

        email = getIntent().getStringExtra("email");
        accessToken = getIntent().getStringExtra("access_token");

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        passwordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        passwordInput = findViewById(R.id.newPasswordEditText);
        confirmPasswordInput = findViewById(R.id.confirmPasswordEditText);
        resetButton = findViewById(R.id.resetPasswordButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        resetButton.setOnClickListener(v -> resetPassword());
    }

    private boolean validatePassword(String password) {
        if (password.length() < 8) {
            passwordLayout.setError("Password must be at least 8 characters");
            return false;
        }
        
        if (!password.matches(".*[A-Z].*")) {
            passwordLayout.setError("Password must contain at least one uppercase letter");
            return false;
        }
        
        if (!password.matches(".*[a-z].*")) {
            passwordLayout.setError("Password must contain at least one lowercase letter");
            return false;
        }
        
        if (!password.matches(".*[0-9].*")) {
            passwordLayout.setError("Password must contain at least one number");
            return false;
        }
        
        passwordLayout.setError(null);
        return true;
    }

    private boolean validatePasswordMatch() {
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            return false;
        }
        
        confirmPasswordLayout.setError(null);
        return true;
    }

    private void resetPassword() {
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            return;
        }

        if (!validatePassword(password)) {
            return;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            return;
        }

        if (!validatePasswordMatch()) {
            return;
        }

        showProgress(true);

        com.roominate.services.SupabaseClient.getInstance().updatePassword(accessToken, password, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ResetPasswordNewPasswordActivity.this, "Password reset successfully! Please login with your new password.", Toast.LENGTH_LONG).show();
                    
                    // Navigate to login
                    Intent intent = new Intent(ResetPasswordNewPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ResetPasswordNewPasswordActivity.this, "Failed to reset password: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        resetButton.setEnabled(!show);
        passwordInput.setEnabled(!show);
        confirmPasswordInput.setEnabled(!show);
    }
}
