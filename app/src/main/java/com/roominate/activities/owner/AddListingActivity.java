package com.roominate.activities.owner;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roominate.R;
import com.roominate.models.Property;
import com.roominate.services.SupabaseClient;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AddListingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputEditText propertyNameEditText, descriptionEditText, addressEditText, priceEditText, depositEditText;
    private TextInputLayout propertyNameInputLayout, descriptionInputLayout, addressInputLayout, priceInputLayout, depositInputLayout;
    private ChipGroup amenitiesChipGroup;
    private MaterialButton saveButton;
    private ProgressBar progressBar;

    private boolean isEditMode = false;
    private String propertyId;
    private Property currentProperty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        initializeViews();
        setupToolbar();
        setupListeners();

        if (getIntent().hasExtra("property_id")) {
            isEditMode = true;
            propertyId = getIntent().getStringExtra("property_id");
            toolbar.setTitle("Edit Listing");
            loadPropertyData();
        } else {
            toolbar.setTitle("Add New Listing");
            currentProperty = new Property();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        propertyNameEditText = findViewById(R.id.propertyNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        priceEditText = findViewById(R.id.priceEditText);
        depositEditText = findViewById(R.id.depositEditText);

        propertyNameInputLayout = findViewById(R.id.propertyNameInputLayout);
        descriptionInputLayout = findViewById(R.id.descriptionInputLayout);
        addressInputLayout = findViewById(R.id.addressInputLayout);
        priceInputLayout = findViewById(R.id.priceInputLayout);
        depositInputLayout = findViewById(R.id.depositInputLayout);

        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveListing());
    }

    private void loadPropertyData() {
        setLoading(true);
        SupabaseClient.getInstance().getPropertyById(propertyId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    // The API returns an array with one item for a specific ID
                    currentProperty = Property.fromJson(response.getJSONArray("data").getJSONObject(0));
                    runOnUiThread(() -> {
                        populateForm();
                        setLoading(false);
                    });
                } catch (Exception e) {
                    onError("Failed to parse property data.");
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AddListingActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    finish(); // Close if we can't load the data
                });
            }
        });
    }

    private void populateForm() {
        if (currentProperty == null) return;

        propertyNameEditText.setText(currentProperty.getName());
        descriptionEditText.setText(currentProperty.getDescription());
        addressEditText.setText(currentProperty.getAddress());
        priceEditText.setText(String.valueOf(currentProperty.getMonthlyRate()));
        depositEditText.setText(String.valueOf(currentProperty.getSecurityDeposit()));

        // Set amenities
        List<String> amenities = currentProperty.getAmenities();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
            if (amenities.contains(chip.getText().toString())) {
                chip.setChecked(true);
            }
        }
    }

    private void saveListing() {
        if (!validateInput()) {
            return;
        }

        setLoading(true);

        // Populate the Property object from the form
        currentProperty.setName(propertyNameEditText.getText().toString().trim());
        currentProperty.setDescription(descriptionEditText.getText().toString().trim());
        currentProperty.setAddress(addressEditText.getText().toString().trim());
        currentProperty.setMonthlyRate(Double.parseDouble(priceEditText.getText().toString()));
        currentProperty.setSecurityDeposit(Double.parseDouble(depositEditText.getText().toString()));
        currentProperty.setStatus("published"); // Or handle draft/publish logic

        // Get selected amenities
        List<String> selectedAmenities = new ArrayList<>();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedAmenities.add(chip.getText().toString());
            }
        }
        currentProperty.setAmenities(selectedAmenities);

        // TODO: Handle image URLs

        SupabaseClient.ApiCallback callback = new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AddListingActivity.this, "Listing saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AddListingActivity.this, "Failed to save: " + error, Toast.LENGTH_LONG).show();
                });
            }
        };

        if (isEditMode) {
            SupabaseClient.getInstance().updateProperty(currentProperty, callback);
        } else {
            SupabaseClient.getInstance().insertProperty(currentProperty, callback);
        }
    }

    private boolean validateInput() {
        // Simple validation, can be expanded
        boolean isValid = true;
        if (propertyNameEditText.getText().toString().trim().isEmpty()) {
            propertyNameInputLayout.setError("Property name is required");
            isValid = false;
        } else {
            propertyNameInputLayout.setError(null);
        }
        if (addressEditText.getText().toString().trim().isEmpty()) {
            addressInputLayout.setError("Address is required");
            isValid = false;
        } else {
            addressInputLayout.setError(null);
        }
        if (priceEditText.getText().toString().trim().isEmpty()) {
            priceInputLayout.setError("Monthly rent is required");
            isValid = false;
        } else {
            priceInputLayout.setError(null);
        }
        return isValid;
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
        }
    }
}
