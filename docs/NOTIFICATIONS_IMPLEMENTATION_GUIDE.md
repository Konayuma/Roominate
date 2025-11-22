# Supabase Push Notifications - Implementation Guide

## ✅ What's Been Implemented

### Android App Components

1. **SupabaseClient.java** - Added notification methods:
   - `createNotification()` - Create a notification
   - `getNotifications()` - Get user's notifications
   - `markNotificationAsRead()` - Mark notification as read
   - `getUnreadNotificationsCount()` - Get count of unread notifications
   - `deleteNotification()` - Delete a notification

2. **NotificationsActivity.java** - Complete notification center:
   - Display all notifications in a list
   - Mark notifications as read on click
   - Navigate to relevant screens based on notification type
   - Delete notifications with swipe/button
   - Shows unread count
   - Empty state when no notifications

3. **NotificationAdapter.java** - RecyclerView adapter:
   - Displays notification title, message, type, and time
   - Highlights unread notifications with different background
   - Shows relative time ("2 hours ago", "3 days ago")
   - Type badges with emojis (🏠 Booking, 💬 Message, ⭐ Review)
   - Delete button for each notification

4. **Layouts**:
   - `activity_notifications.xml` - Main activity layout
   - `item_notification.xml` - Individual notification card
   - `bg_type_badge.xml` - Type badge background
   - `ic_delete.xml` - Delete icon

5. **Updated AndroidManifest.xml** - Registered NotificationsActivity

### Backend Components (Supabase)

1. **Database Migration** - `create_notifications_table.sql`:
   - Creates `notifications` table with proper structure
   - Indexes for fast queries
   - Row Level Security (RLS) policies
   - Triggers for updated_at timestamp
   - Helper function for unread count

2. **Edge Function** - `notify-booking-update/index.ts`:
   - Automatically creates notifications on booking updates
   - Handles different booking statuses (pending, confirmed, cancelled, etc.)
   - Sends notifications to both tenant and owner
   - CORS headers for browser requests

3. **Database Trigger** - `create_booking_notification_trigger.sql`:
   - Automatically calls Edge Function when booking status changes
   - Uses pg_net extension for HTTP requests
   - Error handling to prevent booking failures

## 🚀 Setup Instructions

### Step 1: Create Notifications Table

1. Go to Supabase Dashboard → SQL Editor
2. Copy and paste the content from `supabase/migrations/create_notifications_table.sql`
3. Run the query
4. Verify the table was created: Table Editor → notifications

### Step 2: Deploy Edge Function

#### Option A: Using Supabase CLI (Recommended)

```bash
# Install Supabase CLI if not already installed
npm install -g supabase

# Login to Supabase
supabase login

# Link your project
supabase link --project-ref your-project-ref

# Deploy the function
supabase functions deploy notify-booking-update
```

#### Option B: Manual Deployment (Supabase Dashboard)

1. Go to Supabase Dashboard → Edge Functions
2. Click "Create a new function"
3. Name it: `notify-booking-update`
4. Copy and paste the content from `supabase/functions/notify-booking-update/index.ts`
5. Click "Deploy"

### Step 3: Set Up Database Trigger

1. Go to Supabase Dashboard → SQL Editor
2. Copy the content from `supabase/migrations/create_booking_notification_trigger.sql`
3. **IMPORTANT**: Replace `'https://your-project-ref.supabase.co'` with your actual Supabase URL
4. Run the query

### Step 4: Configure Database Settings

Run these commands in Supabase SQL Editor:

```sql
-- Set your Supabase URL
ALTER DATABASE postgres SET app.settings.supabase_url = 'https://your-project-ref.supabase.co';

-- Set your service role key (get from Settings → API)
ALTER DATABASE postgres SET app.settings.service_role_key = 'your-service-role-key-here';

-- Reload configuration
SELECT pg_reload_conf();
```

### Step 5: Test the Implementation

1. Build and run the Android app
2. Create a test booking
3. Check if notification appears in NotificationsActivity
4. Try marking as read
5. Try deleting notification

## 📱 How to Access Notifications in Your App

You can add a notification bell icon to your main activities:

### Option 1: Add to Toolbar

```java
// In any activity's onCreate()
private void setupNotificationIcon() {
    ImageButton notificationButton = findViewById(R.id.notificationButton);
    notificationButton.setOnClickListener(v -> {
        Intent intent = new Intent(this, NotificationsActivity.class);
        startActivity(intent);
    });
    
    // Load unread count
    updateNotificationBadge();
}

private void updateNotificationBadge() {
    SupabaseClient.getInstance().getUnreadNotificationsCount(new SupabaseClient.ApiCallback() {
        @Override
        public void onSuccess(JSONObject response) {
            runOnUiThread(() -> {
                try {
                    int count = response.getInt("count");
                    // Update badge with count
                    if (count > 0) {
                        badgeTextView.setText(String.valueOf(count));
                        badgeTextView.setVisibility(View.VISIBLE);
                    } else {
                        badgeTextView.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing count", e);
                }
            });
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "Failed to get unread count: " + error);
        }
    });
}
```

### Option 2: Add to Navigation Menu

If you have a navigation drawer or bottom navigation:

```java
MenuItem notificationsItem = navigationView.getMenu().findItem(R.id.nav_notifications);
notificationsItem.setOnMenuItemClickListener(item -> {
    startActivity(new Intent(this, NotificationsActivity.class));
    return true;
});
```

## 🔔 Notification Types

The system supports these notification types:

