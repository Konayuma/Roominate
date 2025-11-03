package com.roominate.activities.auth;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.roominate.R;
import com.roominate.activities.tenant.TenantDashboardActivity;
import com.roominate.activities.owner.OwnerDashboardActivity;
import com.roominate.activities.admin.AdminDashboardActivity;

public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 2500; // 2.5 seconds
    private ImageView logoImageView;
    private TextView appNameTextView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        
        // Start animations
        startAnimations();
        
        // Check session and navigate after delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkSessionAndNavigate();
            }
        }, SPLASH_DELAY);
    }
    
    private void checkSessionAndNavigate() {
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
                
                Log.d(TAG, "Session found for: " + userEmail + " (role: " + userRole + ")");
                navigateToDashboard(userRole);
            } else {
                // Token expired, clear and go to welcome
                Log.d(TAG, "Session expired, redirecting to welcome");
                clearSession(prefs);
                navigateToWelcome();
            }
        } else {
            // Not logged in, go to welcome
            Log.d(TAG, "No session found, redirecting to welcome");
            navigateToWelcome();
        }
    }
    
    private void clearSession(SharedPreferences prefs) {
        prefs.edit()
            .remove("is_logged_in")
            .remove("access_token")
            .remove("refresh_token")
            .remove("token_expires_at")
            .apply();
    }
    
    private void navigateToWelcome() {
        Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
    
    private void navigateToDashboard(String userRole) {
        // Check if owner is using tenant view
        SharedPreferences prefs = getSharedPreferences("roominate_prefs", MODE_PRIVATE);
        boolean ownerUsingTenantView = prefs.getBoolean("owner_using_tenant_view", false);
        
        Intent intent;
        
        // If owner is using tenant view, route to tenant dashboard
        if ("owner".equalsIgnoreCase(userRole) && ownerUsingTenantView) {
            intent = new Intent(this, TenantDashboardActivity.class);
        } else {
            // Normal routing based on user type
            switch (userRole.toLowerCase()) {
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
    
    private void startAnimations() {
        // Logo animation: scale + rotation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoImageView, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 0f, 1.2f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(logoImageView, "rotation", 0f, 360f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        rotation.setDuration(800);
        alpha.setDuration(600);
        
        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(scaleX, scaleY, rotation, alpha);
        logoSet.setStartDelay(200);
        
        // Text animation: slide in from right + fade
        ObjectAnimator textSlide = ObjectAnimator.ofFloat(appNameTextView, "translationX", 300f, 0f);
        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(appNameTextView, "alpha", 0f, 1f);
        
        textSlide.setDuration(700);
        textAlpha.setDuration(700);
        textSlide.setInterpolator(new AccelerateDecelerateInterpolator());
        
        AnimatorSet textSet = new AnimatorSet();
        textSet.playTogether(textSlide, textAlpha);
        textSet.setStartDelay(600);
        
        // Start all animations
        logoSet.start();
        textSet.start();
    }
}
