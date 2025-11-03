# User Data Verification Checklist

## Overview
This checklist ensures that after a successful login, the correct user data is pulled from Supabase and cached properly in the app.

## Step 1: Verify Login Response ✅
**What happens:** User enters email/password → `LoginActivity.performLogin()` calls `SupabaseClient.signIn()`

**What to verify:**
- Access token is returned
- User metadata includes:
  - `role` (tenant/owner/admin)
  - `first_name`
  - `last_name`
  - `email`
  - `phone_verified`
  - `email_verified`
- Logcat shows: `"signIn success: ..."`

**Code location:** `LoginActivity.java` lines 146-165

---

## Step 2: Cache Auth Data ✅
**What happens:** Response is parsed and stored in SharedPreferences

**What to verify in SharedPreferences:**
```
user_data → Full user JSON object
user_id → UUID (e.g., 27a478bd-3064-4555-87ee-f36899e691f6)
user_email → sepokonayuma@gmail.com
access_token → JWT token
refresh_token → Refresh token
token_expires_at → Unix timestamp
is_logged_in → true
user_role → From user_metadata (tenant/owner/admin)
```

**Verification steps:**
1. Open Android Studio's Device File Explorer
2. Navigate: `data/data/com.roominate/shared_prefs/roominate_prefs.xml`
3. Check all fields above are present

**Code location:** `LoginActivity.java` lines 164-185

---

## Step 3: Query Profile Table ✅
**What happens:** `LoginActivity` calls `SupabaseClient.getUserProfile()` to fetch from `public.users` table

**What to verify:**
- Query is executed: `GET /rest/v1/users?id=eq.{user_id}`
- Response includes:
  ```json
  {
    "id": "27a478bd-3064-4555-87ee-f36899e691f6",
    "role": "tenant",
    "first_name": "Sepo",
    "last_name": "Konayuma",
    "phone": "0775682528",
    "avatar_url": null,
    "bio": null,
    "created_at": "2025-11-03T08:59:50.358848Z",
    "updated_at": "2025-11-03T08:59:50.358848Z"
  }
  ```

**Logcat verification:**
- Look for: `"Fetching profile for user ID: 27a478bd-..."`
- Look for: `"User profile found: {...}"`
- Look for: `"Final role determined: tenant"`

**Code location:** 
- `SupabaseClient.java` lines 396-454 (updated getUserProfile)
- `LoginActivity.java` lines 189-234 (calls getUserProfile)

---

## Step 4: Handle Role Determination ✅
**Priority order for role:**
1. From `auth.users` → `user_metadata.role` ✅ (PREFERRED - most reliable)
2. From `public.users` table → `role` field ✅ (FALLBACK)
3. Default to "tenant" ✅ (FINAL FALLBACK)

**Code logic:**
```java
// Prefer role from user_metadata (most reliable)
String role = finalRoleFromAuth != null ? finalRoleFromAuth : profileResponse.optString("role", "tenant");
```

**Code location:** `LoginActivity.java` lines 196-209

---

## Step 5: Store Final Role ✅
**What happens:** Determined role is stored in SharedPreferences

**Verification:**
```
user_role → "tenant" | "owner" | "admin"
owner_using_tenant_view → false (unless owner explicitly switched to tenant view)
```

**Code location:** `LoginActivity.java` line 207

---

## Step 6: Route to Correct Dashboard ✅
**What happens:** Based on role, user is routed to correct dashboard

**Routes:**
- `role == "tenant"` → `TenantDashboardActivity`
- `role == "owner"` → `OwnerDashboardActivity`
- `role == "admin"` → `AdminDashboardActivity`
- Default → `TenantDashboardActivity`

**Special case:** 
- If owner has set `owner_using_tenant_view = true`, they see tenant dashboard

**Code location:** `LoginActivity.java` lines 239-257

---

## Debugging Commands

### Check SharedPreferences
Open Terminal in Android Studio:
```bash
adb shell am dump-heap /data/local/tmp/heap.bin com.roominate
adb pull /data/local/tmp/heap.bin
```

Or manually via Device File Explorer:
```
data/data/com.roominate/shared_prefs/roominate_prefs.xml
```

### Check Logcat for Issues
```bash
adb logcat | grep LoginActivity
adb logcat | grep SupabaseClient
adb logcat | grep "User profile"
```

### Verify Database Data
Check Supabase Console:
1. Auth users → Should have your email with role in user_metadata
2. Database → public.users table → Should have matching record

---

## Common Issues & Fixes

### Issue 1: "No users found for user ID"
**Cause:** User exists in auth but not in public.users table
**Fix:** Manually insert profile:
```sql
INSERT INTO public.users (id, role, first_name, last_name, phone)
VALUES ('27a478bd-3064-4555-87ee-f36899e691f6', 'tenant', 'Sepo', 'Konayuma', '0775682528');
```

### Issue 2: Role is wrong (showing tenant instead of owner)
**Cause:** Role in user_metadata doesn't match public.users table
**Fix:** Check which one is being used:
- If from auth: Update user_metadata in Supabase Auth
- If from profile: Update public.users table

### Issue 3: User can't login after password change
**Cause:** Stored password hash mismatch
**Fix:** Verify you're using the correct password set during signup

### Issue 4: Wrong dashboard appears
**Cause:** Role is null or contains wrong value
**Fix:** Check all three sources (user_metadata, public.users, SharedPreferences)

---

## Post-Login Data Flow

```
User enters credentials
    ↓
LoginActivity.performLogin()
    ↓
SupabaseClient.signIn() → Auth API
    ↓
✅ Parse response → Extract role from user_metadata
✅ Cache auth data → SharedPreferences
    ↓
SupabaseClient.getUserProfile() → Query public.users
    ↓
✅ Merge profile data
✅ Determine final role
✅ Cache role → SharedPreferences
    ↓
redirectToDashboard(role)
    ↓
Navigate to appropriate dashboard
```

---

## Testing Checklist

- [ ] Login with tenant account
  - [ ] Verify user_role = "tenant" in SharedPreferences
  - [ ] Verify TenantDashboardActivity opens
  - [ ] Check logcat for role determination steps

- [ ] Login with owner account
  - [ ] Verify user_role = "owner" in SharedPreferences
  - [ ] Verify OwnerDashboardActivity opens
  - [ ] Check logcat for role determination steps

- [ ] Verify profile data is complete
  - [ ] Name, email, phone match Supabase
  - [ ] Avatar URL is stored if present
  - [ ] Created/updated timestamps are correct

- [ ] Test fallback scenarios
  - [ ] Remove role from user_metadata → Should use public.users role
  - [ ] Remove profile from public.users → Should default to tenant
  - [ ] Verify error messages in logcat for debugging

---

## Modified Files

### `SupabaseClient.java`
- **Method:** `getUserProfile()` (lines 396-454)
- **Change:** Now queries `public.users` table instead of returning mock data
- **Added:** `getCurrentUserId()` helper method (lines 48-59)

### `LoginActivity.java`
- **No changes** - works correctly with updated SupabaseClient

---

## Session Persistence

After login, the app caches:
- User ID
- User email
- User role
- Access token
- Refresh token
- Token expiration time

This allows:
- Users to stay logged in after app restart
- Token refresh before expiration
- Quick access to user info without re-querying

Check `SharedPreferencesManager` or similar for session restoration logic.

---

## Next Steps

1. **Rebuild and test** the signup/login flow with the updated code
2. **Monitor logcat** for role determination and profile queries
3. **Verify SharedPreferences** contains all expected data
4. **Test all three roles** (tenant, owner, admin) if you have test accounts
5. **Check dashboard navigation** routes to the correct activity

