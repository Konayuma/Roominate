package com.roominate.services;

import android.util.Log;
import com.roominate.BuildConfig;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API client for calling Supabase Edge Functions
 * Handles OTP send/verify requests
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    
    // Values are injected at build time via BuildConfig (see app/build.gradle)
    private static final String FUNCTIONS_PATH = "/functions/v1";
    
    private static SupabaseClient instance;
    private final OkHttpClient client;
    
    private SupabaseClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }
    
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    /**
     * Send OTP to email
     */
    public void sendOtp(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            
            String url = BuildConfig.SUPABASE_URL + FUNCTIONS_PATH + "/send-otp";
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
            .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Send OTP failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        
                        if (response.isSuccessful()) {
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString("error", "Unknown error");
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Send OTP exception", e);
            callback.onError("Failed to send OTP: " + e.getMessage());
        }
    }
    
    /**
     * Verify OTP
     */
    public void verifyOtp(String email, String otp, ApiCallback callback) {
        Log.d(TAG, "=== VERIFY OTP CALLED ===");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "OTP: " + otp);
        
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("otp", otp);
            
            String url = BuildConfig.SUPABASE_URL + FUNCTIONS_PATH + "/verify-otp";
            Log.d(TAG, "URL: " + url);
            Log.d(TAG, "Body: " + body.toString());
            
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
            .build();
            
            Log.d(TAG, "Sending verify request...");
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ Verify OTP NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "=== VERIFY OTP RESPONSE ===");
                    Log.d(TAG, "Status code: " + response.code());
                    
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response body: " + responseBody);
                    
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ Verify SUCCESS - calling callback.onSuccess()");
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString("error", "Invalid verification code");
                            Log.e(TAG, "❌ Verify FAILED: " + error);
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Verify OTP exception", e);
            callback.onError("Failed to verify OTP: " + e.getMessage());
        }
    }
    
    /**
     * Create user account with profile data
     * Uses Supabase Auth signUp + creates profile in public.users table
     */
    public void createUser(JSONObject userData, ApiCallback callback) {
        Log.d(TAG, "=== CREATE USER CALLED ===");
        
        try {
            String url = BuildConfig.SUPABASE_URL + "/auth/v1/signup";
            Log.d(TAG, "URL: " + url);
            
            // Prepare signup payload
            JSONObject signupData = new JSONObject();
            signupData.put("email", userData.getString("email"));
            signupData.put("password", userData.getString("password"));
            
            // Add user metadata (stored in auth.users.raw_user_meta_data)
            JSONObject userMetadata = new JSONObject();
            userMetadata.put("role", userData.getString("role"));
            userMetadata.put("first_name", userData.getString("first_name"));
            userMetadata.put("last_name", userData.getString("last_name"));
            userMetadata.put("dob", userData.getString("dob"));
            userMetadata.put("phone", userData.getString("phone"));
            signupData.put("data", userMetadata);
            
            Log.d(TAG, "Signup data: " + signupData.toString());
            
            RequestBody requestBody = RequestBody.create(
                    signupData.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build();
            
            Log.d(TAG, "Sending create user request...");
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ Create user NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "=== CREATE USER RESPONSE ===");
                    Log.d(TAG, "Status code: " + response.code());
                    
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response body: " + responseBody);
                    
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ User created SUCCESS");
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString("error_description", 
                                          json.optString("msg", "Failed to create account"));
                            Log.e(TAG, "❌ Create user FAILED: " + error);
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Create user exception", e);
            callback.onError("Failed to create user: " + e.getMessage());
        }
    }
    
    /**
     * Callback interface for API responses
     */
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
