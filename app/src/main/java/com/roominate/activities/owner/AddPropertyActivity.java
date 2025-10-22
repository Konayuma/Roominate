package com.roominate.activities.owner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.roominate.R;

public class AddPropertyActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText descriptionEditText;
    private EditText addressEditText;
    private EditText cityEditText;
    private EditText provinceEditText;
    private EditText priceEditText;
    private EditText depositEditText;
    private EditText totalRoomsEditText;
    private EditText availableRoomsEditText;
    private EditText contactPersonEditText;
    private EditText contactPhoneEditText;
    private Spinner roomTypeSpinner;
    private CheckBox furnishedCheckBox;
    private CheckBox privateBathroomCheckBox;
    private CheckBox electricityIncludedCheckBox;
    private CheckBox waterIncludedCheckBox;
    private CheckBox internetIncludedCheckBox;
    private ChipGroup amenitiesChipGroup;
    private RecyclerView imagesRecyclerView;
    private ImageButton addImageButton;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_property);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        cityEditText = findViewById(R.id.cityEditText);
        provinceEditText = findViewById(R.id.provinceEditText);
        priceEditText = findViewById(R.id.priceEditText);
        depositEditText = findViewById(R.id.depositEditText);
        totalRoomsEditText = findViewById(R.id.totalRoomsEditText);
        availableRoomsEditText = findViewById(R.id.availableRoomsEditText);
        contactPersonEditText = findViewById(R.id.contactPersonEditText);
        contactPhoneEditText = findViewById(R.id.contactPhoneEditText);
        roomTypeSpinner = findViewById(R.id.roomTypeSpinner);
        furnishedCheckBox = findViewById(R.id.furnishedCheckBox);
        privateBathroomCheckBox = findViewById(R.id.privateBathroomCheckBox);
        electricityIncludedCheckBox = findViewById(R.id.electricityIncludedCheckBox);
        waterIncludedCheckBox = findViewById(R.id.waterIncludedCheckBox);
        internetIncludedCheckBox = findViewById(R.id.internetIncludedCheckBox);
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        addImageButton = findViewById(R.id.addImageButton);
        submitButton = findViewById(R.id.submitButton);
    }

    private void setupListeners() {
        addImageButton.setOnClickListener(v -> addPropertyImages());
        submitButton.setOnClickListener(v -> submitProperty());
    }

    private void addPropertyImages() {
        // TODO: Implement image picker for multiple images
    }

    private void submitProperty() {
        // TODO: Validate all fields
        // TODO: Upload images to Supabase Storage
        // TODO: Create boarding house record in Supabase
        Toast.makeText(this, "Property submission to be implemented", Toast.LENGTH_SHORT).show();
    }

    private boolean validateFields() {
        // TODO: Implement field validation
        return true;
    }
}
