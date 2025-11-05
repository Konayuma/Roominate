package com.roominate.activities.tenant;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.roominate.R;
import com.roominate.adapters.BookingAdapter;
import com.roominate.models.Booking;
import com.roominate.services.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyBookingsActivity extends AppCompatActivity implements BookingAdapter.OnBookingActionListener {

    private static final String TAG = "MyBookingsActivity";
    
    private TabLayout tabLayout;
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter adapter;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        initializeViews();
        setupTabs();
        loadBookings("all");
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new BookingAdapter(this, this);
        bookingsRecyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Confirmed"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String status = tab.getText().toString().toLowerCase();
                loadBookings(status.equals("all") ? "all" : status);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadBookings(String status) {
        SupabaseClient.getInstance().getTenantBookings(status, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray bookingsArray = response.getJSONArray("body");
                    List<Booking> bookingsList = new ArrayList<>();
                    
                    for (int i = 0; i < bookingsArray.length(); i++) {
                        JSONObject bookingJson = bookingsArray.getJSONObject(i);
                        Booking booking = parseBooking(bookingJson);
                        if (booking != null) {
                            bookingsList.add(booking);
                        }
                    }
                    
                    adapter.setBookings(bookingsList);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing bookings", e);
                    Toast.makeText(MyBookingsActivity.this, "Error loading bookings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching bookings: " + error);
                Toast.makeText(MyBookingsActivity.this, "Failed to load bookings: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private Booking parseBooking(JSONObject json) {
        try {
            Booking booking = new Booking();
            
            // Basic fields
            booking.setId(json.optString("id"));
            booking.setTenantId(json.optString("tenant_id"));
            booking.setBoardingHouseId(json.optString("listing_id"));
            booking.setStatus(json.optString("status", "pending"));
            booking.setTotalAmount(json.optDouble("total_amount", 0));
            
            // Parse dates
            String startDate = json.optString("start_date");
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    booking.setMoveInDate(dateFormat.parse(startDate));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing start date: " + startDate, e);
                }
            }
            
            String endDate = json.optString("end_date");
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    booking.setMoveOutDate(dateFormat.parse(endDate));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing end date: " + endDate, e);
                }
            }
            
            // Parse property details from joined boarding_houses
            if (json.has("boarding_houses") && !json.isNull("boarding_houses")) {
                try {
                    JSONObject property = json.getJSONObject("boarding_houses");
                    String title = property.optString("title", null);
                    booking.setPropertyName(title != null ? title : "Unknown Property");
                    booking.setPropertyAddress(property.optString("address", ""));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing boarding_houses", e);
                    booking.setPropertyName("Unknown Property");
                }
            } else {
                booking.setPropertyName("Unknown Property");
            }
            
            // Set created_at
            String createdAt = json.optString("created_at");
            if (createdAt != null && !createdAt.isEmpty()) {
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    booking.setCreatedAt(isoFormat.parse(createdAt));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing created_at: " + createdAt, e);
                }
            }
            
            return booking;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing booking JSON", e);
            return null;
        }
    }

    @Override
    public void onViewDetails(Booking booking) {
        // TODO: Navigate to booking details for tenant
        Toast.makeText(this, "View details: " + booking.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancelBooking(Booking booking) {
        // TODO: Implement tenant booking cancellation
        Toast.makeText(this, "Cancel booking: " + booking.getId(), Toast.LENGTH_SHORT).show();
    }
}
