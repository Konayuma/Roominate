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
        resendText.setOnClickListener(v -> resendCode());

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

        Log.d("VerificationActivity", "=== VERIFY CODE CALLED ===");
        Log.d("VerificationActivity", "Code length: " + code.length());
        Log.d("VerificationActivity", "Code: " + code);
        Log.d("VerificationActivity", "Email: " + email);

        if (code.length() != 6) {
            Toast.makeText(this, "Please enter the complete verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        Log.d("VerificationActivity", "Progress bar visible, calling SupabaseClient.verifyOtp()");

        // Call Supabase Edge Function to verify OTP
        SupabaseClient.getInstance().verifyOtp(email, code, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d("VerificationActivity", "✅✅✅ CALLBACK onSuccess() triggered!");
                Log.d("VerificationActivity", "Response: " + response.toString());
                
                runOnUiThread(() -> {
                    Log.d("VerificationActivity", "Running on UI thread - hiding progress, showing toast");
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignUpEmailVerificationActivity.this, 
                        "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Proceed to password screen
                    Log.d("VerificationActivity", "Calling proceedToPassword()");
                    proceedToPassword();
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("VerificationActivity", "❌❌❌ CALLBACK onError() triggered!");
                Log.e("VerificationActivity", "Error: " + error);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SignUpEmailVerificationActivity.this, error, Toast.LENGTH_LONG).show();
                    
                    // Clear code fields on error
                    code1.setText("");
                    code2.setText("");
                    code3.setText("");
                    code4.setText("");
                    code5.setText("");
                    code6.setText("");
                    code1.requestFocus();
                });
            }
        });
        
        Log.d("VerificationActivity", "verifyOtp() call completed, waiting for callback...");
    }

    private void resendCode() {
        resendText.setEnabled(false);
        
        // Call send-otp again
        SupabaseClient.getInstance().sendOtp(email, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpEmailVerificationActivity.this, 
                        "Verification code resent", Toast.LENGTH_SHORT).show();
                    startResendTimer();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpEmailVerificationActivity.this, 
                        "Failed to resend code: " + error, Toast.LENGTH_LONG).show();
                    resendText.setEnabled(true);
                });
            }
        });
    }

    private void proceedToPassword() {
        Intent intent = new Intent(this, SignUpPasswordActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("firstName", firstName);
        intent.putExtra("lastName", lastName);
        intent.putExtra("dob", dob);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
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
