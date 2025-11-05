package com.roominate.activities.owner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class OwnerBookingsFragment extends Fragment implements BookingAdapter.OnBookingActionListener {

    private static final String TAG = "OwnerBookingsFragment";
    private static final int REQUEST_BOOKING_DETAILS = 1001;
    
    private RecyclerView recyclerView;
    private ImageButton menuButton;
    private TextView emptyStateText;
    private BookingAdapter adapter;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_bookings, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewBookings);
        emptyStateText = v.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        // Initialize adapter
        adapter = new BookingAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            if (getActivity() instanceof OwnerDashboardActivity) {
                ((OwnerDashboardActivity) getActivity()).openDrawer();
            }
        });

        loadBookings();

        return v;
    }
    
    private void loadBookings() {
        SupabaseClient.getInstance().getOwnerBookings(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
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
                        updateEmptyState(bookingsList.isEmpty());
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing bookings", e);
                        Toast.makeText(getContext(), "Error loading bookings", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error fetching bookings: " + error);
                    Toast.makeText(getContext(), "Failed to load bookings: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState(true);
                });
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
            booking.setOwnerId(json.optString("owner_id"));
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
                    String address = property.optString("address", null);
                    
                    Log.d(TAG, "Property from JSON - title: " + title + ", address: " + address);
                    
                    booking.setPropertyName(title != null ? title : "Unknown Property");
                    booking.setPropertyAddress(address != null ? address : "");
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing boarding_houses", e);
                    booking.setPropertyName("Unknown Property");
                }
            } else {
                Log.d(TAG, "No boarding_houses in JSON");
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
            
            Log.d(TAG, "Parsed booking: id=" + booking.getId() + ", propertyName=" + booking.getPropertyName());
            
            return booking;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing booking JSON", e);
            return null;
        }
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewDetails(Booking booking) {
        Intent intent = new Intent(getContext(), BookingDetailsActivity.class);
        
        // Pass booking data
        intent.putExtra("booking_id", booking.getId());
        intent.putExtra("property_name", booking.getPropertyName());
        intent.putExtra("property_address", booking.getPropertyAddress());
        intent.putExtra("status", booking.getStatus());
        intent.putExtra("total_amount", booking.getTotalAmount());
        
        // Format dates for display
        if (booking.getMoveInDate() != null) {
            intent.putExtra("move_in_date", displayDateFormat.format(booking.getMoveInDate()));
        }
        if (booking.getMoveOutDate() != null) {
            intent.putExtra("move_out_date", displayDateFormat.format(booking.getMoveOutDate()));
        }
        if (booking.getCreatedAt() != null) {
            intent.putExtra("created_date", displayDateFormat.format(booking.getCreatedAt()));
        }
        
        startActivityForResult(intent, REQUEST_BOOKING_DETAILS);
    }

    @Override
    public void onCancelBooking(Booking booking) {
        // TODO: Implement booking cancellation
        Toast.makeText(getContext(), "Cancel booking: " + booking.getId(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_BOOKING_DETAILS && resultCode == getActivity().RESULT_OK && data != null) {
            // Booking was updated, refresh the list
            String bookingId = data.getStringExtra("booking_id");
            String newStatus = data.getStringExtra("new_status");
            
            Log.d(TAG, "Booking " + bookingId + " updated to " + newStatus);
            loadBookings(); // Refresh the list
        }
    }
}
