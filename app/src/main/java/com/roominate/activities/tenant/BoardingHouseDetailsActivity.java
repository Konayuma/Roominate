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
import com.roominate.R;
import com.roominate.adapters.ReviewsAdapter;
import com.roominate.models.BoardingHouse;
import com.roominate.services.SupabaseClient;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import okhttp3.*;
import java.io.IOException;

public class BoardingHouseDetailsActivity extends AppCompatActivity {
    private static final String TAG = "BoardingHouseDetails";

    private ViewPager2 imagesViewPager;
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
                        boardingHouse.setPricePerMonth(jsonObject.optDouble("monthly_rate", 0.0));
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
            // If no images, just set a placeholder
            imagesViewPager.setAdapter(new ImageSliderAdapter(new java.util.ArrayList<>()));
            return;
        }
        
        imagesViewPager.setAdapter(new ImageSliderAdapter(boardingHouse.getImageUrls()));
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
            Picasso.get()
                .load(imageUrl)
                .fit()
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.imageView);
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
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("boarding_house_id", boardingHouseId);
        startActivity(intent);
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
