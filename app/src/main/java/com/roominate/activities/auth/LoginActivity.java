package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private TextView forgotPasswordTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
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

        // TODO: Implement Supabase authentication
        // Example:
        // AuthRepository.login(email, password, new AuthCallback() {
        //     @Override
        //     public void onSuccess(User user) {
        //         showProgress(false);
        //         redirectToDashboard(user.getUserType());
        //     }
        //
        //     @Override
        //     public void onError(String error) {
        //         showProgress(false);
        //         Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
        //     }
        // });

        // Temporary simulation
        Toast.makeText(this, "Login functionality to be implemented", Toast.LENGTH_SHORT).show();
        showProgress(false);
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
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
            case "admin":
                // intent = new Intent(this, AdminDashboardActivity.class);
                break;
            default:
                return;
        }
        // startActivity(intent);
        // finish();
    }
}
