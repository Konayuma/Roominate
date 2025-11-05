package com.roominate.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.roominate.R;
import com.roominate.adapters.BookingAdapter;
import com.roominate.models.Booking;
import com.roominate.services.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class MyBookingsFragment extends Fragment {

    private static final String TAG = "MyBookingsFragment";
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter bookingAdapter;
    private List<Booking> bookingsList = new ArrayList<>();
    private TextView emptyStateText;
    private TextView noBookingsMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButton allButton;
    private MaterialButton confirmedButton;
    private MaterialButton pendingButton;
    private String currentFilter = "all";
    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupFilterButtons();
        loadBookings("all");
    }

    private void initializeViews(View view) {
        bookingsRecyclerView = view.findViewById(R.id.bookingsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        noBookingsMessage = view.findViewById(R.id.noBookingsMessage);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        allButton = view.findViewById(R.id.allButton);
        confirmedButton = view.findViewById(R.id.confirmedButton);
        pendingButton = view.findViewById(R.id.pendingButton);
        
        supabaseClient = SupabaseClient.getInstance();
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_blue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBookings(currentFilter);
        });
    }

    private void setupRecyclerView() {
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Create adapter with a callback listener for booking actions
        bookingAdapter = new BookingAdapter(getContext(), new BookingAdapter.OnBookingActionListener() {
            @Override
            public void onViewDetails(Booking booking) {
                Toast.makeText(getContext(), "Booking: " + booking.getPropertyName(), Toast.LENGTH_SHORT).show();
                // TODO: Navigate to booking details
            }

            @Override
            public void onCancelBooking(Booking booking) {
                // Show confirmation dialog before canceling
                new AlertDialog.Builder(requireContext())
                    .setTitle("Cancel Booking")
                    .setMessage("Are you sure you want to cancel this booking?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // TODO: Call API to cancel booking
                        Toast.makeText(getContext(), "Booking canceled", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });
        bookingsRecyclerView.setAdapter(bookingAdapter);
    }

    private void setupFilterButtons() {
        allButton.setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterButtonStyles();
            loadBookings("all");
        });
        
        confirmedButton.setOnClickListener(v -> {
            currentFilter = "confirmed";
            updateFilterButtonStyles();
            loadBookings("confirmed");
        });
        
        pendingButton.setOnClickListener(v -> {
            currentFilter = "pending";
            updateFilterButtonStyles();
            loadBookings("pending");
        });
        
        updateFilterButtonStyles();
    }

    private void updateFilterButtonStyles() {
        // Reset all buttons to outline style
        allButton.setBackgroundTintList(null);
        confirmedButton.setBackgroundTintList(null);
        pendingButton.setBackgroundTintList(null);
        
        // Highlight the current filter
        switch (currentFilter) {
            case "confirmed":
                confirmedButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.system_blue)
                );
                break;
            case "pending":
                pendingButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.system_blue)
                );
                break;
            default: // "all"
                allButton.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.system_blue)
                );
        }
    }

    private void loadBookings(String status) {
        if (supabaseClient == null) {
            supabaseClient = SupabaseClient.getInstance();
        }
        
        // Show loading indicator
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        supabaseClient.getTenantBookings(status, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return; // Fragment may be detached
                
                getActivity().runOnUiThread(() -> {
                    // Hide loading indicator
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    
                    try {
                        JSONArray bookingsArray = response.optJSONArray("body");
                        if (bookingsArray == null) {
                            bookingsArray = new JSONArray();
                        }
                        
                        bookingsList.clear();
                        
                        for (int i = 0; i < bookingsArray.length(); i++) {
                            JSONObject bookingObj = bookingsArray.getJSONObject(i);
                            Booking booking = parseBooking(bookingObj);
                            if (booking != null) {
                                bookingsList.add(booking);
                            }
                        }
                        
                        bookingAdapter.setBookings(bookingsList);
                        updateEmptyState();
                        
                        Log.d(TAG, "Loaded " + bookingsList.size() + " bookings");
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing bookings", e);
                        Toast.makeText(getContext(), "Error loading bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    // Hide loading indicator
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    
                    Log.e(TAG, "Error loading bookings: " + error);
                    Toast.makeText(getContext(), "Failed to load bookings: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    private Booking parseBooking(JSONObject bookingObj) {
        try {
            Booking booking = new Booking();
            booking.setId(bookingObj.optString("id"));
            booking.setTenantId(bookingObj.optString("tenant_id"));
            booking.setBoardingHouseId(bookingObj.optString("boarding_house_id"));
            booking.setStatus(bookingObj.optString("status", "pending"));
            booking.setTotalAmount(bookingObj.optDouble("total_amount", 0.0));
            
            // Parse dates from ISO format strings
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            
            // Parse dates from database column names: start_date, end_date
            // DB returns dates in format: "2025-11-06" (date only, no time)
            SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            
            String startDateStr = bookingObj.optString("start_date", "");
            if (!startDateStr.isEmpty()) {
                try {
                    Date startDate = dateOnlyFormat.parse(startDateStr);
                    booking.setMoveInDate(startDate);
                } catch (Exception e) {
                    Log.w(TAG, "Could not parse start_date: " + startDateStr, e);
                }
            }
            
            String endDateStr = bookingObj.optString("end_date", "");
            if (!endDateStr.isEmpty()) {
                try {
                    Date endDate = dateOnlyFormat.parse(endDateStr);
                    booking.setMoveOutDate(endDate);
                } catch (Exception e) {
                    Log.w(TAG, "Could not parse end_date: " + endDateStr, e);
                }
            }
            
            String createdAtStr = bookingObj.optString("created_at", "");
            if (!createdAtStr.isEmpty()) {
                try {
                    Date createdAt = isoFormat.parse(createdAtStr);
                    booking.setCreatedAt(createdAt);
                } catch (Exception e) {
                    Log.w(TAG, "Could not parse created_at: " + createdAtStr);
                }
            }
            
            // Get property details from nested boarding_houses object
            JSONObject propertyObj = bookingObj.optJSONObject("boarding_houses");
            if (propertyObj != null) {
                booking.setPropertyName(propertyObj.optString("title", propertyObj.optString("name", "Unknown Property")));
                booking.setPropertyAddress(propertyObj.optString("address", ""));
            }
            
            return booking;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing booking object", e);
            return null;
        }
    }

    private void updateEmptyState() {
        if (bookingsList.isEmpty()) {
            bookingsRecyclerView.setVisibility(View.GONE);
            noBookingsMessage.setVisibility(View.VISIBLE);
            
            String message;
            if ("confirmed".equals(currentFilter)) {
                message = "No confirmed bookings yet";
            } else if ("pending".equals(currentFilter)) {
                message = "No pending bookings";
            } else {
                message = "No bookings yet";
            }
            noBookingsMessage.setText(message);
        } else {
            bookingsRecyclerView.setVisibility(View.VISIBLE);
            noBookingsMessage.setVisibility(View.GONE);
        }
    }
}
