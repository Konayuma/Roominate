# Testing & Verification Guide

## Quick Verification After Login

### Step 1: Check Logcat Output
Run your app and login. In Android Studio's Logcat, filter for:
```
LoginActivity
SupabaseClient
```

**Expected log sequence:**
```
D/LoginActivity: signIn success: {"access_token":"...", "user":{"id":"27a478bd-...", "email":"sepokonayuma@gmail.com", "user_metadata":{"role":"tenant", "first_name":"Sepo", ...}}}
D/LoginActivity: Role from user_metadata: tenant
D/SupabaseClient: Fetching profile for user ID: 27a478bd-3064-4555-87ee-f36899e691f6
D/SupabaseClient: getUserProfile response: [{"id":"27a478bd-...", "role":"tenant", "first_name":"Sepo", ...}]
D/SupabaseClient: User profile found: {"id":"27a478bd-...", "role":"tenant", ...}
D/LoginActivity: Final role determined: tenant
D/LoginActivity: Using fallback role: tenant
I/LoginActivity: redirectToDashboard called with userType: tenant
```

### Step 2: Verify SharedPreferences
1. Open Android Studio → Device File Explorer
2. Navigate: `data/data/com.roominate/shared_prefs/roominate_prefs.xml`
3. Verify these keys exist:
   - `user_id` = "27a478bd-3064-4555-87ee-f36899e691f6"
   - `user_email` = "sepokonayuma@gmail.com"
   - `user_role` = "tenant"
   - `is_logged_in` = "true"
   - `access_token` = "ey..." (JWT token)
   - `user_data` = Full JSON object

### Step 3: Verify Correct Dashboard Appears
- **Tenant login** → See `TenantDashboardActivity`
- **Owner login** → See `OwnerDashboardActivity`
- **Admin login** → See `AdminDashboardActivity`

### Step 4: Check Supabase Console
1. Auth → Your Account → Check user_metadata has `role` field
2. SQL Editor → Run:
   ```sql
   SELECT id, email, user_metadata->>'role' AS role FROM auth.users WHERE email = 'sepokonayuma@gmail.com';
   SELECT * FROM public.users WHERE id = '27a478bd-3064-4555-87ee-f36899e691f6';
   ```
3. Verify both have consistent role

---

## Data Validation Matrix

| Data Point | Source | Stored In | Logcat Indicator |
|---|---|---|---|
| User ID | auth.users | SharedPreferences (user_id) | "Retrieved user ID from SharedPreferences: 27a478bd..." |
| Email | auth.users | SharedPreferences (user_email) | "signIn success: ...email...:" |
| Role | user_metadata | SharedPreferences (user_role) | "Role from user_metadata: tenant" |
| First Name | user_metadata | SharedPreferences (user_data) | "First Sign In: ..." (from auth) |
| Phone | user_metadata | SharedPreferences (user_data) | N/A |
| Created Date | public.users | Not cached | "User profile found: ...created_at..." |
| Avatar URL | public.users | Not cached | "User profile found: ...avatar_url..." |

---

## Test Scenarios

### Scenario 1: Fresh Login (Tenant)
**Steps:**
1. App is logged out
2. Navigate to LoginActivity
3. Enter: `sepokonayuma@gmail.com` / `@Admin1111`
4. Tap Login

**Verify:**
- [ ] No errors in logcat
- [ ] TenantDashboardActivity appears
- [ ] Tenant data loads correctly
- [ ] SharedPreferences has user_id and user_role=tenant
- [ ] Logcat shows "Role from user_metadata: tenant"

### Scenario 2: Session Persistence
**Steps:**
1. Login successfully as above
2. Force close app (kill process)
3. Reopen app

**Verify:**
- [ ] Dashboard appears WITHOUT login screen
- [ ] User is automatically logged in
- [ ] SharedPreferences still has is_logged_in=true
- [ ] Token is still valid (not expired)

### Scenario 3: Role Mismatch Detection
**Steps:**
1. In Supabase Console, go to Database
2. Update public.users: `UPDATE public.users SET role='owner' WHERE id='27a478bd-...'`
3. Clear app cache and login again

**Verify:**
- [ ] Auth says role=tenant (from user_metadata)
- [ ] App prioritizes auth role (tenant)
- [ ] Final user_role in SharedPreferences = "tenant"
- [ ] Correct dashboard appears (TenantDashboard, not OwnerDashboard)

### Scenario 4: Missing Profile
**Steps:**
1. Delete public.users record manually in Supabase
2. Clear app cache and login

**Verify:**
- [ ] getUserProfile returns empty array
- [ ] Logcat shows "No profile found for user ID"
- [ ] App creates default profile response
- [ ] Role still determined from auth (tenant)
- [ ] Login still succeeds with correct dashboard

