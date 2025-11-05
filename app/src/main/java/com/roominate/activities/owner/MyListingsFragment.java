package com.roominate.activities.owner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roominate.BuildConfig;
import com.roominate.R;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class MyListingsFragment extends Fragment {

    private static final String TAG = "MyListingsFragment";
    private RecyclerView recyclerView;
    private PropertyAdapter adapter;
    private List<Property> properties = new ArrayList<>();
    private ImageButton menuButton;
    private TextView emptyStateText;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_listings, container, false);
        
        recyclerView = v.findViewById(R.id.recyclerViewProperties);
        emptyStateText = v.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        menuButton = v.findViewById(R.id.menuButton);
        menuButton.setOnClickListener(view -> {
            if (getActivity() instanceof OwnerDashboardActivity) {
                ((OwnerDashboardActivity) getActivity()).openDrawer();
            }
        });

        // Initialize adapter FIRST before loading data
        adapter = new PropertyAdapter(getContext(), properties, property -> {
            // Navigate to EditPropertyActivity to edit the listing
            Intent intent = new Intent(getContext(), EditPropertyActivity.class);
            intent.putExtra("property_id", property.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        
        // Load current user ID from SharedPreferences
        loadUserData();
        
        // Load listings from Supabase
        loadListings();

        return v;
    }

    private void loadUserData() {
        SharedPreferences prefs = getActivity().getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        String userDataJson = prefs.getString("user_data", null);
        
        Log.d(TAG, "Raw user_data from SharedPreferences: " + userDataJson);
        
        if (userDataJson != null) {
            try {
                JSONObject userData = new JSONObject(userDataJson);
                currentUserId = userData.optString("id", null);
                Log.d(TAG, "Loaded owner user ID: " + currentUserId);
                Log.d(TAG, "User email: " + userData.optString("email", "N/A"));
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data", e);
            }
        }
        
        if (currentUserId == null) {
            Log.w(TAG, "No user ID found in SharedPreferences");
            // Try to get access token as fallback
            String accessToken = prefs.getString("access_token", null);
            Log.d(TAG, "Access token present: " + (accessToken != null));
        }
    }

    private void loadListings() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load listings: user ID is null");
            updateEmptyState();
            return;
        }
        
        // Get SharedPreferences BEFORE starting background thread to avoid NPE
        final SharedPreferences prefs = requireActivity().getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        final String accessToken = prefs.getString("access_token", null);
        
        // Fetch listings from Supabase in background thread
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
                
                Log.d(TAG, "Using access token: " + (accessToken != null ? "YES (length=" + accessToken.length() + ")" : "NO"));
                Log.d(TAG, "Querying for owner_id: " + currentUserId);
                
                // Query boarding_houses where owner_id matches current user
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?owner_id=eq." + currentUserId + "&select=*";
                Log.d(TAG, "Query URL: " + url);
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

                // Centralized header wiring: prefer user access_token when available
                requestBuilder = com.roominate.services.SupabaseClient.addAuthHeaders(requestBuilder);

                if (accessToken != null && !accessToken.isEmpty()) {
                    Log.d(TAG, "Using authenticated request with access token");
                } else {
                    Log.w(TAG, "Using anon key - RLS may block results");
                }

                Request request = requestBuilder.build();
                Response response = client.newCall(request).execute();
                
                Log.d(TAG, "Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Listings response: " + responseBody);
                    
                    JSONArray jsonArray = new JSONArray(responseBody);
                    Log.d(TAG, "Found " + jsonArray.length() + " listings in response");
                    
                    properties.clear();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        
                        // Map boarding_houses fields to Property model
                        Property property = new Property();
                        property.setId(jsonObject.optString("id"));
                        property.setOwnerId(jsonObject.optString("owner_id"));
                        property.setName(jsonObject.optString("name"));
                        property.setDescription(jsonObject.optString("description"));
                        property.setAddress(jsonObject.optString("address"));
                        property.setMonthlyRate(jsonObject.optDouble("monthly_rate", 0.0));
                        property.setSecurityDeposit(jsonObject.optDouble("security_deposit", 0.0));
                        property.setStatus(jsonObject.optString("status", "draft"));
                        
                        Log.d(TAG, "Property " + i + ": name=" + property.getName() + ", owner=" + property.getOwnerId());
                        
                        // Parse images JSONB array
                        if (jsonObject.has("images") && !jsonObject.isNull("images")) {
                            JSONArray imagesArray = jsonObject.optJSONArray("images");
                            if (imagesArray != null) {
                                List<String> imageUrls = new ArrayList<>();
                                for (int j = 0; j < imagesArray.length(); j++) {
                                    imageUrls.add(imagesArray.getString(j));
                                }
                                property.setImageUrls(imageUrls);
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
                    
                    // Update UI on main thread - check fragment is attached first
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TAG, "Updating adapter with " + properties.size() + " properties");
                            Log.d(TAG, "Adapter is null: " + (adapter == null));
                            Log.d(TAG, "RecyclerView is null: " + (recyclerView == null));
                            
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "Called notifyDataSetChanged()");
                            }
                            
                            updateEmptyState();
                            Log.d(TAG, "Loaded " + properties.size() + " properties - UI should be updated");
                            
                            if (properties.isEmpty()) {
                                Toast.makeText(getContext(), "No listings found for your account", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Found " + properties.size() + " listing(s)", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Failed to load listings: " + response.code() + " - " + errorBody);
                    
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Failed: " + response.code() + " - Check logcat", Toast.LENGTH_LONG).show();
                            updateEmptyState();
                        });
                    }
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading listings", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    });
                }
            }
        }).start();
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
