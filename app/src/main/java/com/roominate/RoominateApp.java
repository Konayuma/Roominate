package com.roominate;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.roominate.services.SupabaseClient;

public class RoominateApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client with application context so helpers can access prefs
        SupabaseClient.init(this);
        
        // Set default theme to light mode
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        boolean darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false);
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
