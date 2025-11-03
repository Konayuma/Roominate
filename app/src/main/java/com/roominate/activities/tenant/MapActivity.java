package com.roominate.activities.tenant;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

/**
 * MapActivity - Placeholder for map functionality
 * TODO: Add Google Maps dependency and implement map features
 * 
 * To enable Google Maps:
 * 1. Add to app/build.gradle: implementation 'com.google.android.gms:play-services-maps:18.2.0'
 * 2. Add API key to AndroidManifest.xml
 * 3. Implement OnMapReadyCallback
 */
public class MapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        // TODO: Replace with actual map implementation when Google Maps is set up
        TextView placeholderText = findViewById(R.id.mapPlaceholder);
        if (placeholderText != null) {
            placeholderText.setText("Map feature coming soon!\n\nTo enable:\n• Add Google Maps dependency\n• Configure API key\n• Implement map markers");
        }
    }
}