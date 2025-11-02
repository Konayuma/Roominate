package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class EmailConfirmationActivity extends AppCompatActivity {

    private TextView emailText;
    private EditText tokenEditText;
    private Button confirmButton;
    private Button resendButton;
    private ProgressBar progressBar;

    private String email;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_email_confirmation);

        // Get data from previous screen
        email = getIntent().getStringExtra("email");
        userType = getIntent().getStringExtra("userType");

        if (email == null || userType == null) {
            Toast.makeText(this, "Missing registration data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailText = findViewById(R.id.emailText);
        tokenEditText = findViewById(R.id.tokenEditText);
        confirmButton = findViewById(R.id.confirmButton);
        resendButton = findViewById(R.id.resendButton);
        progressBar = findViewById(R.id.progressBar);

        emailText.setText("We've sent a confirmation code to\n" + email);
    }

    private void setupListeners() {
        confirmButton.setOnClickListener(v -> confirmEmail());
        resendButton.setOnClickListener(v -> resendConfirmation());

        // Auto-enable confirm button when token is entered
        tokenEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmButton.setEnabled(s.length() == 6);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void confirmEmail() {
        String otp = tokenEditText.getText().toString().trim();

        if (otp.length() != 6) {
            Toast.makeText(this, "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        SupabaseClient.getInstance().verifyOtp(email, otp, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(EmailConfirmationActivity.this,
                        "Email verified successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to password collection screen
                    navigateToPasswordSetup(otp);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(EmailConfirmationActivity.this,
                        "Verification failed: " + error, Toast.LENGTH_LONG).show();

                    // Clear token field on error
                    tokenEditText.setText("");
                });
            }
        });
    }

    private void resendConfirmation() {
        // Request a new OTP via Edge Function
        resendButton.setEnabled(false);
        resendButton.setText("Sending...");
        SupabaseClient.getInstance().requestOtp(email, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    Toast.makeText(EmailConfirmationActivity.this, "Verification code resent.", Toast.LENGTH_LONG).show();
                    resendButton.setText("Resend");
                    // cooldown timer
                    resendButton.postDelayed(() -> resendButton.setEnabled(true), 30000);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(EmailConfirmationActivity.this, "Failed to resend: " + error, Toast.LENGTH_LONG).show();
                    resendButton.setEnabled(true);
                    resendButton.setText("Resend");
                });
            }
        });
    }

    private void navigateToPasswordSetup(String otp) {
        Intent intent = new Intent(this, SignUpPasswordActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("userType", userType);
        intent.putExtra("firstName", getIntent().getStringExtra("firstName"));
        intent.putExtra("lastName", getIntent().getStringExtra("lastName"));
        intent.putExtra("phone", getIntent().getStringExtra("phone"));
        intent.putExtra("otp", otp);
        // Add dob if present
        if (getIntent().hasExtra("dob")) {
            intent.putExtra("dob", getIntent().getStringExtra("dob"));
        }
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        confirmButton.setEnabled(!show);
        if (!show) {
            resendButton.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow going back - user must confirm email or restart signup
        Toast.makeText(this, "Please confirm your email to continue", Toast.LENGTH_SHORT).show();
    }
}