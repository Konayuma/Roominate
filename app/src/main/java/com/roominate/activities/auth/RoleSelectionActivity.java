package com.roominate.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.roominate.R;

public class RoleSelectionActivity extends AppCompatActivity {

    private CardView tenantCard;
    private CardView ownerCard;
    private RadioButton tenantRadio;
    private RadioButton ownerRadio;
    private Button continueButton;
    private String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide ActionBar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_role_selection);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        tenantCard = findViewById(R.id.tenantCard);
        ownerCard = findViewById(R.id.ownerCard);
        tenantRadio = findViewById(R.id.tenantRadio);
        ownerRadio = findViewById(R.id.ownerRadio);
        continueButton = findViewById(R.id.continueButton);
    }

    private void setupListeners() {
        tenantCard.setOnClickListener(v -> {
            selectedRole = "tenant";
            selectCard(tenantCard, ownerCard, tenantRadio, ownerRadio);
            continueButton.setEnabled(true);
        });

        ownerCard.setOnClickListener(v -> {
            selectedRole = "owner";
            selectCard(ownerCard, tenantCard, ownerRadio, tenantRadio);
            continueButton.setEnabled(true);
        });

        continueButton.setOnClickListener(v -> {
            proceedToNextStep();
        });
    }

    private void selectCard(CardView selectedCard, CardView unselectedCard, 
                           RadioButton selectedRadio, RadioButton unselectedRadio) {
        // Visual feedback for selection
        selectedCard.setCardElevation(8f);
        selectedCard.setAlpha(1.0f);
        unselectedCard.setCardElevation(0f);
        unselectedCard.setAlpha(0.7f);
        
        // Update radio buttons
        selectedRadio.setChecked(true);
        unselectedRadio.setChecked(false);
    }

    private void proceedToNextStep() {
        // Move to Basic Info screen before collecting email
        Intent intent = new Intent(RoleSelectionActivity.this, SignUpBasicInfoActivity.class);
        intent.putExtra("userRole", selectedRole);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            // If BasicInfo activity is missing for some reason, fall back to email screen to avoid crash
            Intent fallback = new Intent(RoleSelectionActivity.this, SignUpEmailActivity.class);
            fallback.putExtra("userRole", selectedRole);
            startActivity(fallback);
        }
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
