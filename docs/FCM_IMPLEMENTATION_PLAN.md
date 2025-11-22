# Firebase Cloud Messaging (FCM) - Implementation Plan

## Overview
This document outlines the implementation plan for adding push notifications to the Roominate app using Firebase Cloud Messaging (FCM).

## Prerequisites

### 1. Firebase Project Setup
- [ ] Create Firebase project at https://console.firebase.google.com
- [ ] Add Android app to Firebase project (package: com.roominate)
- [ ] Download `google-services.json` file
- [ ] Place `google-services.json` in `app/` directory

### 2. Update build.gradle Files

#### Project-level build.gradle
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

#### App-level build.gradle
```gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    // Firebase BOM (Bill of Materials)
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    
    // Firebase Cloud Messaging
    implementation 'com.google.firebase:firebase-messaging'
    
    // Firebase Analytics (optional but recommended)
    implementation 'com.google.firebase:firebase-analytics'
}
```

## Implementation Components

### 1. MyFirebaseMessagingService
**File**: `app/src/main/java/com/roominate/services/MyFirebaseMessagingService.java`

**Features**:
- Handle incoming FCM messages
- Generate local notifications
- Handle notification clicks
- Update FCM token in Supabase

**Key Methods**:
- `onMessageReceived(RemoteMessage)` - Process incoming messages
- `onNewToken(String)` - Handle token refresh
- `sendRegistrationToServer(String)` - Save token to Supabase
- `sendNotification(String, String, Map<String, String>)` - Create local notification

### 2. NotificationHelper
**File**: `app/src/main/java/com/roominate/utils/NotificationHelper.java`

**Features**:
- Create notification channels (Android 8.0+)
- Build and display notifications
- Handle notification actions

**Channels**:
- `booking_updates` - Booking confirmations, cancellations
- `messages` - New chat messages
- `general` - General app notifications

### 3. Update SupabaseClient
**File**: `app/src/main/java/com/roominate/services/SupabaseClient.java`

**New Methods**:
- `saveFcmToken(String token, Callback)` - Save FCM token to user profile
- `sendNotification(String userId, String title, String body, JSONObject data, Callback)` - Trigger server notification

### 4. NotificationsActivity
**File**: `app/src/main/java/com/roominate/activities/NotificationsActivity.java`

**Features**:
- Display notification history
- Mark notifications as read
- Delete notifications
- Navigate to relevant screens

**Components**:
- RecyclerView for notifications list
- NotificationAdapter
- item_notification.xml layout

### 5. Database Schema

#### users table - Add FCM token column
```sql
ALTER TABLE users ADD COLUMN fcm_token TEXT;
CREATE INDEX idx_users_fcm_token ON users(fcm_token);
```

#### notifications table - Store notification history
```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- 'booking', 'message', 'general'
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB, -- Additional data (booking_id, message_id, etc.)
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
```

### 6. Request POST_NOTIFICATIONS Permission (Android 13+)
**File**: `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

**File**: Update relevant activities to request permission

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityResultLauncher<String> notificationPermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission granted, FCM can show notifications
            } else {
                // Permission denied
            }
        });
    
    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
}
```

## Notification Types

### 1. Booking Notifications
**Triggers**:
- Booking created (confirmation to tenant)
- Booking confirmed by owner
- Booking rejected by owner
- Booking cancelled
- Booking completed

**Data Payload**:
```json
{
    "type": "booking",
    "booking_id": "uuid",
    "property_id": "uuid",
    "action": "confirmed|rejected|cancelled|completed"
}
```

### 2. Message Notifications
**Triggers**:
- New chat message received

**Data Payload**:
```json
{
    "type": "message",
    "message_id": "uuid",
    "conversation_id": "uuid",
    "sender_id": "uuid",
    "sender_name": "John Doe"
}
```

### 3. General Notifications
**Triggers**:
- Review received on property
- Favorite property updated
- New properties in saved searches

**Data Payload**:
```json
{
    "type": "general",
    "action": "review_received|property_updated",
    "property_id": "uuid"
}
```

## Edge Functions (Supabase)

### 1. send-booking-notification
**File**: `supabase/functions/send-booking-notification/index.ts`

**Trigger**: Called when booking status changes

**Logic**:
1. Get booking details
2. Determine recipient (tenant or owner)
3. Get recipient's FCM token
4. Send FCM notification via Firebase Admin SDK
5. Store notification in database

### 2. send-message-notification
**File**: `supabase/functions/send-message-notification/index.ts`

**Trigger**: Called when new message is sent

**Logic**:
1. Get message details
2. Get recipient's FCM token
3. Check if recipient is currently in conversation (skip if yes)
4. Send FCM notification
5. Store notification in database

