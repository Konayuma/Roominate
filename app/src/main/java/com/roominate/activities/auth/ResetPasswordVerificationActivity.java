package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import org.json.JSONObject;

public class ResetPasswordVerificationActivity extends AppCompatActivity {
    private static final String TAG = "ResetPwdVerification";

    private EditText code1, code2, code3, code4, code5, code6;
    private MaterialButton verifyButton;
    private TextView resendText, emailText;
    private ProgressBar progressBar;
    private String email;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_reset_password_verification);

        email = getIntent().getStringExtra("email");
        token = getIntent().getStringExtra("token");

        initializeViews();
        setupCodeInputs();
        setupListeners();

        if (email != null) {
            emailText.setText("We've sent a 6-digit code to " + email);
        }
    }

    private void initializeViews() {
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        code5 = findViewById(R.id.code5);
        code6 = findViewById(R.id.code6);
        verifyButton = findViewById(R.id.verifyButton);
        resendText = findViewById(R.id.resendText);
        emailText = findViewById(R.id.emailText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupCodeInputs() {
        EditText[] codes = {code1, code2, code3, code4, code5, code6};
        
        for (int i = 0; i < codes.length; i++) {
            final int index = i;
            codes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < codes.length - 1) {
                        codes[index + 1].requestFocus();
                    }
                    
                    // Auto-verify when all fields are filled
                    if (isCodeComplete()) {
                        verifyCode();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            codes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (codes[index].getText().toString().isEmpty() && index > 0) {
                        codes[index - 1].requestFocus();
                        codes[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void setupListeners() {
        verifyButton.setOnClickListener(v -> verifyCode());
        
        resendText.setOnClickListener(v -> resendCode());
    }

    private boolean isCodeComplete() {
        return !code1.getText().toString().isEmpty() &&
               !code2.getText().toString().isEmpty() &&
               !code3.getText().toString().isEmpty() &&
               !code4.getText().toString().isEmpty() &&
               !code5.getText().toString().isEmpty() &&
               !code6.getText().toString().isEmpty();
    }

    private String getEnteredCode() {
        return code1.getText().toString() +
               code2.getText().toString() +
               code3.getText().toString() +
               code4.getText().toString() +
               code5.getText().toString() +
               code6.getText().toString();
    }

    private void verifyCode() {
        if (!isCodeComplete()) {
            Toast.makeText(this, "Please enter the complete code", Toast.LENGTH_SHORT).show();
            return;
        }

        String code = getEnteredCode();
        showProgress(true);

        com.roominate.services.SupabaseClient.getInstance().verifyOTP(email, code, "recovery", new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        String accessToken = response.optString("access_token", "");
                        
                        // Navigate to new password screen
                        Intent intent = new Intent(ResetPasswordVerificationActivity.this, ResetPasswordNewPasswordActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("access_token", accessToken);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(ResetPasswordVerificationActivity.this, "Verification successful, but an error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ResetPasswordVerificationActivity.this, "Invalid code. Please try again.", Toast.LENGTH_LONG).show();
                    clearCode();
                });
            }
        });
    }

    private void resendCode() {
        showProgress(true);
        
        com.roominate.services.SupabaseClient.getInstance().resetPassword(email, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ResetPasswordVerificationActivity.this, "Code resent to your email", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ResetPasswordVerificationActivity.this, "Failed to resend code", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void clearCode() {
        code1.setText("");
        code2.setText("");
        code3.setText("");
        code4.setText("");
        code5.setText("");
        code6.setText("");
        code1.requestFocus();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!show);
        resendText.setEnabled(!show);
    }
}
