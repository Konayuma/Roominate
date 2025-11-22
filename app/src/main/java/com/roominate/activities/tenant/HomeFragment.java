package com.roominate.activities.tenant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.roominate.BuildConfig;
import com.roominate.R;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;
import com.roominate.services.SupabaseClient;
import com.roominate.utils.LocationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private RecyclerView recyclerView;
    private PropertyAdapter adapter;
    private List<Property> properties = new ArrayList<>();
    private ImageButton menuButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;
    private LocationHelper locationHelper;
    private TextView locationText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewHouses);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary_blue, null));
        swipeRefreshLayout.setOnRefreshListener(this::refreshProperties);
        
        shimmerLayout = v.findViewById(R.id.shimmerLayout);
        locationText = v.findViewById(R.id.locationText);
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            // Open drawer from parent activity
            if (getActivity() instanceof TenantDashboardActivity) {
                ((TenantDashboardActivity) getActivity()).openDrawer();
            }
        });

        // Initialize adapter before loading data
        adapter = new PropertyAdapter(getContext(), properties, property -> {
            Intent i = new Intent(getActivity(), BoardingHouseDetailsActivity.class);
            // BoardingHouseDetailsActivity expects the extra key "boarding_house_id"
            i.putExtra("boarding_house_id", property.getId());
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);
        
        // Initialize location helper for nearby properties
        locationHelper = new LocationHelper(getContext());
        setupLocationTracking();
        
        // Load available properties from Supabase
        loadAvailableProperties();

        return v;
    }

    /**
     * Setup location tracking to show user's current location and nearby properties
     */
    private void setupLocationTracking() {
        if (locationHelper.hasLocationPermission()) {
            // Check if location settings are satisfied
            locationHelper.checkLocationSettings(requireActivity(), satisfied -> {
                if (satisfied) {
                    startLocationUpdates();
                }
            });
        } else {
            // Request permission from Fragment (delegate to parent activity)
            locationHelper.requestLocationPermission(requireActivity());
        }
    }

    /**
     * Start tracking location and update the location text display
     */
    private void startLocationUpdates() {
        locationHelper.startLocationUpdates(new LocationHelper.LocationUpdateListener() {
            @Override
            public void onLocationReceived(Location location) {
                updateLocationText(location);
                Log.d(TAG, "User location: " + location.getLatitude() + ", " + location.getLongitude());
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
                locationText.setText("Location unavailable");
            }
        });
    }

    /**
     * Update the location text display with user's current city/area
     */
    private void updateLocationText(Location location) {
        // Format coordinates to 4 decimal places
        String coordText = String.format("📍 %.2f km away", 0.5); // Placeholder distance
        locationText.setText(coordText);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationTracking();
            } else {
                locationText.setText("Nearby — location permission denied");
            }
        }
    }

    private void loadAvailableProperties() {
        // Show shimmer skeleton during initial load
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
        }
        
        // Get SharedPreferences BEFORE starting background thread
        final SharedPreferences prefs = requireActivity().getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        final String accessToken = prefs.getString("access_token", null);
        
        // Fetch available properties from Supabase in background thread
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
                
                Log.d(TAG, "Loading available properties for tenant...");
                
                // Query boarding_houses where available=true (public listings)
                // Exclude 'images' column since we fetch images separately from properties_media
                // Note: Use price_per_month (new column) instead of monthly_rate (old column)
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?available=eq.true&status=eq.active&select=id,owner_id,name,description,address,city,province,price_per_month,security_deposit,total_rooms,available_rooms,room_type,furnished,private_bathroom,electricity_included,water_included,internet_included,contact_person,contact_phone,amenities,status,latitude,longitude,created_at,updated_at";
                Log.d(TAG, "Query URL: " + url);
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

                // Centralized header wiring: prefer user access_token when available
                requestBuilder = com.roominate.services.SupabaseClient.addAuthHeaders(requestBuilder);

                if (accessToken != null && !accessToken.isEmpty()) {
                    Log.d(TAG, "Using authenticated request");
                } else {
                    Log.d(TAG, "Using anon key");
                }

                Request request = requestBuilder.build();
                Response response = client.newCall(request).execute();
                
                Log.d(TAG, "Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Properties response: " + responseBody);
                    
                    JSONArray jsonArray = new JSONArray(responseBody);
                    Log.d(TAG, "Found " + jsonArray.length() + " available properties");
                    
                    // Fetch images for these properties from properties_media table
                    SupabaseClient.getInstance().fetchImagesForPropertiesStatic(jsonArray, new SupabaseClient.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            try {
                                JSONArray propertiesWithImages = result.optJSONArray("properties_with_images");
                                
                                properties.clear();
                                
                                for (int i = 0; i < propertiesWithImages.length(); i++) {
                                    JSONObject jsonObject = propertiesWithImages.getJSONObject(i);
                                    
                                    // Map boarding_houses fields to Property model
                                    Property property = new Property();
                                    property.setId(jsonObject.optString("id"));
                                    property.setOwnerId(jsonObject.optString("owner_id"));
                                    property.setName(jsonObject.optString("name"));
                                    property.setDescription(jsonObject.optString("description"));
                                    property.setAddress(jsonObject.optString("address"));
                                    // Use price_per_month (new column) with fallback to monthly_rate (old column)
                                    property.setMonthlyRate(jsonObject.optDouble("price_per_month", jsonObject.optDouble("monthly_rate", 0.0)));
                                    property.setSecurityDeposit(jsonObject.optDouble("security_deposit", 0.0));
                                    property.setStatus(jsonObject.optString("status", "draft"));
                                    
                                    // Get images from properties_media (now included via fetchImagesForProperties)
                                    if (jsonObject.has("images") && !jsonObject.isNull("images")) {
                                        JSONArray imagesArray = jsonObject.optJSONArray("images");
                                        if (imagesArray != null && imagesArray.length() > 0) {
                                            List<String> imageUrls = new ArrayList<>();
                                            for (int j = 0; j < imagesArray.length(); j++) {
                                                imageUrls.add(imagesArray.getString(j));
                                            }
                                            property.setImageUrls(imageUrls);
                                            // Set first image as thumbnail
                                            property.setThumbnailUrl(imageUrls.get(0));
                                            Log.d(TAG, "Property " + property.getName() + " has " + imageUrls.size() + " images");
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
                                
                                // Update UI on main thread
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Log.d(TAG, "Updating adapter with " + properties.size() + " properties");
                                        
                                        // Hide shimmer skeleton
                                        if (shimmerLayout != null) {
                                            shimmerLayout.setVisibility(View.GONE);
                                        }
                                        
                                        if (adapter != null) {
                                            adapter.notifyDataSetChanged();
                                            Log.d(TAG, "Called notifyDataSetChanged()");
                                        }
                                        
                                        if (properties.isEmpty()) {
                                            Toast.makeText(getContext(), "No properties available at the moment", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getContext(), "Found " + properties.size() + " property(s)", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing properties with images", e);
                                if (isAdded() && getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (shimmerLayout != null) {
                                            shimmerLayout.setVisibility(View.GONE);
                                        }
                                        Toast.makeText(getContext(), "Error processing properties: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error fetching images: " + error);
                            // Still show properties even if images fail
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (shimmerLayout != null) {
                                        shimmerLayout.setVisibility(View.GONE);
                                    }
                                    if (adapter != null) {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Failed to load properties: " + response.code() + " - " + errorBody);
                    
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Hide shimmer on error
                            if (shimmerLayout != null) {
                                shimmerLayout.setVisibility(View.GONE);
                            }
                            
                            Toast.makeText(getContext(), "Failed to load properties", Toast.LENGTH_LONG).show();
                        });
                    }
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading properties", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Hide shimmer on error
                        if (shimmerLayout != null) {
                            shimmerLayout.setVisibility(View.GONE);
                        }
                        
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    /**
     * Load thumbnail images for all properties in background
     */
    private void loadPropertyThumbnails() {
        new Thread(() -> {
            Log.d(TAG, "Starting to load " + properties.size() + " property thumbnails");
            
            for (int i = 0; i < properties.size(); i++) {
                Property property = properties.get(i);
                String propertyId = property.getId();
                
                Log.d(TAG, "Loading thumbnail for property: " + propertyId);
                
                if (propertyId != null && !propertyId.isEmpty()) {
                    String thumbnailUrl = SupabaseClient.getInstance().getPropertyThumbnailSync(propertyId);
                    
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        Log.d(TAG, "Got thumbnail URL: " + thumbnailUrl);
                        property.setThumbnailUrl(thumbnailUrl);
                        
                        // Notify adapter on UI thread
                        final int position = i;
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (adapter != null) {
                                    adapter.notifyItemChanged(position);
                                }
                            });
                        }
                    } else {
                        Log.d(TAG, "No thumbnail URL found for property: " + propertyId);
                    }
                } else {
                    Log.w(TAG, "Property ID is empty or null");
                }
            }
            
            Log.d(TAG, "Finished loading all property thumbnails");
        }).start();
    }

    /**
     * Refresh properties when user pulls down to refresh
     */
    private void refreshProperties() {
        // Show shimmer skeleton
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
        }
        
        // Fetch fresh data
        new Thread(() -> {
            try {
                final SharedPreferences prefs = requireActivity().getSharedPreferences("roominate_prefs", MODE_PRIVATE);
                final String accessToken = prefs.getString("access_token", null);
                
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
                
                Log.d(TAG, "Refreshing available properties...");
                
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?available=eq.true&status=eq.active&select=*";
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

                requestBuilder = com.roominate.services.SupabaseClient.addAuthHeaders(requestBuilder);

                Request request = requestBuilder.build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    
                    properties.clear();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        
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
                    
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Hide shimmer and show content
                            if (shimmerLayout != null) {
                                shimmerLayout.setVisibility(View.GONE);
                            }
                            
                            // Stop refresh animation
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            
                            // Update adapter
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            
                            // Load thumbnails
                            loadPropertyThumbnails();
                            
                            Toast.makeText(getContext(), "Refreshed " + properties.size() + " properties", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Failed to refresh properties: " + response.code() + " - " + errorBody);
                    
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Hide shimmer
                            if (shimmerLayout != null) {
                                shimmerLayout.setVisibility(View.GONE);
                            }
                            
                            // Stop refresh animation
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            
                            Toast.makeText(getContext(), "Failed to refresh properties", Toast.LENGTH_LONG).show();
                        });
                    }
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing properties", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Hide shimmer
                        if (shimmerLayout != null) {
                            shimmerLayout.setVisibility(View.GONE);
                        }
                        
                        // Stop refresh animation
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop location updates to save battery when fragment is not visible
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume location updates when fragment becomes visible
        if (locationHelper != null && locationHelper.hasLocationPermission()) {
            startLocationUpdates();
        }
    }