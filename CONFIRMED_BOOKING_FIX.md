# Confirmed Booking Registration - Issues & Fixes

## Summary
Confirmed bookings were not showing for tenants. Root cause analysis identified **3 critical issues** that have been partially fixed.

---

## 🔴 Issues Found

### Issue #1: Database Field Name Mismatch ✅ FIXED
**Severity:** CRITICAL

**Problem:**
- Database column names: `start_date`, `end_date`, `listing_id`
- Java code was looking for: `move_in_date`, `move_out_date`, `boarding_house_id`
- API would return data with the wrong field names, causing parsing to fail

**File:** `app/src/main/java/com/roominate/ui/fragments/MyBookingsFragment.java`
**Line:** ~230-245

**Fix Applied:**
```java
// Changed from:
String moveInDateStr = bookingObj.optString("move_in_date", "");

// To:
String startDateStr = bookingObj.optString("start_date", "");  // Matches DB
booking.setMoveInDate(startDate);  // Store in model's existing field
```

The model class (`Booking.java`) uses `moveInDate`/`moveOutDate` internally, but now correctly parses from database columns `start_date`/`end_date`.

---

### Issue #2: Missing Authentication Token ⚠️ NEEDS VERIFICATION

**Severity:** HIGH

**Problem:**
- RLS policy requires valid JWT token: `auth.uid()` must match `tenant_id`
- If `access_token` is not saved in SharedPreferences after login → query fails
- If `access_token` is expired → query fails
- If fallback uses anon key as bearer → RLS blocks query

**File:** `app/src/main/java/com/roominate/services/SupabaseClient.java`
**Line:** 1241-1300

**How to Verify:**
1. Check Android logcat for messages from `getTenantBookings`
2. Look for token preview and tenant UUID
3. If token is NULL → login didn't save it properly

**What We Added:**
Added comprehensive debug logging:
```java
Log.d(TAG, "getTenantBookings - Tenant ID from prefs: " + tenantId);
Log.d(TAG, "getTenantBookings - Access Token exists: " + (accessToken != null && !accessToken.isEmpty()));
Log.d(TAG, "getTenantBookings - Found " + arr.length() + " bookings");
```

---

### Issue #3: Possible RLS/Policy Enforcement 🔍 NEEDS INVESTIGATION

**Severity:** HIGH

**Problem:**
- RLS policy: `CREATE POLICY "Tenants view own bookings" ... USING (auth.uid() = tenant_id);`
- If RLS is working but no bookings exist in confirmed status → empty result
- If JWT token payload has wrong user ID → RLS denies access
- If booking data has wrong `tenant_id` → won't match

**Database Query:**
```sql
-- In Supabase Dashboard, run this to check if confirmed bookings exist:
SELECT id, tenant_id, status, start_date, end_date 
FROM public.bookings 
WHERE status = 'confirmed' 
LIMIT 10;
```

---

## ✅ Fixes Applied

### Fix 1: Updated Field Name Parsing
**File:** `MyBookingsFragment.java`

**What Changed:**
- Parsing now looks for `start_date` and `end_date` (matching actual DB columns)
- No longer looks for non-existent `move_in_date` and `move_out_date`
- Data correctly flows from DB → API response → Java parser → Model

### Fix 2: Added Debug Logging
**File:** `SupabaseClient.java` in `getTenantBookings()` method

**What to Look For in Logcat:**
```
D/SupabaseClient: getTenantBookings - Tenant ID from prefs: [YOUR-UUID]
D/SupabaseClient: getTenantBookings - Access Token exists: true
D/SupabaseClient: getTenantBookings - Token preview: eyJhbGciOiJIUzI1NiIsInR5cCI...
D/SupabaseClient: getTenantBookings - Request URL: https://...
D/SupabaseClient: getTenantBookings - Response code: 200
D/SupabaseClient: getTenantBookings - Found 3 bookings
```

