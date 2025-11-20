package com.roominate.activities.tenant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.roominate.BuildConfig;
import com.roominate.R;
import com.roominate.adapters.ReviewsAdapter;
import com.roominate.models.BoardingHouse;
import com.roominate.services.PaymentService;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import okhttp3.*;
import java.io.IOException;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;

public class BoardingHouseDetailsActivity extends AppCompatActivity {
    private static final String TAG = "BoardingHouseDetails";
    private static final int REQUEST_CODE_PAYMENT = 1001;

    private ViewPager2 imagesViewPager;
    private LinearLayout dotsIndicator;
    private TextView imageCounter;
    private MaterialToolbar toolbar;
    private TextView nameTextView;
    private TextView addressTextView;
    private TextView priceTextView;
    private TextView descriptionTextView;
    private RatingBar ratingBar;
    private TextView ratingCountTextView;
    private TextView availableRoomsTextView;
    private ChipGroup amenitiesChipGroup;
    private RecyclerView reviewsRecyclerView;
    private MaterialButton writeReviewButton;
    private Button contactOwnerButton;
    private Button bookNowButton;
    private FloatingActionButton favoriteButton;
    private FloatingActionButton shareButton;

    private BoardingHouse boardingHouse;
    private String boardingHouseId;
    private String userId;
    private boolean isFavorite = false;
    private ReviewsAdapter reviewsAdapter;
    private JSONArray reviewsData;
    private String favoriteId = null;
    private OkHttpClient httpClient;

    private AlertDialog paymentStatusDialog;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL_MS = 5000; // 5 seconds
    private static final int POLLING_TIMEOUT_MS = 120000; // 2 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_house_details);

        // Get boarding house ID from intent
        boardingHouseId = getIntent().getStringExtra("boarding_house_id");
        
        // Initialize HTTP client
        httpClient = new OkHttpClient();
        
        // Load user ID
        loadUserId();

