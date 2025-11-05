package com.roominate.activities.tenant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.roominate.R;
import com.roominate.services.SupabaseClient;
import com.roominate.activities.tenant.BoardingHouseDetailsActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private MapView map = null;
    private SupabaseClient supabaseClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK); // Set the tile source

        // Add map controls
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Set initial map center and zoom level to Ndola, Zambia
        IMapController mapController = map.getController();
        mapController.setZoom(12.0);
        GeoPoint startPoint = new GeoPoint(-12.9605, 28.6480); // Ndola, Zambia
        mapController.setCenter(startPoint);

        // Initialize Supabase client
        supabaseClient = SupabaseClient.getInstance();

        // Fetch and display properties with coordinates
        loadPropertiesOnMap();
    }

    /**
     * Fetch all properties with geocoded coordinates and display them as markers on the map
     */
    private void loadPropertiesOnMap() {
        Log.d(TAG, "Loading properties from Supabase...");
        Toast.makeText(this, "Loading properties with coordinates...", Toast.LENGTH_SHORT).show();

        supabaseClient.getAllPropertiesWithCoordinates(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray properties = response.getJSONArray("properties");
                    Log.d(TAG, "Received " + properties.length() + " properties with coordinates");

                    if (properties.length() == 0) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(MapActivity.this, "No properties with coordinates found. Make sure properties have been geocoded.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        // Clear existing markers
                        map.getOverlays().clear();

                        GeoPoint firstLocation = null;

                        // Add a marker for each property
                        for (int i = 0; i < properties.length(); i++) {
                            try {
                                JSONObject property = properties.getJSONObject(i);
                                double latitude = property.getDouble("latitude");
                                double longitude = property.getDouble("longitude");
                                String propertyId = property.getString("id");
                                String name = property.optString("name", "Property");
                                int availableRooms = property.optInt("available_rooms", 0);
                                double price = property.optDouble("price_per_month", 0);

                                Log.d(TAG, "Adding marker: " + name + " at " + latitude + ", " + longitude);

                                GeoPoint location = new GeoPoint(latitude, longitude);

                                // Create marker
                                Marker marker = new Marker(map);
                                marker.setPosition(location);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                marker.setTitle(name);
                                marker.setSnippet(String.format(
                                    "K%.0f/month | %d room%s available",
                                    price,
                                    availableRooms,
                                    availableRooms != 1 ? "s" : ""
                                ));

                                // Store property ID in marker for later retrieval
                                marker.setRelatedObject(propertyId);

                                // Add click listener to navigate to property details
                                marker.setOnMarkerClickListener((marker1, mapView) -> {
                                    String id = (String) marker1.getRelatedObject();
                                    if (id != null) {
                                        Intent intent = new Intent(MapActivity.this, BoardingHouseDetailsActivity.class);
                                        intent.putExtra("boarding_house_id", id);
                                        startActivity(intent);
                                    }
                                    return true;
                                });

                                map.getOverlays().add(marker);

                                // Set first property as map center
                                if (firstLocation == null) {
                                    firstLocation = location;
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error adding marker for property", e);
                            }
                        }

                        // Center map on first property
                        if (firstLocation != null) {
                            IMapController controller = map.getController();
                            controller.setZoom(13.0);
                            controller.setCenter(firstLocation);
                        }

                        map.invalidate();
                        Toast.makeText(MapActivity.this, "Loaded " + properties.length() + " properties", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing properties", e);
                    Toast.makeText(MapActivity.this, "Error loading properties: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching properties: " + error);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(MapActivity.this, "Failed to load properties: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // This is needed for osmdroid's lifecycle management
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // This is needed for osmdroid's lifecycle management
        map.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up osmdroid resources
        if (map != null) {
            map.onDetach();
        }
    }
}