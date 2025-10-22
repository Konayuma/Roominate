package com.roominate.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        checkUserSession();
    }

    private void checkUserSession() {
        // TODO: Implement session check with Supabase
        // If logged in, redirect to appropriate dashboard based on user type
        // If not logged in, redirect to login
    }

    private void redirectToDashboard(String userType) {
        Intent intent;
        switch (userType) {
            case "tenant":
                // intent = new Intent(this, TenantDashboardActivity.class);
                break;
            case "owner":
                // intent = new Intent(this, OwnerDashboardActivity.class);
                break;
            case "admin":
                // intent = new Intent(this, AdminDashboardActivity.class);
                break;
            default:
                // intent = new Intent(this, LoginActivity.class);
                break;
        }
        // startActivity(intent);
        // finish();
    }
}
