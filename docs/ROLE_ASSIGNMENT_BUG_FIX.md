# Bug Fix: Wrong Role During Signup

## Issues Found and Fixed

### Issue 1: Wrong Table Name in Profile Insertion
**File:** `SupabaseClient.java` line 675
**Problem:** Code was inserting user profile into `/rest/v1/profiles` table, but the actual table is `public.users`
**Result:** Profile insertion was silently failing, so the profile with role information was never created

**Fix:** Changed endpoint from:
```java
.url(supabaseUrl + "/rest/v1/profiles")
```
To:
```java
.url(supabaseUrl + "/rest/v1/users")
```

### Issue 2: Intent Key Mismatch During Navigation
**File:** `SignUpPasswordActivity.java` line 50
**Problem:** The activity was looking for intent extra key `"userType"`, but previous activity was passing key `"userRole"`
**Result:** `userType` variable was null, causing role to default to wrong value

**Flow:**
- `SignUpEmailVerificationActivity` passes: `intent.putExtra("userRole", userRole)` ✓
- `SignUpPasswordActivity` retrieves: `getIntent().getStringExtra("userType")` ✗ (wrong key!)

**Fix:** Changed key name to match:
```java
userType = getIntent().getStringExtra("userRole");
```

### Issue 3: Added Debug Logging
**File:** `SignUpPasswordActivity.java` line 56
**Added:** `Log.d("SignUpPasswordActivity", "Received user role: " + userType);`
**Purpose:** To help debug similar issues in the future

---

## Complete Flow After Fix

1. **RoleSelectionActivity** → Passes `"userRole"` = "tenant" or "owner"
2. **SignUpBasicInfoActivity** → Passes `"userRole"` correctly
3. **SignUpEmailActivity** → Passes `"userRole"` correctly
4. **SignUpEmailVerificationActivity** → Passes `"userRole"` correctly ✓
5. **SignUpPasswordActivity** → Now receives `"userRole"` correctly ✓
6. **completeSignup()** → Receives correct role ("tenant" or "owner")
   - Creates auth user with role in user_metadata ✓
   - Creates profile in public.users table with role ✓
7. **navigateToDashboard()** → Routes based on correct role ✓

---

## Verification After Fix

When signing up as "tenant", check:

### Logcat
```
D/SignUpPasswordActivity: Received user role: tenant
```

### Supabase Auth Console
```
user_metadata: {
  "role": "tenant",
  "first_name": "...",
  ...
}
```

### Supabase public.users Table
```
id | role   | first_name | ...
---|--------|------------|----
.. | tenant | ...        | ...
```

### App Navigation
- Should route to `TenantDashboardActivity`
- Should NOT route to `OwnerDashboardActivity`

---

## Testing Checklist

- [ ] Sign up as Tenant → Verify TenantDashboardActivity appears
- [ ] Sign up as Owner → Verify OwnerDashboardActivity appears
- [ ] Check Supabase auth user_metadata has correct role
- [ ] Check public.users table has correct role
- [ ] Check logcat shows "Received user role: tenant" or "owner"

