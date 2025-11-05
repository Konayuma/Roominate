package com.roominate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.roominate.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private Context context;
    private JSONArray notifications;
    private NotificationClickListener listener;

    public interface NotificationClickListener {
        void onNotificationClick(JSONObject notification);
        void onDeleteClick(String notificationId, int position);
    }

    public NotificationAdapter(Context context, JSONArray notifications, NotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        try {
            JSONObject notification = notifications.getJSONObject(position);
            
            String title = notification.getString("title");
            String message = notification.getString("message");
            String createdAt = notification.getString("created_at");
            boolean isRead = notification.getBoolean("is_read");
            String type = notification.getString("type");

            holder.titleTextView.setText(title);
            holder.messageTextView.setText(message);
            holder.timeTextView.setText(formatDate(createdAt));

            // Set type icon based on notification type
            String typeLabel = getTypeLabel(type);
            holder.typeTextView.setText(typeLabel);

            // Highlight unread notifications
            if (!isRead) {
                holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.unread_notification_bg, null));
                holder.titleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white, null));
                holder.titleTextView.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Handle click
            holder.cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });

            // Handle delete
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    try {
                        String notificationId = notification.getString("id");
                        listener.onDeleteClick(notificationId, position);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return notifications.length();
    }

    public void updateNotifications(JSONArray newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            
            long timeDiff = System.currentTimeMillis() - date.getTime();
            long minutes = timeDiff / (60 * 1000);
            long hours = timeDiff / (60 * 60 * 1000);
            long days = timeDiff / (24 * 60 * 60 * 1000);

            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            return dateString;
        }
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case "booking_update":
                return "🏠 Booking";
            case "new_message":
                return "💬 Message";
            case "review":
                return "⭐ Review";
            case "property_update":
                return "🏠 Property";
            default:
                return "📢 General";
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView titleTextView;
        TextView messageTextView;
        TextView timeTextView;
        TextView typeTextView;
        ImageButton deleteButton;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            titleTextView = itemView.findViewById(R.id.titleTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
