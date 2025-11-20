package com.roominate.services;

import android.util.Log;
import com.roominate.BuildConfig;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import java.io.IOException;

public class PaymentService {
    private static final String TAG = "PaymentService";
    private static PaymentService instance;

    private PaymentService() {}

    public static synchronized PaymentService getInstance() {
        if (instance == null) {
            instance = new PaymentService();
        }
        return instance;
    }

    public void initiatePayment(String bookingId, double amount, String currency, String email,
                                String phoneNumber, String firstName, String lastName,
                                SupabaseClient.ApiCallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("booking_id", bookingId);
            requestBody.put("amount", amount); // Lenco mobile money expects amount in base currency (Kwacha)
            requestBody.put("currency", currency);
            requestBody.put("email", email);
            requestBody.put("phone_number", phoneNumber);
            requestBody.put("first_name", firstName);
            requestBody.put("last_name", lastName);

            SupabaseClient.getInstance().invokeEdgeFunction("lenco-payment", requestBody, callback);

        } catch (Exception e) {
            Log.e(TAG, "Error creating payment request body", e);
            callback.onError("Client-side error preparing payment request.");
        }
    }

    public void getPaymentStatus(String reference, SupabaseClient.ApiCallback callback) {
        new Thread(() -> {
            try {
                String supabaseUrl = BuildConfig.SUPABASE_URL;
                String url = supabaseUrl + "/rest/v1/bookings?payment_reference=eq." + reference + "&select=payment_status";

                Request.Builder requestBuilder = new Request.Builder().url(url).get();
                requestBuilder = SupabaseClient.addAuthHeaders(requestBuilder);
                Request request = requestBuilder.build();

                OkHttpClient client = new OkHttpClient();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Error checking payment status", e);
                        callback.onError(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Failed to get payment status: " + response.message());
                            return;
                        }
                        
                        try {
                            String responseBody = response.body().string();
                            JSONArray jsonArray = new JSONArray(responseBody);
                            if (jsonArray.length() > 0) {
                                JSONObject payment = jsonArray.getJSONObject(0);
                                String status = payment.optString("payment_status", payment.optString("status", "pending"));
                                JSONObject wrapper = new JSONObject();
                                wrapper.put("status", status);
                                callback.onSuccess(wrapper);
                            } else {
                                // Not an error, just not found yet. Return an object indicating pending.
                                JSONObject pendingStatus = new JSONObject();
                                pendingStatus.put("status", "pending");
                                callback.onSuccess(pendingStatus);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing payment status response", e);
                            callback.onError("Error parsing payment status.");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error building payment status request", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
