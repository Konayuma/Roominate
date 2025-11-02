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
import com.roominate.services.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Log;

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
        
        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
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
        // Prefill email from last OTP/email submission if available
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
            String lastOtpEmail = prefs.getString("last_otp_email", null);
            if (lastOtpEmail == null) lastOtpEmail = prefs.getString("last_signed_email", null);
            if (lastOtpEmail != null) {
                emailEditText.setText(lastOtpEmail);
            }
        } catch (Exception ignored) {}
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
        SupabaseClient.getInstance().signIn(email, password, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("LoginActivity", "signIn success: " + response.toString());

                // Store user data from sign-in response for later use
                try {
                    if (response.has("user")) {
                        JSONObject user = response.getJSONObject("user");
                        android.content.SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                        prefs.edit()
                            .putString("user_data", user.toString())
                            .putString("last_signed_email", email)
                            .apply();
                    }
                } catch (Exception e) {
                    Log.e("LoginActivity", "Error storing user data", e);
                }

                // Get user profile to determine role
                SupabaseClient.getInstance().getUserProfile(new SupabaseClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject profileResponse) {
                        runOnUiThread(() -> {
                            showProgress(false);
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            try {
                                String role = profileResponse.optString("role", "tenant");
                                redirectToDashboard(role);
                            } catch (Exception e) {
                                Log.e("LoginActivity", "Error parsing user role", e);
                                redirectToDashboard("tenant"); // Default fallback
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("LoginActivity", "Error getting user profile: " + error);
                        runOnUiThread(() -> {
                            showProgress(false);
                            Toast.makeText(LoginActivity.this, "Login successful, but failed to load profile", Toast.LENGTH_SHORT).show();
                            redirectToDashboard("tenant"); // Default fallback
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.d("LoginActivity", "signIn error: " + error);
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
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
        switch (userType.toLowerCase()) {
            case "tenant":
                intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
                break;
            case "owner":
                intent = new Intent(this, com.roominate.activities.owner.OwnerDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(this, com.roominate.activities.admin.AdminDashboardActivity.class);
                break;
            default:
                intent = new Intent(this, com.roominate.activities.tenant.TenantDashboardActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