---

## Troubleshooting

### Problem: Wrong dashboard appears
**Debugging:**
```bash
# Check what role is stored
adb shell "grep 'user_role' /data/data/com.roominate/shared_prefs/roominate_prefs.xml"

# Check logcat for role determination
adb logcat | grep "Final role determined"

# Check what auth returned
adb logcat | grep "signIn success"
```

**Solution:**
1. Check user_metadata in Supabase Auth console
2. Verify public.users table has correct role
3. Priority: user_metadata > public.users > default (tenant)

### Problem: User profile not loading
**Debugging:**
```bash
# Check for network errors
adb logcat | grep "getUserProfile"

# Check if user_id is being retrieved
adb logcat | grep "Retrieved user ID from SharedPreferences"

# Check if request is being made
adb logcat | grep "Fetching profile for user ID"
```

**Solution:**
1. Verify user_id is stored in SharedPreferences after login
2. Verify public.users table has a record for this user_id
3. Check API permissions (RLS policies)

### Problem: App keeps logging out
**Debugging:**
```bash
# Check token expiration
adb shell "grep 'token_expires_at' /data/data/com.roominate/shared_prefs/roominate_prefs.xml"

# Check is_logged_in flag
adb shell "grep 'is_logged_in' /data/data/com.roominate/shared_prefs/roominate_prefs.xml"
```

**Solution:**
1. Verify token hasn't expired
2. Implement token refresh before expiration
3. Check if logout is being called inadvertently

---

## Database Queries for Verification

### Query 1: Check User Auth Record
```sql
SELECT 
  id,
  email,
  email_confirmed_at,
  created_at,
  user_metadata->>'role' as role,
  user_metadata->>'first_name' as first_name,
  user_metadata->>'phone' as phone
FROM auth.users 
WHERE email = 'sepokonayuma@gmail.com';
```

**Expected output:**
```
id                                   | email                  | email_confirmed_at        | role   | first_name | phone
27a478bd-3064-4555-87ee-f36899e691f6 | sepokonayuma@gmail.com | 2025-11-03T08:59:50.382162Z | tenant | Sepo       | 0775682528
```

### Query 2: Check User Profile Record
```sql
SELECT 
  id,
  role,
  first_name,
  last_name,
  phone,
  avatar_url,
  created_at,
  updated_at
FROM public.users 
WHERE id = '27a478bd-3064-4555-87ee-f36899e691f6';
```

**Expected output:**
```
id                                   | role   | first_name | last_name | phone      | avatar_url | created_at
27a478bd-3064-4555-87ee-f36899e691f6 | tenant | Sepo       | Konayuma  | 0775682528 | NULL       | 2025-11-03T08:59:50.358848Z
```

### Query 3: Check for Role Mismatches
```sql
SELECT 
  a.id,
  a.email,
  a.user_metadata->>'role' as auth_role,
  p.role as profile_role,
  CASE 
    WHEN a.user_metadata->>'role' = p.role THEN 'MATCH'
    ELSE 'MISMATCH'
  END as status
FROM auth.users a
LEFT JOIN public.users p ON a.id = p.id
WHERE a.email = 'sepokonayuma@gmail.com';
```

---

## Network Request Inspection

### To inspect actual API calls:
1. Open Android Studio → View → Tool Windows → Network Inspector
2. Login with your credentials
3. Look for these requests:

**Request 1: Authentication**
```
POST /auth/v1/token?grant_type=password
Body: {"email":"sepokonayuma@gmail.com","password":"@Admin1111"}
Response: {"access_token":"...", "user":{...}}
```

**Request 2: Get Profile**
```
GET /rest/v1/users?id=eq.27a478bd-3064-4555-87ee-f36899e691f6
Headers: Authorization: Bearer {access_token}
Response: [{"id":"...", "role":"tenant", ...}]
```

---

## Performance Checklist

- [ ] Login completes in < 2 seconds
- [ ] No excessive API calls (should be exactly 2: auth + profile)
- [ ] No memory leaks in callbacks
- [ ] Token is refreshed before expiration (not after)
- [ ] Offline mode gracefully falls back to cached data
- [ ] Profile data is cached and not re-fetched on every navigation

---

## Final Checklist Before Deployment

- [ ] All three roles (tenant, owner, admin) can login successfully
- [ ] Correct dashboard appears for each role
- [ ] Role from user_metadata is prioritized correctly
- [ ] Profile data matches auth data
- [ ] SharedPreferences persists across app restarts
- [ ] Error handling works for network failures
- [ ] Logcat shows clear debugging information
- [ ] No hardcoded user IDs or roles in code
- [ ] RLS policies allow authenticated users to read their own profile
- [ ] Token refresh works when token expires

