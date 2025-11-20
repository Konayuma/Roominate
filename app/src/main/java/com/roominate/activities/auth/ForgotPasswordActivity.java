package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private MaterialButton resetPasswordButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailLayout = findViewById(R.id.emailLayout);
        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        resetPasswordButton.setOnClickListener(v -> attemptPasswordReset());
    }

    private void attemptPasswordReset() {
        emailLayout.setError(null);

        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!isEmailValid(email)) {
            emailLayout.setError("Invalid email address");
            emailEditText.requestFocus();
            return;
        }

        performPasswordReset(email);
    }

    private void performPasswordReset(String email) {
        showProgress(true);

        com.roominate.services.SupabaseClient.getInstance().resetPassword(email, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ForgotPasswordActivity.this, 
                        "Verification code sent to your email", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Navigate to OTP verification screen
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordVerificationActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ForgotPasswordActivity.this, 
                        "Failed to send reset email: " + error, 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        resetPasswordButton.setEnabled(!show);
    }
}
