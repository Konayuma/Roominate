package com.roominate.activities.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.activities.auth.LoginActivity;

public class TenantProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText occupationEditText;
    private Button saveButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_profile);

        initializeViews();
        loadUserProfile();
        setupListeners();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        occupationEditText = findViewById(R.id.occupationEditText);
        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void loadUserProfile() {
        // TODO: Load user profile from Supabase
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProfile());
        logoutButton.setOnClickListener(v -> logout());
        profileImageView.setOnClickListener(v -> changeProfileImage());
    }

    private void saveProfile() {
        // TODO: Save profile updates to Supabase
        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void changeProfileImage() {
        // TODO: Implement image picker and upload
    }

    private void logout() {
        // TODO: Clear session and logout
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
