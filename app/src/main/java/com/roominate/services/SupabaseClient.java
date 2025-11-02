package com.roominate.services;

import android.util.Log;
import com.roominate.BuildConfig;
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
        // This needs the owner's user ID. For now, we can't get it reliably without auth state.
        // This should be updated once auth state management is in place.
        // For now, let's fetch all properties as a placeholder.
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/properties?select=*")
                .get()
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
                        // The result is a JSON array, so we wrap it in an object
                        JSONObject result = new JSONObject();
                        result.put("data", new JSONArray(bodyString));
                        callback.onSuccess(result);
                    } else {
                        callback.onError("Failed to fetch properties: " + bodyString);
                    }
                } catch (Exception e) {
                    callback.onError("Failed to parse properties response.");
                }
            }
        });
    }

    public void getPropertyById(String propertyId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/properties?id=eq." + propertyId + "&select=*")
                .get()
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
            // We need the owner ID. This should be retrieved from auth state.
            // For now, this will fail if owner_id is a required foreign key.
            // You'll need to pass the logged-in user's ID to this method.
            // property.setOwnerId( "USER_ID_FROM_AUTH" ); 

            RequestBody body = RequestBody.create(property.toJson().toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/properties")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey) // Use user's token
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            callback.onError("Failed to create insert request.");
        }
    }

    public void updateProperty(Property property, ApiCallback callback) {
        try {
            RequestBody body = RequestBody.create(property.toJson().toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/properties?id=eq." + property.getId())
                    .patch(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey) // Use user's token
                    .addHeader("Prefer", "return=representation")
                    .build();

            client.newCall(request).enqueue(createGenericCallback(callback));
        } catch (Exception e) {
            callback.onError("Failed to create update request.");
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
        // This would require a valid access token from a previous login
        // For now, return a mock response
        try {
            JSONObject mockProfile = new JSONObject();
            mockProfile.put("role", "tenant"); // Default role
            callback.onSuccess(mockProfile);
        } catch (Exception e) {
            callback.onError("Failed to get user profile");
        }
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
            // Recommended flow: perform client-side signup with anon key, then
            // insert a profile row into the `profiles` table. This avoids using
            // the service role on the client and keeps signup atomic from the
            // app's perspective.

            // Call the existing signUp method which performs /auth/v1/signup
            SupabaseClient.this.signUp(firstName, lastName, email, phone, password, role, new ApiCallback() {
                @Override
                public void onSuccess(JSONObject signupResp) {
                    try {
                        // Attempt to extract user id from signup response
                        String userId = null;
                        if (signupResp.has("user")) {
                            JSONObject userObj = signupResp.optJSONObject("user");
                            if (userObj != null) userId = userObj.optString("id", null);
                        }
                        // Some responses return user at top-level
                        if (userId == null && signupResp.has("id")) {
                            userId = signupResp.optString("id", null);
                        }

                        // If we couldn't find user id, still return signupResp
                        if (userId == null || userId.isEmpty()) {
                            callback.onSuccess(signupResp);
                            return;
                        }

                        // Build profile object to insert
                        JSONObject profile = new JSONObject();
                        profile.put("id", userId);
                        profile.put("email", email);
                        profile.put("first_name", firstName != null ? firstName : JSONObject.NULL);
                        profile.put("last_name", lastName != null ? lastName : JSONObject.NULL);
                        profile.put("role", role != null ? role : JSONObject.NULL);
                        profile.put("dob", (dob != null && !dob.isEmpty()) ? dob : JSONObject.NULL);
                        profile.put("phone", phone != null ? phone : JSONObject.NULL);

                        // Capture userId into a final variable so inner classes can reference it
                        final String capturedUserId = userId;

                        // Helper to perform profile POST using provided token (async)
                        java.util.function.Consumer<String> doProfileInsert = (token) -> {
                            RequestBody body = RequestBody.create(profile.toString(), MediaType.parse("application/json"));
                            Request req = new Request.Builder()
                                    .url(supabaseUrl + "/rest/v1/profiles")
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
                                    // Signup succeeded but profile insert failed; surface signup result
                                    callback.onSuccess(signupResp);
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    try {
                                        String respBody = response.body().string();
                                        Log.d(TAG, "Profile insert response: " + respBody);
                                        if (response.isSuccessful()) {
                                            JSONObject insertJson = null;
                                            try {
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
                                            } catch (Exception e) {
                                                Log.w(TAG, "Failed to parse profile insert response, returning empty profile object", e);
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
                                                    String patchUrl = supabaseUrl + "/rest/v1/profiles?id=eq." + capturedUserId;
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

                                            Log.w(TAG, "Profile insert non-OK: " + response.code());
                                            // Return signup result even if profile insert failed
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
                            // No token returned from signup; attempt to sign in (password grant) to obtain token
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
                                            // Can't get token; return signup response
                                            callback.onSuccess(signupResp);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error extracting token from signin response", e);
                                        callback.onSuccess(signupResp);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    // Propagate signup success but include sign-in error
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
                    // Propagate signup error
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating completeSignup request", e);
            callback.onError("Failed to create request");
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
