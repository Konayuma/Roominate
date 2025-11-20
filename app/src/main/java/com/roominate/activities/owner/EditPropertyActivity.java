package com.roominate.activities.owner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.roominate.R;
import com.roominate.adapters.ImagePreviewAdapter;
import com.roominate.services.SupabaseClient;
import com.roominate.models.BoardingHouse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class EditPropertyActivity extends AppCompatActivity {

    private static final String TAG = "EditPropertyActivity";

    private ImageButton backButton;
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
    private MaterialButton addImageButton;
    private MapView mapView;
    private ProgressBar progressBar;
    private TextView geocodingStatusText;
    private MaterialButton geocodeButton;
    private MaterialButton saveButton;

    private String propertyId;
    private BoardingHouse currentProperty;
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private SupabaseClient supabaseClient;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> existingImageUrls = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_property);

        // Initialize osmdroid with User-Agent
        Context ctx = getApplicationContext();
        Configuration.getInstance().setUserAgentValue("Roominate/1.0");

        propertyId = getIntent().getStringExtra("property_id");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        supabaseClient = SupabaseClient.getInstance();
        initializeViews();
        setupImagePicker();
        setupSpinners();
        setupAmenities();
        setupListeners();
        loadPropertyData();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
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
        addImageButton = findViewById(R.id.uploadImageButton);  // Changed from addImageButton
        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        geocodingStatusText = findViewById(R.id.geocodingStatusText);
        geocodeButton = findViewById(R.id.geocodeButton);
        saveButton = findViewById(R.id.saveButton);
        
        // Setup RecyclerView for image preview (if views exist in layout)
        if (imagesRecyclerView != null) {
            imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            imagePreviewAdapter = new ImagePreviewAdapter(this, selectedImageUris, this::removeImage);
            imagesRecyclerView.setAdapter(imagePreviewAdapter);
        }

        // Setup map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        progressBar.setVisibility(View.GONE);
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    
                    if (data.getClipData() != null) {
                        // Multiple images selected
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count && selectedImageUris.size() < 5; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            selectedImageUris.add(imageUri);
                        }
                    } else if (data.getData() != null) {
                        // Single image selected
                        Uri imageUri = data.getData();
                        if (selectedImageUris.size() < 5) {
                            selectedImageUris.add(imageUri);
                        }
                    }
                    
                    if (imagePreviewAdapter != null) {
                        imagePreviewAdapter.notifyDataSetChanged();
                    }
                    if (imagesRecyclerView != null) {
                        imagesRecyclerView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    
                    if (selectedImageUris.size() >= 5) {
                        Toast.makeText(this, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    private void removeImage(int position) {
        if (position >= 0 && position < selectedImageUris.size()) {
            selectedImageUris.remove(position);
            if (imagePreviewAdapter != null) {
                imagePreviewAdapter.notifyItemRemoved(position);
            }
            if (imagesRecyclerView != null) {
                imagesRecyclerView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void setupSpinners() {
        String[] roomTypes = {"Single", "Double", "Triple", "Quadruple", "Studio", "Bed Space"};
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        roomTypeSpinner.setAdapter(roomTypeAdapter);
    }

    private void setupAmenities() {
        String[] amenities = {"WiFi", "Air Conditioning", "Parking", "Laundry", "Kitchen",
                "Study Area", "CCTV", "24/7 Security", "Water Heater", "Refrigerator"};

        for (String amenity : amenities) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setChipStrokeColorResource(R.color.black);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(getResources().getColor(R.color.black));
            chip.setCheckedIconVisible(true);
            amenitiesChipGroup.addView(chip);
        }
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        geocodeButton.setOnClickListener(v -> geocodeAddressFromForm());
        
        if (addImageButton != null) {
            addImageButton.setOnClickListener(v -> {
                if (selectedImageUris.size() >= 5) {
                    Toast.makeText(this, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                imagePickerLauncher.launch(Intent.createChooser(intent, "Select Property Images"));
            });
        }

        saveButton.setOnClickListener(v -> {
            if (validateFields()) {
                savePropertyChanges();
            }
        });

        // Auto-geocode when address field loses focus
        addressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !TextUtils.isEmpty(addressEditText.getText())) {
                geocodeAddressFromForm();
            }
        });
    }

    private void loadPropertyData() {
        progressBar.setVisibility(View.VISIBLE);
        supabaseClient.getPropertyById(propertyId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        // Extract the data array from the wrapper
                        JSONArray dataArray = response.optJSONArray("data");
                        if (dataArray == null || dataArray.length() == 0) {
                            Log.e(TAG, "Empty or null data array from getPropertyById");
                            Toast.makeText(EditPropertyActivity.this, "Property not found", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Get the first (and only) property object
                        JSONObject propertyJson = dataArray.getJSONObject(0);
                        currentProperty = parsePropertyFromJson(propertyJson);
                        currentLatitude = propertyJson.optDouble("latitude", 0);
                        currentLongitude = propertyJson.optDouble("longitude", 0);

                        // Populate form fields
                        nameEditText.setText(currentProperty.getName());
                        descriptionEditText.setText(currentProperty.getDescription());
                        addressEditText.setText(currentProperty.getAddress());
                        cityEditText.setText(currentProperty.getCity());
                        provinceEditText.setText(currentProperty.getProvince());
                        priceEditText.setText(String.valueOf(currentProperty.getPricePerMonth()));
                        depositEditText.setText(String.valueOf(currentProperty.getSecurityDeposit()));
                        totalRoomsEditText.setText(String.valueOf(currentProperty.getTotalRooms()));
                        availableRoomsEditText.setText(String.valueOf(currentProperty.getAvailableRooms()));
                        contactPersonEditText.setText(currentProperty.getContactPerson());
                        contactPhoneEditText.setText(currentProperty.getContactPhone());

                        // Set room type spinner
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) roomTypeSpinner.getAdapter();
                        int position = adapter.getPosition(currentProperty.getRoomType());
                        roomTypeSpinner.setSelection(position >= 0 ? position : 0);

                        // Set checkboxes
                        furnishedCheckBox.setChecked(currentProperty.isFurnished());
                        privateBathroomCheckBox.setChecked(currentProperty.isHasPrivateBathroom());
                        electricityIncludedCheckBox.setChecked(currentProperty.isElectricityIncluded());
                        waterIncludedCheckBox.setChecked(currentProperty.isWaterIncluded());
                        internetIncludedCheckBox.setChecked(currentProperty.isInternetIncluded());

                        // Set amenities
                        if (!currentProperty.getAmenities().isEmpty()) {
                            for (String amenity : currentProperty.getAmenities()) {
                                for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
                                    Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
                                    if (chip.getText().toString().equals(amenity)) {
                                        chip.setChecked(true);
                                    }
                                }
                            }
                        }

                        // Update map with coordinates
                        updateMapMarker();
                        updateGeocodingStatus();
                        
                        // Load existing images
                        loadExistingImages();

                        progressBar.setVisibility(View.GONE);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing property data", e);
                        Toast.makeText(EditPropertyActivity.this, "Error loading property", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error loading property: " + error);
                    Toast.makeText(EditPropertyActivity.this, "Failed to load property: " + error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private BoardingHouse parsePropertyFromJson(JSONObject json) throws JSONException {
        BoardingHouse property = new BoardingHouse();
        property.setId(json.optString("id"));
        property.setName(json.optString("name"));
        property.setDescription(json.optString("description"));
        property.setAddress(json.optString("address"));
        property.setCity(json.optString("city"));
        property.setProvince(json.optString("province"));
        property.setLatitude(json.optDouble("latitude", 0));
        property.setLongitude(json.optDouble("longitude", 0));
        property.setPricePerMonth(json.optDouble("price_per_month", 0));
        property.setSecurityDeposit(json.optDouble("security_deposit", 0));
        property.setTotalRooms(json.optInt("total_rooms", 0));
        property.setAvailableRooms(json.optInt("available_rooms", 0));
        property.setRoomType(json.optString("room_type"));
        property.setFurnished(json.optBoolean("furnished", false));
        property.setHasPrivateBathroom(json.optBoolean("private_bathroom", false));
        property.setElectricityIncluded(json.optBoolean("electricity_included", false));
        property.setWaterIncluded(json.optBoolean("water_included", false));
        property.setInternetIncluded(json.optBoolean("internet_included", false));
        property.setContactPerson(json.optString("contact_person"));
        property.setContactPhone(json.optString("contact_phone"));

        JSONArray amenitiesArray = json.optJSONArray("amenities");
        if (amenitiesArray != null) {
            for (int i = 0; i < amenitiesArray.length(); i++) {
                property.addAmenity(amenitiesArray.getString(i));
            }
        }

        return property;
    }

    private void geocodeAddressFromForm() {
        String address = addressEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String province = provinceEditText.getText().toString().trim();

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = address + ", " + city + ", " + province + ", Zambia";

        progressBar.setVisibility(View.VISIBLE);
        geocodingStatusText.setText("Geocoding address...");

        supabaseClient.geocodeAddress(fullAddress, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        currentLatitude = response.getDouble("latitude");
                        currentLongitude = response.getDouble("longitude");

                        updateMapMarker();
                        updateGeocodingStatus();

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditPropertyActivity.this,
                                "Address geocoded successfully!", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing geocoding response", e);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditPropertyActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Geocoding error: " + error);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditPropertyActivity.this, "Geocoding failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateMapMarker() {
        if (currentLatitude == 0 && currentLongitude == 0) {
            return;
        }

        mapView.getOverlays().clear();

        GeoPoint location = new GeoPoint(currentLatitude, currentLongitude);
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(nameEditText.getText().toString());
        mapView.getOverlays().add(marker);

        IMapController controller = mapView.getController();
        controller.setZoom(16.0);
        controller.setCenter(location);

        mapView.invalidate();
    }

    private void updateGeocodingStatus() {
        if (currentLatitude != 0 && currentLongitude != 0) {
            geocodingStatusText.setText(String.format("📍 Coordinates: %.4f, %.4f", currentLatitude, currentLongitude));
            geocodingStatusText.setTextColor(getColor(android.R.color.holo_green_light));
        } else {
            geocodingStatusText.setText("❌ No coordinates found");
            geocodingStatusText.setTextColor(getColor(android.R.color.holo_red_light));
        }
    }

    private boolean validateFields() {
        boolean isValid = true;

        if (TextUtils.isEmpty(nameEditText.getText())) {
            nameEditText.setError("Property name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(addressEditText.getText())) {
            addressEditText.setError("Address is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(cityEditText.getText())) {
            cityEditText.setError("City is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(provinceEditText.getText())) {
            provinceEditText.setError("Province is required");
            isValid = false;
        }

        if (currentLatitude == 0 || currentLongitude == 0) {
            Toast.makeText(this, "Please geocode the address first", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void savePropertyChanges() {
        progressBar.setVisibility(View.VISIBLE);

        List<String> selectedAmenities = new ArrayList<>();
        for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedAmenities.add(chip.getText().toString());
            }
        }

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("name", nameEditText.getText().toString().trim());
            updateData.put("description", descriptionEditText.getText().toString().trim());
            updateData.put("address", addressEditText.getText().toString().trim());
            updateData.put("city", cityEditText.getText().toString().trim());
            updateData.put("province", provinceEditText.getText().toString().trim());
            updateData.put("latitude", currentLatitude);
            updateData.put("longitude", currentLongitude);
            
            // Parse and log price before sending
            String priceText = priceEditText.getText().toString().trim();
            double price = Double.parseDouble(priceText);
            Log.d(TAG, "Updating price_per_month to: " + price);
            updateData.put("price_per_month", price);
            
            updateData.put("security_deposit", TextUtils.isEmpty(depositEditText.getText()) ? 0 : Double.parseDouble(depositEditText.getText().toString()));
            updateData.put("total_rooms", Integer.parseInt(totalRoomsEditText.getText().toString()));
            updateData.put("available_rooms", Integer.parseInt(availableRoomsEditText.getText().toString()));
            updateData.put("room_type", roomTypeSpinner.getSelectedItem().toString());
            updateData.put("furnished", furnishedCheckBox.isChecked());
            updateData.put("private_bathroom", privateBathroomCheckBox.isChecked());
            updateData.put("electricity_included", electricityIncludedCheckBox.isChecked());
            updateData.put("water_included", waterIncludedCheckBox.isChecked());
            updateData.put("internet_included", internetIncludedCheckBox.isChecked());
            updateData.put("contact_person", contactPersonEditText.getText().toString().trim());
            updateData.put("contact_phone", contactPhoneEditText.getText().toString().trim());
            updateData.put("amenities", new JSONArray(selectedAmenities));

            // Update all property fields in database
            supabaseClient.updateProperty(propertyId, updateData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // After updating property, upload new images if any
                    if (!selectedImageUris.isEmpty()) {
                        uploadNewImages();
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(EditPropertyActivity.this, "Property updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error updating property: " + error);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditPropertyActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error building update data", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadExistingImages() {
        if (propertyId == null) return;
        
        supabaseClient.getPropertyImages(propertyId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray imagesArray = response.optJSONArray("data");
                    if (imagesArray != null && imagesArray.length() > 0) {
                        existingImageUrls.clear();
                        for (int i = 0; i < imagesArray.length(); i++) {
                            JSONObject imageObj = imagesArray.getJSONObject(i);
                            String imageUrl = imageObj.optString("image_url");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                existingImageUrls.add(imageUrl);
                            }
                        }
                        runOnUiThread(() -> {
                            Log.d(TAG, "Loaded " + existingImageUrls.size() + " existing images");
                            // Note: ImagePreviewAdapter currently only handles Uri objects
                            // You may want to enhance it to also display URLs from network
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing existing images", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading existing images: " + error);
            }
        });
    }
    
    private void uploadNewImages() {
        if (selectedImageUris.isEmpty()) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditPropertyActivity.this, "Property updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }
        
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(EditPropertyActivity.this, "Uploading " + selectedImageUris.size() + " image(s)...", Toast.LENGTH_SHORT).show();
        });
        
        // Upload images one by one
        uploadImageAtIndex(0);
    }
    
    private void uploadImageAtIndex(int index) {
        if (index >= selectedImageUris.size()) {
            // All images uploaded successfully
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditPropertyActivity.this, "Property and images updated successfully!", Toast.LENGTH_LONG).show();
                finish();
            });
            return;
        }
        
        Uri imageUri = selectedImageUris.get(index);
        boolean isPrimary = (index == 0 && existingImageUrls.isEmpty()); // First image is primary only if no existing images
        
        runOnUiThread(() -> {
            // Update progress
            int progress = (int) (((float) index / selectedImageUris.size()) * 100);
            progressBar.setProgress(progress);
        });
        
        supabaseClient.uploadPropertyImage(propertyId, imageUri, isPrimary, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Image " + (index + 1) + " uploaded successfully");
                runOnUiThread(() -> {
                    Toast.makeText(EditPropertyActivity.this, 
                        "Uploaded image " + (index + 1) + " of " + selectedImageUris.size(), 
                        Toast.LENGTH_SHORT).show();
                });
                // Upload next image
                uploadImageAtIndex(index + 1);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error uploading image " + (index + 1) + ": " + error);
                runOnUiThread(() -> {
                    Toast.makeText(EditPropertyActivity.this, 
                        "Warning: Failed to upload image " + (index + 1) + " - " + error, 
                        Toast.LENGTH_LONG).show();
                });
                // Continue with next image even on error
                uploadImageAtIndex(index + 1);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
