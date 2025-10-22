package com.roominate.activities.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.chip.ChipGroup;
import com.roominate.R;
import com.roominate.models.BoardingHouse;

public class BoardingHouseDetailsActivity extends AppCompatActivity {

    private ViewPager2 imagesViewPager;
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
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    private BoardingHouse boardingHouse;
    private String boardingHouseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_house_details);

        // Get boarding house ID from intent
        boardingHouseId = getIntent().getStringExtra("boarding_house_id");

        initializeViews();
        loadBoardingHouseDetails();
        setupListeners();
    }

    private void initializeViews() {
        imagesViewPager = findViewById(R.id.imagesViewPager);
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

    private void loadBoardingHouseDetails() {
        // TODO: Load boarding house details from Supabase
        // BoardingHouseRepository.getById(boardingHouseId, new DataCallback() {
        //     @Override
        //     public void onSuccess(BoardingHouse bh) {
        //         boardingHouse = bh;
        //         displayBoardingHouseDetails();
        //     }
        //
        //     @Override
        //     public void onError(String error) {
        //         Toast.makeText(BoardingHouseDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
        //     }
        // });
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
        // TODO: Add/remove from favorites
    }

    private void shareBoardingHouse() {
        // TODO: Implement share functionality
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this boarding house: " + boardingHouse.getName());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