### 3. Setup Firebase Admin SDK in Edge Functions
```typescript
import * as admin from 'firebase-admin';

// Initialize Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert({
        projectId: Deno.env.get('FIREBASE_PROJECT_ID'),
        clientEmail: Deno.env.get('FIREBASE_CLIENT_EMAIL'),
        privateKey: Deno.env.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n')
    })
});

// Send notification
await admin.messaging().send({
    token: fcmToken,
    notification: {
        title: title,
        body: body
    },
    data: data,
    android: {
        priority: 'high',
        notification: {
            channelId: 'booking_updates',
            sound: 'default'
        }
    }
});
```

## Implementation Steps

### Phase 1: Basic FCM Setup
1. [ ] Create Firebase project and download google-services.json
2. [ ] Update gradle files with Firebase dependencies
3. [ ] Create MyFirebaseMessagingService
4. [ ] Create NotificationHelper for notification channels
5. [ ] Request POST_NOTIFICATIONS permission
6. [ ] Update SupabaseClient to save FCM tokens
7. [ ] Test token generation and storage

### Phase 2: Notification UI
1. [ ] Create NotificationsActivity
2. [ ] Create NotificationAdapter and item_notification.xml
3. [ ] Add notification bell icon to toolbar with badge
4. [ ] Implement mark as read functionality
5. [ ] Create notifications table in Supabase

### Phase 3: Booking Notifications
1. [ ] Create send-booking-notification Edge Function
2. [ ] Update booking status change logic to trigger notifications
3. [ ] Test booking confirmation notifications
4. [ ] Test booking cancellation notifications

### Phase 4: Message Notifications
1. [ ] Create send-message-notification Edge Function
2. [ ] Update chat message send logic to trigger notifications
3. [ ] Implement conversation state tracking
4. [ ] Test message notifications

### Phase 5: Additional Features
1. [ ] Add notification settings page
2. [ ] Allow users to enable/disable notification types
3. [ ] Implement notification sound preferences
4. [ ] Add notification scheduling (quiet hours)

## Testing Checklist

### FCM Token
- [ ] FCM token generated on app launch
- [ ] Token saved to Supabase user profile
- [ ] Token refreshes when changed
- [ ] Token updates on user login

### Notifications
- [ ] Notifications received when app is in foreground
- [ ] Notifications received when app is in background
- [ ] Notifications received when app is closed
- [ ] Notification taps open correct screens
- [ ] Notification channels work properly
- [ ] POST_NOTIFICATIONS permission requested (Android 13+)

### Booking Notifications
- [ ] Tenant receives notification on booking creation
- [ ] Owner receives notification on new booking
- [ ] Tenant receives notification on booking confirmation
- [ ] Tenant receives notification on booking rejection
- [ ] Both parties notified on cancellation

### Message Notifications
- [ ] Recipient receives notification for new messages
- [ ] No notification when user is in active conversation
- [ ] Notification includes sender name and message preview
- [ ] Notification tap opens conversation

### Notification Center
- [ ] Notifications display in list
- [ ] Unread notifications highlighted
- [ ] Mark as read works
- [ ] Delete notifications works
- [ ] Badge count updates correctly

## Files to Create

1. `app/src/main/java/com/roominate/services/MyFirebaseMessagingService.java`
2. `app/src/main/java/com/roominate/utils/NotificationHelper.java`
3. `app/src/main/java/com/roominate/activities/NotificationsActivity.java`
4. `app/src/main/java/com/roominate/adapters/NotificationAdapter.java`
5. `app/src/main/res/layout/activity_notifications.xml`
6. `app/src/main/res/layout/item_notification.xml`
7. `supabase/functions/send-booking-notification/index.ts`
8. `supabase/functions/send-message-notification/index.ts`

## Files to Modify

1. `build.gradle` (project-level) - Add google-services plugin
2. `app/build.gradle` - Add Firebase dependencies
3. `AndroidManifest.xml` - Add service declaration and permission
4. `app/src/main/java/com/roominate/services/SupabaseClient.java` - Add FCM token methods
5. Main activities - Add notification permission request
6. Booking activities - Trigger notifications on status changes
7. Chat activities - Trigger notifications on messages

## Resources

- Firebase Console: https://console.firebase.google.com
- FCM Documentation: https://firebase.google.com/docs/cloud-messaging
- Firebase Admin SDK: https://firebase.google.com/docs/admin/setup
- Android Notification Channels: https://developer.android.com/develop/ui/views/notifications/channels

## Notes

- FCM requires google-services.json file (NOT committed to git for security)
- POST_NOTIFICATIONS permission required for Android 13+ (API 33+)
- Notification channels required for Android 8.0+ (API 26+)
- Firebase Admin SDK needed for server-side notifications
- Store FCM private key in Supabase secrets/environment variables
- Test notifications on both foreground and background states
- Handle notification token refresh properly
- Consider notification batching for multiple updates
- Implement notification rate limiting to avoid spam
