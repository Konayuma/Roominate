package com.roominate.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.activities.auth.WelcomeActivity;
import com.roominate.activities.owner.OwnerDashboardActivity;
import com.roominate.activities.tenant.TenantDashboardActivity;
import com.roominate.activities.admin.AdminDashboardActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        checkUserSession();
    }

    private void checkUserSession() {
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        
        if (isLoggedIn) {
            // Check if token is still valid
            long expiresAt = prefs.getLong("token_expires_at", 0);
            long currentTime = System.currentTimeMillis() / 1000;
            
            if (expiresAt > currentTime) {
                // Token is still valid, restore session
                String userRole = prefs.getString("user_role", "tenant");
                String userEmail = prefs.getString("user_email", "");
                
                Log.d(TAG, "Restoring session for user: " + userEmail + " with role: " + userRole);
                redirectToDashboard(userRole);
            } else {
                // Token expired, need to re-login
                Log.d(TAG, "Session expired, redirecting to welcome");
                clearSession();
                redirectToWelcome();
            }
        } else {
            // Not logged in, go to welcome
            Log.d(TAG, "No active session, redirecting to welcome");
            redirectToWelcome();
        }
    }
    
    private void clearSession() {
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        prefs.edit()
            .remove("is_logged_in")
            .remove("access_token")
            .remove("refresh_token")
            .remove("token_expires_at")
            .apply();
    }
    
    private void redirectToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToDashboard(String userType) {
        // Check if owner is using tenant view
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        boolean ownerUsingTenantView = prefs.getBoolean("owner_using_tenant_view", false);
        
        Intent intent;
        
        // If owner is using tenant view, route to tenant dashboard
        if ("owner".equalsIgnoreCase(userType) && ownerUsingTenantView) {
            intent = new Intent(this, TenantDashboardActivity.class);
        } else {
            // Normal routing based on user type
            switch (userType.toLowerCase()) {
                case "tenant":
                    intent = new Intent(this, TenantDashboardActivity.class);
                    break;
                case "owner":
                    intent = new Intent(this, OwnerDashboardActivity.class);
                    break;
                case "admin":
                    intent = new Intent(this, AdminDashboardActivity.class);
                    break;
                default:
                    intent = new Intent(this, TenantDashboardActivity.class);
                    break;
            }
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
