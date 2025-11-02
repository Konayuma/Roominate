package com.roominate.activities.tenant;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText phoneEditText;
    private Button saveButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupListeners();
        loadUserSettings();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        
        saveButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            
            // TODO: Save settings to Supabase
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserSettings() {
        // TODO: Load user settings from Supabase
        // For now, use placeholder values
        emailEditText.setText("user@example.com");
        phoneEditText.setText("+63 912 345 6789");
    }
}
