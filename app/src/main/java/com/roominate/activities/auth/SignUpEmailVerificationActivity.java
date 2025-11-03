package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;

public class SignUpEmailVerificationActivity extends AppCompatActivity {

    private ImageView logoImage;
    private TextView titleText;
    private TextView emailText;
    private EditText code1, code2, code3, code4, code5, code6;
    private TextView resendText;
    private ProgressBar progressBar;
    
    private String userRole, firstName, lastName, dob, phone, email;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_email_verification);

        // Get data from previous screens
        userRole = getIntent().getStringExtra("userRole");
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        dob = getIntent().getStringExtra("dob");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");

        initializeViews();
        setupListeners();
        startResendTimer();

        // Send OTP when activity starts using the recently implemented function
        sendOtp();
    }

    private void initializeViews() {
        logoImage = findViewById(R.id.logoImage);
        titleText = findViewById(R.id.titleText);
        emailText = findViewById(R.id.emailText);
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        code5 = findViewById(R.id.code5);
        code6 = findViewById(R.id.code6);
        resendText = findViewById(R.id.resendText);
        progressBar = findViewById(R.id.progressBar);

        emailText.setText("We've sent a 6-digit code to\n" + email);
    }

    private void setupListeners() {
        resendText.setOnClickListener(v -> {
            if (resendText.isEnabled()) {
                sendOtp();
            }
        });

        // Auto-focus next field with backspace support
        setupCodeInput(code1, null, code2);
        setupCodeInput(code2, code1, code3);
        setupCodeInput(code3, code2, code4);
        setupCodeInput(code4, code3, code5);
        setupCodeInput(code5, code4, code6);
        setupCodeInput(code6, code5, null);
    }

    private void setupCodeInput(EditText current, EditText previous, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            private boolean isDeleting = false;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    if (next != null) {
                        next.requestFocus();
                    }
                    // Check if all fields are filled for auto-verification
                    checkAndAutoVerify();
                } else if (s.length() == 0 && previous != null && isDeleting) {
                    previous.requestFocus();
                    // Select all text in previous field so it can be replaced
                    previous.setSelection(previous.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Limit to single character
                if (s.length() > 1) {
                    s.delete(1, s.length());
                }
            }
        });
        
        // Handle backspace on empty field
        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && 
                event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                current.getText().toString().isEmpty() && previous != null) {
                previous.requestFocus();
                previous.setSelection(previous.getText().length());
                return true;
            }
            return false;
        });
    }
    
    private void checkAndAutoVerify() {
        String code = code1.getText().toString() + 
                     code2.getText().toString() + 
                     code3.getText().toString() + 
                     code4.getText().toString() + 
                     code5.getText().toString() + 
                     code6.getText().toString();

        if (code.length() == 6) {
            // All fields filled, auto-verify
            verifyCode();
        }
    }

    private void startResendTimer() {
        resendText.setEnabled(false);
        resendText.setAlpha(0.5f);
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendText.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                resendText.setText("Resend Code");
                resendText.setEnabled(true);
                resendText.setAlpha(1.0f);
            }
        }.start();
    }

    private void verifyCode() {
        String code = code1.getText().toString() + 
                     code2.getText().toString() + 
                     code3.getText().toString() + 
                     code4.getText().toString() + 
                     code5.getText().toString() + 
                     code6.getText().toString();
        if (code.length() != 6) {
            Toast.makeText(this, "Please enter the complete verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        // We no longer verify the OTP from this screen. The password screen will call
        // the combined flow which verifies the OTP server-side and creates the account.
        // This avoids duplicate verification requests and keeps the password creation
        // atomic during signup.

        // Navigate to password entry screen with the OTP
        proceedToPassword(code);
    }

    private void sendOtp() {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Missing email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress and disable resend
        progressBar.setVisibility(View.VISIBLE);
        resendText.setEnabled(false);
        resendText.setAlpha(0.5f);

        SupabaseClient.getInstance().requestOtp(email, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignUpEmailVerificationActivity.this, "Verification code sent — check your email.", Toast.LENGTH_LONG).show();
                    // restart resend timer
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    startResendTimer();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resendText.setEnabled(true);
                    resendText.setAlpha(1.0f);
                    Toast.makeText(SignUpEmailVerificationActivity.this, "Failed to send verification code: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void resendCode() {
        // This method is kept for backward compatibility but we'll call sendOtp now
    }

    private void proceedToPassword(String otp) {
        Intent intent = new Intent(this, SignUpPasswordActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("firstName", firstName);
        intent.putExtra("lastName", lastName);
        intent.putExtra("dob", dob);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
        intent.putExtra("otp", otp);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
