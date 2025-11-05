package com.roominate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.roominate.R;
import com.roominate.adapters.NotificationAdapter;
import com.roominate.services.SupabaseClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsActivity";

    private MaterialToolbar toolbar;
    private RecyclerView notificationsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private NotificationAdapter adapter;
    private JSONArray notificationsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadNotifications();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        notificationsData = new JSONArray();
        adapter = new NotificationAdapter(this, notificationsData, new NotificationAdapter.NotificationClickListener() {
            @Override
            public void onNotificationClick(JSONObject notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onDeleteClick(String notificationId, int position) {
                deleteNotification(notificationId, position);
            }
        });
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        notificationsRecyclerView.setVisibility(View.GONE);

        SupabaseClient.getInstance().getNotifications(new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        progressBar.setVisibility(View.GONE);
                        notificationsData = response.getJSONArray("body");
                        
                        if (notificationsData.length() == 0) {
                            emptyTextView.setVisibility(View.VISIBLE);
                            notificationsRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyTextView.setVisibility(View.GONE);
                            notificationsRecyclerView.setVisibility(View.VISIBLE);
                            adapter.updateNotifications(notificationsData);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notifications", e);
                        showError("Failed to load notifications");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showError(error);
                });
            }
        });
    }

    private void handleNotificationClick(JSONObject notification) {
        try {
            String notificationId = notification.getString("id");
            String type = notification.getString("type");
            String relatedId = notification.optString("related_id", null);
            boolean isRead = notification.getBoolean("is_read");

            // Mark as read if not already
            if (!isRead) {
                markAsRead(notificationId);
            }

            // Navigate based on notification type
            switch (type) {
                case "booking_update":
                    if (relatedId != null) {
                        // Navigate to booking details
                        // TODO: Create BookingDetailsActivity or use BookingActivity
                        /*
                        Intent bookingIntent = new Intent(this, com.roominate.activities.tenant.BookingDetailsActivity.class);
                        bookingIntent.putExtra("booking_id", relatedId);
                        startActivity(bookingIntent);
                        */
                        Toast.makeText(this, "Opening booking details...", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case "new_message":
                    if (relatedId != null) {
                        // Navigate to chat/conversation
                        Toast.makeText(this, "Opening message...", Toast.LENGTH_SHORT).show();
                        // TODO: Navigate to messaging activity when implemented
                    }
                    break;

                case "review":
                    if (relatedId != null) {
                        // Navigate to property details
                        Intent propertyIntent = new Intent(this, com.roominate.activities.tenant.BoardingHouseDetailsActivity.class);
                        propertyIntent.putExtra("boarding_house_id", relatedId);
                        startActivity(propertyIntent);
                    }
                    break;

                case "property_update":
                    if (relatedId != null) {
                        // Navigate to property details
                        Intent propertyIntent = new Intent(this, com.roominate.activities.tenant.BoardingHouseDetailsActivity.class);
                        propertyIntent.putExtra("boarding_house_id", relatedId);
                        startActivity(propertyIntent);
                    }
                    break;

                default:
                    Toast.makeText(this, "Notification opened", Toast.LENGTH_SHORT).show();
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling notification click", e);
            Toast.makeText(this, "Failed to open notification", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsRead(String notificationId) {
        SupabaseClient.getInstance().markNotificationAsRead(notificationId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    // Update the notification in the list
                    for (int i = 0; i < notificationsData.length(); i++) {
                        try {
                            JSONObject notif = notificationsData.getJSONObject(i);
                            if (notif.getString("id").equals(notificationId)) {
                                notif.put("is_read", true);
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating notification", e);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to mark notification as read: " + error);
            }
        });
    }

    private void deleteNotification(String notificationId, int position) {
        SupabaseClient.getInstance().deleteNotification(notificationId, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    // Remove from list
                    JSONArray newArray = new JSONArray();
                    for (int i = 0; i < notificationsData.length(); i++) {
                        try {
                            JSONObject notif = notificationsData.getJSONObject(i);
                            if (!notif.getString("id").equals(notificationId)) {
                                newArray.put(notif);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error removing notification", e);
                        }
                    }
                    notificationsData = newArray;
                    adapter.updateNotifications(notificationsData);
                    
                    if (notificationsData.length() == 0) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        notificationsRecyclerView.setVisibility(View.GONE);
                    }
                    
                    Toast.makeText(NotificationsActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NotificationsActivity.this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notifications when returning to the activity
        loadNotifications();
    }
}
