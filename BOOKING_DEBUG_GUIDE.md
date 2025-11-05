# Confirmed Booking Registration Issue - Diagnostic Guide

## Problem Summary
Confirmed bookings are not showing up for tenants in `MyBookingsFragment`. The issue is likely in one of these areas:

---

## 🔴 CRITICAL ISSUE #1: Database Field Mismatch

### The Problem
**Database Schema** (in `supabase_schema.sql`):
```sql
CREATE TABLE public.bookings (
  id uuid PRIMARY KEY,
  listing_id uuid NOT NULL,      -- ← Uses listing_id
  tenant_id uuid NOT NULL,
  start_date date NOT NULL,       -- ← Uses start_date
  end_date date,                  -- ← Uses end_date
  total_amount numeric(10,2),
  status text NOT NULL DEFAULT 'pending',
  ...
);
```

**Java Query Code** (in `SupabaseClient.java:1241`):
```java
// ❌ WRONG: Queries using field names that don't exist in DB!
String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?tenant_id=eq." + tenantId 
  + "&select=*,boarding_houses(id,title,address)&order=created_at.desc";
```

**Java Parsing Code** (in `MyBookingsFragment.java:227`):
```java
// ❌ Trying to parse fields that aren't in the response:
booking.setMoveInDate(...)   // Should be: start_date
booking.setMoveOutDate(...)  // Should be: end_date
```

### Root Cause
The database table uses `listing_id`, `start_date`, and `end_date`, but the Java code is built expecting `boarding_house_id`, `move_in_date`, and `move_out_date`. The query succeeds but returns empty or mismatched data.

---

## 🔴 CRITICAL ISSUE #2: RLS Policy Blocking Query

### The Problem
The RLS policy requires a valid JWT token with matching `auth.uid()`:

```sql
CREATE POLICY "Tenants view own bookings" ON public.bookings 
  FOR SELECT USING (auth.uid() = tenant_id);
```

**What This Means:**
- The JWT token's `uid` claim must exactly match the `tenant_id` in the bookings row
- If the token is invalid, expired, or has wrong user ID → query returns 0 rows
- If using the anon key without a real token → RLS denies the query

### Current Code Flow (in `SupabaseClient.java:61`):
```java
public static Request.Builder addAuthHeaders(Request.Builder builder) {
    builder.addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY);
    
    if (appContext != null) {
        String accessToken = prefs.getString("access_token", null);
        if (accessToken != null && !accessToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + accessToken);  // ← Uses stored token
            return builder;
        }
    }
    
    // Fallback: sends anon key as bearer (will fail RLS)
    builder.addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY);
    return builder;
}
```