        initializeViews();
        setupToolbar();
        setupReviewsRecyclerView();
        loadBoardingHouseDetails();
        checkFavoriteStatus();
        loadReviews();
        setupListeners();
    }
    
    private void loadUserId() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        // Also try roominate_prefs
        if (userId == null || userId.isEmpty()) {
            prefs = getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE);
            userId = prefs.getString("user_id", null);
        }
    }

    private void initializeViews() {
        imagesViewPager = findViewById(R.id.imagesViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        imageCounter = findViewById(R.id.imageCounter);
        toolbar = findViewById(R.id.toolbar);
        nameTextView = findViewById(R.id.nameTextView);
        addressTextView = findViewById(R.id.addressTextView);
        priceTextView = findViewById(R.id.priceTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        ratingBar = findViewById(R.id.ratingBar);
        ratingCountTextView = findViewById(R.id.ratingCountTextView);
        availableRoomsTextView = findViewById(R.id.availableRoomsTextView);
        amenitiesChipGroup = findViewById(R.id.amenitiesChipGroup);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        writeReviewButton = findViewById(R.id.writeReviewButton);
        contactOwnerButton = findViewById(R.id.contactOwnerButton);
        bookNowButton = findViewById(R.id.bookNowButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        shareButton = findViewById(R.id.shareButton);
    }
    
    private void setupReviewsRecyclerView() {
        reviewsData = new JSONArray();
        reviewsAdapter = new ReviewsAdapter(this, reviewsData);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewsAdapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadBoardingHouseDetails() {
        if (boardingHouseId == null || boardingHouseId.isEmpty()) {
            Toast.makeText(this, "Property ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        new Thread(() -> {
            try {
                String supabaseUrl = com.roominate.BuildConfig.SUPABASE_URL;
                String supabaseKey = com.roominate.BuildConfig.SUPABASE_ANON_KEY;
                
                SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                String accessToken = prefs.getString("access_token", null);
                
                String url = supabaseUrl + "/rest/v1/boarding_houses?id=eq." + boardingHouseId + "&select=*";
                Log.d(TAG, "Loading property details from: " + url);
                
                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .get();

                // Centralized header wiring: prefer user's access token when available
                requestBuilder = com.roominate.services.SupabaseClient.addAuthHeaders(requestBuilder);

                Request request = requestBuilder.build();
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Property response: " + responseBody);
                    
                    JSONArray jsonArray = new JSONArray(responseBody);
                    
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        
                        // Create BoardingHouse object from JSON
                        boardingHouse = new BoardingHouse();
                        boardingHouse.setId(jsonObject.optString("id"));
                        boardingHouse.setOwnerId(jsonObject.optString("owner_id"));
                        boardingHouse.setName(jsonObject.optString("name"));
                        boardingHouse.setDescription(jsonObject.optString("description"));
                        boardingHouse.setAddress(jsonObject.optString("address"));
                        boardingHouse.setCity(jsonObject.optString("city"));
                        boardingHouse.setProvince(jsonObject.optString("province"));
                        boardingHouse.setPricePerMonth(jsonObject.optDouble("price_per_month", jsonObject.optDouble("monthly_rate", 0.0)));
                        boardingHouse.setSecurityDeposit(jsonObject.optDouble("security_deposit", 0.0));
                        boardingHouse.setAvailableRooms(jsonObject.optInt("available_rooms", 0));
                        boardingHouse.setTotalRooms(jsonObject.optInt("total_rooms", 0));
                        boardingHouse.setContactPerson(jsonObject.optString("contact_person"));
                        boardingHouse.setContactPhone(jsonObject.optString("contact_phone"));
                        
                        // Parse images
                        if (jsonObject.has("images") && !jsonObject.isNull("images")) {
                            JSONArray imagesArray = jsonObject.optJSONArray("images");
                            if (imagesArray != null) {
                                java.util.List<String> imageUrls = new java.util.ArrayList<>();
                                for (int i = 0; i < imagesArray.length(); i++) {
                                    imageUrls.add(imagesArray.getString(i));
                                }
                                boardingHouse.setImageUrls(imageUrls);
                            }
                        }
                        
                        // Parse amenities
                        if (jsonObject.has("amenities") && !jsonObject.isNull("amenities")) {
                            JSONArray amenitiesArray = jsonObject.optJSONArray("amenities");
                            if (amenitiesArray != null) {
                                java.util.List<String> amenities = new java.util.ArrayList<>();
                                for (int i = 0; i < amenitiesArray.length(); i++) {
                                    amenities.add(amenitiesArray.getString(i));
                                }
                                boardingHouse.setAmenities(amenities);
                            }
                        }
                        
                        new Handler(Looper.getMainLooper()).post(this::displayBoardingHouseDetails);
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Failed to load property: " + response.code() + " - " + errorBody);
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, "Failed to load property details", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading property details", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void displayBoardingHouseDetails() {
        if (boardingHouse == null) return;

        nameTextView.setText(boardingHouse.getName());
        addressTextView.setText(boardingHouse.getAddress());
        priceTextView.setText(String.format("K%.2f/month", boardingHouse.getPricePerMonth()));
        descriptionTextView.setText(boardingHouse.getDescription());
        ratingBar.setRating((float) boardingHouse.getAverageRating());
        ratingCountTextView.setText(String.format("(%d reviews)", boardingHouse.getTotalReviews()));
        availableRoomsTextView.setText(String.format("%d rooms available", boardingHouse.getAvailableRooms()));

        // Set up image slider, amenities chips, and reviews list
        setupImageSlider();
        setupAmenitiesChips();
    }

    private void setupImageSlider() {
        if (boardingHouse == null || boardingHouse.getImageUrls() == null || boardingHouse.getImageUrls().isEmpty()) {
            // If no images, show placeholder in ViewPager
            Log.d(TAG, "No images available, showing placeholder");
            java.util.List<String> placeholderList = new java.util.ArrayList<>();
            placeholderList.add("placeholder"); // Will trigger placeholder loading
            imagesViewPager.setAdapter(new ImageSliderAdapter(placeholderList));
            imageCounter.setText("1/1");
            setupDotIndicators(1);
            return;
        }
        
        Log.d(TAG, "Setting up image slider with " + boardingHouse.getImageUrls().size() + " images");
        for (String url : boardingHouse.getImageUrls()) {
            Log.d(TAG, "Image URL: " + url);
        }
        
        final int imageCount = boardingHouse.getImageUrls().size();
        imagesViewPager.setAdapter(new ImageSliderAdapter(boardingHouse.getImageUrls()));
        
        // Setup dot indicators
        setupDotIndicators(imageCount);
        
        // Update counter
        imageCounter.setText("1/" + imageCount);
        
        // Listen for page changes
        imagesViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                imageCounter.setText((position + 1) + "/" + imageCount);
                updateDotIndicators(position);
            }
        });
    }
    
    private void setupDotIndicators(int count) {
        dotsIndicator.removeAllViews();
        
        if (count <= 1) {
            dotsIndicator.setVisibility(View.GONE);
            return;
        }
        
        dotsIndicator.setVisibility(View.VISIBLE);
        
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (8 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(
                (int) (4 * getResources().getDisplayMetrics().density),
                0,
                (int) (4 * getResources().getDisplayMetrics().density),
                0
            );
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            dotsIndicator.addView(dot);
        }
    }
    
    private void updateDotIndicators(int position) {
        for (int i = 0; i < dotsIndicator.getChildCount(); i++) {
            View dot = dotsIndicator.getChildAt(i);
            dot.setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
            
            // Animate the active dot
            if (i == position) {
                dot.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start();
            } else {
                dot.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
            }
        }
    }
    
    /**
     * Fix malformed image URLs by properly encoding the storage path.
     * Extracts the path from a full URL and re-encodes it correctly.
     */
    private String fixImageUrl(String url) {
        if (url == null || url.isEmpty() || url.equals("placeholder")) {
            return url;
        }
        
        try {
            // Check if this is a full Supabase Storage URL
            if (url.contains("/storage/v1/object/public/property-images/")) {
                // Extract the storage path after "property-images/"
                int pathStartIndex = url.indexOf("/storage/v1/object/public/property-images/") + 
                                   "/storage/v1/object/public/property-images/".length();
                String storagePath = url.substring(pathStartIndex);
                
                // Re-encode the path properly
                String[] segments = storagePath.split("/");
                StringBuilder encodedPath = new StringBuilder();
                for (int i = 0; i < segments.length; i++) {
                    if (i > 0) encodedPath.append("/");
                    encodedPath.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8.toString())
                        .replace("+", "%20"));
                }
                
                // Reconstruct the full URL with properly encoded path
                String fixedUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + encodedPath.toString();
                Log.d(TAG, "Fixed image URL: " + fixedUrl);
                return fixedUrl;
            }
            
            // If not a recognized format, return as-is
            return url;
        } catch (Exception e) {
            Log.e(TAG, "Error fixing image URL: " + url, e);
            return url;
        }
    }

    private void setupAmenitiesChips() {
        if (boardingHouse == null || boardingHouse.getAmenities() == null) {
            return;
        }
        
        amenitiesChipGroup.removeAllViews();
        for (String amenity : boardingHouse.getAmenities()) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(amenity);
            chip.setEnabled(false);
            amenitiesChipGroup.addView(chip);
        }
    }

    /**
     * Inner class for handling image slider in ViewPager2
     */
    private class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
        private java.util.List<String> imageUrls;

        public ImageSliderAdapter(java.util.List<String> imageUrls) {
            this.imageUrls = imageUrls != null ? imageUrls : new java.util.ArrayList<>();
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(BoardingHouseDetailsActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            Log.d(TAG, "Loading image at position " + position + ": " + imageUrl);
            
            if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("placeholder")) {
                // Show placeholder
                holder.imageView.setImageResource(R.drawable.ic_house_placeholder);
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                // Fix the image URL by properly encoding the storage path
                String fixedUrl = fixImageUrl(imageUrl);
                Log.d(TAG, "Using fixed URL: " + fixedUrl);
                
                Picasso.get()
                    .load(fixedUrl)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.ic_house_placeholder)
                    .error(R.drawable.ic_house_placeholder)
                    .into(holder.imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded successfully at position " + position);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to load image at position " + position, e);
                        }
                    });
            }
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        public class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ImageViewHolder(ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }

    private void setupListeners() {
        contactOwnerButton.setOnClickListener(v -> contactOwner());
        bookNowButton.setOnClickListener(v -> bookBoardingHouse());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> shareBoardingHouse());
        writeReviewButton.setOnClickListener(v -> showReviewDialog());
    }
    
    private void loadReviews() {
        if (boardingHouseId == null) {
            Log.e(TAG, "Cannot load reviews: boardingHouseId is null");
            return;
        }
        
        SupabaseClient.getInstance().getReviews(boardingHouseId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray arr = response.optJSONArray("reviews");
                    if (arr != null) {
                        reviewsData = arr;
                        reviewsAdapter.updateReviews(reviewsData);
                        calculateAverageRating(reviewsData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing reviews response", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading reviews: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(BoardingHouseDetailsActivity.this, 
                        "Failed to load reviews", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void calculateAverageRating(JSONArray reviews) {
        if (reviews == null || reviews.length() == 0) {
            ratingBar.setRating(0);
            ratingCountTextView.setText("No reviews yet");
            return;
        }
        
        float totalRating = 0;
        for (int i = 0; i < reviews.length(); i++) {
            try {
                JSONObject review = reviews.getJSONObject(i);
                totalRating += review.getInt("rating");
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing review rating", e);
            }
        }
        
        float averageRating = totalRating / reviews.length();
        ratingBar.setRating(averageRating);
        ratingCountTextView.setText(String.format(Locale.getDefault(), 
            "%.1f (%d reviews)", averageRating, reviews.length()));
    }
    
    private void showReviewDialog() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please sign in to write a review", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Inflate dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_submit_review, null);
        RatingBar dialogRatingBar = dialogView.findViewById(R.id.ratingBar);
        EditText commentEditText = dialogView.findViewById(R.id.commentEditText);
        
        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Submit", (dialog, which) -> {
                float rating = dialogRatingBar.getRating();
                String comment = commentEditText.getText().toString().trim();
                
                if (rating == 0) {
                    Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (comment.isEmpty()) {
                    Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                submitReview((int) rating, comment);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void submitReview(int rating, String comment) {
        if (boardingHouseId == null) {
            Toast.makeText(this, "Error: Property ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "submitReview called with boardingHouseId: " + boardingHouseId + ", rating: " + rating + ", comment: " + comment);
        
        SupabaseClient.getInstance().submitReview(boardingHouseId, rating, comment, 
            new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        Toast.makeText(BoardingHouseDetailsActivity.this, 
                            "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                        loadReviews(); // Reload reviews to show new review
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(BoardingHouseDetailsActivity.this, 
                            "Failed to submit review: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void contactOwner() {
        // TODO: Open contact dialog or messaging activity
    }

    private void bookBoardingHouse() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please sign in to book", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (boardingHouse == null) {
            Toast.makeText(this, "Property information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showBookingDialog();
    }
    
    private void showBookingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking_details, null);
        TextInputEditText moveInDateEdit = dialogView.findViewById(R.id.moveInDateEditText);
        TextInputEditText durationEdit = dialogView.findViewById(R.id.durationEditText);
        TextView totalAmountText = dialogView.findViewById(R.id.totalAmountTextView);
        
        // Set up date picker for move-in date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        moveInDateEdit.setText(dateFormat.format(calendar.getTime()));
        
        moveInDateEdit.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    moveInDateEdit.setText(dateFormat.format(calendar.getTime()));
                    updateTotalAmount(totalAmountText, durationEdit.getText().toString());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });
        
        // Update total when duration changes
        durationEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalAmount(totalAmountText, s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setView(dialogView)
            .setPositiveButton("Confirm & Pay", null)
            .setNegativeButton("Cancel", null)
            .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmButton.setOnClickListener(v -> {
                String moveInDate = moveInDateEdit.getText().toString();
                String durationStr = durationEdit.getText().toString();
                
                if (moveInDate.isEmpty() || durationStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    int duration = Integer.parseInt(durationStr);
                    if (duration < 1) {
                        Toast.makeText(this, "Duration must be at least 1 month", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    double totalAmount = (boardingHouse.getPricePerMonth() + boardingHouse.getSecurityDeposit()) * duration;
                    dialog.dismiss();
                    createBookingAndInitiatePayment(moveInDate, duration, totalAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        dialog.show();
    }
    
    private void updateTotalAmount(TextView totalAmountText, String durationStr) {
        if (boardingHouse == null || durationStr.isEmpty()) {
            totalAmountText.setText("Total: ZMW 0.00");
            return;
        }
        
        try {
            int duration = Integer.parseInt(durationStr);
            double total = (boardingHouse.getPricePerMonth() + boardingHouse.getSecurityDeposit()) * duration;
            totalAmountText.setText(String.format(Locale.US, "Total: ZMW %.2f", total));
        } catch (NumberFormatException e) {
            totalAmountText.setText("Total: ZMW 0.00");
        }
    }
    
    private void createBookingAndInitiatePayment(String moveInDate, int duration, double totalAmount) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your booking...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get user details from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE);
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        String firstName = prefs.getString("first_name", "");
        String lastName = prefs.getString("last_name", "");

        if (phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "User details not found. Please update your profile.", Toast.LENGTH_LONG).show();
            progressDialog.dismiss();
            return;
        }

        try {
            // 1. Create the booking with a 'pending' status first
            JSONObject bookingData = new JSONObject();
            bookingData.put("listing_id", boardingHouseId);
            bookingData.put("start_date", moveInDate);
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            calendar.setTime(dateFormat.parse(moveInDate));
            calendar.add(Calendar.MONTH, duration);
            String endDate = dateFormat.format(calendar.getTime());
            bookingData.put("end_date", endDate);
            bookingData.put("total_amount", totalAmount);
            bookingData.put("status", "pending"); // Start as pending
            bookingData.put("payment_status", "pending");

            SupabaseClient.getInstance().createBooking(bookingData, new SupabaseClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        JSONObject booking = response.getJSONObject("booking");
                        String bookingId = booking.getString("id");
                        Log.d(TAG, "Booking created with pending status. ID: " + bookingId);

                        // 2. Initiate payment via Lenco
                        initiateLencoPayment(progressDialog, bookingId, totalAmount, email, phone, firstName, lastName, moveInDate, endDate, duration);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing booking response", e);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(BoardingHouseDetailsActivity.this, "Failed to create booking.", Toast.LENGTH_LONG).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Booking creation failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing booking data", e);
            progressDialog.dismiss();
            Toast.makeText(this, "Error creating booking data", Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateLencoPayment(ProgressDialog progressDialog, String bookingId, double totalAmount, String email, String phone, String firstName, String lastName, String moveInDate, String endDate, int duration) {
        progressDialog.setMessage("Initiating payment...");

        PaymentService.getInstance().initiatePayment(bookingId, totalAmount, "ZMW", email, phone, firstName, lastName, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    try {
                        String reference = response.getString("reference");
                        boolean requiresAuth = response.optBoolean("requires_authorization", false);
                        String message = response.optString("message", "Payment initiated");
                        String paymentStatus = response.optString("payment_status", "unknown");
                        
                        Log.d(TAG, "Lenco payment initiated. Reference: " + reference + ", Status: " + paymentStatus + ", Requires Auth: " + requiresAuth);
                        
                        showPaymentStatusDialog(reference, moveInDate, endDate, duration, totalAmount, requiresAuth, message);
                        startPollingPaymentStatus(reference, bookingId, moveInDate, endDate, duration, totalAmount);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing payment initiation response", e);
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Payment initiation failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Payment initiation failed: " + error);
                    Toast.makeText(BoardingHouseDetailsActivity.this, "Failed to initiate payment: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showPaymentStatusDialog(String reference, String moveInDate, String endDate, int duration, double totalAmount, boolean requiresAuth, String message) {
        if (paymentStatusDialog != null && paymentStatusDialog.isShowing()) {
            paymentStatusDialog.dismiss();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_status, null);
        TextView statusMessage = dialogView.findViewById(R.id.tv_status_message);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel_payment);

        // Update message based on authorization requirement
        if (requiresAuth) {
            statusMessage.setText("Please check your phone and authorize the mobile money payment. You will receive a prompt from your mobile network operator.");
        } else {
            statusMessage.setText(message != null ? message : "Processing your payment. Please wait...");
        }

        paymentStatusDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        cancelButton.setOnClickListener(v -> {
            stopPolling();
            paymentStatusDialog.dismiss();
            Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show();
        });

        paymentStatusDialog.show();
    }

    private void startPollingPaymentStatus(String reference, String bookingId, String moveInDate, String endDate, int duration, double totalAmount) {
        stopPolling(); // Ensure no other polling is running

        pollingHandler = new Handler(Looper.getMainLooper());
        final long startTime = System.currentTimeMillis();

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - startTime > POLLING_TIMEOUT_MS) {
                    Log.w(TAG, "Polling timed out for reference: " + reference);
                    stopPolling();
                    runOnUiThread(() -> {
                        if (paymentStatusDialog != null && paymentStatusDialog.isShowing()) {
                            paymentStatusDialog.dismiss();
                        }
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Payment timed out. Please try again.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Log.d(TAG, "Polling for payment status for reference: " + reference);
                checkPaymentStatus(reference, bookingId, moveInDate, endDate, duration, totalAmount);
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };

        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingHandler = null;
            pollingRunnable = null;
        }
    }

    private void checkPaymentStatus(String reference, String bookingId, String moveInDate, String endDate, int duration, double totalAmount) {
        PaymentService.getInstance().getPaymentStatus(reference, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String status = response.optString("status", "unknown");
                    Log.d(TAG, "Payment status for " + reference + " is " + status);

                    if ("successful".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                        stopPolling();
                        runOnUiThread(() -> {
                            if (paymentStatusDialog != null && paymentStatusDialog.isShowing()) {
                                paymentStatusDialog.dismiss();
                            }
                            // The booking status is updated by a webhook, but we can show the receipt now.
                            SharedPreferences prefs = getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE);
                            String phone = prefs.getString("phone", "");
                            showBookingReceiptDialog(bookingId, moveInDate, endDate, duration, totalAmount, "Mobile Money", phone);
                        });
                    } else if ("failed".equalsIgnoreCase(status)) {
                        stopPolling();
                        runOnUiThread(() -> {
                            if (paymentStatusDialog != null && paymentStatusDialog.isShowing()) {
                                paymentStatusDialog.dismiss();
                            }
                            Toast.makeText(BoardingHouseDetailsActivity.this, "Payment failed. Please try again.", Toast.LENGTH_LONG).show();
                        });
                    }
                    // If status is still 'pending', the poller will just run again.

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing payment status response", e);
                    // Continue polling
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking payment status: " + error);
                // Continue polling, maybe a transient network error
            }
        });
    }

    
    /**
     * Show booking receipt dialog after successful booking
     */
    private void showBookingReceiptDialog(String bookingId, String moveInDate, String endDate, int duration, double totalAmount, String provider, String phoneNumber) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking_receipt, null);
        
        TextView bookingIdText = dialogView.findViewById(R.id.bookingIdText);
        TextView receiptPropertyName = dialogView.findViewById(R.id.receiptPropertyName);
        TextView receiptPropertyAddress = dialogView.findViewById(R.id.receiptPropertyAddress);
        TextView receiptMoveInDate = dialogView.findViewById(R.id.receiptMoveInDate);
        TextView receiptDuration = dialogView.findViewById(R.id.receiptDuration);
        TextView receiptMoveOutDate = dialogView.findViewById(R.id.receiptMoveOutDate);
        TextView receiptMonthlyRate = dialogView.findViewById(R.id.receiptMonthlyRate);
        TextView receiptSecurityDeposit = dialogView.findViewById(R.id.receiptSecurityDeposit);
        TextView receiptTotalAmount = dialogView.findViewById(R.id.receiptTotalAmount);
        TextView receiptContactPerson = dialogView.findViewById(R.id.receiptContactPerson);
        TextView receiptContactPhone = dialogView.findViewById(R.id.receiptContactPhone);
        ImageView receiptPropertyImage = dialogView.findViewById(R.id.receiptPropertyImage);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        
        // Set booking details
        bookingIdText.setText("Booking ID: #" + bookingId.substring(0, Math.min(8, bookingId.length())));
        
        if (boardingHouse != null) {
            receiptPropertyName.setText(boardingHouse.getName());
            receiptPropertyAddress.setText(boardingHouse.getAddress());
            receiptMonthlyRate.setText(String.format(Locale.US, "K%.2f", boardingHouse.getPricePerMonth()));
            receiptSecurityDeposit.setText(String.format(Locale.US, "K%.2f", boardingHouse.getSecurityDeposit()));
            receiptContactPerson.setText(boardingHouse.getContactPerson());
            receiptContactPhone.setText(boardingHouse.getContactPhone());
            
            // Load property image
            if (boardingHouse.getImageUrls() != null && !boardingHouse.getImageUrls().isEmpty()) {
                Picasso.get()
                    .load(fixImageUrl(boardingHouse.getImageUrls().get(0)))
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.ic_house_placeholder)
                    .error(R.drawable.ic_house_placeholder)
                    .into(receiptPropertyImage);
            }
        }
        
        // Format dates
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            receiptMoveInDate.setText(outputFormat.format(inputFormat.parse(moveInDate)));
            receiptMoveOutDate.setText(outputFormat.format(inputFormat.parse(endDate)));
        } catch (Exception e) {
            receiptMoveInDate.setText(moveInDate);
            receiptMoveOutDate.setText(endDate);
        }
        
        receiptDuration.setText(String.format(Locale.US, "%d month%s", duration, duration > 1 ? "s" : ""));
        receiptTotalAmount.setText(String.format(Locale.US, "K%.2f", totalAmount));
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(BoardingHouseDetailsActivity.this, 
                "Booking confirmed! Check 'My Bookings' for details.", Toast.LENGTH_LONG).show();
            finish();
        });
        
        dialog.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PAYMENT) {
            if (resultCode == RESULT_OK && data != null) {
                String status = data.getStringExtra("payment_status");
                if ("completed".equals(status)) {
                    Toast.makeText(this, "Booking confirmed! Payment successful.", Toast.LENGTH_LONG).show();
                    // Optionally navigate to bookings list
                    finish();
                } else {
                    Toast.makeText(this, "Payment status: " + status, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Payment cancelled or failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleFavorite() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please sign in to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (boardingHouseId == null || boardingHouseId.isEmpty()) {
            Toast.makeText(this, "Property information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isFavorite) {
            removeFavorite();
        } else {
            addFavorite();
        }
    }
    
    private void checkFavoriteStatus() {
        if (userId == null || userId.isEmpty() || boardingHouseId == null) {
            return;
        }
        
        com.roominate.services.SupabaseClient.getInstance().isFavorite(boardingHouseId, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    isFavorite = response.optBoolean("is_favorite", false);
                    runOnUiThread(() -> updateFavoriteButton());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing favorite status", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking favorite status: " + error);
            }
        });
    }
    
    private void addFavorite() {
        favoriteButton.setEnabled(false);
        
        com.roominate.services.SupabaseClient.getInstance().addFavorite(boardingHouseId, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    isFavorite = true;
                    updateFavoriteButton();
                    Toast.makeText(BoardingHouseDetailsActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(BoardingHouseDetailsActivity.this, "Failed to add favorite: " + error, Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }
        });
    }
    
    private void removeFavorite() {
        favoriteButton.setEnabled(false);
        
        com.roominate.services.SupabaseClient.getInstance().removeFavorite(boardingHouseId, new com.roominate.services.SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    isFavorite = false;
                    favoriteId = null;
                    updateFavoriteButton();
                    Toast.makeText(BoardingHouseDetailsActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(BoardingHouseDetailsActivity.this, "Failed to remove favorite: " + error, Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }
        });
    }
    
    private void updateFavoriteButton() {
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_heart_filled);
            favoriteButton.setContentDescription("Remove from favorites");
        } else {
            favoriteButton.setImageResource(R.drawable.ic_heart_outline);
            favoriteButton.setContentDescription("Add to favorites");
        }
    }

    private void shareBoardingHouse() {
        // TODO: Implement share functionality
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this boarding house: " + boardingHouse.getName());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
