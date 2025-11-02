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
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private RadioGroup userTypeRadioGroup;
    private RadioButton tenantRadioButton;
    private RadioButton ownerRadioButton;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_register);

        initializeViews();
        setupListeners();

        // Pre-fill email if provided
        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            emailEditText.setText(prefilledEmail);
        }

        // Handle back pressed using OnBackPressedDispatcher for compatibility
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Go back to login
            }
        });
    }

    private void initializeViews() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
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
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);
        emailEditText.setError(null);
        phoneEditText.setError(null);

        // Get values
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        int selectedUserTypeId = userTypeRadioGroup.getCheckedRadioButtonId();
        String userType = selectedUserTypeId == tenantRadioButton.getId() ? "tenant" : "owner";

        // Validate
        boolean cancel = false;
        View focusView = null;

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

        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Last name is required");
            focusView = lastNameEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("First name is required");
            focusView = firstNameEditText;
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
            performRegister(firstName, lastName, email, phone, userType);
        }
    }

    private void performRegister(String firstName, String lastName, String email,
                                 String phone, String userType) {
        showProgress(true);

        try {
                // Request OTP via Edge Function (Resend) instead of default Supabase signup email
                SupabaseClient.getInstance().requestOtp(email, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(RegisterActivity.this,
                                "Verification code sent — check your email.",
                            Toast.LENGTH_LONG).show();

                        // Navigate to email confirmation screen
                        Intent intent = new Intent(RegisterActivity.this, SignUpEmailVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("userRole", userType);
                        intent.putExtra("firstName", firstName);
                        intent.putExtra("lastName", lastName);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(RegisterActivity.this, "Failed to send verification code: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            showProgress(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }
}
