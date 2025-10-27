package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class SignUpEmailActivity extends AppCompatActivity {

    private ImageView logoImage;
    private TextView titleText;
    private TextView subtitleText;
    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
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
        
        setContentView(R.layout.activity_signup_email);

        // Get user role from previous screen
        userRole = getIntent().getStringExtra("userRole");

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        logoImage = findViewById(R.id.logoImage);
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        emailLayout = findViewById(R.id.emailLayout);
        emailEditText = findViewById(R.id.emailEditText);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> validateAndContinue());

        // Real-time validation
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });
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

    private void validateAndContinue() {
        if (validateEmail()) {
            proceedToVerification();
        }
    }

    private void proceedToVerification() {
        String email = emailEditText.getText().toString().trim();
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        continueButton.setEnabled(false);
        
        // Send OTP via Supabase Edge Function
        SupabaseClient.getInstance().sendOtp(email, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    continueButton.setEnabled(true);
                    
                    // OTP sent successfully, proceed to verification screen
                    Intent intent = new Intent(SignUpEmailActivity.this, SignUpEmailVerificationActivity.class);
                    intent.putExtra("userRole", userRole);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    continueButton.setEnabled(true);
                    Toast.makeText(SignUpEmailActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
