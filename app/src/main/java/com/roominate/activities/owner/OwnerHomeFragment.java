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
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;
import com.roominate.activities.tenant.BoardingHouseDetailsActivity;
import com.roominate.services.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OwnerHomeFragment extends Fragment {

    private static final String TAG = "OwnerHomeFragment";
    private RecyclerView recyclerView;
    private PropertyAdapter adapter;
    private List<Property> properties = new ArrayList<>();
    private ImageButton menuButton;
    private TextView emptyStateText;
    
    // Statistics views
    private TextView propertiesCountText;
    private TextView totalBookingsText;
    private TextView pendingBookingsText;
    private TextView totalRevenueText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_home, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewProperties);
        emptyStateText = v.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize statistics views - use IDs from actual layout
        propertiesCountText = v.findViewById(R.id.totalListings);
        totalBookingsText = v.findViewById(R.id.totalBookings);
        pendingBookingsText = null;  // Not present in layout
        totalRevenueText = null;     // Not present in layout
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            // Open drawer from parent activity
            if (getActivity() instanceof OwnerDashboardActivity) {
                ((OwnerDashboardActivity) getActivity()).openDrawer();
            }
        });

        adapter = new PropertyAdapter(getContext(), properties, property -> {
            // Navigate to edit property
            Intent i = new Intent(getActivity(), EditPropertyActivity.class);
            i.putExtra("property_id", property.getId());
            startActivity(i);
        });
        
        recyclerView.setAdapter(adapter);
        
        // Load statistics and properties
        loadOwnerStats();
        loadOwnerProperties();

        return v;
    }
    
    private void loadOwnerStats() {
        Log.d(TAG, "Loading owner statistics...");
        
        SupabaseClient.getInstance().getOwnerStats(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    try {
                        int propertiesCount = response.optInt("properties_count", 0);
                        int totalBookings = response.optInt("total_bookings", 0);
                        int pendingBookings = response.optInt("pending_bookings", 0);
                        double totalRevenue = response.optDouble("total_revenue", 0.0);
                        
                        // Update UI
                        if (propertiesCountText != null) {
                            propertiesCountText.setText(String.valueOf(propertiesCount));
                        }
                        if (totalBookingsText != null) {
                            totalBookingsText.setText(String.valueOf(totalBookings));
                        }
                        if (pendingBookingsText != null) {
                            pendingBookingsText.setText(String.valueOf(pendingBookings));
                        }
                        if (totalRevenueText != null) {
                            totalRevenueText.setText(String.format("K%.0f", totalRevenue));
                        }
                        
                        Log.d(TAG, "Statistics loaded: " + propertiesCount + " properties, " + totalBookings + " bookings, K" + totalRevenue + " revenue");
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing statistics", e);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading statistics: " + error);
                // Silently fail - statistics are not critical
            }
        });
    }

    private void loadOwnerProperties() {
        Log.d(TAG, "Loading properties for current owner...");
        
        SupabaseClient.getInstance().getPropertiesByOwner(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONArray propertiesArray = response.optJSONArray("data");
                        
                        if (propertiesArray != null) {
                            properties.clear();
                            Log.d(TAG, "Received " + propertiesArray.length() + " properties");
                            
                            for (int i = 0; i < propertiesArray.length(); i++) {
                                JSONObject jsonObject = propertiesArray.getJSONObject(i);
                                
                                // Map boarding_houses fields to Property model
                                Property property = new Property();
                                property.setId(jsonObject.optString("id"));
                                property.setOwnerId(jsonObject.optString("owner_id"));
                                property.setName(jsonObject.optString("name"));
                                property.setDescription(jsonObject.optString("description"));
                                property.setAddress(jsonObject.optString("address"));
                                property.setMonthlyRate(jsonObject.optDouble("price_per_month", jsonObject.optDouble("monthly_rate", 0.0)));
                                property.setSecurityDeposit(jsonObject.optDouble("security_deposit", 0.0));
                                property.setStatus(jsonObject.optString("status", "draft"));
                                
                                // Parse images JSONB array
                                if (jsonObject.has("images") && !jsonObject.isNull("images")) {
                                    JSONArray imagesArray = jsonObject.optJSONArray("images");
                                    if (imagesArray != null && imagesArray.length() > 0) {
                                        List<String> imageUrls = new ArrayList<>();
                                        for (int j = 0; j < imagesArray.length(); j++) {
                                            imageUrls.add(imagesArray.getString(j));
                                        }
                                        property.setImageUrls(imageUrls);
                                        property.setThumbnailUrl(imageUrls.get(0));
                                    }
                                }
                                
                                // Parse amenities JSONB array
                                if (jsonObject.has("amenities") && !jsonObject.isNull("amenities")) {
                                    JSONArray amenitiesArray = jsonObject.optJSONArray("amenities");
                                    if (amenitiesArray != null) {
                                        List<String> amenities = new ArrayList<>();
                                        for (int j = 0; j < amenitiesArray.length(); j++) {
                                            amenities.add(amenitiesArray.getString(j));
                                        }
                                        property.setAmenities(amenities);
                                    }
                                }
                                
                                properties.add(property);
                            }
                            
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                            
                            if (properties.isEmpty()) {
                                Toast.makeText(getContext(), "No properties yet. Tap + to add your first property!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Loaded " + properties.size() + " property(s)", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing properties", e);
                        Toast.makeText(getContext(), "Error loading properties: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateEmptyState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading properties: " + error);
                
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load properties: " + error, Toast.LENGTH_LONG).show();
                    updateEmptyState();
                });
            }
        });
    }
    
    private void updateEmptyState() {
        if (properties.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
}
