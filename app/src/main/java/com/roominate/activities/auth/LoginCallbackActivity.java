package com.roominate.activities.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.content.SharedPreferences;
import java.security.GeneralSecurityException;

import com.roominate.activities.MainActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginCallbackActivity extends AppCompatActivity {
    private static final String TAG = "LoginCallbackActivity";
    private static final String PREF_FILE = "supabase_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent() != null ? getIntent().getData() : null;
        String fragment = null;
        if (uri != null) {
            fragment = uri.getFragment();
            Log.d(TAG, "Received callback URI: " + uri.toString());
        } else {
            Log.w(TAG, "No callback URI present on intent");
        }

        if (fragment == null || fragment.isEmpty()) {
            // Some providers may return tokens in query parameters; try both
            if (uri != null && uri.getQuery() != null) {
                fragment = uri.getQuery();
            }
        }

        if (fragment == null || fragment.isEmpty()) {
            Toast.makeText(this, "Login callback missing tokens.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Map<String, String> params = parseFragment(fragment);

        String accessToken = params.get("access_token");
        String refreshToken = params.get("refresh_token");
        String expiresIn = params.get("expires_in");
        String tokenType = params.get("token_type");

        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

        SharedPreferences prefs = EncryptedSharedPreferences.create(
            this,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        SharedPreferences.Editor editor = prefs.edit();
            if (accessToken != null) editor.putString("supabase_access_token", accessToken);
            if (refreshToken != null) editor.putString("supabase_refresh_token", refreshToken);
            if (tokenType != null) editor.putString("supabase_token_type", tokenType);
            if (expiresIn != null) {
                try {
                    long secs = Long.parseLong(expiresIn);
                    long expiresAt = System.currentTimeMillis() + (secs * 1000L);
                    editor.putLong("supabase_expires_at", expiresAt);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid expires_in: " + expiresIn, e);
                }
            }
            editor.apply();

            Toast.makeText(this, "Signed in successfully.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Stored OAuth tokens in EncryptedSharedPreferences");

            // Open main activity (or whatever is appropriate in your flow)
            Intent nxt = new Intent(this, MainActivity.class);
            nxt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(nxt);
            finish();

        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "Failed to create encrypted preferences", e);
            Toast.makeText(this, "Failed to store tokens securely.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Map<String, String> parseFragment(String fragment) {
        Map<String, String> map = new HashMap<>();
        String[] parts = fragment.split("&");
        for (String p : parts) {
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String k = p.substring(0, eq);
            String v = p.substring(eq + 1);
            map.put(k, Uri.decode(v));
        }
        return map;
    }
}
