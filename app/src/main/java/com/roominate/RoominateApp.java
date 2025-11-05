package com.roominate;

import android.app.Application;
import com.roominate.services.SupabaseClient;

public class RoominateApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client with application context so helpers can access prefs
        SupabaseClient.init(this);
    }
}
