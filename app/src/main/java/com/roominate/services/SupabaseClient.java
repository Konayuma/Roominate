package com.roominate.services;

import android.util.Log;
import android.content.Context;
import com.roominate.BuildConfig;
import com.roominate.models.Property;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Supabase client for authentication and database operations using REST API
 */
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    // Application context set during app startup. Must be initialized via SupabaseClient.init(context)
    private static Context appContext;
    private final OkHttpClient client;
    private final String supabaseUrl;
    private final String supabaseKey;

    private SupabaseClient() {
        this.supabaseUrl = BuildConfig.SUPABASE_URL;
        this.supabaseKey = BuildConfig.SUPABASE_ANON_KEY;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

    // Increase timeouts for function calls that may take longer (e.g. admin user creation)
    this.client = new OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    /**
     * Initialize SupabaseClient with an application context. This must be called once
     * from Application.onCreate() or an Activity before methods that rely on SharedPreferences
     * (for example getUserProfile) are used.
     */
    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            Log.d(TAG, "SupabaseClient initialized with application context");
        }
    }

    /**
     * Get the current user's ID from SharedPreferences (stored during login)
     */
    private String getCurrentUserId() {
        try {
            if (appContext == null) {
                Log.e(TAG, "App context not initialized. Call SupabaseClient.init(context) from Application or Activity.");
                return null;
            }

            android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);
            Log.d(TAG, "Retrieved user ID from SharedPreferences: " + userId);
            return userId;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user ID", e);
            return null;
        }
    }

    public void signUpUser(String email, String password, String role, String firstName, String lastName, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);

            JSONObject userMetadata = new JSONObject();
            userMetadata.put("first_name", firstName);
            userMetadata.put("last_name", lastName);
            userMetadata.put("role", role);

            requestBody.put("data", userMetadata);

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/signup")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        String bodyString = responseBody.string();
                        if (response.isSuccessful()) {
                            callback.onSuccess(new JSONObject(bodyString));
                        } else {
                            JSONObject errorJson = new JSONObject(bodyString);
                            callback.onError(errorJson.optString("msg", "Sign up failed"));
                        }
                    } catch (Exception e) {
                        callback.onError("Failed to parse response.");
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Failed to create sign up request.");
        }
    }

    public void getPropertiesByOwner(ApiCallback callback) {
        // Get current user ID from SharedPreferences
        String ownerId = getCurrentUserId();
        
        if (ownerId == null || ownerId.isEmpty()) {
            Log.e(TAG, "Cannot fetch properties: user ID not available");
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Fetching properties for owner: " + ownerId);
        
        // Query boarding_houses table filtering by owner_id
        String url = supabaseUrl + "/rest/v1/boarding_houses?owner_id=eq." + ownerId + "&select=*&order=created_at.desc";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getPropertiesByOwner request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String bodyString = response.body().string();
                    Log.d(TAG, "getPropertiesByOwner response (" + response.code() + "): " + bodyString);
                    
                    if (response.isSuccessful()) {
                        // The result is a JSON array of boarding_houses
                        JSONArray propertiesArray = new JSONArray(bodyString);
                        JSONObject result = new JSONObject();
                        result.put("data", propertiesArray);
                        result.put("count", propertiesArray.length());
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch properties: HTTP " + response.code() + " - " + bodyString);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing getPropertiesByOwner response", e);
                    callback.onError("Failed to parse properties response: " + e.getMessage());
                }
            }
        });
    }

    public void getPropertyById(String propertyId, ApiCallback callback) {
        Log.d(TAG, "Fetching property by ID: " + propertyId);
        
        // Query boarding_houses table by id
        String url = supabaseUrl + "/rest/v1/boarding_houses?id=eq." + propertyId + "&select=*";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getPropertyById request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String bodyString = responseBody.string();
                    if (response.isSuccessful()) {
                        JSONObject result = new JSONObject();
                        result.put("data", new JSONArray(bodyString));
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch property: " + bodyString);
                    }
                } catch (Exception e) {
                    callback.onError("Failed to parse property response.");
                }
            }
        });
    }

    public void insertProperty(Property property, ApiCallback callback) {
        try {
            // Get the current user ID and set it as owner_id
            String ownerId = getCurrentUserId();
            if (ownerId == null || ownerId.isEmpty()) {
                callback.onError("User not authenticated");
                return;
            }
            
            property.setOwnerId(ownerId);
            Log.d(TAG, "Inserting property for owner: " + ownerId);

            RequestBody body = RequestBody.create(property.toJson().toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/boarding_houses")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create insert request", e);
            callback.onError("Failed to create insert request: " + e.getMessage());
        }
    }

    public void updateProperty(Property property, ApiCallback callback) {
        try {
            Log.d(TAG, "Updating property: " + property.getId());
            
            RequestBody body = RequestBody.create(property.toJson().toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/boarding_houses?id=eq." + property.getId())
                    .patch(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create update request", e);
            callback.onError("Failed to create update request: " + e.getMessage());
        }
    }

    private Callback createGenericCallback(final ApiCallback callback) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String bodyString = responseBody.string();
                    if (response.isSuccessful()) {
                        // Response for insert/update is an array with one object
                        JSONArray jsonArray = new JSONArray(bodyString);
                        callback.onSuccess(jsonArray.getJSONObject(0));
                    } else {
                        callback.onError("Operation failed: " + bodyString);
                    }
                } catch (Exception e) {
                    callback.onError("Failed to parse response.");
                }
            }
        };
    }

    /**
     * Sign up a new user
     */
    public void signUp(String firstName, String lastName, String email, String phone, String password, String role, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);

            JSONObject userMetadata = new JSONObject();
            userMetadata.put("first_name", firstName);
            userMetadata.put("last_name", lastName);
            userMetadata.put("phone", phone);
            userMetadata.put("role", role);

            requestBody.put("data", userMetadata);

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/signup")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SignUp failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "SignUp response: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            callback.onSuccess(jsonResponse);
                        } else {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("msg", "Sign up failed");
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing signup response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating signup request", e);
            callback.onError("Failed to create request");
        }
    }


    /**
     * Confirm email with token
     */
    public void confirmEmail(String email, String token, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("token", token);
            requestBody.put("type", "email");

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/verify")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Confirm email failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Confirm email response: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            callback.onSuccess(jsonResponse);
                        } else {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("msg", "Email confirmation failed");
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing confirm email response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating confirm email request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("password", password);

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/token?grant_type=password")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SignIn failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "SignIn response: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            callback.onSuccess(jsonResponse);
                        } else {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("msg", "Sign in failed");
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing signin response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating signin request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Get user profile
     */
    public void getUserProfile(ApiCallback callback) {
        // Get the current user's ID from auth session
        String currentUserId = getCurrentUserId();
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "No current user ID available");
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Fetching profile for user ID: " + currentUserId);
        
        // Query the public.users table for the current user's profile
        String url = supabaseUrl + "/rest/v1/users?id=eq." + currentUserId + "&select=*";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getUserProfile request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "getUserProfile response: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JSONArray responseArray = new JSONArray(responseBody);
                        if (responseArray.length() > 0) {
                            JSONObject userProfile = responseArray.getJSONObject(0);
                            Log.d(TAG, "User profile found: " + userProfile.toString());
                            callback.onSuccess(userProfile);
                        } else {
                            Log.w(TAG, "No profile found for user ID: " + currentUserId);
                            // Return a default response with tenant role
                            JSONObject defaultProfile = new JSONObject();
                            defaultProfile.put("id", currentUserId);
                            defaultProfile.put("role", "tenant");
                            callback.onSuccess(defaultProfile);
                        }
                    } else {
                        Log.e(TAG, "getUserProfile HTTP error: " + response.code());
                        callback.onError("HTTP " + response.code() + ": " + responseBody);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing getUserProfile response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Sign out
     */
    public void signOut(ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/logout")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SignOut failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            callback.onSuccess(new JSONObject());
                        } else {
                            callback.onError("Sign out failed");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing signout response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating signout request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Request OTP via Edge Function
     */
    public void requestOtp(String email, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);

            // Debug log: show outgoing request info
            Log.d(TAG, "requestOtp -> url=" + supabaseUrl + "/functions/v1/send-otp" + " body=" + requestBody.toString());

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/functions/v1/send-otp")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "SendOtp failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "SendOtp response: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            callback.onSuccess(jsonResponse);
                        } else {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("error", "Send OTP failed");
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing sendOtp response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating sendOtp request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Verify OTP via Edge Function
     */
    public void verifyOtp(String email, String otp, ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("email", email);
            requestBody.put("otp", otp);

            Log.d(TAG, "verifyOtp -> url=" + supabaseUrl + "/functions/v1/verify-otp" + " body=" + requestBody.toString());

            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/functions/v1/verify-otp")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "VerifyOtp failed", e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "VerifyOtp response: " + responseBody);

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            callback.onSuccess(jsonResponse);
                        } else {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("error", "Verify OTP failed");
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing verifyOtp response", e);
                        callback.onError("Failed to parse response");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating verifyOtp request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Complete signup via Edge Function
     */
    public void completeSignup(String email, String password, String otp, String firstName, String lastName, String phone, String role, String dob, ApiCallback callback) {
        try {
            // New recommended flow: verify OTP first (server-side via Edge function), then create the auth user.
            // This ensures the password the user enters becomes the account password during creation.
            SupabaseClient.this.verifyOtp(email, otp, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject verifyResp) {
                    try {
                        // Check if OTP verification succeeded
                        // The verify-otp function returns "success": true when OTP is valid
                        // It may also return "confirmed": true if the user was found and updated in auth
                        boolean otpVerified = verifyResp.optBoolean("success", false);
                        
                        if (!otpVerified) {
                            callback.onError("Invalid or expired verification code. Please try again.");
                            return;
                        }
                        
                        // OTP is valid, proceed with signup
                        // Note: The user will be created in the next step with email already verified

                        // OTP confirmed server-side — proceed to create the auth user
                        SupabaseClient.this.signUp(firstName, lastName, email, phone, password, role, new ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject signupResp) {
                                try {
                                    // Extract user id if present
                                    String userId = null;
                                    if (signupResp.has("user")) {
                                        JSONObject userObj = signupResp.optJSONObject("user");
                                        if (userObj != null) userId = userObj.optString("id", null);
                                    }
                                    if (userId == null && signupResp.has("id")) {
                                        userId = signupResp.optString("id", null);
                                    }

                                    // Build profile object
                                    JSONObject profile = new JSONObject();
                                    if (userId != null) profile.put("id", userId);
                                    profile.put("email", email);
                                    profile.put("first_name", firstName != null ? firstName : JSONObject.NULL);
                                    profile.put("last_name", lastName != null ? lastName : JSONObject.NULL);
                                    profile.put("role", role != null ? role : JSONObject.NULL);
                                    profile.put("dob", (dob != null && !dob.isEmpty()) ? dob : JSONObject.NULL);
                                    profile.put("phone", phone != null ? phone : JSONObject.NULL);

                                    // Capture userId for use inside inner classes (must be final/effectively final)
                                    final String capturedUserId = userId;

                                    // Helper to insert profile using a token (if available)
                                    java.util.function.Consumer<String> doProfileInsert = (token) -> {
                                        RequestBody body = RequestBody.create(profile.toString(), MediaType.parse("application/json"));
                                        Request req = new Request.Builder()
                                                .url(supabaseUrl + "/rest/v1/users")
                                                .post(body)
                                                .addHeader("apikey", supabaseKey)
                                                .addHeader("Authorization", "Bearer " + (token != null ? token : supabaseKey))
                                                .addHeader("Content-Type", "application/json")
                                                .addHeader("Prefer", "return=representation")
                                                .build();

                                        client.newCall(req).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                Log.w(TAG, "Profile insert failed", e);
                                                callback.onSuccess(signupResp);
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                try {
                                                    String respBody = response.body().string();
                                                    if (response.isSuccessful()) {
                                                        JSONObject insertJson = null;
                                                        String trimmed = respBody != null ? respBody.trim() : "";
                                                        if (trimmed.startsWith("[")) {
                                                            JSONArray arr = new JSONArray(trimmed);
                                                            if (arr.length() > 0) insertJson = arr.getJSONObject(0);
                                                            else insertJson = new JSONObject();
                                                        } else if (trimmed.startsWith("{")) {
                                                            insertJson = new JSONObject(trimmed);
                                                        } else {
                                                            insertJson = new JSONObject();
                                                        }
                                                        JSONObject merged = new JSONObject();
                                                        merged.put("signup", signupResp);
                                                        merged.put("profile", insertJson);
                                                        callback.onSuccess(merged);
                                                    } else {
                                                        // If conflict (profile exists) try PATCH (upsert)
                                                        if (response.code() == 409) {
                                                            try {
                                                                RequestBody patchBody = RequestBody.create(profile.toString(), MediaType.parse("application/json"));
                                                                String patchUrl = supabaseUrl + "/rest/v1/profiles" + (capturedUserId != null ? "?id=eq." + capturedUserId : "");
                                                                Request patchReq = new Request.Builder()
                                                                        .url(patchUrl)
                                                                        .patch(patchBody)
                                                                        .addHeader("apikey", supabaseKey)
                                                                        .addHeader("Authorization", "Bearer " + (token != null ? token : supabaseKey))
                                                                        .addHeader("Content-Type", "application/json")
                                                                        .addHeader("Prefer", "return=representation")
                                                                        .build();

                                                                client.newCall(patchReq).enqueue(new Callback() {
                                                                    @Override
                                                                    public void onFailure(Call call, IOException e) {
                                                                        Log.w(TAG, "Profile patch failed", e);
                                                                        callback.onSuccess(signupResp);
                                                                    }

                                                                    @Override
                                                                    public void onResponse(Call call, Response patchResp) throws IOException {
                                                                        try {
                                                                            String patchBodyStr = patchResp.body().string();
                                                                            if (patchResp.isSuccessful()) {
                                                                                JSONObject insertJson = null;
                                                                                String trimmed = patchBodyStr != null ? patchBodyStr.trim() : "";
                                                                                if (trimmed.startsWith("[")) {
                                                                                    JSONArray arr = new JSONArray(trimmed);
                                                                                    if (arr.length() > 0) insertJson = arr.getJSONObject(0);
                                                                                    else insertJson = new JSONObject();
                                                                                } else if (trimmed.startsWith("{")) {
                                                                                    insertJson = new JSONObject(trimmed);
                                                                                } else {
                                                                                    insertJson = new JSONObject();
                                                                                }
                                                                                JSONObject merged = new JSONObject();
                                                                                merged.put("signup", signupResp);
                                                                                merged.put("profile", insertJson);
                                                                                callback.onSuccess(merged);
                                                                            } else {
                                                                                callback.onSuccess(signupResp);
                                                                            }
                                                                        } catch (Exception e) {
                                                                            Log.e(TAG, "Error parsing profile patch response", e);
                                                                            callback.onSuccess(signupResp);
                                                                        }
                                                                    }
                                                                });
                                                                return; // patch will call callback
                                                            } catch (Exception e) {
                                                                Log.e(TAG, "Error attempting profile patch", e);
                                                                callback.onSuccess(signupResp);
                                                                return;
                                                            }
                                                        }

                                                        callback.onSuccess(signupResp);
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error parsing profile insert response", e);
                                                    callback.onSuccess(signupResp);
                                                }
                                            }
                                        });
                                    };

                                    // Try to prefer the access token returned by signup for the profile insert.
                                    String accessToken = null;
                                    try {
                                        if (signupResp.has("access_token")) {
                                            accessToken = signupResp.optString("access_token", null);
                                        } else if (signupResp.has("session")) {
                                            JSONObject session = signupResp.optJSONObject("session");
                                            if (session != null) accessToken = session.optString("access_token", null);
                                        } else if (signupResp.has("data")) {
                                            JSONObject data = signupResp.optJSONObject("data");
                                            if (data != null) accessToken = data.optString("access_token", null);
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "Unable to extract access token from signup response", e);
                                    }

                                    if (accessToken != null && !accessToken.isEmpty()) {
                                        doProfileInsert.accept(accessToken);
                                    } else {
                                        // No token returned: attempt sign-in to obtain token
                                        SupabaseClient.this.signIn(email, password, new ApiCallback() {
                                            @Override
                                            public void onSuccess(JSONObject signinResp) {
                                                try {
                                                    String signinToken = null;
                                                    if (signinResp.has("access_token")) {
                                                        signinToken = signinResp.optString("access_token", null);
                                                    } else if (signinResp.has("session")) {
                                                        JSONObject session = signinResp.optJSONObject("session");
                                                        if (session != null) signinToken = session.optString("access_token", null);
                                                    } else if (signinResp.has("data")) {
                                                        JSONObject data = signinResp.optJSONObject("data");
                                                        if (data != null) signinToken = data.optString("access_token", null);
                                                    }

                                                    if (signinToken != null && !signinToken.isEmpty()) {
                                                        doProfileInsert.accept(signinToken);
                                                    } else {
                                                        callback.onSuccess(signupResp);
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error extracting token from signin response", e);
                                                    callback.onSuccess(signupResp);
                                                }
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.w(TAG, "Sign-in after signup failed: " + error);
                                                callback.onError(error);
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error finishing signup/profile insert", e);
                                    callback.onError("Signup succeeded but post-processing failed");
                                }
                            }

                            @Override
                            public void onError(String error) {
                                // If the signup failed because the user already exists, attempt to sign in
                                try {
                                    if (error != null && (error.contains("user_already_exists") || error.toLowerCase().contains("already exists") || error.toLowerCase().contains("user already"))) {
                                        Log.w(TAG, "SignUp returned user_already_exists, attempting sign-in instead");
                                        SupabaseClient.this.signIn(email, password, new ApiCallback() {
                                            @Override
                                            public void onSuccess(JSONObject signinResp) {
                                                try {
                                                    String signinToken = null;
                                                    if (signinResp.has("access_token")) {
                                                        signinToken = signinResp.optString("access_token", null);
                                                    } else if (signinResp.has("session")) {
                                                        JSONObject session = signinResp.optJSONObject("session");
                                                        if (session != null) signinToken = session.optString("access_token", null);
                                                    } else if (signinResp.has("data")) {
                                                        JSONObject data = signinResp.optJSONObject("data");
                                                        if (data != null) signinToken = data.optString("access_token", null);
                                                    }

                                                    if (signinToken != null && !signinToken.isEmpty()) {
                                                        // Insert or patch profile using token
                                                        JSONObject signupResp = new JSONObject();
                                                        signupResp.put("message", "User existed; signed in");
                                                        try {
                                                            JSONObject profile = new JSONObject();
                                                            profile.put("email", email);
                                                            profile.put("first_name", firstName != null ? firstName : JSONObject.NULL);
                                                            profile.put("last_name", lastName != null ? lastName : JSONObject.NULL);
                                                            profile.put("role", role != null ? role : JSONObject.NULL);
                                                            profile.put("dob", (dob != null && !dob.isEmpty()) ? dob : JSONObject.NULL);
                                                            profile.put("phone", phone != null ? phone : JSONObject.NULL);

                                                            RequestBody reqBody = RequestBody.create(profile.toString(), MediaType.parse("application/json"));
                                                            Request req = new Request.Builder()
                                                                    .url(supabaseUrl + "/rest/v1/profiles")
                                                                    .post(reqBody)
                                                                    .addHeader("apikey", supabaseKey)
                                                                    .addHeader("Authorization", "Bearer " + signinToken)
                                                                    .addHeader("Content-Type", "application/json")
                                                                    .addHeader("Prefer", "return=representation")
                                                                    .build();

                                                            client.newCall(req).enqueue(new Callback() {
                                                                @Override
                                                                public void onFailure(Call call, IOException e) {
                                                                    Log.w(TAG, "Profile insert after sign-in failed", e);
                                                                    callback.onSuccess(signupResp);
                                                                }

                                                                @Override
                                                                public void onResponse(Call call, Response response) throws IOException {
                                                                    try {
                                                                        String respBody = response.body().string();
                                                                        if (response.isSuccessful()) {
                                                                            JSONObject insertJson = null;
                                                                            String trimmed = respBody != null ? respBody.trim() : "";
                                                                            if (trimmed.startsWith("[")) {
                                                                                JSONArray arr = new JSONArray(trimmed);
                                                                                if (arr.length() > 0) insertJson = arr.getJSONObject(0);
                                                                                else insertJson = new JSONObject();
                                                                            } else if (trimmed.startsWith("{")) {
                                                                                insertJson = new JSONObject(trimmed);
                                                                            } else {
                                                                                insertJson = new JSONObject();
                                                                            }
                                                                            JSONObject merged = new JSONObject();
                                                                            merged.put("signup", signupResp);
                                                                            merged.put("profile", insertJson);
                                                                            callback.onSuccess(merged);
                                                                        } else {
                                                                            callback.onSuccess(signupResp);
                                                                        }
                                                                    } catch (Exception e) {
                                                                        Log.e(TAG, "Error parsing profile insert response after sign-in", e);
                                                                        callback.onSuccess(signupResp);
                                                                    }
                                                                }
                                                            });
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Error performing profile insert after sign-in", e);
                                                            callback.onSuccess(signupResp);
                                                        }
                                                    } else {
                                                        callback.onError("Unable to sign in existing user: no token returned");
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error extracting token from signin response (after existing user)", e);
                                                    callback.onError("Sign-in after existing user failed");
                                                }
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Log.w(TAG, "Sign-in attempt for existing user failed: " + error);
                                                callback.onError(error);
                                            }
                                        });
                                        return;
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error handling signUp error fallback", ex);
                                }

                                // Default: propagate signup error
                                callback.onError(error);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling verifyOtp success response", e);
                        callback.onError("OTP verification succeeded but post-processing failed");
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "verifyOtp failed: " + error);
                    callback.onError("OTP verification failed: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating completeSignup request", e);
            callback.onError("Failed to create request");
        }
    }

    /**
     * Get bookings for the current user (tenant view)
     */
    public void getUserBookings(ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Fetching bookings for user: " + userId);
        
        String url = supabaseUrl + "/rest/v1/bookings?tenant_id=eq." + userId + "&select=*,boarding_houses(*)&order=created_at.desc";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getUserBookings request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String bodyString = response.body().string();
                    Log.d(TAG, "getUserBookings response (" + response.code() + "): " + bodyString);
                    
                    if (response.isSuccessful()) {
                        JSONArray bookingsArray = new JSONArray(bodyString);
                        JSONObject result = new JSONObject();
                        result.put("data", bookingsArray);
                        result.put("count", bookingsArray.length());
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch bookings: HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing getUserBookings response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Get favorites for the current user
     */
    public void getUserFavorites(ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Fetching favorites for user: " + userId);
        
        String url = supabaseUrl + "/rest/v1/favorites?user_id=eq." + userId + "&select=*,boarding_houses(*)&order=created_at.desc";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getUserFavorites request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String bodyString = response.body().string();
                    Log.d(TAG, "getUserFavorites response (" + response.code() + "): " + bodyString);
                    
                    if (response.isSuccessful()) {
                        JSONArray favoritesArray = new JSONArray(bodyString);
                        JSONObject result = new JSONObject();
                        result.put("data", favoritesArray);
                        result.put("count", favoritesArray.length());
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch favorites: HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing getUserFavorites response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Get notifications for the current user
     */
    public void getUserNotifications(ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Fetching notifications for user: " + userId);
        
        String url = supabaseUrl + "/rest/v1/notifications?user_id=eq." + userId + "&select=*&order=created_at.desc&limit=50";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "getUserNotifications request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String bodyString = response.body().string();
                    Log.d(TAG, "getUserNotifications response (" + response.code() + "): " + bodyString);
                    
                    if (response.isSuccessful()) {
                        JSONArray notificationsArray = new JSONArray(bodyString);
                        JSONObject result = new JSONObject();
                        result.put("data", notificationsArray);
                        result.put("count", notificationsArray.length());
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch notifications: HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing getUserNotifications response", e);
                    callback.onError("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Add a property to favorites
     */
    public void addToFavorites(String listingId, ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", userId);
            requestBody.put("listing_id", listingId);
            
            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/favorites")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            Log.e(TAG, "Error creating addToFavorites request", e);
            callback.onError("Failed to add to favorites: " + e.getMessage());
        }
    }

    /**
     * Remove a property from favorites
     */
    public void removeFromFavorites(String listingId, ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        String url = supabaseUrl + "/rest/v1/favorites?user_id=eq." + userId + "&listing_id=eq." + listingId;
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "removeFromFavorites request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        JSONObject result = new JSONObject();
                        result.put("success", true);
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to remove from favorites: HTTP " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling removeFromFavorites response", e);
                    callback.onError("Failed to process response: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Create a new booking request
     */
    public void createBooking(String listingId, String startDate, String endDate, double totalAmount, ApiCallback callback) {
        String userId = getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("listing_id", listingId);
            requestBody.put("tenant_id", userId);
            requestBody.put("start_date", startDate);
            requestBody.put("end_date", endDate);
            requestBody.put("total_amount", totalAmount);
            requestBody.put("status", "pending");
            
            RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/bookings")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            Log.e(TAG, "Error creating booking request", e);
            callback.onError("Failed to create booking: " + e.getMessage());
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
