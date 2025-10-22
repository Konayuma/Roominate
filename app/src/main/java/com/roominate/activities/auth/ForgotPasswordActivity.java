package com.roominate.activities.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetPasswordButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        resetPasswordButton.setOnClickListener(v -> attemptPasswordReset());
    }

    private void attemptPasswordReset() {
        emailEditText.setError(null);

        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!isEmailValid(email)) {
            emailEditText.setError("Invalid email address");
            emailEditText.requestFocus();
            return;
        }

        performPasswordReset(email);
    }

    private void performPasswordReset(String email) {
        showProgress(true);

        // TODO: Implement Supabase password reset
        // Example:
        // AuthRepository.resetPassword(email, new AuthCallback() {
        //     @Override
        //     public void onSuccess() {
        //         showProgress(false);
        //         Toast.makeText(ForgotPasswordActivity.this, 
        //             "Password reset link sent to your email", Toast.LENGTH_LONG).show();
        //         finish();
        //     }
        //
        //     @Override
        //     public void onError(String error) {
        //         showProgress(false);
        //         Toast.makeText(ForgotPasswordActivity.this, error, Toast.LENGTH_SHORT).show();
        //     }
        // });

        // Temporary simulation
        Toast.makeText(this, "Password reset functionality to be implemented", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        resetPasswordButton.setEnabled(!show);
    }
}