**Potential Problems:**
1. `access_token` in SharedPreferences is expired or invalid
2. `access_token` was never stored after login
3. `access_token` has wrong user ID encoded in JWT
4. Network request is using anon-key-as-bearer fallback (which won't pass RLS)

---

## 🔴 CRITICAL ISSUE #3: Foreign Key Reference Mismatch

### The Problem
**Database foreign key:**
```sql
listing_id uuid NOT NULL REFERENCES public.boarding_houses(id)
```

But the JOIN in code might be wrong:
```java
// In getTenantBookings():
String url = "...&select=*,boarding_houses(id,title,address)&order=created_at.desc";
```

**Problem:** The query doesn't specify HOW to join `boarding_houses`. PostgREST needs to know the relationship!

**Correct Query Should Be:**
```
GET /rest/v1/bookings?
  tenant_id=eq.{userId}
  &select=*,boarding_houses(id,title,address)
  &order=created_at.desc
```

But PostgREST will only auto-join if there's an explicit foreign key. If the FK points to `boarding_houses.id` under the column name `listing_id`, the join should still work, BUT if any of these don't match, PostgREST will return null for the joined object.

---

## Diagnostic Steps

### Step 1: Verify Database Schema
**Expected columns in `bookings` table:**
```sql
SELECT column_name, data_type FROM information_schema.columns 
WHERE table_name = 'bookings' 
ORDER BY column_name;
```

**Expected result:**
```
created_at       | timestamp
end_date         | date
id               | uuid
listing_id       | uuid
start_date       | date
status           | text
tenant_id        | uuid
total_amount     | numeric
updated_at       | timestamp
```

**If you see `move_in_date`, `move_out_date`, or `boarding_house_id` → Schema mismatch!**

### Step 2: Test Token Validity
In the Android app, add logging to verify the token:

**File:** `SupabaseClient.java`, in `getTenantBookings()` method, add this right after building the URL:

```java
String accessToken = prefs.getString("access_token", null);
Log.d(TAG, "getTenantBookings - Access Token: " + (accessToken != null ? accessToken.substring(0, 50) + "..." : "NULL"));
Log.d(TAG, "getTenantBookings - Tenant ID: " + tenantId);
Log.d(TAG, "getTenantBookings - URL: " + url);
```

**Expected output in logcat:**
- Token should be 50+ characters (JWT format: header.payload.signature)
- Tenant ID should be a valid UUID (8-4-4-4-12 format)
- URL should include the tenant's UUID

**If Token is NULL → Token was never saved after login!**

### Step 3: Test Query Directly
Use Postman or cURL to test the booking query:

```bash
curl -X GET \
  'https://YOUR_SUPABASE_URL/rest/v1/bookings?tenant_id=eq.UUID&select=*,boarding_houses(id,title,address)' \
  -H 'apikey: YOUR_ANON_KEY' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**Expected response:**
```json
[
  {
    "id": "...",
    "listing_id": "...",
    "tenant_id": "...",
    "start_date": "2025-01-15",
    "end_date": "2025-07-15",
    "status": "confirmed",
    "total_amount": 9000,
    "boarding_houses": {
      "id": "...",
      "title": "Property Name",
      "address": "Property Address"
    }
  }
]
```

**If you get:**
- `[]` (empty array) → RLS is blocking it or no confirmed bookings exist
- `null` for `boarding_houses` → FK join failed, check column name matches
- Error 401/403 → Token issue
- Error 400 → Query syntax issue

### Step 4: Check RLS Policies
In Supabase Dashboard:
1. Go to **SQL Editor**
2. Run:
```sql
SELECT * FROM pg_policies WHERE tablename = 'bookings';
```

**Expected policies:**
```
Tenants insert bookings   | INSERT
Tenants view own bookings | SELECT
Tenants update own bookings | UPDATE
Owners view listing bookings | SELECT
```

---

## 🔧 FIXES Required

### Fix #1: Update Field Names in `MyBookingsFragment.java`

**Change this (lines ~227-245):**
```java
// WRONG - field names don't match DB
booking.setMoveInDate(...)   
booking.setMoveOutDate(...)
```

**To this:**
```java
// CORRECT - match actual database columns
String startDateStr = bookingObj.optString("start_date", "");  // Changed
if (!startDateStr.isEmpty()) {
    try {
        Date startDate = isoFormat.parse(startDateStr);
        booking.setMoveInDate(startDate);  // Store in existing model field
    } catch (Exception e) {
        Log.w(TAG, "Could not parse start_date: " + startDateStr);
    }
}

String endDateStr = bookingObj.optString("end_date", "");  // Changed
if (!endDateStr.isEmpty()) {
    try {
        Date endDate = isoFormat.parse(endDateStr);
        booking.setMoveOutDate(endDate);  // Store in existing model field
    } catch (Exception e) {
        Log.w(TAG, "Could not parse end_date: " + endDateStr);
    }
}
```

### Fix #2: Add Debugging to `SupabaseClient.getTenantBookings()`

Add logging before the network call:

```java
public void getTenantBookings(String status, ApiCallback callback) {
    try {
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("roominate_prefs", android.content.Context.MODE_PRIVATE);
        String tenantId = prefs.getString("user_id", null);
        String accessToken = prefs.getString("access_token", null);

        // ADD THESE LOGS:
        Log.d(TAG, "getTenantBookings - Tenant ID from prefs: " + tenantId);
        Log.d(TAG, "getTenantBookings - Access Token exists: " + (accessToken != null && !accessToken.isEmpty()));
        if (accessToken != null && accessToken.length() > 50) {
            Log.d(TAG, "getTenantBookings - Token sample: " + accessToken.substring(0, 50) + "...");
        }

        // Build URL...
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/bookings?tenant_id=eq." + tenantId + "&select=*,boarding_houses(id,title,address)&order=created_at.desc";
        Log.d(TAG, "getTenantBookings - Full URL: " + url);
        
        // ... rest of method
    }
}
```

### Fix #3: Verify Login Saves Token

**File:** Check login endpoint (likely in `SupabaseClient.signInWithEmail()`)

Ensure after successful login, you save the access token:

```java
// After successful login response:
String accessToken = loginResponse.optString("access_token");
prefs.edit()
    .putString("access_token", accessToken)
    .putString("user_id", userId)  // MUST save user_id too
    .apply();

Log.d(TAG, "Login successful - saved token and user_id");
```

### Fix #4: Test with Service Role (Bypass RLS)

As a temporary test to isolate the RLS issue:

**In Supabase Dashboard → SQL:**
```sql
-- This should return results (uses service role, bypasses RLS)
SELECT id, tenant_id, start_date, end_date, status, listing_id 
FROM public.bookings 
WHERE tenant_id = 'PUT_YOUR_TENANT_UUID_HERE' 
AND status = 'confirmed';
```

If this returns bookings but your app doesn't → **RLS/Token Issue**
If this returns nothing → **No confirmed bookings exist or data corruption**

---

## Test Checklist

- [ ] Database schema checked - columns are `start_date`, `end_date`, `listing_id`
- [ ] Logcat shows valid access token being sent
- [ ] Logcat shows correct tenant UUID
- [ ] Direct API test (Postman/cURL) returns bookings
- [ ] RLS policies are in place and configured
- [ ] Login flow saves `access_token` to SharedPreferences
- [ ] Field name parsing in `MyBookingsFragment` matches database columns
- [ ] Rebuilt app and tested again

---

## Expected Result After Fixes

When a tenant navigates to "My Bookings" tab:
1. `MyBookingsFragment.onViewCreated()` calls `loadBookings("all")`
2. `SupabaseClient.getTenantBookings()` sends request with valid JWT token
3. RLS policy allows query (token's `auth.uid()` matches `tenant_id`)
4. Database returns bookings with `start_date`, `end_date` fields
5. `MyBookingsFragment.parseBooking()` correctly parses these fields
6. RecyclerView displays bookings with move-in date, address, amount, status badge ✅
