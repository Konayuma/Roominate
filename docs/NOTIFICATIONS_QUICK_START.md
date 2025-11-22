# 🚀 Quick Start - Notifications Setup

## 📋 Complete Implementation Summary

✅ **Android App** - All code implemented and ready
✅ **Database Schema** - SQL scripts created  
✅ **Edge Function** - TypeScript function ready to deploy
✅ **Triggers** - Auto-notification triggers configured

---

## 🎯 3-Minute Setup

### 1️⃣ Run SQL in Supabase (2 minutes)

Go to Supabase Dashboard → SQL Editor → paste this:

```sql
-- File: supabase/migrations/setup_notifications_complete.sql
-- Copy the entire content and run it
```

**⚠️ Before running**: Edit lines 68-69 to add YOUR credentials:
```sql
ALTER DATABASE postgres SET app.settings.supabase_url = 'https://YOUR-PROJECT.supabase.co';
ALTER DATABASE postgres SET app.settings.service_role_key = 'YOUR-SERVICE-ROLE-KEY';
```

Get these from: Settings → API

---

### 2️⃣ Deploy Edge Function (1 minute)

**Option A: Supabase Dashboard**
1. Dashboard → Edge Functions → Create function
2. Name: `notify-booking-update`
3. Copy code from `supabase/functions/notify-booking-update/index.ts`
4. Click Deploy

**Option B: CLI**
```bash
supabase functions deploy notify-booking-update
```

---

### 3️⃣ Test It!

1. Build and run your app
2. Create a booking
3. Go to NotificationsActivity (you'll need to add a button to access it)
4. See your notification! 🎉

---

## 📱 Add Notification Bell to Your App

Add this to any activity's layout (e.g., TenantHomeActivity):

```xml
<ImageButton
    android:id="@+id/notificationButton"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:src="@drawable/ic_notifications"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="Notifications" />
```

Then in the activity:

```java
ImageButton notificationButton = findViewById(R.id.notificationButton);
notificationButton.setOnClickListener(v -> {
    startActivity(new Intent(this, NotificationsActivity.class));
});
```

---

## 🔍 Verify Everything Works

Run this in Supabase SQL Editor:

```sql
-- Check if table exists
SELECT * FROM notifications LIMIT 5;

-- Check triggers
SELECT tgname FROM pg_trigger WHERE tgname LIKE '%notify%';

-- Test manual notification
INSERT INTO notifications (user_id, title, message, type)
VALUES (
    (SELECT id FROM auth.users LIMIT 1),
    'Test Notification',
    'This is a test',
    'general'
);
```

---

## 📂 Files You Need

### SQL Scripts (Run in Supabase):
- `supabase/migrations/setup_notifications_complete.sql` - ONE script that does everything

### Edge Function (Deploy to Supabase):
- `supabase/functions/notify-booking-update/index.ts` - Auto-notification function

### Android (Already in your project):
- ✅ SupabaseClient.java - Updated with notification methods
- ✅ NotificationsActivity.java - Notification center
- ✅ NotificationAdapter.java - List adapter
- ✅ All layouts created
- ✅ AndroidManifest.xml updated

---

## 🐛 Troubleshooting

### "No notifications showing"
1. Check SQL: `SELECT * FROM notifications;`
2. Check Edge Function logs in Dashboard
3. Verify triggers: `SELECT * FROM pg_trigger WHERE tgname LIKE '%notify%';`

### "Edge Function not being called"
1. Verify database settings:
   ```sql
   SHOW app.settings.supabase_url;
   SHOW app.settings.service_role_key;
   ```
2. Check if pg_net is enabled: `CREATE EXTENSION IF NOT EXISTS pg_net;`

### "Can't see notifications in app"
1. Check if user is logged in
2. Verify RLS policies: Dashboard → Database → notifications → Policies
3. Check app logs for errors

---

## 🎨 Customize Notifications

### Change Colors
Edit `app/src/main/res/values/colors.xml`:
```xml
<color name="unread_notification_bg">#FFF0F8FF</color>
```

### Add More Types
1. Update SQL constraint (add your type to the list)
2. Update `NotificationAdapter.getTypeLabel()` method
3. Update `NotificationsActivity.handleNotificationClick()` navigation

---

## 📚 Documentation

Detailed guides available:
- `NOTIFICATIONS_IMPLEMENTATION_GUIDE.md` - Complete documentation
- `FCM_IMPLEMENTATION_PLAN.md` - Original FCM plan (not needed now)

---

## ✨ What You Get

✅ **Automatic Notifications** - Created on booking status changes  
✅ **Notification Center** - Full UI to view/manage notifications  
✅ **Smart Navigation** - Tap notification → go to relevant screen  
✅ **Unread Tracking** - Visual distinction for unread items  
✅ **Time Formatting** - "2 hours ago", "3 days ago"  
✅ **Type Badges** - 🏠 Booking, 💬 Message, ⭐ Review  
✅ **Delete Function** - Remove unwanted notifications  
✅ **Secure** - Row Level Security enabled  

---

## 🎯 Next Steps

1. ✅ Run the SQL script
2. ✅ Deploy Edge Function  
3. ✅ Test with a booking
4. 🔲 Add notification bell to your UI
5. 🔲 Customize colors/styles if needed
6. 🔲 Add more notification triggers (reviews, messages)

---

**Need help?** Check `NOTIFICATIONS_IMPLEMENTATION_GUIDE.md` for detailed instructions!
