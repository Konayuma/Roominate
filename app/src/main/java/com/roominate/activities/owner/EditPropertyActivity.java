package com.roominate.activities.owner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class EditPropertyActivity extends AppCompatActivity {

    private String propertyId;
    // Similar fields as AddPropertyActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_property);

        propertyId = getIntent().getStringExtra("property_id");

        initializeViews();
        loadPropertyDetails();
        setupListeners();
    }

    private void initializeViews() {
        // TODO: Initialize views (similar to AddPropertyActivity)
    }

    private void loadPropertyDetails() {
        // TODO: Load property details from Supabase
    }

    private void setupListeners() {
        // TODO: Setup save and delete buttons
    }

    private void updateProperty() {
        // TODO: Update property in Supabase
        Toast.makeText(this, "Property updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void deleteProperty() {
        // TODO: Show confirmation dialog and delete property
    }
}
