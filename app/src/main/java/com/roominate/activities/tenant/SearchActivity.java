package com.roominate.activities.tenant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.roominate.R;
import com.roominate.services.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import com.roominate.adapters.PropertyAdapter;
import com.roominate.models.Property;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private EditText searchEditText;
    private Button filterButton;
    private FloatingActionButton mapToggleButton;
    private RecyclerView resultsRecyclerView;
    private TextView resultsCountTextView;
    private MapView mapView;
    private View listContainer;
    private View mapContainer;
    
    private MyLocationNewOverlay myLocationOverlay;
    private boolean isMapView = false;
    private Location currentLocation = null;
    
    // Search results
    private List<Property> searchResults = new ArrayList<>();
    private PropertyAdapter resultsAdapter;
    
    // Filter values
    private Double minPrice = null;
    private Double maxPrice = null;
    private String location = null;
    
    // Filter UI elements
    private SeekBar priceRangeSeekBar;
    private TextView priceTextView;
    private EditText locationEditText;
    
    // Permission launcher
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize osmdroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_search);

        initializeViews();
        setupRecyclerView();
        setupMap();
        setupPermissionLauncher();
        setupListeners();
        
        // Check location permission on startup
        checkLocationPermission();
        
        // Perform initial search
        performSearch("");
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        mapToggleButton = findViewById(R.id.mapToggleButton);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        resultsCountTextView = findViewById(R.id.resultsCountTextView);
        listContainer = findViewById(R.id.listContainer);
        mapContainer = findViewById(R.id.mapContainer);
        mapView = findViewById(R.id.mapView);
    }
    
    private void setupMap() {
        Configuration.getInstance().setUserAgentValue("Roominate/1.0");
        
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        
        // Set default view to a generic location (e.g., Lusaka, Zambia)
        GeoPoint startPoint = new GeoPoint(-15.4167, 28.2833);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(startPoint);
        
        // Initialize location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }
    
    private void setupPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                
                if (fineLocationGranted || coarseLocationGranted) {
                    // Permission granted
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                    enableMyLocation();
                } else {
                    // Permission denied
                    Toast.makeText(this, "Location permission denied. Map features limited.", Toast.LENGTH_LONG).show();
                }
            }
        );
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            enableMyLocation();
        } else {
            // Request permission
            requestLocationPermission();
        }
    }
    
    private void requestLocationPermission() {
        new AlertDialog.Builder(this)
            .setTitle("Location Permission")
            .setMessage("This app needs location access to show nearby properties and enable map-based search.")
            .setPositiveButton("Grant", (dialog, which) -> {
                locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
            })
            .setNegativeButton("Deny", (dialog, which) -> {
                Toast.makeText(this, "Location features will be limited", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void enableMyLocation() {
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.runOnFirstFix(() -> {
                runOnUiThread(() -> {
                    GeoPoint myLocation = myLocationOverlay.getMyLocation();
                    if (myLocation != null) {
                        currentLocation = new Location("gps");
                        currentLocation.setLatitude(myLocation.getLatitude());
                        currentLocation.setLongitude(myLocation.getLongitude());
                        
                        mapView.getController().animateTo(myLocation);
                        Toast.makeText(this, "Location found", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }
    
    private void toggleMapView() {
        isMapView = !isMapView;
        
        // TODO: Add map toggle functionality when UI elements are available in layout
        /*
        if (isMapView) {
            // Show map, hide list
            listContainer.setVisibility(View.GONE);
            mapContainer.setVisibility(View.VISIBLE);
            mapToggleButton.setImageResource(R.drawable.ic_list); // Change icon to list
            
            // Load properties on map
            loadPropertiesOnMap();
        } else {
            // Show list, hide map
            listContainer.setVisibility(View.VISIBLE);
            mapContainer.setVisibility(View.GONE);
            mapToggleButton.setImageResource(R.drawable.ic_map); // Change icon to map
        }
        */
    }
    
    private void loadPropertiesOnMap() {
        // Clear existing markers
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker && overlay != myLocationOverlay);
        
        // Search around current location if available
        if (currentLocation != null) {
            // Use current location as search center
            // You can implement radius-based search here
            performSearch(searchEditText.getText().toString());
        } else {
            performSearch(searchEditText.getText().toString());
        }
    }
    
    private void addPropertyMarkers(JSONArray properties) {
        if (properties == null) return;
        
        try {
            for (int i = 0; i < properties.length(); i++) {
                JSONObject property = properties.getJSONObject(i);
                
                // Check if property has coordinates
                double latitude = property.optDouble("latitude", 0);
                double longitude = property.optDouble("longitude", 0);
                
                if (latitude != 0 && longitude != 0) {
                    GeoPoint point = new GeoPoint(latitude, longitude);
                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle(property.optString("name", "Property"));
                    
                    String price = String.format("K%.0f/month", property.optDouble("monthly_rate", 0));
                    marker.setSnippet(price);
                    
                    // Store property ID in marker for click handling
                    marker.setRelatedObject(property.optString("id"));
                    
                    marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                        // Navigate to property details
                        String propertyId = (String) clickedMarker.getRelatedObject();
                        if (propertyId != null) {
                            Intent intent = new Intent(SearchActivity.this, BoardingHouseDetailsActivity.class);
                            intent.putExtra("boarding_house_id", propertyId);
                            startActivity(intent);
                        }
                        return true;
                    });
                    
                    mapView.getOverlays().add(marker);
                }
            }
            
            mapView.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error adding property markers", e);
        }
    }

    private void setupRecyclerView() {
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new PropertyAdapter(this, searchResults, property -> {
            // Navigate to property details
            Intent intent = new Intent(SearchActivity.this, BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", property.getId());
            startActivity(intent);
        });
        resultsRecyclerView.setAdapter(resultsAdapter);
    }

    private void setupListeners() {
        filterButton.setOnClickListener(v -> showFilterDialog());
        
        // Map toggle button
        mapToggleButton.setOnClickListener(v -> toggleMapView());
        
        // Add text watcher for search with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable searchRunnable;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                
                searchRunnable = () -> performSearch(s.toString());
                handler.postDelayed(searchRunnable, 500); // 500ms debounce
            }
        });
    }

    private void showFilterDialog() {
        View filterView = getLayoutInflater().inflate(R.layout.dialog_search_filters, null);
        
        priceRangeSeekBar = filterView.findViewById(R.id.priceRangeSeekBar);
        priceTextView = filterView.findViewById(R.id.priceTextView);
        locationEditText = filterView.findViewById(R.id.locationEditText);
        
        // Set current values
        if (maxPrice != null) {
            priceRangeSeekBar.setProgress(maxPrice.intValue() / 100);
            priceTextView.setText(String.format("Max: K%.0f", maxPrice));
        } else {
            priceTextView.setText("Max: No limit");
        }
        
        if (location != null) {
            locationEditText.setText(location);
        }
        
        priceRangeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int price = progress * 100;
                if (price == 0) {
                    priceTextView.setText("Max: No limit");
                } else {
                    priceTextView.setText(String.format("Max: K%d", price));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        new AlertDialog.Builder(this)
            .setTitle("Search Filters")
            .setView(filterView)
            .setPositiveButton("Apply", (dialog, which) -> {
                applyFilters(filterView);
            })
            .setNegativeButton("Clear Filters", (dialog, which) -> {
                clearFilters();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void performSearch(String query) {
        resultsCountTextView.setText("Searching...");
        
        SupabaseClient.getInstance().searchProperties(query, minPrice, maxPrice, location, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        JSONArray properties = response.optJSONArray("body");
                        if (properties != null) {
                            searchResults.clear();
                            
                            // Convert JSON objects to Property objects
                            for (int i = 0; i < properties.length(); i++) {
                                JSONObject propObj = properties.getJSONObject(i);
                                Property property = new Property();
                                property.setId(propObj.optString("id"));
                                property.setName(propObj.optString("name"));
                                property.setAddress(propObj.optString("address"));
                                property.setMonthlyRate((int) propObj.optDouble("price_per_month", 0));
                                property.setThumbnailUrl(propObj.optString("thumbnail_url", ""));
                                searchResults.add(property);
                            }
                            
                            int count = searchResults.size();
                            resultsCountTextView.setText(String.format("%d properties found", count));
                            resultsAdapter.notifyDataSetChanged();
                            
                            // Load thumbnails in background
                            loadPropertyThumbnails();
                            
                            // Update map markers if in map view
                            if (isMapView) {
                                addPropertyMarkers(properties);
                            }
                            
                            Log.d(TAG, "Search returned " + count + " properties");
                            
                            if (count == 0) {
                                Toast.makeText(SearchActivity.this, "No properties found matching your search", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            resultsCountTextView.setText("No results");
                            searchResults.clear();
                            resultsAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing search results", e);
                        resultsCountTextView.setText("Search failed");
                        Toast.makeText(SearchActivity.this, "Error loading results: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Search error: " + error);
                    resultsCountTextView.setText("Search failed");
                    searchResults.clear();
                    resultsAdapter.notifyDataSetChanged();
                    Toast.makeText(SearchActivity.this, "Search failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void applyFilters(View filterView) {
        // Get price filter
        int priceProgress = priceRangeSeekBar.getProgress();
        if (priceProgress > 0) {
            maxPrice = (double) (priceProgress * 100);
        } else {
            maxPrice = null;
        }
        
        // Get location filter
        String locationText = locationEditText.getText().toString().trim();
        if (!locationText.isEmpty()) {
            location = locationText;
        } else {
            location = null;
        }
        
        // Update filter button text to show active filters
        int activeFilters = 0;
        if (maxPrice != null) activeFilters++;
        if (location != null) activeFilters++;
        
        if (activeFilters > 0) {
            filterButton.setText(String.format("Filters (%d)", activeFilters));
        } else {
            filterButton.setText("Filters");
        }
        
        // Perform search with new filters
        performSearch(searchEditText.getText().toString());
    }

    private void clearFilters() {
        minPrice = null;
        maxPrice = null;
        location = null;
        filterButton.setText("Filters");
        
        // Perform search without filters
        performSearch(searchEditText.getText().toString());
        
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Load thumbnail images for all properties in background
     */
    private void loadPropertyThumbnails() {
        new Thread(() -> {
            for (int i = 0; i < searchResults.size(); i++) {
                Property property = searchResults.get(i);
                String propertyId = property.getId();
                
                if (propertyId != null && !propertyId.isEmpty()) {
                    String thumbnailUrl = SupabaseClient.getInstance().getPropertyThumbnailSync(propertyId);
                    if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                        property.setThumbnailUrl(thumbnailUrl);
                        
                        // Notify adapter on UI thread
                        final int position = i;
                        runOnUiThread(() -> resultsAdapter.notifyItemChanged(position));
                    }
                }
            }
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }
}