**If you see instead:**
```
D/SupabaseClient: getTenantBookings - Access Token exists: false
```
→ **Token was never saved after login**

```
D/SupabaseClient: getTenantBookings - Response code: 401
```
→ **Token is invalid or expired**

```
D/SupabaseClient: getTenantBookings - Found 0 bookings
```
→ **Either no confirmed bookings exist, or RLS is blocking the query**

---

## 🔧 Next Steps - What YOU Should Check

### Step 1: Rebuild and Test
```powershell
# Terminal in workspace root
gradle clean
gradle build
```

Then run the app and check **Logcat** for the debug messages.

### Step 2: If Bookings Still Not Showing
Check these logs first:

**If Token is NULL:**
- Go to `SupabaseClient.signInWithEmail()` or login success handler
- Ensure after successful login, you do:
```java
prefs.edit()
    .putString("access_token", response.optString("access_token"))
    .putString("user_id", userId)
    .apply();
```

**If Response code is 401/403:**
- Token is invalid/expired
- Check if token refresh logic exists and is working

**If Response code is 200 but Found 0 bookings:**
- Check Supabase Dashboard → **bookings** table
- Verify at least one booking exists with `status = 'confirmed'`
- Verify the `tenant_id` in the booking matches your logged-in user UUID

### Step 3: Test with Supabase Dashboard
In Supabase Dashboard, go to **SQL Editor** and run:
```sql
SELECT id, tenant_id, listing_id, status, start_date, end_date 
FROM public.bookings 
WHERE status = 'confirmed'
ORDER BY created_at DESC
LIMIT 5;
```

**Expected:** At least 1-2 rows with confirmed bookings

**If empty:** No bookings exist in the database. You may need to:
1. Owner confirms a pending booking (owner view implementation needed)
2. Or manually update booking status in Supabase UI for testing

### Step 4: Manual Test with Postman
**To verify the API works:**

```
GET https://[your-supabase-url]/rest/v1/bookings?tenant_id=eq.[YOUR-TENANT-UUID]&select=*,boarding_houses(id,title,address)

Headers:
- apikey: [your-anon-key]
- Authorization: Bearer [your-access-token]
- Content-Type: application/json
```

**Expected:** Array of bookings with all fields (including `start_date`, `end_date`, nested `boarding_houses`)

---

## 📋 Checklist Before Declaring Fixed

- [ ] App rebuilt and runs without crashes
- [ ] Logcat shows valid tenant ID and token
- [ ] Logcat shows "Found X bookings" (X > 0)
- [ ] MyBookingsFragment displays bookings in RecyclerView
- [ ] "Confirmed" filter shows confirmed bookings only
- [ ] Booking cards show move-in date, property name, address, amount, status badge
- [ ] Property images/thumbnails load or show placeholder
- [ ] Pull-to-refresh updates the list
- [ ] No "move_in_date" null pointer exceptions in logcat

---

## 🐛 Known Remaining Issues

1. **Owner Confirmation Flow** - Owners don't have UI to confirm/reject pending bookings yet
2. **Thumbnail Images** - Property images may not load if URLs are incorrect
3. **Empty State** - Should show different messages based on filter selection
4. **Date Formatting** - UI should format dates nicely (e.g., "Jan 15, 2025")

---

## Files Modified

1. ✅ `MyBookingsFragment.java` - Fixed field name parsing
2. ✅ `SupabaseClient.java` - Added debug logging
3. 📄 `BOOKING_DEBUG_GUIDE.md` - Comprehensive diagnostic reference (new file)

---

## Quick Summary

**Root Cause:** Java code was parsing fields that don't exist in the database schema.

**Solution:** Updated parsing to use actual database column names (`start_date`, `end_date`).

**Verification:** Check Android logcat for debug output to confirm token, tenant ID, and booking count.

**Next Action:** Rebuild app, run, and check logcat. If bookings still don't show, verify:
1. Confirmed bookings exist in database
2. Access token is being saved after login
3. Token is not expired
