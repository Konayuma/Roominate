package com.roominate.activities.tenant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.roominate.R;
import com.roominate.models.BoardingHouse;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private Button contactOwnerButton;
    private Button bookNowButton;
    private FloatingActionButton favoriteButton;
    private FloatingActionButton shareButton;

    private BoardingHouse boardingHouse;
    private String boardingHouseId;
    private String userId;
    private boolean isFavorite = false;
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
        loadBoardingHouseDetails();
        checkFavoriteStatus();
        setupListeners();
    }
    
    private void loadUserId() {
        SharedPreferences prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
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
        contactOwnerButton = findViewById(R.id.contactOwnerButton);
        bookNowButton = findViewById(R.id.bookNowButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        shareButton = findViewById(R.id.shareButton);
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
                        .get()
                        .addHeader("apikey", supabaseKey);
                
                if (accessToken != null && !accessToken.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer " + supabaseKey);
                }
                
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
        priceTextView.setText(String.format("₱%.2f/month", boardingHouse.getPricePerMonth()));
        descriptionTextView.setText(boardingHouse.getDescription());
        ratingBar.setRating((float) boardingHouse.getAverageRating());
        ratingCountTextView.setText(String.format("(%d reviews)", boardingHouse.getTotalReviews()));
        availableRoomsTextView.setText(String.format("%d rooms available", boardingHouse.getAvailableRooms()));

        // TODO: Set up image slider, amenities chips, and reviews list
    }

    private void setupListeners() {
        contactOwnerButton.setOnClickListener(v -> contactOwner());
        bookNowButton.setOnClickListener(v -> bookBoardingHouse());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> shareBoardingHouse());
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
        
        new Thread(() -> {
            try {
                String supabaseUrl = com.roominate.BuildConfig.SUPABASE_URL;
                String supabaseKey = com.roominate.BuildConfig.SUPABASE_ANON_KEY;
                
                String url = supabaseUrl + "/rest/v1/favorites?user_id=eq." + userId + "&listing_id=eq." + boardingHouseId;
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body().string();
                
                if (response.isSuccessful()) {
                    JSONArray favoritesArray = new JSONArray(responseBody);
                    
                    if (favoritesArray.length() > 0) {
                        JSONObject favoriteObj = favoritesArray.getJSONObject(0);
                        favoriteId = favoriteObj.optString("id");
                        isFavorite = true;
                    } else {
                        isFavorite = false;
                        favoriteId = null;
                    }
                    
                    new Handler(Looper.getMainLooper()).post(this::updateFavoriteButton);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking favorite status", e);
            }
        }).start();
    }
    
    private void addFavorite() {
        favoriteButton.setEnabled(false);
        
        new Thread(() -> {
            try {
                String supabaseUrl = com.roominate.BuildConfig.SUPABASE_URL;
                String supabaseKey = com.roominate.BuildConfig.SUPABASE_ANON_KEY;
                
                JSONObject requestBody = new JSONObject();
                requestBody.put("user_id", userId);
                requestBody.put("listing_id", boardingHouseId);
                
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
                );
                
                Request request = new Request.Builder()
                        .url(supabaseUrl + "/rest/v1/favorites")
                        .post(body)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body().string();
                
                if (response.isSuccessful()) {
                    JSONArray resultArray = new JSONArray(responseBody);
                    if (resultArray.length() > 0) {
                        JSONObject favoriteObj = resultArray.getJSONObject(0);
                        favoriteId = favoriteObj.optString("id");
                        isFavorite = true;
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            updateFavoriteButton();
                            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                            favoriteButton.setEnabled(true);
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, "Failed to add favorite", Toast.LENGTH_SHORT).show();
                        favoriteButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding favorite", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void removeFavorite() {
        if (favoriteId == null) {
            return;
        }
        
        favoriteButton.setEnabled(false);
        
        new Thread(() -> {
            try {
                String supabaseUrl = com.roominate.BuildConfig.SUPABASE_URL;
                String supabaseKey = com.roominate.BuildConfig.SUPABASE_ANON_KEY;
                
                String url = supabaseUrl + "/rest/v1/favorites?id=eq." + favoriteId;
                
                Request request = new Request.Builder()
                        .url(url)
                        .delete()
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    isFavorite = false;
                    favoriteId = null;
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateFavoriteButton();
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        favoriteButton.setEnabled(true);
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                        favoriteButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing favorite", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    favoriteButton.setEnabled(true);
                });
            }
        }).start();
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
