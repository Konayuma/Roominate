package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class SignUpVerificationActivity extends AppCompatActivity {

    private TextView emailText;
    private EditText code1, code2, code3, code4, code5, code6;
    private Button verifyButton;
    private TextView resendText;
    private TextView timerText;
    private ProgressBar progressBar;
    
    private String userRole, fullName, email, phone, password;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_verification);

        // Get data from previous screens
        userRole = getIntent().getStringExtra("userRole");
        fullName = getIntent().getStringExtra("fullName");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");
        password = getIntent().getStringExtra("password");

        initializeViews();
        setupListeners();
        startResendTimer();
    }

    private void initializeViews() {
        emailText = findViewById(R.id.emailText);
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        code5 = findViewById(R.id.code5);
        code6 = findViewById(R.id.code6);
        verifyButton = findViewById(R.id.verifyButton);
        resendText = findViewById(R.id.resendText);
        timerText = findViewById(R.id.timerText);
        progressBar = findViewById(R.id.progressBar);

        emailText.setText("We've sent a verification code to " + email);
    }

    private void setupListeners() {
        verifyButton.setOnClickListener(v -> verifyCode());
        resendText.setOnClickListener(v -> resendCode());

        // Auto-focus next field
        setupCodeInput(code1, null, code2);
        setupCodeInput(code2, code1, code3);
        setupCodeInput(code3, code2, code4);
        setupCodeInput(code4, code3, code5);
        setupCodeInput(code5, code4, code6);
        setupCodeInput(code6, code5, null);
    }

    private void setupCodeInput(EditText current, EditText previous, EditText next) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) {
                    next.requestFocus();
                } else if (s.length() == 0 && previous != null) {
                    previous.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void startResendTimer() {
        resendText.setEnabled(false);
        resendText.setAlpha(0.5f);
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                timerText.setText("");
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

        // TODO: Implement actual verification with Supabase
        // For now, simulate verification
        progressBar.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(false);

        // Simulate API call
        verifyButton.postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            verifyButton.setEnabled(true);
            
            // Proceed to welcome screen
            proceedToWelcome();
        }, 2000);
    }

    private void resendCode() {
        // TODO: Implement resend code with Supabase
        Toast.makeText(this, "Verification code resent", Toast.LENGTH_SHORT).show();
        startResendTimer();
    }

    private void proceedToWelcome() {
        Intent intent = new Intent(this, SignUpWelcomeActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("fullName", fullName);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
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
