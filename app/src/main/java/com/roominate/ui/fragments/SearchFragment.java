package com.roominate.ui.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.roominate.R;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;
import com.roominate.services.SupabaseClient;
import com.roominate.activities.tenant.BoardingHouseDetailsActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    // Views
    private TextInputEditText searchEditText;
    private MaterialButton filterButton;
    private MaterialButton listViewButton;
    private MaterialButton mapViewButton;
    private MaterialButton listViewButton2;
    private MaterialButton mapViewButton2;
    private RecyclerView resultsRecyclerView;
    private TextView resultsCountTextView;
    private LinearLayout emptyStateLayout;
    private LinearLayout listViewLayout;
    private View mapViewLayout;
    private ProgressBar progressBar;
    private MapView mapView;

    // Data
    private List<Property> allProperties = new ArrayList<>();
    private List<Property> filteredProperties = new ArrayList<>();
    private PropertyAdapter adapter;

    // Filter state
    private Double minPrice = null;
    private Double maxPrice = null;
    private String locationFilter = null;
    private Integer minRooms = null;
    private boolean isMapViewActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize osmdroid configuration
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadAllProperties();
    }

    private void initializeViews(View view) {
        searchEditText = view.findViewById(R.id.searchEditText);
        filterButton = view.findViewById(R.id.filterButton);
        listViewButton = view.findViewById(R.id.listViewButton);
        mapViewButton = view.findViewById(R.id.mapViewButton);
        listViewButton2 = view.findViewById(R.id.listViewButton2);
        mapViewButton2 = view.findViewById(R.id.mapViewButton2);
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);
        resultsCountTextView = view.findViewById(R.id.resultsCountTextView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        listViewLayout = view.findViewById(R.id.listViewLayout);
        mapViewLayout = view.findViewById(R.id.mapViewLayout);
        progressBar = view.findViewById(R.id.progressBar);
        mapView = view.findViewById(R.id.mapView);
        
        // Setup map
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        // Default center (Zambia - Lusaka)
        mapView.getController().setCenter(new GeoPoint(-15.4167, 28.2833));
    }

    private void setupRecyclerView() {
        adapter = new PropertyAdapter(requireContext(), filteredProperties, property -> {
            // Navigate to property details
            Intent intent = new Intent(requireContext(), BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", property.getId());
            startActivity(intent);
        });
        
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Search text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProperties(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter button - open filter dialog
        filterButton.setOnClickListener(v -> showFilterDialog());

        // View toggle buttons (list view layout)
        listViewButton.setOnClickListener(v -> switchToListView());
        mapViewButton.setOnClickListener(v -> switchToMapView());

        // View toggle buttons (map view layout)
        listViewButton2.setOnClickListener(v -> switchToListView());
        mapViewButton2.setOnClickListener(v -> switchToMapView());
    }

    private void showFilterDialog() {
        View filterView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search_filters, null);

        // Get filter UI elements
        EditText locationEditText = filterView.findViewById(R.id.locationEditText);
        SeekBar priceSeekBar = filterView.findViewById(R.id.priceRangeSeekBar);
        TextView priceTextView = filterView.findViewById(R.id.priceTextView);

        // Set current values
        if (locationFilter != null) {
            locationEditText.setText(locationFilter);
        }

        if (maxPrice != null) {
            priceSeekBar.setProgress((maxPrice.intValue()) / 100);
            priceTextView.setText(String.format("Max: K%.0f", maxPrice));
        } else {
            priceTextView.setText("Max: No limit");
        }

        // Price slider listener
        priceSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                int price = progress * 100;
                if (price == 0) {
                    priceTextView.setText("Max: No limit");
                } else {
                    priceTextView.setText(String.format("Max: K%,d", price));
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        // Show dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Search Filters")
            .setView(filterView)
            .setPositiveButton("Apply", (dialog, which) -> {
                applyFilters(locationEditText.getText().toString(), priceSeekBar.getProgress());
            })
            .setNegativeButton("Clear Filters", (dialog, which) -> {
                clearFilters();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void applyFilters(String location, int priceProgress) {
        // Set filter values
        locationFilter = location.isEmpty() ? null : location;
        maxPrice = priceProgress == 0 ? null : (double) (priceProgress * 100);
        minPrice = null;

        // Update filter button to show active count
        int activeCount = 0;
        if (locationFilter != null) activeCount++;
        if (maxPrice != null) activeCount++;

        if (activeCount > 0) {
            filterButton.setText("Filters (" + activeCount + ")");
        } else {
            filterButton.setText("Filter");
        }

        // Re-filter properties
        applyAllFilters();
    }

    private void clearFilters() {
        minPrice = null;
        maxPrice = null;
        locationFilter = null;
        minRooms = null;
        filterButton.setText("Filter");

        // Re-filter properties
        applyAllFilters();
        Toast.makeText(requireContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
    }

    private void applyAllFilters() {
        filteredProperties.clear();

        for (Property property : allProperties) {
            boolean matches = true;

            // Check price filter
            if (maxPrice != null && property.getMonthlyRate() > maxPrice) {
                matches = false;
            }

            if (minPrice != null && property.getMonthlyRate() < minPrice) {
                matches = false;
            }

            // Check location filter
            if (locationFilter != null && !locationFilter.isEmpty()) {
                String searchLower = locationFilter.toLowerCase().trim();
                if (!property.getAddress().toLowerCase().contains(searchLower) &&
                    !property.getName().toLowerCase().contains(searchLower)) {
                    matches = false;
                }
            }

            // Check available rooms filter
            if (minRooms != null && property.getAvailableRooms() < minRooms) {
                matches = false;
            }

            if (matches) {
                filteredProperties.add(property);
            }
        }

        updateUI();
    }

    private void loadAllProperties() {
        showLoading(true);
        
        SupabaseClient.getInstance().getAllPropertiesWithCoordinates(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (getActivity() == null) return;
                
                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONArray propertiesArray = response.getJSONArray("properties");
                        allProperties.clear();
                        
                        for (int i = 0; i < propertiesArray.length(); i++) {
                            JSONObject propertyJson = propertiesArray.getJSONObject(i);
                            Property property = parseProperty(propertyJson);
                            if (property != null) {
                                allProperties.add(property);
                                // Load thumbnail for each property asynchronously
                                loadPropertyThumbnail(property);
                            }
                        }
                        
                        filteredProperties.clear();
                        filteredProperties.addAll(allProperties);
                        
                        updateUI();
                        showLoading(false);
                        
                        Log.d(TAG, "Loaded " + allProperties.size() + " properties");
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing properties", e);
                        showError("Failed to load properties");
                        showLoading(false);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error loading properties: " + error);
                    showError("Failed to load properties: " + error);
                    showLoading(false);
                });
            }
        });
    }

    private Property parseProperty(JSONObject json) {
        try {
            Property property = new Property();
            property.setId(json.optString("id"));
            property.setName(json.optString("name"));
            property.setAddress(json.optString("address"));
            property.setMonthlyRate(json.optDouble("price_per_month", 0.0));
            
            // Add coordinates
            property.setLatitude(json.optDouble("latitude", 0.0));
            property.setLongitude(json.optDouble("longitude", 0.0));
            
            // Add description if available
            if (json.has("description")) {
                property.setDescription(json.optString("description"));
            }
            
            // Add available rooms if available
            if (json.has("available_rooms")) {
                int availableRooms = json.optInt("available_rooms", 0);
                property.setAvailableRooms(availableRooms);
                property.getAmenities().add(availableRooms + " rooms available");
            }
            
            return property;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing property", e);
            return null;
        }
    }

    private void loadPropertyThumbnail(Property property) {
        if (property == null || property.getId() == null) {
            Log.w(TAG, "Cannot load thumbnail - property or ID is null");
            return;
        }
        
        Log.d(TAG, "Loading thumbnail for property: " + property.getId() + " (" + property.getName() + ")");
        
        new Thread(() -> {
            try {
                String thumbnailUrl = SupabaseClient.getInstance().getPropertyThumbnailSync(property.getId());
                Log.d(TAG, "Thumbnail URL for " + property.getName() + ": " + thumbnailUrl);
                
                if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    property.setThumbnailUrl(thumbnailUrl);
                    Log.d(TAG, "Set thumbnail URL for property: " + property.getName());
                    
                    // Notify adapter on UI thread
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "Notified adapter of thumbnail update");
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "No thumbnail URL found for property: " + property.getName() + " (ID: " + property.getId() + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail for property: " + property.getId(), e);
            }
        }).start();
    }

    private void filterProperties(String query) {
        // If search is empty, apply stored filters
        if (query == null || query.trim().isEmpty()) {
            applyAllFilters();
            return;
        }

        // Filter by search query combined with existing filters
        String lowerQuery = query.toLowerCase().trim();
        filteredProperties.clear();

        for (Property property : allProperties) {
            boolean matchesSearch = property.getName().toLowerCase().contains(lowerQuery) ||
                                   property.getAddress().toLowerCase().contains(lowerQuery);

            if (!matchesSearch) continue;

            boolean matches = true;

            // Apply stored filters
            if (maxPrice != null && property.getMonthlyRate() > maxPrice) {
                matches = false;
            }
            if (minPrice != null && property.getMonthlyRate() < minPrice) {
                matches = false;
            }
            if (locationFilter != null && !locationFilter.isEmpty()) {
                String searchLower = locationFilter.toLowerCase().trim();
                if (!property.getAddress().toLowerCase().contains(searchLower) &&
                    !property.getName().toLowerCase().contains(searchLower)) {
                    matches = false;
                }
            }
            if (minRooms != null && property.getAvailableRooms() < minRooms) {
                matches = false;
            }

            if (matches) {
                filteredProperties.add(property);
            }
        }

        updateUI();
    }

    private void switchToListView() {
        isMapViewActive = false;
        listViewLayout.setVisibility(View.VISIBLE);
        mapViewLayout.setVisibility(View.GONE);
    }

    private void switchToMapView() {
        isMapViewActive = true;
        listViewLayout.setVisibility(View.GONE);
        mapViewLayout.setVisibility(View.VISIBLE);
        
        // Update map markers
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        // Clear existing markers
        mapView.getOverlays().clear();
        
        if (filteredProperties.isEmpty()) {
            return;
        }
        
        // Add markers for each property with coordinates
        boolean firstMarker = true;
        GeoPoint firstPoint = null;
        
        for (Property property : filteredProperties) {
            try {
                // Skip properties without valid coordinates
                if (property.getLatitude() == 0.0 && property.getLongitude() == 0.0) {
                    continue;
                }
                
                GeoPoint point = new GeoPoint(property.getLatitude(), property.getLongitude());
                
                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(property.getName());
                marker.setSnippet(property.getAddress() + "\nZK" + String.format("%.0f", property.getMonthlyRate()) + "/mo");
                
                marker.setOnMarkerClickListener((clickedMarker, mapViewParam) -> {
                    clickedMarker.showInfoWindow();
                    return true;
                });
                
                mapView.getOverlays().add(marker);
                
                if (firstMarker) {
                    firstPoint = point;
                    firstMarker = false;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding marker for property: " + property.getName(), e);
            }
        }
        
        // Center map on first property
        if (firstPoint != null) {
            mapView.getController().setCenter(firstPoint);
        }
        
        mapView.invalidate();
    }

    private void updateUI() {
        if (getActivity() == null) return;
        
        // Update count
        int count = filteredProperties.size();
        resultsCountTextView.setText(count + (count == 1 ? " property found" : " properties found"));
        
        // Update list
        adapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (filteredProperties.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
        }
        
        // Update map if in map view
        if (isMapViewActive) {
            updateMapMarkers();
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() == null) return;
        
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (getActivity() == null) return;
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        emptyStateLayout.setVisibility(View.VISIBLE);
        resultsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
