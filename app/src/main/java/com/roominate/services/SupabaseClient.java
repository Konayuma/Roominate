package com.roominate.services;

import android.util.Log;
import com.roominate.   BuildConfig;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import android.content.Context;

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
    private static android.content.Context appContext = null;
    // Guard for concurrent refresh attempts
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    
    private SupabaseClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
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
     * Initialize SupabaseClient with application context so helper methods can access
     * stored session tokens in SharedPreferences.
     */
    public static void init(android.content.Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Encode each segment of a storage path (slash-separated) for safe URL construction
     */
    private String encodeStoragePath(String path) {
        if (path == null) return null;
        try {
            String[] parts = path.split("/");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append("/");
                String encoded = java.net.URLEncoder.encode(parts[i], "UTF-8");
                // URLEncoder uses + for spaces; convert to %20 for path segments
                encoded = encoded.replace("+", "%20");
                sb.append(encoded);
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to encode storage path: " + path, e);
            return path;
        }
    }

    /**
     * Generate a signed URL for a storage path (works with private buckets)
     * @param storagePath The path in storage (e.g., "properties/user123/image.jpg")
     * @param expiresIn Expiration time in seconds (default: 3600 = 1 hour)
     */
    public String getSignedUrl(String storagePath, int expiresIn) {
        try {
            // Use Supabase Storage API to create signed URL
            String signedUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/sign/property-images/" + encodeStoragePath(storagePath) + "?expiresIn=" + expiresIn;
            Log.d(TAG, "Generated signed URL request: " + signedUrl);
            return signedUrl;
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate signed URL for: " + storagePath, e);
            // Fallback to public URL
            return BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + encodeStoragePath(storagePath);
        }
    }

    /**
     * Attach Supabase authentication headers to a Request.Builder.
     * Adds the public anon API key as `apikey` and prefers the user's access_token
     * (from SharedPreferences `roominate_prefs.access_token`) for the Authorization header.
     * Falls back to the anon key as the Bearer token when no access token is available.
     */
    public static Request.Builder addAuthHeaders(Request.Builder builder) {
        // Always include apikey header
        builder.addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
        
        if (appContext != null) {
            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String accessToken = prefs.getString("access_token", null);
            if (accessToken != null && !accessToken.isEmpty()) {
                builder.addHeader("Authorization", "Bearer " + accessToken);
                return builder;
            }
        }

        // Fallback to anon key as bearer (will not work for RLS-protected writes)
        builder.addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY);
        return builder;
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
        // Instead of creating the auth user directly from the client, call a server-side
        // Edge Function that will verify the prior OTP and create the user using the
        // service_role key. This prevents requiring the service key in the client.
        String url = BuildConfig.SUPABASE_URL + FUNCTIONS_PATH + "/complete-signup";
        Log.d(TAG, "Calling complete-signup function: " + url);

        JSONObject payload = new JSONObject();
        payload.put("email", userData.getString("email"));
        payload.put("password", userData.getString("password"));
        payload.put("first_name", userData.optString("first_name", ""));
        payload.put("last_name", userData.optString("last_name", ""));
        payload.put("role", userData.optString("role", "tenant"));
        payload.put("dob", userData.optString("dob", ""));
        payload.put("phone", userData.optString("phone", ""));

        Log.d(TAG, "complete-signup payload: " + payload.toString());

        RequestBody requestBody = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
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
                    int status = response.code();
                    Log.d(TAG, "Status code: " + status);

                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response body: " + responseBody);
                    Log.d(TAG, "Request payload: " + payload.toString());
                    
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ User created SUCCESS");
                            callback.onSuccess(json);
                        } else {
                            // Try to provide more context in the error message
                            String error = json.optString(
                                    "error_description",
                                    json.optString("msg", json.optString("error", "Failed to create account"))
                            );
                            String detailed = "Status=" + status + " error=" + error + " body=" + json.toString();
                            Log.e(TAG, "❌ Create user FAILED: " + detailed);
                            callback.onError(detailed);
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

    public void createBooking(String boardingHouseId, String moveInDate, String endDate, double totalAmount, ApiCallback callback) {
        createBookingInternal(boardingHouseId, moveInDate, endDate, totalAmount, callback, false);
    }

    private void createBookingInternal(String boardingHouseId, String moveInDate, String endDate, double totalAmount, ApiCallback callback, boolean refreshAttempted) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            // Build payload
            org.json.JSONObject requestBodyJson = new org.json.JSONObject();
            // tenant_id should be the authenticated user's id stored in SharedPreferences
            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String tenantId = prefs.getString("user_id", null);

            if (tenantId == null || tenantId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            // Skip profile check - proceed directly to booking insert and let DB FK handle missing profile
                        requestBodyJson.put("listing_id", boardingHouseId);
                        requestBodyJson.put("tenant_id", tenantId);
                        requestBodyJson.put("start_date", moveInDate);
                        requestBodyJson.put("end_date", endDate);
                        requestBodyJson.put("total_amount", totalAmount);
                        requestBodyJson.put("status", "pending");

                        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings";

                        RequestBody body = RequestBody.create(
                                requestBodyJson.toString(),
                                MediaType.parse("application/json; charset=utf-8")
                        );

                        String anonKey = BuildConfig.SUPABASE_ANON_KEY;
                        String accessToken = prefs.getString("access_token", null);

                        Request.Builder reqB = new Request.Builder()
                                .url(url)
                                .post(body)
                                .addHeader("apikey", anonKey)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Prefer", "return=representation");

                        if (accessToken != null && !accessToken.isEmpty()) {
                            reqB.addHeader("Authorization", "Bearer " + accessToken);
                        } else {
                            // Fall back to anon key (will fail for RLS-protected inserts)
                            reqB.addHeader("Authorization", "Bearer " + anonKey);
                        }

                        Request request = reqB.build();

                        // Guard to ensure we only attempt refresh once on booking insert
                        final AtomicBoolean bookingRefreshTried = new AtomicBoolean(refreshAttempted);

                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                android.util.Log.e(TAG, "createBooking network failure", e);
                                callback.onError("Network error: " + e.getMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                try {
                                    if (response.isSuccessful()) {
                                        org.json.JSONArray arr = new org.json.JSONArray(responseBody);
                                        org.json.JSONObject wrapper = new org.json.JSONObject();
                                        wrapper.put("body", arr);
                                        callback.onSuccess(wrapper);
                                    } else {
                                        // detect expired JWT and attempt refresh once
                                        if ((response.code() == 401 || (responseBody != null && (responseBody.contains("PGRST303") || responseBody.toLowerCase().contains("jwt expired")))) && !bookingRefreshTried.get()) {
                                            bookingRefreshTried.set(true);
                                            android.content.SharedPreferences prefsLocal2 = appContext.getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE);
                                            String refreshToken2 = prefsLocal2.getString("refresh_token", null);
                                            if (refreshToken2 == null || refreshToken2.isEmpty()) {
                                                callback.onError("Session expired. Please sign in again.");
                                                return;
                                            }
                                            refreshSession(refreshToken2, new ApiCallback() {
                                                @Override
                                                public void onSuccess(JSONObject responseJson) {
                                                    // retry the booking insert once
                                                    createBookingInternal(boardingHouseId, moveInDate, endDate, totalAmount, callback, true);
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    callback.onError("Session refresh failed: " + error);
                                                }
                                            });
                                            return;
                                        }

                                        // If FK error (23503) or other DB constraint, surface a helpful message
                                        if (responseBody != null && responseBody.contains("23503")) {
                                            callback.onError("Database foreign key error: user profile missing (23503). Please complete signup or contact support.");
                                        } else {
                                            callback.onError("Status=" + response.code() + " body=" + responseBody);
                                        }
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e(TAG, "createBooking parse error", e);
                                    callback.onError("Failed to parse response");
                                }
                            }
                        });
        } catch (Exception e) {
            android.util.Log.e(TAG, "createBooking exception", e);
            callback.onError("Failed to create booking: " + e.getMessage());
        }
    }

    /**
     * Create a booking with custom JSON payload.
     * This overload allows flexible field submission for payment integration.
     * 
     * @param bookingData JSONObject with fields like listing_id, tenant_id, start_date, end_date, total_amount, status, payment_reference
     * @param callback ApiCallback for success/error handling
     */
    public void createBooking(org.json.JSONObject bookingData, ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String tenantId = prefs.getString("user_id", null);
            String accessToken = prefs.getString("access_token", null);

            if (tenantId == null || tenantId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            // Ensure tenant_id is set from session if not provided
            if (!bookingData.has("tenant_id")) {
                bookingData.put("tenant_id", tenantId);
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings";

            RequestBody body = RequestBody.create(
                    bookingData.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            String anonKey = BuildConfig.SUPABASE_ANON_KEY;

            Request.Builder reqB = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            if (accessToken != null && !accessToken.isEmpty()) {
                reqB.addHeader("Authorization", "Bearer " + accessToken);
            } else {
                reqB.addHeader("Authorization", "Bearer " + anonKey);
            }

            Request request = reqB.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e(TAG, "createBooking(JSONObject) network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(responseBody);
                            if (arr.length() > 0) {
                                org.json.JSONObject bookingResult = arr.getJSONObject(0);
                                org.json.JSONObject wrapper = new org.json.JSONObject();
                                wrapper.put("booking", bookingResult);
                                callback.onSuccess(wrapper);
                            } else {
                                callback.onError("Booking created but no data returned");
                            }
                        } else {
                            if (responseBody != null && responseBody.contains("23503")) {
                                callback.onError("Database foreign key error: user profile missing (23503). Please complete signup.");
                            } else {
                                callback.onError("Status=" + response.code() + " body=" + responseBody);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "createBooking(JSONObject) parse error", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "createBooking(JSONObject) exception", e);
            callback.onError("Failed to create booking: " + e.getMessage());
        }
    }

    /**
     * Generic method to invoke a Supabase Edge Function.
     *
     * @param functionName The name of the function to invoke (e.g., "lenco-payment").
     * @param body The JSON payload to send to the function.
     * @param callback The callback to handle success or error responses.
     */
    public void invokeEdgeFunction(String functionName, JSONObject body, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + FUNCTIONS_PATH + "/" + functionName;
            Log.d(TAG, "Invoking Edge Function: " + url);
            Log.d(TAG, "Payload: " + body.toString());

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json");

            // Add auth headers (API key and Bearer token)
            addAuthHeaders(requestBuilder);

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Invoke function '" + functionName + "' failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Function '" + functionName + "' response (" + response.code() + "): " + responseBody);

                    try {
                        // Handle cases where the function returns a non-JSON success response (e.g., just a string)
                        if (response.isSuccessful() && (responseBody.isEmpty() || !responseBody.trim().startsWith("{"))) {
                            JSONObject successWrapper = new JSONObject();
                            successWrapper.put("message", responseBody);
                            callback.onSuccess(successWrapper);
                            return;
                        }
                        
                        JSONObject json = new JSONObject(responseBody);

                        if (response.isSuccessful()) {
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString("error", "Unknown error from function " + functionName);
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error for function '" + functionName + "' response", e);
                        callback.onError("Failed to parse response: " + responseBody);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Invoke function '" + functionName + "' exception", e);
            callback.onError("Failed to invoke function: " + e.getMessage());
        }
    }

    /**
     * Callback interface for API responses
     */
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    /**
     * Sign in with email + password. Returns the token response which includes access_token and user.
     */
    public void signIn(String email, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader(
                            "Authorization",
                            "Bearer " + BuildConfig.SUPABASE_ANON_KEY
                    )
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SignIn NETWORK FAILURE", e);
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
                            String error = json.optString(
                                    "error_description",
                                    json.optString("msg", json.optString("error", "Failed to sign in"))
                            );
                            callback.onError(
                                    "Status=" + response.code() + " error=" + error + " body=" + responseBody
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "SignIn parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "SignIn exception", e);
            callback.onError("Failed to sign in: " + e.getMessage());
        }
    }

    /**
     * Request a password reset email for the given email address.
     * Supabase will send an email with a password reset link to the user.
     */
    public void resetPassword(String email, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);

            String url = BuildConfig.SUPABASE_URL + "/auth/v1/recover";

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "ResetPassword NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (response.isSuccessful()) {
                            // Supabase returns 200 even if email doesn't exist for security reasons
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString(
                                    "error_description",
                                    json.optString("msg", json.optString("error", "Failed to send reset email"))
                            );
                            callback.onError("Status=" + response.code() + " error=" + error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "ResetPassword parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "ResetPassword exception", e);
            callback.onError("Failed to reset password: " + e.getMessage());
        }
    }

    /**
     * Verify OTP code for password reset or email verification.
     * 
     * @param email The user's email address
     * @param token The OTP code received via email
     * @param type The type of verification ("recovery" for password reset, "signup" for email verification)
     * @param callback The callback to handle success or error responses
     */
    public void verifyOTP(String email, String token, String type, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("token", token);
            body.put("type", type != null ? type : "recovery");

            String url = BuildConfig.SUPABASE_URL + "/auth/v1/verify";

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "VerifyOTP NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "VerifyOTP response: " + response.code() + " - " + responseBody);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (response.isSuccessful()) {
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString(
                                    "error_description",
                                    json.optString("msg", json.optString("error", "Invalid verification code"))
                            );
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "VerifyOTP parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "VerifyOTP exception", e);
            callback.onError("Failed to verify OTP: " + e.getMessage());
        }
    }

    /**
     * Update user's password using an access token from OTP verification.
     * 
     * @param accessToken The access token obtained from verifyOTP
     * @param newPassword The new password to set
     * @param callback The callback to handle success or error responses
     */
    public void updatePassword(String accessToken, String newPassword, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("password", newPassword);

            String url = BuildConfig.SUPABASE_URL + "/auth/v1/user";

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "UpdatePassword NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "UpdatePassword response: " + response.code() + " - " + responseBody);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (response.isSuccessful()) {
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString(
                                    "error_description",
                                    json.optString("msg", json.optString("error", "Failed to update password"))
                            );
                            callback.onError(error);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "UpdatePassword parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "UpdatePassword exception", e);
            callback.onError("Failed to update password: " + e.getMessage());
        }
    }

    /**
     * Refresh the Supabase access token using a stored refresh_token.
     * Writes new access_token/refresh_token to SharedPreferences on success and calls the callback.
     * This method prevents duplicate parallel refreshes by returning the refreshed tokens to
     * any callers that arrive while a refresh is already in progress.
     */
    public void refreshSession(String refreshToken, ApiCallback callback) {
        if (appContext == null) {
            callback.onError("Supabase client not initialized");
            return;
        }

        // If another refresh is already running, wait a short while for it to complete and
        // return the updated tokens from SharedPreferences.
        if (!isRefreshing.compareAndSet(false, true)) {
            // Another refresh in progress — poll SharedPreferences for updated token
            new Thread(() -> {
                try {
                    String prevAccess = appContext.getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE).getString("access_token", null);
                    int attempts = 0;
                    while (attempts < 20) {
                        Thread.sleep(200);
                        String newAccess = appContext.getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE).getString("access_token", null);
                        if (newAccess != null && !newAccess.equals(prevAccess)) {
                            org.json.JSONObject resp = new org.json.JSONObject();
                            resp.put("access_token", newAccess);
                            resp.put("refresh_token", appContext.getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE).getString("refresh_token", ""));
                            isRefreshing.set(false);
                            callback.onSuccess(resp);
                            return;
                        }
                        attempts++;
                    }
                    isRefreshing.set(false);
                    callback.onError("Refresh in progress timed out");
                } catch (Exception e) {
                    isRefreshing.set(false);
                    callback.onError("Refresh wait failed: " + e.getMessage());
                }
            }).start();
            return;
        }

        try {
            String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token";

            okhttp3.FormBody form = new okhttp3.FormBody.Builder()
                    .add("refresh_token", refreshToken)
                    .add("grant_type", "refresh_token")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(form)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    isRefreshing.set(false);
                    Log.e(TAG, "refreshSession network failure", e);
                    callback.onError("Network error while refreshing session: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        if (response.isSuccessful()) {
                            // Persist new tokens
                            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", Context.MODE_PRIVATE);
                            android.content.SharedPreferences.Editor editor = prefs.edit();
                            if (json.has("access_token")) editor.putString("access_token", json.optString("access_token"));
                            if (json.has("refresh_token")) editor.putString("refresh_token", json.optString("refresh_token"));
                            // If user object present, persist user id
                            if (json.has("user")) {
                                org.json.JSONObject user = json.optJSONObject("user");
                                if (user != null && user.has("id")) {
                                    editor.putString("user_id", user.optString("id"));
                                }
                            }
                            editor.apply();

                            isRefreshing.set(false);
                            callback.onSuccess(json);
                        } else {
                            isRefreshing.set(false);
                            String err = json.optString("error_description", json.optString("error", body));
                            callback.onError("Failed to refresh session: " + err);
                        }
                    } catch (Exception e) {
                        isRefreshing.set(false);
                        Log.e(TAG, "refreshSession parse error", e);
                        callback.onError("Failed to parse refresh response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            isRefreshing.set(false);
            Log.e(TAG, "refreshSession exception", e);
            callback.onError("Failed to refresh session: " + e.getMessage());
        }
    }

    /**
     * Fetch profile from public.users by auth uid (user id). Returns JSON array of matching profiles.
     */
    public void getUserProfile(String userId, String accessToken, ApiCallback callback) {
        try {
            // Fetch from profiles table instead of users table
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=*";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Accept", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getUserProfile NETWORK FAILURE", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    try {
                        // Supabase returns an array for REST select
                        JSONArray arr = new JSONArray(responseBody);
                        JSONObject result = new JSONObject();
                        
                        if (arr.length() > 0) {
                            // Return the first profile object directly
                            result = arr.getJSONObject(0);
                        }
                        
                        if (response.isSuccessful()) {
                            callback.onSuccess(result);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getUserProfile parse error", e);
                        callback.onError("Failed to parse profile response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getUserProfile exception", e);
            callback.onError("Failed to fetch profile: " + e.getMessage());
        }
    }

    // --- Backwards-compatible wrappers used across the app ---
    /**
     * Legacy wrapper: requestOtp -> sendOtp
     */
    public void requestOtp(String email, ApiCallback callback) {
        sendOtp(email, callback);
    }

    /**
     * Legacy wrapper: completeSignup used by several activities.
     * Builds a payload and forwards to createUser which calls the edge function.
     */
    public void completeSignup(String email, String password, String otp, String firstName, String lastName, String phone, String role, org.json.JSONObject extra, ApiCallback callback) {
        try {
            org.json.JSONObject u = new org.json.JSONObject();
            u.put("email", email);
            u.put("password", password);
            u.put("first_name", firstName != null ? firstName : "");
            u.put("last_name", lastName != null ? lastName : "");
            u.put("phone", phone != null ? phone : "");
            u.put("role", role != null ? role : "tenant");
            // otp may be required by the edge function; include if present
            if (otp != null) u.put("otp", otp);
            if (extra != null) {
                // merge extra fields
                org.json.JSONArray names = extra.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String key = names.getString(i);
                        u.put(key, extra.opt(key));
                    }
                }
            }
            createUser(u, callback);
        } catch (Exception e) {
            Log.e(TAG, "completeSignup error", e);
            callback.onError("Failed to complete signup: " + e.getMessage());
        }
    }

    /**
     * Legacy wrapper: signUpUser used by owner signup flows
     */
    public void signUpUser(String email, String password, String role, String firstName, String lastName, ApiCallback callback) {
        try {
            org.json.JSONObject u = new org.json.JSONObject();
            u.put("email", email);
            u.put("password", password);
            u.put("first_name", firstName != null ? firstName : "");
            u.put("last_name", lastName != null ? lastName : "");
            u.put("role", role != null ? role : "owner");
            createUser(u, callback);
        } catch (Exception e) {
            Log.e(TAG, "signUpUser error", e);
            callback.onError("Failed to sign up user: " + e.getMessage());
        }
    }

    /**
     * Legacy wrapper: getUserProfile() with no args reads stored session and forwards
     */
    public void getUserProfile(ApiCallback callback) {
        if (appContext == null) {
            callback.onError("Supabase client not initialized");
            return;
        }
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String accessToken = prefs.getString("access_token", null);
        if (userId == null || accessToken == null) {
            callback.onError("User not signed in");
            return;
        }
        getUserProfile(userId, accessToken, callback);
    }
    
    /**
     * Update user profile in profiles table
     */
    public void updateUserProfile(org.json.JSONObject updateData, ApiCallback callback) {
        if (appContext == null) {
            callback.onError("Supabase client not initialized");
            return;
        }
        
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null) {
            callback.onError("User not signed in");
            return;
        }
        
        try {
            // Update profiles table
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;
            RequestBody rbBody = RequestBody.create(updateData.toString(), MediaType.parse("application/json; charset=utf-8"));
            
            Request.Builder rb = new Request.Builder()
                .url(url)
                .patch(rbBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);
            
            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "updateUserProfile network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            Log.e(TAG, "updateUserProfile failed: " + response.code() + " - " + body);
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updateUserProfile parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "updateUserProfile exception", e);
            callback.onError("Failed to update profile: " + e.getMessage());
        }
    }

    /**
     * Fetch properties for the current owner (wrapper used by OwnerHomeFragment)
     */
    public void getPropertiesByOwner(ApiCallback callback) {
        if (appContext == null) {
            callback.onError("Supabase client not initialized");
            return;
        }
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
        String ownerId = prefs.getString("user_id", null);
        if (ownerId == null) {
            callback.onError("User not signed in");
            return;
        }

        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?owner_id=eq." + ownerId + "&select=*";
            Request.Builder rb = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
            addAuthHeaders(rb);
            Request request = rb.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getPropertiesByOwner network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        org.json.JSONArray propertiesArray = new org.json.JSONArray(body);
                        // Fetch images for each property and attach them
                        fetchImagesForProperties(propertiesArray, new ApiCallback() {
                            @Override
                            public void onSuccess(org.json.JSONObject result) {
                                org.json.JSONObject wrapper = new org.json.JSONObject();
                                try {
                                    wrapper.put("data", result.optJSONArray("properties_with_images"));
                                    callback.onSuccess(wrapper);
                                } catch (org.json.JSONException e) {
                                    callback.onError("Error processing images: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onError(String error) {
                                // Still return properties even if image fetching fails
                                org.json.JSONObject wrapper = new org.json.JSONObject();
                                try {
                                    wrapper.put("data", propertiesArray);
                                    callback.onSuccess(wrapper);
                                } catch (org.json.JSONException e) {
                                    callback.onError("Error: " + e.getMessage());
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "getPropertiesByOwner parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getPropertiesByOwner exception", e);
            callback.onError("Failed to fetch properties: " + e.getMessage());
        }
    }
    
    /**
     * Fetch images from properties_media table and attach them to properties
     */
    private void fetchImagesForProperties(org.json.JSONArray properties, ApiCallback callback) {
        try {
            org.json.JSONArray propertyIds = new org.json.JSONArray();
            for (int i = 0; i < properties.length(); i++) {
                propertyIds.put(properties.getJSONObject(i).optString("id"));
            }
            
            if (propertyIds.length() == 0) {
                org.json.JSONObject result = new org.json.JSONObject();
                result.put("properties_with_images", properties);
                callback.onSuccess(result);
                return;
            }
            
            // Fetch all images for these properties
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/properties_media?listing_id=in.(" 
                + propertyIds.join(",") + ")&select=listing_id,url&order=ordering";
            Log.d(TAG, "Fetching images from: " + url);
            
            Request.Builder rb = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
            addAuthHeaders(rb);
            Request request = rb.build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "fetchImagesForProperties network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "[]";
                    try {
                        org.json.JSONArray mediaArray = new org.json.JSONArray(body);
                        
                        // Group images by listing_id
                        java.util.Map<String, org.json.JSONArray> imagesByListing = new java.util.HashMap<>();
                        for (int i = 0; i < mediaArray.length(); i++) {
                            org.json.JSONObject media = mediaArray.getJSONObject(i);
                            String listingId = media.optString("listing_id");
                            String imageUrl = media.optString("url");
                            
                            // Convert storage path to full URL if needed
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                    // Already a full URL - extract the storage path and re-encode it
                                    String bucketPrefix = "/storage/v1/object/public/property-images/";
                                    int pathStart = imageUrl.indexOf(bucketPrefix);
                                    if (pathStart != -1) {
                                        String storagePath = imageUrl.substring(pathStart + bucketPrefix.length());
                                        imageUrl = BuildConfig.SUPABASE_URL + bucketPrefix + encodeStoragePath(storagePath);
                                        Log.d(TAG, "Re-encoded full URL: " + imageUrl);
                                    }
                                } else {
                                    // It's a storage path, convert to public URL and ensure safe encoding
                                    imageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + encodeStoragePath(imageUrl);
                                    Log.d(TAG, "Converted storage path to URL: " + imageUrl);
                                }
                            }
                            
                            if (!imagesByListing.containsKey(listingId)) {
                                imagesByListing.put(listingId, new org.json.JSONArray());
                            }
                            imagesByListing.get(listingId).put(imageUrl);
                        }
                        
                        // Attach images to properties
                        for (int i = 0; i < properties.length(); i++) {
                            org.json.JSONObject prop = properties.getJSONObject(i);
                            String propId = prop.optString("id");
                            org.json.JSONArray images = imagesByListing.getOrDefault(propId, new org.json.JSONArray());
                            prop.put("images", images);
                            Log.d(TAG, "Property " + propId + " has " + images.length() + " images");
                        }
                        
                        org.json.JSONObject result = new org.json.JSONObject();
                        result.put("properties_with_images", properties);
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        Log.e(TAG, "fetchImagesForProperties parse error", e);
                        callback.onError("Failed to parse images: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchImagesForProperties exception", e);
            callback.onError("Error fetching images: " + e.getMessage());
        }
    }

    /**
     * Public method to fetch images for properties
     * Used by HomeFragment and other views to attach images from properties_media table
     */
    public void fetchImagesForPropertiesStatic(JSONArray properties, ApiCallback callback) {
        fetchImagesForProperties(properties, callback);
    }

    /**
     * Legacy: getPropertyById
     */
    public void getPropertyById(String propertyId, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?id=eq." + propertyId + "&select=*";
            Request.Builder rb = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
            addAuthHeaders(rb);
            Request request = rb.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getPropertyById network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        org.json.JSONArray propertiesArray = new org.json.JSONArray(body);
                        
                        // Fetch images for this property
                        if (propertiesArray.length() > 0) {
                            fetchImagesForProperties(propertiesArray, new ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject result) {
                                    org.json.JSONObject wrapper = new org.json.JSONObject();
                                    try {
                                        wrapper.put("data", result.optJSONArray("properties_with_images"));
                                        callback.onSuccess(wrapper);
                                    } catch (org.json.JSONException e) {
                                        callback.onError("Error processing images: " + e.getMessage());
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    // Still return property even if image fetching fails
                                    org.json.JSONObject wrapper = new org.json.JSONObject();
                                    try {
                                        wrapper.put("data", propertiesArray);
                                        callback.onSuccess(wrapper);
                                    } catch (org.json.JSONException e) {
                                        callback.onError("Error: " + e.getMessage());
                                    }
                                }
                            });
                        } else {
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", propertiesArray);
                            callback.onSuccess(wrapper);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getPropertyById parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getPropertyById exception", e);
            callback.onError("Failed to fetch property: " + e.getMessage());
        }
    }

    /**
     * Get property images from properties_media table
     */
    public void getPropertyImages(String propertyId, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/properties_media?listing_id=eq." + propertyId + "&order=ordering.asc,created_at.asc&select=*";
            Request.Builder rb = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
            addAuthHeaders(rb);
            Request request = rb.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getPropertyImages network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        org.json.JSONObject wrapper = new org.json.JSONObject();
                        wrapper.put("data", new org.json.JSONArray(body));
                        if (response.isSuccessful()) {
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getPropertyImages parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getPropertyImages exception", e);
            callback.onError("Failed to fetch images: " + e.getMessage());
        }
    }

    /**
     * Get the primary/first image URL for a property (synchronous - use carefully!)
     * Returns null if no image found
     */
    public String getPropertyThumbnailSync(String propertyId) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/properties_media?listing_id=eq." + propertyId + "&order=ordering.asc,created_at.asc&select=url&limit=1";
            Request.Builder rb = new Request.Builder().url(url).get().addHeader("Accept", "application/json");
            addAuthHeaders(rb);
            Request request = rb.build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                Log.d(TAG, "getPropertyThumbnailSync response: " + body);
                
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(body);
                    if (arr.length() > 0) {
                        String imageUrl = arr.getJSONObject(0).optString("url", null);
                        Log.d(TAG, "getPropertyThumbnailSync found raw URL/path: " + imageUrl);
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Check if it's already a full URL
                            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                // Extract the storage path from the full URL and re-encode it
                                try {
                                    String storagePrefix = "/storage/v1/object/public/property-images/";
                                    int pathStartIndex = imageUrl.indexOf(storagePrefix);
                                    if (pathStartIndex != -1) {
                                        String storagePath = imageUrl.substring(pathStartIndex + storagePrefix.length());
                                        String publicUrl = BuildConfig.SUPABASE_URL + storagePrefix + encodeStoragePath(storagePath);
                                        Log.d(TAG, "Re-encoded existing URL. Original: " + imageUrl + ", New: " + publicUrl);
                                        return publicUrl;
                                    } else {
                                        Log.w(TAG, "Unexpected URL format, returning as-is: " + imageUrl);
                                        return imageUrl;
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error re-encoding URL: " + imageUrl, e);
                                    return imageUrl;
                                }
                            } else {
                                // It's a storage path, convert to public URL
                                String publicUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + encodeStoragePath(imageUrl);
                                Log.d(TAG, "Converted to public URL: " + publicUrl);
                                return publicUrl;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getPropertyThumbnailSync parse error for body: " + body, e);
                }
            } else {
                Log.e(TAG, "getPropertyThumbnailSync failed: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "getPropertyThumbnailSync error", e);
        }
        return null;
    }
    
    /**
     * Upload a property image to Supabase Storage and insert into properties_media table
     */
    public void uploadPropertyImage(String propertyId, android.net.Uri imageUri, boolean isPrimary, ApiCallback callback) {
        new Thread(() -> {
            try {
                if (appContext == null) {
                    callback.onError("Context not initialized");
                    return;
                }
                
                android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
                String userId = prefs.getString("user_id", null);
                
                if (userId == null) {
                    callback.onError("User not logged in");
                    return;
                }
                
                // Generate unique filename
                String fileName = "property_" + java.util.UUID.randomUUID().toString() + ".jpg";
                String storagePath = "properties/" + userId + "/" + fileName;
                
                // Read image data
                java.io.InputStream inputStream = appContext.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    callback.onError("Failed to read image");
                    return;
                }
                
                java.io.File tempFile = java.io.File.createTempFile("upload", ".jpg", appContext.getCacheDir());
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                inputStream.close();
                outputStream.close();
                
                // Read file into bytes
                byte[] fileBytes = new byte[(int) tempFile.length()];
                java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
                fis.read(fileBytes);
                fis.close();
                
                // Upload to Supabase Storage
                RequestBody requestBody = RequestBody.create(fileBytes, MediaType.parse("image/jpeg"));
                
                OkHttpClient uploadClient = new OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                
                String uploadUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/property-images/" + storagePath;
                Log.d(TAG, "Uploading image to: " + uploadUrl);
                
                Request.Builder reqBuilder = new Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("Content-Type", "image/jpeg");
                addAuthHeaders(reqBuilder);
                
                Request uploadRequest = reqBuilder.build();
                Response uploadResponse = uploadClient.newCall(uploadRequest).execute();
                
                tempFile.delete();
                
                if (!uploadResponse.isSuccessful()) {
                    String errorBody = uploadResponse.body() != null ? uploadResponse.body().string() : "Unknown error";
                    Log.e(TAG, "Upload failed: " + uploadResponse.code() + " - " + errorBody);
                    callback.onError("Upload failed: " + errorBody);
                    return;
                }
                
                uploadResponse.close();
                
                // Store just the storage path in the database, not the full URL
                // We'll construct the public URL at read time
                String storagePath_only = storagePath;
                Log.d(TAG, "Image uploaded successfully to storage path: " + storagePath_only);
                
                // Insert into properties_media table
                org.json.JSONObject mediaRecord = new org.json.JSONObject();
                mediaRecord.put("listing_id", propertyId);  // Changed from property_id
                mediaRecord.put("url", storagePath_only);   // Store just the path, not the full URL
                mediaRecord.put("filename", fileName);
                mediaRecord.put("mime_type", "image/jpeg");
                mediaRecord.put("ordering", isPrimary ? 0 : 1);  // Primary images get ordering 0
                
                String insertUrl = BuildConfig.SUPABASE_URL + "/rest/v1/properties_media";
                RequestBody insertBody = RequestBody.create(mediaRecord.toString(), MediaType.parse("application/json"));
                
                Request.Builder insertBuilder = new Request.Builder()
                    .url(insertUrl)
                    .post(insertBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
                addAuthHeaders(insertBuilder);
                
                Request insertRequest = insertBuilder.build();
                Response insertResponse = client.newCall(insertRequest).execute();
                
                if (insertResponse.isSuccessful()) {
                    String body = insertResponse.body() != null ? insertResponse.body().string() : "[]";
                    org.json.JSONArray arr = new org.json.JSONArray(body);
                    org.json.JSONObject wrapper = new org.json.JSONObject();
                    wrapper.put("data", arr);
                    // Construct full public URL for return (ensure encoded path)
                    String publicUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/public/property-images/" + encodeStoragePath(storagePath_only);
                    wrapper.put("image_url", publicUrl);
                    callback.onSuccess(wrapper);
                } else {
                    String errorBody = insertResponse.body() != null ? insertResponse.body().string() : "Unknown error";
                    Log.e(TAG, "Failed to insert media record: " + insertResponse.code() + " - " + errorBody);
                    callback.onError("Failed to save image record: " + errorBody);
                }
                
                insertResponse.close();
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading property image", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Insert property (POST to boarding_houses)
     */
    public void insertProperty(com.roominate.models.Property property, ApiCallback callback) {
        try {
            org.json.JSONObject payload = property.toJson();
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses";
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .post(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "insertProperty network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "insertProperty parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "insertProperty exception", e);
            callback.onError("Failed to insert property: " + e.getMessage());
        }
    }

    /**
     * Update property (PATCH to boarding_houses?id=eq.<id>)
     */
    public void updateProperty(com.roominate.models.Property property, ApiCallback callback) {
        try {
            if (property.getId() == null || property.getId().isEmpty()) {
                callback.onError("Property id is required for update");
                return;
            }
            org.json.JSONObject payload = property.toJson();
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?id=eq." + property.getId();
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "updateProperty network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updateProperty parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "updateProperty exception", e);
            callback.onError("Failed to update property: " + e.getMessage());
        }
    }

    /**
     * Fetch bookings for the current owner based on their properties
     * Uses owner_id field which is set by DB trigger from boarding_houses.owner_id
     */
    public void getOwnerBookings(ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String ownerId = prefs.getString("user_id", null);

            if (ownerId == null || ownerId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            // Query bookings with owner_id and join with boarding_houses for property details
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?owner_id=eq." + ownerId + "&select=*,boarding_houses(id,title,address)&order=created_at.desc";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getOwnerBookings network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getOwnerBookings parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getOwnerBookings exception", e);
            callback.onError("Failed to fetch bookings: " + e.getMessage());
        }
    }

    /**
     * Fetch bookings for the current tenant (user as tenant_id)
     */
    public void getTenantBookings(String status, ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String tenantId = prefs.getString("user_id", null);
            String accessToken = prefs.getString("access_token", null);

            // DEBUG LOGGING
            Log.d(TAG, "getTenantBookings - Tenant ID from prefs: " + tenantId);
            Log.d(TAG, "getTenantBookings - Access Token exists: " + (accessToken != null && !accessToken.isEmpty()));
            if (accessToken != null && accessToken.length() > 50) {
                Log.d(TAG, "getTenantBookings - Token preview: " + accessToken.substring(0, 50) + "...");
            }

            if (tenantId == null || tenantId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            // Build URL with status filter if not "all"
            String url;
            if (status == null || status.isEmpty() || status.equals("all")) {
                url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?tenant_id=eq." + tenantId + "&select=*,boarding_houses(id,title,address)&order=created_at.desc";
            } else {
                url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?tenant_id=eq." + tenantId + "&status=eq." + status + "&select=*,boarding_houses(id,title,address)&order=created_at.desc";
            }
            
            Log.d(TAG, "getTenantBookings - Request URL: " + url);

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getTenantBookings network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "getTenantBookings - Response code: " + response.code());
                    Log.d(TAG, "getTenantBookings - Response body length: " + body.length());
                    if (body.length() < 500) {
                        Log.d(TAG, "getTenantBookings - Response body: " + body);
                    }
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            Log.d(TAG, "getTenantBookings - Found " + arr.length() + " bookings");
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            Log.e(TAG, "getTenantBookings - HTTP error: " + response.code());
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getTenantBookings parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getTenantBookings exception", e);
            callback.onError("Failed to fetch bookings: " + e.getMessage());
        }
    }

    /**
     * Update booking status (PATCH to bookings?id=eq.<id>)
     */
    public void updateBookingStatus(String bookingId, String newStatus, ApiCallback callback) {
        try {
            if (bookingId == null || bookingId.isEmpty()) {
                callback.onError("Booking ID is required");
                return;
            }

            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("status", newStatus);
            payload.put("updated_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?id=eq." + bookingId;
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "updateBookingStatus network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updateBookingStatus parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "updateBookingStatus exception", e);
            callback.onError("Failed to update booking: " + e.getMessage());
        }
    }

    /**
     * Cancel booking with reason (PATCH to bookings?id=eq.<id>)
     */
    public void cancelBooking(String bookingId, String cancellationReason, ApiCallback callback) {
        try {
            if (bookingId == null || bookingId.isEmpty()) {
                callback.onError("Booking ID is required");
                return;
            }

            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("status", "cancelled");
            payload.put("cancellation_reason", cancellationReason != null ? cancellationReason : "");
            payload.put("cancellation_date", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            payload.put("updated_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?id=eq." + bookingId;
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "cancelBooking network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "cancelBooking parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "cancelBooking exception", e);
            callback.onError("Failed to cancel booking: " + e.getMessage());
        }
    }

    /**
     * Add property to favorites (INSERT into favorites)
     */
    public void addFavorite(String propertyId, ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("user_id", userId);
            payload.put("listing_id", propertyId);

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites";
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .post(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "addFavorite network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "addFavorite parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "addFavorite exception", e);
            callback.onError("Failed to add favorite: " + e.getMessage());
        }
    }

    /**
     * Remove property from favorites (DELETE from favorites)
     */
    public void removeFavorite(String propertyId, ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId + "&listing_id=eq." + propertyId;

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "removeFavorite network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("message", "Favorite removed");
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "removeFavorite parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "removeFavorite exception", e);
            callback.onError("Failed to remove favorite: " + e.getMessage());
        }
    }

    /**
     * Get user's favorite properties (GET favorites with join to boarding_houses)
     */
    public void getFavorites(ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId + "&select=*,boarding_houses(*)&order=created_at.desc";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getFavorites network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getFavorites parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getFavorites exception", e);
            callback.onError("Failed to fetch favorites: " + e.getMessage());
        }
    }

    /**
     * Submit a review for a property (INSERT into reviews)
     */
    public void submitReview(String propertyId, int rating, String comment, ApiCallback callback) {
        try {
            if (appContext == null) {
                Log.e(TAG, "submitReview: appContext is null");
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "submitReview: userId is null or empty");
                callback.onError("User not signed in");
                return;
            }

            if (rating < 1 || rating > 5) {
                Log.e(TAG, "submitReview: Invalid rating: " + rating);
                callback.onError("Rating must be between 1 and 5");
                return;
            }

            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("listing_id", propertyId);
            payload.put("reviewer_id", userId);
            payload.put("rating", rating);
            payload.put("comment", comment != null ? comment : "");

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews";
            Log.d(TAG, "submitReview URL: " + url);
            Log.d(TAG, "submitReview payload: " + payload.toString());
            
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .post(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "submitReview network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "submitReview response code: " + response.code());
                    Log.d(TAG, "submitReview response body: " + body);
                    
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("data", arr);
                            Log.d(TAG, "submitReview success");
                            callback.onSuccess(wrapper);
                        } else {
                            String errorMsg = "Status=" + response.code() + " body=" + body;
                            Log.e(TAG, "submitReview failed: " + errorMsg);
                            callback.onError(errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "submitReview parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "submitReview exception", e);
            callback.onError("Failed to submit review: " + e.getMessage());
        }
    }

    /**
     * Get reviews for a property (GET reviews with user details)
     */
    public void getReviews(String propertyId, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/reviews?listing_id=eq." + propertyId + "&select=*,users(id,display_name,avatar_url)&order=created_at.desc";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getReviews network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getReviews parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getReviews exception", e);
            callback.onError("Failed to fetch reviews: " + e.getMessage());
        }
    }

    /**
     * Search properties by location, price range, and other filters
     */
    public void searchProperties(String searchQuery, Double minPrice, Double maxPrice, String location, ApiCallback callback) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?");
            
            // Add filters
            if (searchQuery != null && !searchQuery.isEmpty()) {
                urlBuilder.append("or=(name.ilike.*").append(searchQuery).append("*,description.ilike.*").append(searchQuery).append("*)&");
            }
            
            if (minPrice != null) {
                urlBuilder.append("price_per_month=gte.").append(minPrice).append("&");
            }
            
            if (maxPrice != null) {
                urlBuilder.append("price_per_month=lte.").append(maxPrice).append("&");
            }
            
            if (location != null && !location.isEmpty()) {
                urlBuilder.append("address.ilike.*").append(location).append("*&");
            }
            
            // Only show available properties and include basic info
            urlBuilder.append("available=eq.true&select=id,name,description,address,price_per_month,available_rooms,latitude,longitude&order=created_at.desc");

            String url = urlBuilder.toString();
            Log.d(TAG, "Search URL: " + url);

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "searchProperties network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "searchProperties parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "searchProperties exception", e);
            callback.onError("Failed to search properties: " + e.getMessage());
        }
    }

    /**
     * Get owner dashboard statistics
     */
    public void getOwnerStats(ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String ownerId = prefs.getString("user_id", null);

            if (ownerId == null || ownerId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            // Fetch properties count, bookings count, and total revenue
            String propertiesUrl = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?owner_id=eq." + ownerId + "&select=count";
            String bookingsUrl = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?owner_id=eq." + ownerId + "&select=status,total_amount";

            org.json.JSONObject stats = new org.json.JSONObject();
            final AtomicBoolean propertiesDone = new AtomicBoolean(false);
            final AtomicBoolean bookingsDone = new AtomicBoolean(false);

            // Fetch properties count
            Request.Builder propertiesRb = new Request.Builder()
                    .url(propertiesUrl)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "count=exact");
            addAuthHeaders(propertiesRb);

            client.newCall(propertiesRb.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getOwnerStats properties failure", e);
                    callback.onError("Failed to fetch properties count");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String contentRange = response.header("Content-Range");
                        int propertiesCount = 0;
                        if (contentRange != null && contentRange.contains("/")) {
                            String[] parts = contentRange.split("/");
                            propertiesCount = Integer.parseInt(parts[1]);
                        }
                        stats.put("properties_count", propertiesCount);
                        propertiesDone.set(true);

                        if (bookingsDone.get()) {
                            callback.onSuccess(stats);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getOwnerStats properties parse error", e);
                        callback.onError("Failed to parse properties count");
                    }
                }
            });

            // Fetch bookings data
            Request.Builder bookingsRb = new Request.Builder()
                    .url(bookingsUrl)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(bookingsRb);

            client.newCall(bookingsRb.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getOwnerStats bookings failure", e);
                    callback.onError("Failed to fetch bookings data");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        org.json.JSONArray bookings = new org.json.JSONArray(body);
                        int totalBookings = bookings.length();
                        int pendingBookings = 0;
                        int completedBookings = 0;
                        double totalRevenue = 0;

                        for (int i = 0; i < bookings.length(); i++) {
                            org.json.JSONObject booking = bookings.getJSONObject(i);
                            String status = booking.optString("status", "");
                            double amount = booking.optDouble("total_amount", 0);

                            if ("pending".equals(status)) pendingBookings++;
                            if ("completed".equals(status)) {
                                completedBookings++;
                                totalRevenue += amount;
                            }
                        }

                        stats.put("total_bookings", totalBookings);
                        stats.put("pending_bookings", pendingBookings);
                        stats.put("completed_bookings", completedBookings);
                        stats.put("total_revenue", totalRevenue);
                        bookingsDone.set(true);

                        if (propertiesDone.get()) {
                            callback.onSuccess(stats);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getOwnerStats bookings parse error", e);
                        callback.onError("Failed to parse bookings data");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getOwnerStats exception", e);
            callback.onError("Failed to fetch owner stats: " + e.getMessage());
        }
    }

    /**
     * Check if property is in user's favorites
     */
    public void isFavorite(String propertyId, ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId + "&listing_id=eq." + propertyId;

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "isFavorite network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray arr = new org.json.JSONArray(body);
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("is_favorite", arr.length() > 0);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "isFavorite parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "isFavorite exception", e);
            callback.onError("Failed to check favorite status: " + e.getMessage());
        }
    }

    /**
     * Create a notification (INSERT into notifications table)
     */
    public void createNotification(String userId, String title, String message, String type, String relatedId, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("user_id", userId);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("type", type);
            payload.put("related_id", relatedId);
            payload.put("is_read", false);

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications";
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .post(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "createNotification network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            JSONArray arr = new JSONArray(body);
                            JSONObject wrapper = new JSONObject();
                            wrapper.put("data", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "createNotification parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "createNotification exception", e);
            callback.onError("Failed to create notification: " + e.getMessage());
        }
    }

    /**
     * Get user's notifications
     */
    public void getNotifications(ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?user_id=eq." + userId + "&select=*&order=created_at.desc";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getNotifications network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            JSONArray arr = new JSONArray(body);
                            JSONObject wrapper = new JSONObject();
                            wrapper.put("body", arr);
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getNotifications parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getNotifications exception", e);
            callback.onError("Failed to fetch notifications: " + e.getMessage());
        }
    }

    /**
     * Mark notification as read
     */
    public void markNotificationAsRead(String notificationId, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("is_read", true);

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?id=eq." + notificationId;
            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(rbBody)
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "markNotificationAsRead network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            JSONObject wrapper = new JSONObject();
                            wrapper.put("message", "Notification marked as read");
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "markNotificationAsRead parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "markNotificationAsRead exception", e);
            callback.onError("Failed to mark notification as read: " + e.getMessage());
        }
    }

    /**
     * Get unread notifications count
     */
    public void getUnreadNotificationsCount(ApiCallback callback) {
        try {
            if (appContext == null) {
                callback.onError("Supabase client not initialized");
                return;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null || userId.isEmpty()) {
                callback.onError("User not signed in");
                return;
            }

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?user_id=eq." + userId + "&is_read=eq.false&select=count";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "count=exact");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getUnreadNotificationsCount network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String contentRange = response.header("Content-Range");
                        int count = 0;
                        if (contentRange != null && contentRange.contains("/")) {
                            String[] parts = contentRange.split("/");
                            count = Integer.parseInt(parts[1]);
                        }
                        JSONObject wrapper = new JSONObject();
                        wrapper.put("count", count);
                        callback.onSuccess(wrapper);
                    } catch (Exception e) {
                        Log.e(TAG, "getUnreadNotificationsCount parse error", e);
                        callback.onError("Failed to parse count");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getUnreadNotificationsCount exception", e);
            callback.onError("Failed to fetch unread count: " + e.getMessage());
        }
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(String notificationId, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/notifications?id=eq." + notificationId;

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/json");
            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "deleteNotification network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            JSONObject wrapper = new JSONObject();
                            wrapper.put("message", "Notification deleted");
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "deleteNotification parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "deleteNotification exception", e);
            callback.onError("Failed to delete notification: " + e.getMessage());
        }
    }

    /**
     * Sign out the current user. Calls Supabase auth logout endpoint and clears stored session.
     */
    public void signOut(ApiCallback callback) {
        if (appContext == null) {
            callback.onError("Supabase client not initialized");
            return;
        }
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        try {
            String url = BuildConfig.SUPABASE_URL + "/auth/v1/logout";
            RequestBody rbBody = RequestBody.create("", MediaType.parse("application/json"));
            Request.Builder rb = new Request.Builder().url(url).post(rbBody).addHeader("Content-Type", "application/json");
            if (accessToken != null && !accessToken.isEmpty()) {
                rb.addHeader("Authorization", "Bearer " + accessToken);
            }
            rb.addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "signOut network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Regardless of response, clear local session
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("access_token");
                    editor.remove("user_id");
                    editor.remove("user_data");
                    editor.apply();

                    if (response.isSuccessful()) {
                        callback.onSuccess(new org.json.JSONObject());
                    } else {
                        String body = response.body() != null ? response.body().string() : "";
                        callback.onError("Status=" + response.code() + " body=" + body);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "signOut exception", e);
            callback.onError("Failed to sign out: " + e.getMessage());
        }
    }

    /**
     * Request server-side profile creation via the confirm-user Edge Function.
     * This calls the Edge Function `/functions/v1/confirm-user` with user_id and optional email.
     * Note: The Edge Function may require a secret header; if so, configure the function to accept requests
     * from the client or call it from a trusted server. This method attempts the call and returns the function
     * response to the caller.
     */
    public void requestProfileCreation(String userId, String email, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + FUNCTIONS_PATH + "/confirm-user";

            org.json.JSONObject payload = new org.json.JSONObject();
            if (userId != null) payload.put("user_id", userId);
            if (email != null) payload.put("email", email);

            RequestBody rbBody = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .post(rbBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    // Authorization here uses anon key; the Edge Function uses its CONFIRM_SECRET to authorize.
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "requestProfileCreation network failure", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONObject wrapper = new org.json.JSONObject();
                            wrapper.put("body", new org.json.JSONArray(body));
                            callback.onSuccess(wrapper);
                        } else {
                            callback.onError("Status=" + response.code() + " body=" + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "requestProfileCreation parse error", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "requestProfileCreation exception", e);
            callback.onError("Failed to request profile creation: " + e.getMessage());
        }
    }

    // ============================================================================
    // GEOCODING METHODS - Convert addresses to latitude/longitude coordinates
    // ============================================================================

    /**
     * Geocode an address using multiple strategies:
     * 1. Try the full address with Nominatim
     * 2. If that fails, try extracting the district/province (works better for plus codes)
     * 3. If that fails, try just city + country
     * 
     * @param address The address to geocode (supports plus codes, street addresses, etc.)
     * @param callback Returns {"latitude": 14.123, "longitude": 120.456} on success
     */
    public void geocodeAddress(String address, ApiCallback callback) {
        try {
            if (address == null || address.isEmpty()) {
                callback.onError("Address cannot be empty");
                return;
            }

            Log.d(TAG, "Geocoding address: " + address);
            geocodeWithNominatim(address, callback);

        } catch (Exception e) {
            Log.e(TAG, "geocodeAddress exception", e);
            callback.onError("Geocoding failed: " + e.getMessage());
        }
    }

    /**
     * Attempt geocoding using Nominatim OSM service
     */
    private void geocodeWithNominatim(String address, ApiCallback callback) {
        try {
            String encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=5&countrycodes=zm";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Roominate-App/1.0")
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Nominatim network failure", e);
                    callback.onError("Failed to geocode address: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "Nominatim error: " + response.code());
                            callback.onError("Geocoding service error: " + response.code());
                            return;
                        }

                        org.json.JSONArray results = new org.json.JSONArray(body);
                        
                        // If no results from full address, try simplified searches
                        if (results.length() == 0) {
                            Log.d(TAG, "No results for full address, trying simplified search");
                            geocodeSimplified(address, callback);
                            return;
                        }

                        org.json.JSONObject location = results.getJSONObject(0);
                        double latitude = location.getDouble("lat");
                        double longitude = location.getDouble("lon");
                        String displayName = location.optString("display_name", "");

                        org.json.JSONObject result = new org.json.JSONObject();
                        result.put("latitude", latitude);
                        result.put("longitude", longitude);
                        result.put("display_name", displayName);

                        Log.d(TAG, "Geocoded '" + address + "' -> " + latitude + ", " + longitude);
                        callback.onSuccess(result);

                    } catch (Exception e) {
                        Log.e(TAG, "Nominatim parse error", e);
                        callback.onError("Failed to parse geocoding response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "geocodeWithNominatim exception", e);
            callback.onError("Geocoding failed: " + e.getMessage());
        }
    }

    /**
     * Try simplified geocoding by extracting just district/province
     */
    private void geocodeSimplified(String address, ApiCallback callback) {
        try {
            // Extract district/province from the address
            // Format is typically: "address, city, district, Zambia"
            String[] parts = address.split(",");
            
            String simplifiedAddress;
            if (parts.length >= 3) {
                // Try: "district, Zambia"
                simplifiedAddress = parts[parts.length - 2].trim() + ", Zambia";
            } else if (parts.length == 2) {
                // Try: "city, Zambia"
                simplifiedAddress = parts[parts.length - 1].trim() + ", Zambia";
            } else {
                simplifiedAddress = address;
            }

            Log.d(TAG, "Trying simplified search: " + simplifiedAddress);
            String encodedAddress = java.net.URLEncoder.encode(simplifiedAddress, "UTF-8");
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=5&countrycodes=zm";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Roominate-App/1.0")
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Simplified search network failure", e);
                    callback.onError("Address not found in Zambia. Try entering just the district name (e.g., 'Ndola, Copperbelt, Zambia')");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "Simplified search error: " + response.code());
                            callback.onError("Address not found");
                            return;
                        }

                        org.json.JSONArray results = new org.json.JSONArray(body);
                        if (results.length() == 0) {
                            Log.d(TAG, "No results for simplified search either");
                            callback.onError("Address '" + address + "' not found. Make sure the city/district name is correct. Examples: 'Ndola, Copperbelt, Zambia' or 'Lusaka, Lusaka, Zambia'");
                            return;
                        }

                        org.json.JSONObject location = results.getJSONObject(0);
                        double latitude = location.getDouble("lat");
                        double longitude = location.getDouble("lon");
                        String displayName = location.optString("display_name", "");

                        org.json.JSONObject result = new org.json.JSONObject();
                        result.put("latitude", latitude);
                        result.put("longitude", longitude);
                        result.put("display_name", displayName);

                        Log.d(TAG, "Geocoded simplified '" + simplifiedAddress + "' -> " + latitude + ", " + longitude);
                        callback.onSuccess(result);

                    } catch (Exception e) {
                        Log.e(TAG, "Simplified search parse error", e);
                        callback.onError("Failed to parse geocoding response");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "geocodeSimplified exception", e);
            callback.onError("Geocoding failed");
        }
    }

    /**
     * Update a property's coordinates in Supabase
     * @param propertyId The UUID of the boarding_house
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param callback Called on success/failure
     */
    public void updatePropertyCoordinates(String propertyId, double latitude, double longitude, ApiCallback callback) {
        try {
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);

            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?id=eq." + propertyId;

            RequestBody requestBody = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(requestBody)
                    .addHeader("Content-Type", "application/json");

            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "updatePropertyCoordinates network failure", e);
                    callback.onError("Failed to update coordinates: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONObject result = new org.json.JSONObject();
                            result.put("latitude", latitude);
                            result.put("longitude", longitude);
                            Log.d(TAG, "Updated property " + propertyId + " coordinates");
                            callback.onSuccess(result);
                        } else {
                            callback.onError("Failed to update: " + response.code() + " - " + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updatePropertyCoordinates parse error", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "updatePropertyCoordinates exception", e);
            callback.onError("Update failed: " + e.getMessage());
        }
    }

    /**
     * Update multiple property fields in one operation
     * @param propertyId The UUID of the boarding_house
     * @param updateData JSONObject containing fields to update
     * @param callback Called on success/failure
     */
    public void updateProperty(String propertyId, org.json.JSONObject updateData, ApiCallback callback) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?id=eq." + propertyId;

            Log.d(TAG, "Updating property " + propertyId + " with data: " + updateData.toString());

            RequestBody requestBody = RequestBody.create(
                    updateData.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .patch(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation");

            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "updateProperty network failure", e);
                    callback.onError("Failed to update property: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Updated property " + propertyId + " - Response: " + body);
                            org.json.JSONArray updatedArray = new org.json.JSONArray(body);
                            if (updatedArray.length() > 0) {
                                org.json.JSONObject updatedProperty = updatedArray.getJSONObject(0);
                                Log.d(TAG, "Updated price_per_month: " + updatedProperty.optDouble("price_per_month"));
                            }
                            callback.onSuccess(new org.json.JSONObject().put("success", true).put("data", body));
                        } else {
                            Log.e(TAG, "Update failed: " + response.code() + " - " + body);
                            callback.onError("Failed to update: " + response.code() + " - " + body);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updateProperty parse error", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "updateProperty exception", e);
            callback.onError("Update failed: " + e.getMessage());
        }
    }

    /**
     * Fetch all properties with coordinates for map display
     * @param callback Returns JSONArray of properties with lat/long
     */
    public void getAllPropertiesWithCoordinates(ApiCallback callback) {
        try {
            // Select only properties that have coordinates and are available
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/boarding_houses?" +
                    "select=id,name,address,latitude,longitude,price_per_month,available_rooms" +
                    "&latitude=not.is.null" +
                    "&longitude=not.is.null" +
                    "&available=eq.true" +
                    "&order=created_at.desc";

            Request.Builder rb = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json");

            addAuthHeaders(rb);

            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "getAllPropertiesWithCoordinates network failure", e);
                    callback.onError("Failed to fetch properties: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        if (response.isSuccessful()) {
                            org.json.JSONArray properties = new org.json.JSONArray(body);
                            Log.d(TAG, "Fetched " + properties.length() + " properties with coordinates");
                            
                            // Fetch images for these properties
                            fetchImagesForProperties(properties, new ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject result) {
                                    try {
                                        callback.onSuccess(new org.json.JSONObject()
                                            .put("properties", result.optJSONArray("properties_with_images")));
                                    } catch (org.json.JSONException e) {
                                        callback.onError("Error processing images: " + e.getMessage());
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    // Still return properties even if image fetching fails
                                    try {
                                        callback.onSuccess(new org.json.JSONObject().put("properties", properties));
                                    } catch (org.json.JSONException e) {
                                        callback.onError("Error: " + e.getMessage());
                                    }
                                }
                            });
                        } else {
                            callback.onError("Failed to fetch: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getAllPropertiesWithCoordinates parse error", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "getAllPropertiesWithCoordinates exception", e);
            callback.onError("Fetch failed: " + e.getMessage());
        }
    }


}
