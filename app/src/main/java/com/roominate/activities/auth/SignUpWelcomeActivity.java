package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.activities.tenant.TenantDashboardActivity;
import com.roominate.activities.owner.OwnerDashboardActivity;

public class SignUpWelcomeActivity extends AppCompatActivity {

    private TextView welcomeNameText;
    private TextView welcomeMessageText;
    private Button getStartedButton;
    private Button completeProfileButton;
    
    private String userRole, fullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_welcome);

        // Get data from previous screens
        userRole = getIntent().getStringExtra("userRole");
        String email = getIntent().getStringExtra("email");
        
        // Extract first name from email (before @)
        fullName = email != null && email.contains("@") ? 
                  email.substring(0, email.indexOf("@")) : "User";

        initializeViews();
        setupListeners();
        customizeWelcomeMessage();
    }

    private void initializeViews() {
        welcomeNameText = findViewById(R.id.welcomeNameText);
        welcomeMessageText = findViewById(R.id.welcomeMessageText);
        getStartedButton = findViewById(R.id.getStartedButton);
        completeProfileButton = findViewById(R.id.completeProfileButton);
    }

    private void setupListeners() {
        getStartedButton.setOnClickListener(v -> proceedToDashboard());
        completeProfileButton.setOnClickListener(v -> proceedToProfileSetup());
    }

    private void customizeWelcomeMessage() {
        welcomeNameText.setText("Welcome, " + fullName + "!");
        
        if ("tenant".equals(userRole)) {
            welcomeMessageText.setText("You're all set! Start browsing available boarding houses and find your perfect space.");
        } else {
            welcomeMessageText.setText("You're all set! Start listing your properties and connect with potential tenants.");
        }
    }

    private void proceedToDashboard() {
        Intent intent;
        if ("tenant".equals(userRole)) {
            intent = new Intent(this, TenantDashboardActivity.class);
        } else {
            intent = new Intent(this, OwnerDashboardActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void proceedToProfileSetup() {
        // TODO: Create profile setup activity
        // For now, just go to dashboard
        proceedToDashboard();
    }
}
