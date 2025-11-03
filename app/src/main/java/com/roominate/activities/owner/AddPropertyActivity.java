package com.roominate.activities.owner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
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
import com.roominate.BuildConfig;
import com.roominate.services.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddPropertyActivity extends AppCompatActivity {

    private static final String TAG = "AddPropertyActivity";
    
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
    private MaterialButton submitButton;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    private ProgressDialog progressDialog;
    private String currentUserId;
    private String currentUserEmail;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_property);

        loadUserData();
        initializeViews();
        setupSpinners();
        setupAmenities();
        setupImagePicker();
        setupListeners();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        String userDataJson = prefs.getString("user_data", null);
        
        if (userDataJson != null) {
            try {
                JSONObject userData = new JSONObject(userDataJson);
                currentUserId = userData.optString("id", null);
                currentUserEmail = userData.optString("email", null);
                Log.d(TAG, "Loaded user: " + currentUserEmail);
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data", e);
            }
        }
        
        if (currentUserId == null) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
        }
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
        addImageButton = findViewById(R.id.addImageButton);
        submitButton = findViewById(R.id.submitButton);
        
        // Setup RecyclerView for image preview
        imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagePreviewAdapter = new ImagePreviewAdapter(this, selectedImageUris, this::removeImage);
        imagesRecyclerView.setAdapter(imagePreviewAdapter);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void setupSpinners() {
        // Room Type Spinner
        String[] roomTypes = {"Single", "Double", "Triple", "Quadruple", "Studio", "Bed Space"};
        ArrayAdapter<String> roomTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomTypeSpinner.setAdapter(roomTypeAdapter);
    }

    private void setupAmenities() {
        String[] amenities = {"WiFi", "Air Conditioning", "Parking", "Laundry", "Kitchen", 
                             "Study Area", "CCTV", "24/7 Security", "Water Heater", "Refrigerator"};
        
        for (String amenity : amenities) {
            Chip chip = new Chip(this);
            chip.setText(amenity);
            chip.setCheckable(true);
            // Use color state lists for background and text
            chip.setChipBackgroundColor(getResources().getColorStateList(R.color.chip_background_color, getTheme()));
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_color, getTheme()));
            chip.setChipStrokeColorResource(R.color.system_blue);
            chip.setChipStrokeWidth(2f);
            chip.setCheckedIconVisible(true);
            // Provide a ColorStateList to setCheckedIconTint instead of an int color to avoid type mismatch
            chip.setCheckedIconTint(getResources().getColorStateList(R.color.white, getTheme()));
            amenitiesChipGroup.addView(chip);
        }
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
                    
                    imagePreviewAdapter.notifyDataSetChanged();
                    imagesRecyclerView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
                    
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
            imagePreviewAdapter.notifyItemRemoved(position);
            imagesRecyclerView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        
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
        
        submitButton.setOnClickListener(v -> {
            if (validateFields()) {
                submitProperty();
            }
        });
    }

    private boolean validateFields() {
        boolean isValid = true;
        
        if (TextUtils.isEmpty(nameEditText.getText())) {
            nameEditText.setError("Property name is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(descriptionEditText.getText())) {
            descriptionEditText.setError("Description is required");
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
        
        if (TextUtils.isEmpty(priceEditText.getText())) {
            priceEditText.setError("Monthly rate is required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceEditText.getText().toString());
                if (price <= 0) {
                    priceEditText.setError("Price must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                priceEditText.setError("Invalid price format");
                isValid = false;
            }
        }
        
        if (TextUtils.isEmpty(totalRoomsEditText.getText())) {
            totalRoomsEditText.setError("Total rooms is required");
            isValid = false;
        } else {
            try {
                int totalRooms = Integer.parseInt(totalRoomsEditText.getText().toString());
                if (totalRooms <= 0) {
                    totalRoomsEditText.setError("Total rooms must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                totalRoomsEditText.setError("Invalid number format");
                isValid = false;
            }
        }
        
        if (TextUtils.isEmpty(availableRoomsEditText.getText())) {
            availableRoomsEditText.setError("Available rooms is required");
            isValid = false;
        } else {
            try {
                int availableRooms = Integer.parseInt(availableRoomsEditText.getText().toString());
                int totalRooms = TextUtils.isEmpty(totalRoomsEditText.getText()) ? 0 : Integer.parseInt(totalRoomsEditText.getText().toString());
                
                if (availableRooms < 0) {
                    availableRoomsEditText.setError("Available rooms cannot be negative");
                    isValid = false;
                } else if (availableRooms > totalRooms) {
                    availableRoomsEditText.setError("Available rooms cannot exceed total rooms");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                availableRoomsEditText.setError("Invalid number format");
                isValid = false;
            }
        }
        
        if (TextUtils.isEmpty(contactPersonEditText.getText())) {
            contactPersonEditText.setError("Contact person is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(contactPhoneEditText.getText())) {
            contactPhoneEditText.setError("Contact phone is required");
            isValid = false;
        }
        
        // Images are now optional - property can be created without them
        
        return isValid;
    }

    private void submitProperty() {
        progressDialog.setMessage("Uploading property...");
        progressDialog.show();
        
        new Thread(() -> {
            try {
                // Step 1: Upload images to Supabase Storage (optional)
                List<String> imageUrls = uploadImages();
                
                if (!imageUrls.isEmpty()) {
                    Log.d(TAG, "Uploaded " + imageUrls.size() + " images successfully");
                } else {
                    Log.w(TAG, "No images were uploaded, but continuing with property creation");
                }
                
                // Step 2: Create property record in database (regardless of image upload status)
                boolean success = createPropertyRecord(imageUrls);
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (success) {
                        String message = imageUrls.isEmpty() 
                            ? "Property added successfully! (without images)" 
                            : "Property added successfully!";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to add property", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error submitting property", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private List<String> uploadImages() {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            for (Uri imageUri : selectedImageUris) {
                try {
                    String fileName = "property_" + UUID.randomUUID().toString() + ".jpg";
                    String storagePath = "properties/" + currentUserId + "/" + fileName;
                    
                    // Read image data from content resolver
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream == null) {
                        Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                        continue;
                    }
                    
                    File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    inputStream.close();
                    outputStream.close();
                    
                    // Read the file into bytes for RequestBody
                    byte[] fileBytes = new byte[(int) tempFile.length()];
                    java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
                    fis.read(fileBytes);
                    fis.close();
                    
                    // Create RequestBody from bytes with proper media type
                    RequestBody requestBody = RequestBody.create(
                        fileBytes, 
                        MediaType.parse("image/jpeg")
                    );
                    
                    OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();
                    
                    String uploadUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/property-images/" + storagePath;
                    Log.d(TAG, "Uploading image to: " + uploadUrl);
                    Log.d(TAG, "File size: " + fileBytes.length + " bytes");
                    
                    Request request = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .put(requestBody)  // Use PUT instead of POST for Supabase Storage
                        .build();
                    
                    Response response = client.newCall(request).execute();
                    Log.d(TAG, "Upload response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        String publicUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + storagePath;
                        imageUrls.add(publicUrl);
                        Log.d(TAG, "Successfully uploaded image: " + publicUrl);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        Log.e(TAG, "Failed to upload image. Status: " + response.code() + " | Error: " + errorBody);
                    }
                    
                    response.close();
                    tempFile.delete();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading individual image", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uploading images", e);
        }
        
        return imageUrls;
    }

    private boolean createPropertyRecord(List<String> imageUrls) {
        try {
            // Get selected amenities
            List<String> selectedAmenities = new ArrayList<>();
            for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedAmenities.add(chip.getText().toString());
                }
            }
            
            // Build property JSON
            JSONObject property = new JSONObject();
            property.put("owner_id", currentUserId);
            property.put("name", nameEditText.getText().toString().trim());
            property.put("description", descriptionEditText.getText().toString().trim());
            property.put("address", addressEditText.getText().toString().trim());
            property.put("city", cityEditText.getText().toString().trim());
            property.put("province", provinceEditText.getText().toString().trim());
            property.put("monthly_rate", Double.parseDouble(priceEditText.getText().toString()));
            property.put("security_deposit", TextUtils.isEmpty(depositEditText.getText()) ? 0 : Double.parseDouble(depositEditText.getText().toString()));
            property.put("total_rooms", Integer.parseInt(totalRoomsEditText.getText().toString()));
            property.put("available_rooms", Integer.parseInt(availableRoomsEditText.getText().toString()));
            property.put("room_type", roomTypeSpinner.getSelectedItem().toString());
            property.put("furnished", furnishedCheckBox.isChecked());
            property.put("private_bathroom", privateBathroomCheckBox.isChecked());
            property.put("electricity_included", electricityIncludedCheckBox.isChecked());
            property.put("water_included", waterIncludedCheckBox.isChecked());
            property.put("internet_included", internetIncludedCheckBox.isChecked());
            property.put("contact_person", contactPersonEditText.getText().toString().trim());
            property.put("contact_phone", contactPhoneEditText.getText().toString().trim());
            property.put("images", new JSONArray(imageUrls));
            property.put("amenities", new JSONArray(selectedAmenities));
            property.put("status", "active");
            
            // Insert into Supabase
            RequestBody requestBody = RequestBody.create(
                property.toString(),
                MediaType.parse("application/json")
            );
            
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
            
            Request request = new Request.Builder()
                .url(BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(requestBody)
                .build();
            
            Response response = client.newCall(request).execute();
            boolean success = response.isSuccessful();
            
            if (!success) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e(TAG, "Failed to create property: " + response.code() + " - " + errorBody);
            } else {
                Log.d(TAG, "Property created successfully");
            }
            
            response.close();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating property record", e);
            return false;
        }
    }
}
