package com.roominate.activities.owner;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.models.Booking;
import com.roominate.services.SupabaseClient;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookingDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BookingDetailsActivity";
    
    // UI Elements
    private MaterialToolbar toolbar;
    private TextView statusBadge;
    private TextView propertyNameText;
    private TextView propertyAddressText;
    private TextView bookingIdText;
    private TextView moveInDateText;
    private TextView moveOutDateText;
    private TextView totalAmountText;
    private TextView createdDateText;
    private MaterialButton completeBookingButton;
    private MaterialButton cancelBookingButton;
    
    // Data
    private Booking booking;
    private SimpleDateFormat displayDateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);
        
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        initializeViews();
        loadBookingData();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        statusBadge = findViewById(R.id.statusBadge);
        propertyNameText = findViewById(R.id.propertyNameText);
        propertyAddressText = findViewById(R.id.propertyAddressText);
        bookingIdText = findViewById(R.id.bookingIdText);
        moveInDateText = findViewById(R.id.moveInDateText);
        moveOutDateText = findViewById(R.id.moveOutDateText);
        totalAmountText = findViewById(R.id.totalAmountText);
        createdDateText = findViewById(R.id.createdDateText);
        completeBookingButton = findViewById(R.id.completeBookingButton);
        cancelBookingButton = findViewById(R.id.cancelBookingButton);
        
        completeBookingButton.setOnClickListener(v -> showCompleteConfirmation());
        cancelBookingButton.setOnClickListener(v -> showCancelConfirmation());
    }
    
    private void loadBookingData() {
        Intent intent = getIntent();
        
        // Get booking data from intent
        String bookingId = intent.getStringExtra("booking_id");
        String propertyName = intent.getStringExtra("property_name");
        String propertyAddress = intent.getStringExtra("property_address");
        String status = intent.getStringExtra("status");
        String moveInDate = intent.getStringExtra("move_in_date");
        String moveOutDate = intent.getStringExtra("move_out_date");
        double totalAmount = intent.getDoubleExtra("total_amount", 0);
        String createdDate = intent.getStringExtra("created_date");
        
        Log.d(TAG, "Booking ID: " + bookingId);
        Log.d(TAG, "Property Name: " + propertyName);
        Log.d(TAG, "Property Address: " + propertyAddress);
        Log.d(TAG, "Status: " + status);
        
        // Create booking object
        booking = new Booking();
        booking.setId(bookingId);
        booking.setPropertyName(propertyName != null ? propertyName : "Unknown");
        booking.setPropertyAddress(propertyAddress != null ? propertyAddress : "No address");
        booking.setStatus(status != null ? status : "pending");
        booking.setTotalAmount(totalAmount);
        
        // Display data
        displayBookingDetails();
    }
    
    private void displayBookingDetails() {
        Intent intent = getIntent();
        
        // Set property info
        propertyNameText.setText(booking.getPropertyName());
        propertyAddressText.setText(booking.getPropertyAddress());
        
        // Set booking info
        bookingIdText.setText(booking.getId());
        
        String moveInDate = intent.getStringExtra("move_in_date");
        if (moveInDate != null && !moveInDate.isEmpty()) {
            moveInDateText.setText(moveInDate);
        }
        
        String moveOutDate = intent.getStringExtra("move_out_date");
        if (moveOutDate != null && !moveOutDate.isEmpty()) {
            moveOutDateText.setText(moveOutDate);
        } else {
            moveOutDateText.setText("Not specified");
        }
        
        totalAmountText.setText(String.format("K%.0f", booking.getTotalAmount()));
        
        String createdDate = intent.getStringExtra("created_date");
        if (createdDate != null && !createdDate.isEmpty()) {
            createdDateText.setText(createdDate);
        }
        
        // Set status badge
        statusBadge.setText(booking.getFormattedStatus());
        statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(booking.getStatusColor())));
        
        // Show/hide action buttons based on status
        updateActionButtons();
    }
    
    private void updateActionButtons() {
        String status = booking.getStatus().toLowerCase();
        
        // Show complete button for pending, confirmed, or active bookings
        if (status.equals("pending") || status.equals("confirmed") || status.equals("active")) {
            completeBookingButton.setVisibility(View.VISIBLE);
        } else {
            completeBookingButton.setVisibility(View.GONE);
        }
        
        // Show cancel button only for pending/confirmed bookings
        if (status.equals("pending") || status.equals("confirmed")) {
            cancelBookingButton.setVisibility(View.VISIBLE);
        } else {
            cancelBookingButton.setVisibility(View.GONE);
        }
    }
    
    private void showCompleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Complete Booking")
            .setMessage("Mark this booking as completed? This action cannot be undone.")
            .setPositiveButton("Complete", (dialog, which) -> completeBooking())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showCancelConfirmation() {
        // Create dialog with EditText for cancellation reason
        final android.widget.EditText reasonInput = new android.widget.EditText(this);
        reasonInput.setHint("Enter cancellation reason (optional)");
        reasonInput.setPadding(50, 20, 50, 20);
        
        new AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setView(reasonInput)
            .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                String reason = reasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    reason = "Cancelled by owner";
                }
                cancelBooking(reason);
            })
            .setNegativeButton("No", null)
            .show();
    }
    
    private void completeBooking() {
        updateBookingStatus("completed");
    }
    
    private void cancelBooking(String reason) {
        // Show loading state
        completeBookingButton.setEnabled(false);
        cancelBookingButton.setEnabled(false);
        Toast.makeText(this, "Cancelling booking...", Toast.LENGTH_SHORT).show();
        
        SupabaseClient.getInstance().cancelBooking(booking.getId(), reason, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    booking.setStatus("cancelled");
                    statusBadge.setText(booking.getFormattedStatus());
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(booking.getStatusColor())));
                    updateActionButtons();
                    
                    Toast.makeText(BookingDetailsActivity.this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    
                    // Return result to caller
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("booking_id", booking.getId());
                    resultIntent.putExtra("new_status", "cancelled");
                    setResult(RESULT_OK, resultIntent);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error cancelling booking: " + error);
                    Toast.makeText(BookingDetailsActivity.this, 
                        "Failed to cancel booking: " + error, Toast.LENGTH_LONG).show();
                    
                    // Re-enable buttons
                    completeBookingButton.setEnabled(true);
                    cancelBookingButton.setEnabled(true);
                });
            }
        });
    }
    
    private void updateBookingStatus(String newStatus) {
        // Show loading state
        completeBookingButton.setEnabled(false);
        cancelBookingButton.setEnabled(false);
        Toast.makeText(this, "Updating booking...", Toast.LENGTH_SHORT).show();
        
        SupabaseClient.getInstance().updateBookingStatus(booking.getId(), newStatus, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    booking.setStatus(newStatus);
                    statusBadge.setText(booking.getFormattedStatus());
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(booking.getStatusColor())));
                    updateActionButtons();
                    
                    String message = newStatus.equals("completed") ? 
                        "Booking marked as completed" : "Booking cancelled";
                    Toast.makeText(BookingDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    // Return result to caller
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("booking_id", booking.getId());
                    resultIntent.putExtra("new_status", newStatus);
                    setResult(RESULT_OK, resultIntent);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error updating booking status: " + error);
                    Toast.makeText(BookingDetailsActivity.this, 
                        "Failed to update booking: " + error, Toast.LENGTH_LONG).show();
                    
                    // Re-enable buttons
                    completeBookingButton.setEnabled(true);
                    cancelBookingButton.setEnabled(true);
                });
            }
        });
    }
}
