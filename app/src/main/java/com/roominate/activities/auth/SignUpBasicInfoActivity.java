package com.roominate.activities.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;

public class SignUpBasicInfoActivity extends AppCompatActivity {

    private TextInputLayout firstNameLayout;
    private TextInputEditText firstNameEditText;
    private TextInputLayout lastNameLayout;
    private TextInputEditText lastNameEditText;
    private TextInputLayout dobLayout;
    private TextInputEditText dobEditText;
    private TextInputLayout phoneLayout;
    private TextInputEditText phoneEditText;
    private Button continueButton;
    private ProgressBar progressBar;
    private String userRole;
    private int dobYear = -1;
    private int dobMonth = -1;
    private int dobDay = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_signup_basic_info);

        // Get user role from previous screen
        userRole = getIntent().getStringExtra("userRole");

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        firstNameLayout = findViewById(R.id.firstNameLayout);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameLayout = findViewById(R.id.lastNameLayout);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        dobLayout = findViewById(R.id.dobLayout);
        dobEditText = findViewById(R.id.dobEditText);
        phoneLayout = findViewById(R.id.phoneLayout);
        phoneEditText = findViewById(R.id.phoneEditText);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> validateAndContinue());

        // Real-time validation
        firstNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateFirstName();
        });

        lastNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateLastName();
        });

        phoneEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePhone();
        });

        dobEditText.setOnClickListener(v -> showDatePicker());
        dobEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
        });
    }

    private boolean validateFirstName() {
        String first = firstNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(first)) {
            firstNameLayout.setError("First name is required");
            return false;
        } else if (first.length() < 2) {
            firstNameLayout.setError("Enter a valid first name");
            return false;
        } else {
            firstNameLayout.setError(null);
            return true;
        }
    }

    private boolean validateLastName() {
        String last = lastNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(last)) {
            lastNameLayout.setError("Last name is required");
            return false;
        } else if (last.length() < 2) {
            lastNameLayout.setError("Enter a valid last name");
            return false;
        } else {
            lastNameLayout.setError(null);
            return true;
        }
    }

    private boolean validateEmail() {
        // email validation no longer handled here; keep method for backward compatibility
        return true;
    }

    private boolean validatePhone() {
        String phone = phoneEditText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError("Phone number is required");
            return false;
        } else if (phone.length() < 10) {
            phoneLayout.setError("Invalid phone number");
            return false;
        } else {
            phoneLayout.setError(null);
            return true;
        }
    }

    private boolean validateDob() {
        String dob = dobEditText.getText().toString().trim();
        if (TextUtils.isEmpty(dob) || dobYear < 0) {
            dobLayout.setError("Date of birth is required");
            return false;
        }
        // Simple age check: user must be at least 13 years old
        java.util.Calendar today = java.util.Calendar.getInstance();
        int age = today.get(java.util.Calendar.YEAR) - dobYear;
        if (age < 13) {
            dobLayout.setError("You must be at least 13 years old");
            return false;
        }
        dobLayout.setError(null);
        return true;
    }

    private void showDatePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR) - 18; // default to 18 years back
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            dobYear = y; dobMonth = m; dobDay = d;
            // format as YYYY-MM-DD
            String formatted = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            dobEditText.setText(formatted);
            dobLayout.setError(null);
        }, year, month, day);

        // limit selectable years to reasonable range
        java.util.Calendar min = java.util.Calendar.getInstance();
        min.add(java.util.Calendar.YEAR, -100);
        dpd.getDatePicker().setMinDate(min.getTimeInMillis());
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
        dpd.show();
    }

    private void validateAndContinue() {
        boolean okFirst = validateFirstName();
        boolean okLast = validateLastName();
        boolean okDob = validateDob();
        boolean okPhone = validatePhone();

        if (okFirst && okLast && okDob && okPhone) {
            proceedToEmailScreen();
        }
    }

    private void proceedToEmailScreen() {
        Intent intent = new Intent(this, SignUpEmailActivity.class);
        intent.putExtra("userRole", userRole);
        intent.putExtra("firstName", firstNameEditText.getText().toString().trim());
        intent.putExtra("lastName", lastNameEditText.getText().toString().trim());
        intent.putExtra("dob", dobEditText.getText().toString().trim());
        intent.putExtra("phone", phoneEditText.getText().toString().trim());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
