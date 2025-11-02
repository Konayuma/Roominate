package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;
import com.roominate.activities.owner.OwnerDashboardActivity;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class OwnerSignupActivity extends AppCompatActivity {

    private TextInputEditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private TextInputLayout firstNameInputLayout, lastNameInputLayout, emailInputLayout, passwordInputLayout;
    private MaterialButton signUpButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_signup);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        firstNameInputLayout = findViewById(R.id.firstNameInputLayout);
        lastNameInputLayout = findViewById(R.id.lastNameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.signInTextView).setOnClickListener(v -> {
            // Navigate to LoginActivity, assuming it can handle both user types
            Intent intent = new Intent(OwnerSignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void setupListeners() {
        signUpButton.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        if (!validateInput()) {
            return;
        }

        setLoading(true);

        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        SupabaseClient.getInstance().signUpUser(email, password, "owner", firstName, lastName, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(OwnerSignupActivity.this, "Sign up successful! Please check your email to verify.", Toast.LENGTH_LONG).show();
                    // Navigate to a waiting-for-verification screen or back to login
                    Intent intent = new Intent(OwnerSignupActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(OwnerSignupActivity.this, "Sign up failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean validateInput() {
        // Clear previous errors
        firstNameInputLayout.setError(null);
        lastNameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        boolean isValid = true;

        if (firstNameEditText.getText().toString().trim().isEmpty()) {
            firstNameInputLayout.setError("First name is required");
            isValid = false;
        }

        if (lastNameEditText.getText().toString().trim().isEmpty()) {
            lastNameInputLayout.setError("Last name is required");
            isValid = false;
        }

        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Invalid email address");
            isValid = false;
        }

        String password = passwordEditText.getText().toString().trim();
        if (password.isEmpty()) {
            passwordInputLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signUpButton.setText("");
            signUpButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            signUpButton.setText("Create Account");
            signUpButton.setEnabled(true);
        }
    }
}