1. **booking_update** - Booking status changes
   - New booking request (owner)
   - Booking confirmed (tenant)
   - Booking cancelled
   - Booking completed

2. **new_message** - New chat messages (when implemented)

3. **review** - New review on property (owner)

4. **property_update** - Property changes

5. **general** - General notifications

## 🎯 How It Works

### Automatic Notifications (Booking Updates)

```
User creates/updates booking
    ↓
Database trigger fires
    ↓
Calls Edge Function via pg_net
    ↓
Edge Function creates notification in notifications table
    ↓
User sees notification in NotificationsActivity
```

### Manual Notifications (From App)

```java
// Create a notification manually
String userId = "user-uuid";
String title = "New Review";
String message = "Someone reviewed your property";
String type = "review";
String relatedId = "property-uuid";

SupabaseClient.getInstance().createNotification(
    userId, title, message, type, relatedId,
    new SupabaseClient.ApiCallback() {
        @Override
        public void onSuccess(JSONObject response) {
            Log.d(TAG, "Notification created");
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "Failed to create notification: " + error);
        }
    }
);
```

## 🔧 Troubleshooting

### Notifications not appearing?

1. **Check database**:
   ```sql
   SELECT * FROM notifications ORDER BY created_at DESC LIMIT 10;
   ```

2. **Check Edge Function logs**:
   - Go to Dashboard → Edge Functions → notify-booking-update → Logs

3. **Check trigger execution**:
   ```sql
   -- Check if trigger exists
   SELECT * FROM pg_trigger WHERE tgname LIKE '%notify%';
   
   -- Check pg_net requests
   SELECT * FROM net._http_response ORDER BY created_at DESC LIMIT 10;
   ```

4. **Verify RLS policies**:
   ```sql
   -- Check if user can see notifications
   SELECT * FROM notifications WHERE user_id = 'your-user-id';
   ```

### Edge Function not being called?

1. Verify pg_net extension is enabled:
   ```sql
   CREATE EXTENSION IF NOT EXISTS pg_net;
   ```

2. Check database settings:
   ```sql
   SHOW app.settings.supabase_url;
   SHOW app.settings.service_role_key;
   ```

3. Check trigger function:
   ```sql
   SELECT * FROM pg_proc WHERE proname = 'notify_booking_status_change';
   ```

## 📊 Monitoring & Analytics

### Get notification statistics:

```sql
-- Total notifications by type
SELECT type, COUNT(*) as count
FROM notifications
GROUP BY type;

-- Unread notifications per user
SELECT user_id, COUNT(*) as unread_count
FROM notifications
WHERE is_read = false
GROUP BY user_id;

-- Notifications created in last 24 hours
SELECT COUNT(*) as recent_count
FROM notifications
WHERE created_at > now() - interval '24 hours';
```

## 🎨 Customization

### Change notification colors:

Edit `colors.xml`:
```xml
<color name="unread_notification_bg">#FFF0F8FF</color> <!-- Light blue -->
```

### Change notification badge style:

Edit `bg_type_badge.xml`:
```xml
<solid android:color="#E6F7F7" />
<corners android:radius="12dp" />
```

### Add more notification types:

1. Add to database constraint:
   ```sql
   ALTER TABLE notifications 
   DROP CONSTRAINT IF EXISTS notifications_type_check;
   
   ALTER TABLE notifications 
   ADD CONSTRAINT notifications_type_check 
   CHECK (type IN ('booking_update', 'new_message', 'review', 'property_update', 'general', 'your_new_type'));
   ```

2. Update adapter `getTypeLabel()` method

3. Update NotificationsActivity navigation logic

## 🔐 Security Notes

- ✅ Row Level Security (RLS) enabled - users can only see their own notifications
- ✅ Service role key stored in database settings, not in code
- ✅ Edge Function uses service_role for inserting notifications
- ✅ Users can only update/delete their own notifications
- ⚠️ Keep service_role_key secret - never commit to git

## 🚧 Future Enhancements

1. **Real-time notifications**: Use Supabase Realtime to push notifications instantly
2. **Notification sounds**: Add sound/vibration when new notification arrives
3. **Notification preferences**: Let users choose which notifications to receive
4. **Push notifications**: Integrate FCM for background notifications
5. **Notification grouping**: Group similar notifications together
6. **Mark all as read**: Bulk operations
7. **Notification filters**: Filter by type or date

## 📝 Files Created/Modified

### New Files:
- `app/src/main/java/com/roominate/activities/NotificationsActivity.java`
- `app/src/main/java/com/roominate/adapters/NotificationAdapter.java`
- `app/src/main/res/layout/activity_notifications.xml`
- `app/src/main/res/layout/item_notification.xml`
- `app/src/main/res/drawable/bg_type_badge.xml`
- `app/src/main/res/drawable/ic_delete.xml`
- `supabase/functions/notify-booking-update/index.ts`
- `supabase/migrations/create_notifications_table.sql`
- `supabase/migrations/create_booking_notification_trigger.sql`

### Modified Files:
- `app/src/main/java/com/roominate/services/SupabaseClient.java`
- `app/src/main/res/values/colors.xml`
- `app/src/main/AndroidManifest.xml`

## ✨ Next Steps

1. Run the SQL migrations in Supabase
2. Deploy the Edge Function
3. Set up the database trigger
4. Test creating a booking
5. Add notification bell icon to your main activities
6. Consider implementing real-time updates with Supabase Realtime

Enjoy your new notification system! 🎉
